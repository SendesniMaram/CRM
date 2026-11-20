# Rotation CRM — procédure exécutable préparée, non exécutée

État constaté le 17 octobre 2026 : branche `microservices`, HEAD `814a0ef`.
**Aucune commande de bascule ci-dessous n'a été exécutée.** L'autorisation
d'exécution devra couvrir sauvegarde, restauration isolée, CREATE USER/GRANT,
révocation des refresh tokens, ALTER USER, modification du `.env` et redémarrages.
Ne pas lancer les blocs tant que cette autorisation n'est pas donnée.

## Inventaire et périmètre exact

| Service | Base | Préfixe des nouvelles variables | Compte proposé |
|---|---|---|---|
| identity-service | identity_db | IDENTITY | crm_identity |
| employee-service | employee_db | EMPLOYEE | crm_employee |
| department-service | department_db | DEPARTMENT | crm_department |
| customer-service | customer_db | CUSTOMER | crm_customer |
| hr-service | hr_db | HR | crm_hr |
| invoice-service | invoice_db | INVOICE | crm_invoice |
| fees-service | fees_db | FEES | crm_fees |
| payroll-service | payroll_db | PAYROLL | crm_payroll |

Les huit bases de microservices comprennent Identity ; `crm_db`, neuvième base
historique, existe sans table et est incluse dans la sauvegarde. Il n'existe pas
huit bases métier distinctes plus Identity dans cet environnement.

Chaque application utilise actuellement `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME=root`, `SPRING_DATASOURCE_PASSWORD`, `APP_JWT_SECRET`,
`EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` et `SPRING_JPA_HIBERNATE_DDL_AUTO`.
L'override préparé impose également `SPRING_SQL_INIT_MODE=never` et `validate`.
Pour chaque préfixe, il remplace les identifiants par `<PREFIX>_DB_USERNAME` et
`<PREFIX>_DB_PASSWORD` et sélectionne `<PREFIX>_ROTATION_IMAGE`.

Gateway ne valide pas les JWT et n'utilise ni base ni secret JWT : elle relaie
Authorization. Eureka n'a pas de secret à remplacer. Le backend historique est
hors Compose et hors bascule ; s'il est réellement utilisé ailleurs, arrêter la
procédure pour inventorier son lanceur et les autres clients root avant rotation.

MySQL : `crm-mysql-1`, volume nommé **`crm_mysql-data`**, montage `/var/lib/mysql`,
réseau `crm_crm-network`, sous-réseau **172.19.0.0/16**. Les tables sont InnoDB.
Deux comptes root existent : `root@%` et `root@localhost`, plugin
`caching_sha2_password`. Aucun compte `crm_*` constaté. Réinspecter ces éléments
le jour de l'exécution ; toute divergence impose un arrêt et une adaptation.

Identity dépend de MySQL healthy et Eureka ; les services métier dépendent de
MySQL, Identity, Gateway et Eureka. Pour éviter une boucle lors de la maintenance,
les démarrages ci-dessous emploient `--no-deps` **après** vérification explicite
des prérequis. Ne pas lancer un simple `docker compose up` pendant cette opération.

## Conditions avant exécution

- Fenêtre de maintenance annoncée : les sessions seront invalidées. Aucun autre
  opérateur, job SQL, migration ou modification de schéma pendant l'opération.
- Bloquer les accès utilisateurs, y compris les ports directs des applications,
  et tous les autres producteurs d'écritures. Les checksums avant/après dump
  détectent une variation et arrêtent la procédure, mais ne remplacent pas ce gel.
- Autorisation explicite reçue ; Docker, Node et Windows PowerShell disponibles.
- Espace mémoire/disque pour un dump de moins de 64 MiB, limite du helper préparé.
  Au-delà, arrêter et préparer une sauvegarde chiffrée en flux.
- Répertoire de sauvegarde **hors dépôt**, accessible seulement à l'opérateur et
  SYSTEM, sur disque protégé. Le bundle est chiffré/authentifié par DPAPI CurrentUser.
  Sa restauration nécessite ce compte Windows et cette machine : prévoir la
  récupération du profil ou une autre sauvegarde chiffrée hors machine avant
  toute opération irréversible. Ne jamais afficher les objets déchiffrés.
- Aucun mode transcription, `Set-PSDebug`, journalisation des variables, impression
  des réponses de login, ni `docker compose config` sans `--quiet`.
- Vérifier que le général log/les outils de capture externes ne collectent pas les
  entrées secrètes. Aucun secret dans les arguments natifs : le helper utilise
  stdin et un fichier d'options temporaire privé dans le conteneur, supprimé à la fin.

## Initialisation PowerShell — uniquement après autorisation

```powershell
Set-Location C:\Users\ASUS\Documents\CRM
$ErrorActionPreference = 'Stop'
Set-PSDebug -Off
. .\scripts\rotation\Rotation-Helpers.ps1
$admin = Read-Host 'Mot de passe MySQL root actuellement valide' -AsSecureString
$apps = @('identity-service','department-service','employee-service',
          'customer-service','hr-service','invoice-service','fees-service','payroll-service')
$dc = @('compose','--project-name','crm','--env-file','.env',
        '-f','docker-compose.yml','-f','docker-compose.rotation.yml')
$directory = Join-Path $env:USERPROFILE ('Documents\CRM-Backups\rotation-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
[void](New-Item -ItemType Directory -Path $directory)
$operator = [Security.Principal.WindowsIdentity]::GetCurrent().Name
$acl = New-Object Security.AccessControl.DirectorySecurity
$acl.SetAccessRuleProtection($true, $false)
foreach ($identity in @($operator, 'S-1-5-18')) {
    $principal = if ($identity -eq 'S-1-5-18') {
        New-Object Security.Principal.SecurityIdentifier($identity)
    } else { New-Object Security.Principal.NTAccount($identity) }
    $rule = New-Object Security.AccessControl.FileSystemAccessRule(
        $principal, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')
    $acl.AddAccessRule($rule)
}
Set-Acl -LiteralPath $directory -AclObject $acl
$backup = Join-Path $directory 'crm-before-rotation.dpapi'
$pending = Join-Path $directory 'accounts-pending.dpapi'
Test-CrmDemoData -Password $admin
```

## 1. Sauvegarde puis restauration vérifiée — arrêt obligatoire en cas d'échec

```powershell
Backup-CrmDatabases -BackupPath $backup -Password $admin -Authorized
Test-CrmBackup -BackupPath $backup -Authorized
$receipt = Get-Content -LiteralPath ($backup + '.verified.json') -Raw | ConvertFrom-Json
if ($receipt.BackupSha256 -ne (Get-FileHash -LiteralPath $backup).Hash) {
    throw 'Sauvegarde non verifiee : aucune rotation autorisee.'
}
$snapshot = Read-CrmProtectedState $backup
```

Un seul `mysqldump --single-transaction --quick --databases` couvre les neuf bases,
avec routines/événements/triggers et données binaires. Aucun dump SQL en clair
n'est écrit sur le poste. Le bundle DPAPI contient le dump, les checksums complets
des tables, l'ancien `.env` pour secours et les inspections Docker pour retrouver
les images/configurations antérieures. Il contient donc des secrets chiffrés.

La restauration est effectuée dans un conteneur MySQL **distinct**, image identique
à l'actuelle, réseau `none`, aucun port publié, données sur tmpfs. Son compte root
sans mot de passe n'est utilisé que pour ce test sans réseau. Tous les checksums
doivent correspondre ; le conteneur temporaire est supprimé après le test. Le
volume réel n'est jamais monté. Un SHA256 seul n'est pas une preuve de restauration.

Tout échec Docker, dump incomplet, changement des données pendant le dump,
checksum NULL/différent, erreur DPAPI, espace insuffisant ou restauration échouée :
**STOP**, aucune création de compte ni modification du `.env`.

## 2. Préparer les comptes et leurs droits

La portée host ci-dessous correspond au sous-réseau constaté ; elle exclut les
clients hors réseau CRM. Aucun `GRANT ... ON *.*`, GRANT OPTION, rôle, privilège
administratif ou DDL. `USAGE ON *.*` peut apparaître dans SHOW GRANTS : ce n'est
pas un privilège global effectif. Les schémas et tables existants restent intacts.

```powershell
$accountHost = '172.19.0.0/255.255.0.0'
$definitions = @('identity','employee','department','customer','hr','invoice','fees','payroll')
$existing = Invoke-CrmMysql -Password $admin -Sql "SELECT User FROM mysql.user WHERE User LIKE 'crm_%';"
if ($existing.Trim()) { throw 'Comptes existants : examiner les droits et ne pas relancer CREATE USER.' }
$accounts = foreach ($name in $definitions) {
    @{ Prefix=$name.ToUpperInvariant(); User=('crm_' + $name);
       Database=($name + '_db'); Password=(New-CrmSecret 32) }
}
# Sauvegarde de reprise AVANT le premier CREATE USER, sans impression des valeurs.
Save-CrmProtectedState -Path $pending -Value @{Host=$accountHost; Accounts=$accounts} -Authorized
foreach ($account in $accounts) {
    $sql = "CREATE USER '$($account.User)'@'$accountHost' IDENTIFIED WITH caching_sha2_password BY '$($account.Password)'; " +
           "GRANT SELECT,INSERT,UPDATE,DELETE ON ``$($account.Database)``.* TO '$($account.User)'@'$accountHost';"
    [void](Invoke-CrmMysql -Password $admin -Sql $sql -AllowWrite -Authorized)
}
# Vérifier authentification depuis le réseau Docker avec chaque nouveau compte.
foreach ($account in $accounts) {
    $client = "[client]`nuser=$($account.User)`npassword=$($account.Password)`n"
    $result = Invoke-CrmDocker -Arguments @('run','--rm','--pull=never','-i',
        '--network','crm_crm-network','--entrypoint','mysql',$snapshot.MysqlImage,
        '--defaults-extra-file=/dev/stdin','--host=mysql','--batch','--skip-column-names',
        "--database=$($account.Database)",'-e','SELECT CURRENT_USER(),DATABASE();') -InputText $client
    if (-not $result.StartsWith($account.User + '@')) { throw 'Nouveau compte non authentifie.' }
    $global = Invoke-CrmMysql -Password $admin -Sql (
        "SELECT COUNT(*) FROM information_schema.USER_PRIVILEGES WHERE GRANTEE=CONCAT(CHAR(39),'$($account.User)',CHAR(39),'@',CHAR(39),'$accountHost',CHAR(39)) AND PRIVILEGE_TYPE<>'USAGE';")
    if ($global.Trim() -ne '0') { throw 'Privilege global inattendu.' }
    $grants = Invoke-CrmMysql -Password $admin -Sql "SHOW GRANTS FOR '$($account.User)'@'$accountHost';"
    # Examiner les lignes de droits uniquement, elles ne contiennent pas les mots de passe.
    $grants
}
```

Vérifier huit comptes, chacun avec exactement les quatre droits de sa propre base.
Les CREATE USER/GRANT font des commits implicites : si l'un échoue, ne pas relancer
en aveugle. Conserver `$pending` chiffré et examiner les comptes déjà créés.
Aucune suppression automatique de compte ou de base n'est prévue.

## 3. Mise à jour locale et images épinglées

```powershell
$updates = @{}
foreach ($account in $accounts) {
    $updates[$account.Prefix + '_DB_USERNAME'] = $account.User
    $updates[$account.Prefix + '_DB_PASSWORD'] = $account.Password
}
foreach ($container in $snapshot.Containers) {
    $service = $container.Config.Labels.'com.docker.compose.service'
    if ($service -eq 'mysql') { $prefix = 'MYSQL' }
    elseif ($service -eq 'gateway-service') { $prefix = 'GATEWAY' }
    elseif ($apps -contains $service) { $prefix = $service.Replace('-service','').ToUpperInvariant() }
    else { continue }
    $updates[$prefix + '_ROTATION_IMAGE'] = $container.Image
}
$updates['APP_JWT_SECRET'] = New-CrmSecret 64
Set-CrmEnvironment -Updates $updates -Authorized
[void](Invoke-CrmDocker -Arguments ($dc + @('config','--quiet')))
```

La clé JWT neuve est générée en mémoire et stockée seulement dans le `.env` ignoré
(remplacement atomique via un fichier `.env.rotation.tmp` également ignoré).
Aucune copie en clair dans les rapports. Les conteneurs en cours conservent leurs
anciennes variables jusqu'à recréation. Les variables `<PREFIX>_ROTATION_IMAGE`
épinglent les IDs locaux sauvegardés, évitant un changement involontaire des tags
`latest` par rapport aux images `demo-local`/`honoraires-local` actuellement actives.

## 4. Reconstruction : aucune nécessaire pour cette rotation de variables

La rotation ne change aucun code Java de microservice ni Dockerfile. **Ne pas
reconstruire les images pendant la bascule de configuration.** Le backend
historique corrigé n'est pas une image de cette stack.

Si une reconstruction applicative est décidée séparément, la faire et la tester
AVANT la maintenance, après `mvn -f microservices/common-security/pom.xml install`,
puis `mvn -f microservices/<service>/pom.xml package` pour les huit applications,
puis `docker compose build <service>`. Les Dockerfiles copient les JAR de target :
`docker compose build` ne compile pas Java. Inspecter et approuver les nouveaux IDs
avant de remplacer les variables d'images. Ne pas mêler une mise à jour métier non
validée à la rotation. Les commandes de démarrage ci-dessous imposent `--no-build`.

## 5. Bascule coordonnée — maintenance, SQL root puis redémarrages

```powershell
# Stopper l'entrée, puis tous les émetteurs/validateurs JWT et connexions root.
[void](Invoke-CrmDocker -Arguments ($dc + @('stop','gateway-service')))
[void](Invoke-CrmDocker -Arguments ($dc + @('stop') + $apps))
# Seulement si l'autorisation d'exécution inclut l'invalidation de toutes les sessions :
[void](Invoke-CrmMysql -Password $admin -Sql 'UPDATE identity_db.refresh_tokens SET revoked=1 WHERE revoked=0;' -AllowWrite -Authorized)
# Un nouveau secret root distinct ; conserver un état de reprise chiffré avant ALTER.
$newRoot = New-CrmSecret 32
Save-CrmProtectedState -Path (Join-Path $directory 'root-pending.dpapi') -Value @{Password=$newRoot} -Authorized
$rootSql = "ALTER USER 'root'@'%' IDENTIFIED BY '$newRoot'; ALTER USER 'root'@'localhost' IDENTIFIED BY '$newRoot';"
[void](Invoke-CrmMysql -Password $admin -Sql $rootSql -AllowWrite -Authorized)
$newAdmin = ConvertTo-SecureString $newRoot -AsPlainText -Force
[void](Invoke-CrmMysql -Password $newAdmin -Sql 'SELECT CURRENT_USER();')
Set-CrmEnvironment -Updates @{MYSQL_ROOT_PASSWORD=$newRoot} -Authorized
$newRoot = $null; $rootSql = $null
# Recréer MySQL avec le même ID d'image et LE MÊME volume existant.
[void](Invoke-CrmDocker -Arguments ($dc + @('up','-d','--no-deps','--no-build','--pull','never','--force-recreate','mysql')))
# ATTENDRE healthy, puis SELECT 1 authentifié avec $newAdmin. Ne pas continuer sinon.
```

`ALTER USER` modifie réellement MySQL ; modifier `.env` seul ne suffit pas. Les deux
ALTER sont non atomiques : en cas d'échec partiel, garder la maintenance et les
deux états chiffrés, tester les deux identifiants sans les afficher, puis réparer
le compte concerné explicitement. Ne jamais réinitialiser le volume.

```powershell
$healthy = $false
for ($attempt=0; $attempt -lt 60; $attempt++) {
    $state = (Invoke-CrmDocker -Arguments @('inspect','crm-mysql-1') | ConvertFrom-Json)[0]
    if ($state.State.Health.Status -eq 'healthy') { $healthy=$true; break }
    Start-Sleep -Seconds 2
}
if (-not $healthy) { throw 'MySQL pas healthy : maintien en maintenance.' }
[void](Invoke-CrmMysql -Password $newAdmin -Sql 'SELECT 1;')
# Eureka reste en place. Vérifier UP avant de démarrer Identity.
if ((Invoke-CrmHttp 'http://localhost:8761/actuator/health').Json.status -ne 'UP') { throw 'Eureka indisponible.' }
$ports = @{ 'identity-service'=8082; 'department-service'=8084; 'employee-service'=8083;
            'customer-service'=8085; 'hr-service'=8086; 'invoice-service'=8089;
            'fees-service'=8088; 'payroll-service'=8087 }
foreach ($service in $apps) {
    [void](Invoke-CrmDocker -Arguments ($dc + @('up','-d','--no-deps','--no-build','--pull','never','--force-recreate',$service)))
    $up = $false
    for ($attempt=0; $attempt -lt 60; $attempt++) {
        try { if ((Invoke-CrmHttp "http://localhost:$($ports[$service])/actuator/health").Json.status -eq 'UP') { $up=$true; break } }
        catch { }
        Start-Sleep -Seconds 2
    }
    if (-not $up) { throw "Service indisponible : $service. Ne pas rouvrir Gateway." }
}
# Gateway est recréée en dernier pour retrouver une topologie saine.
[void](Invoke-CrmDocker -Arguments ($dc + @('up','-d','--no-deps','--no-build','--pull','never','--force-recreate','gateway-service')))
```

Ordre : **MySQL healthy → Eureka déjà UP → Identity → Department → Employee →
Customer → HR → Invoice → Fees → Payroll → Gateway**. Angular reste en place ;
il faudra effacer sa session périmée/revenir au login. Pas de redémarrage Eureka.

## 6. Validation avant réouverture

1. `/actuator/health` UP pour les dix services Java et MySQL healthy ; authentifier
   réellement `SELECT 1` (le nouveau healthcheck remplace `mysqladmin ping`, qui
   pouvait réussir malgré un refus d'authentification).
2. `GET http://localhost:8761/eureka/apps` avec Accept application/json : les neuf
   applications attendues ont chacune une instance UP. Attendre la convergence,
   vérifier les ports et absence d'anciennes instances actives avant réouverture.
3. Inspecter Docker **en mémoire**, jamais imprimer Config.Env : huit usernames
   différents de root, huit mots de passe dédiés, une même nouvelle clé JWT.
   Vérifier `CURRENT_USER()` avec chaque compte depuis le réseau CRM ; les comptes
   n'ont que leur base. Vérifier `information_schema.PROCESSLIST` : aucune connexion
   root issue d'une application (la connexion d'administration locale est normale).
4. Vérifier après le redémarrage, en mémoire et sans imprimer les logs bruts,
   absence de `Access denied`, `Schema-validation`, `SQLGrammarException`, erreurs
   Hibernate ou datasource. Un motif doit bloquer la réouverture et donner seulement
   le nom du service ; les messages peuvent contenir des données sensibles.
5. **Avant tout nouveau login**, comparer les checksums des tables métier à
   `$snapshot.Checksums`. Les tables Identity contenant les refresh tokens ont une
   variation attendue due à la révocation, pas les utilisateurs/rôles. Vérifier
   séparément `SELECT COUNT(*) FROM identity_db.refresh_tokens WHERE revoked=0`
   égal à zéro à cet instant. Ne pas exiger l'ancien checksum de refresh_tokens.
6. Refaire les effectifs et invariants DEMO-TNC : départements 3, employés 6,
   clients 5, congés 6, factures 8, honoraires 6, paies 6 ; références, dates et
   calculs conformes. Les checksums métier complets doivent rester identiques.
7. Avec comptes existants ADMIN, EMPLOYEE, CLIENT, saisir les mots de passe via
   `Get-Credential`/champ masqué. Capturer les réponses de login dans des variables,
   sans les imprimer : `POST /identity/api/auth/login` via Gateway. Vérifier les
   accès autorisés et HTTP 403 pour les modules interdits. Les GET anonymes des
   sept modules doivent rester HTTP 401.
8. Un ancien JWT/refresh token connu, conservé uniquement en mémoire avant bascule,
   doit être refusé. Ne pas fabriquer de JWT avec la clé exposée pour ce contrôle.
   L'absence d'un ancien jeton doit être consignée comme test non réalisé.
   Le changement de clé seul ne révoque pas les refresh tokens ; leur révocation
   explicite ci-dessus force la reconnexion. Les nouveaux logins/refresh écrivent
   des tokens techniques : inclus dans l'autorisation future, aucune écriture métier.
9. Dans Angular : login puis `/home`, dashboard avec données autorisées, navigation
   des sept modules, rôles et déconnexion. Aucun formulaire de création/modification
   sur les données DEMO-TNC. Ne pas confondre réponse HTML 200 et rendu fonctionnel.

Les endpoints métier sont `/employee/api/employees`, `/department/api/departments`,
`/hr/api/leaves`, `/customer/api/customers`, `/invoice/api/invoices`, `/fees/api/fees`,
`/payroll/api/payrolls` via `http://localhost:8080`.

Commandes de contrôle préparées, à lancer dans la même session après la bascule :

```powershell
if ((Invoke-CrmHttp 'http://localhost:8080/actuator/health').Json.status -ne 'UP') { throw 'Gateway indisponible.' }
$registry = Invoke-CrmHttp -Uri 'http://localhost:8761/eureka/apps' -Headers @{Accept='application/json'}
foreach ($name in ($apps + @('gateway-service'))) {
    $entry = @($registry.Json.applications.application | Where-Object { $_.name -eq $name.ToUpperInvariant() })
    if ($entry.Count -ne 1 -or @($entry[0].instance).Count -ne 1 -or $entry[0].instance.status -ne 'UP') {
        throw "Registre non converge pour $name : attendre puis recontroler."
    }
}
foreach ($account in $accounts) {
    $name = $account.Prefix.ToLowerInvariant() + '-service'
    $container = (Invoke-CrmDocker -Arguments @('inspect',('crm-' + $name + '-1')) | ConvertFrom-Json)[0]
    $environment = @{}
    foreach ($line in $container.Config.Env) {
        $parts=$line.Split(@('='),2); $environment[$parts[0]]=$parts[1]
    }
    if ($environment['SPRING_DATASOURCE_USERNAME'] -ne $account.User -or
        $environment['SPRING_DATASOURCE_PASSWORD'] -cne $account.Password -or
        $environment['APP_JWT_SECRET'] -cne $updates['APP_JWT_SECRET']) {
        throw "Variables incorrectes pour $name. Valeurs non affichees."
    }
    $logs = Invoke-CrmDocker -Arguments @('logs',('--since=' + $container.State.StartedAt),$container.Name) -IncludeStandardError
    if ($logs -match 'Access denied|Schema-validation|SQLGrammarException|JDBCConnectionException|ERROR[^\r\n]*(Hibernate|datasource)') {
        throw "Erreur de demarrage pour $name. Logs conserves en memoire, non affiches."
    }
}
$rootConnections = Invoke-CrmMysql -Password $newAdmin -Sql "SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE USER='root' AND HOST NOT LIKE 'localhost%' AND HOST NOT LIKE '127.0.0.1%';"
if ($rootConnections.Trim() -ne '0') { throw 'Connexion root distante inattendue.' }
$checksums = Invoke-CrmMysql -Password $newAdmin -Sql $snapshot.ChecksumSql
$oldBusiness = ($snapshot.Checksums.Trim().Split("`n") | Where-Object { $_ -notmatch '^identity_db\.refresh_tokens\t' }) -join "`n"
$newBusiness = ($checksums.Trim().Split("`n") | Where-Object { $_ -notmatch '^identity_db\.refresh_tokens\t' }) -join "`n"
if ($oldBusiness -cne $newBusiness) { throw 'Donnees metier modifiees : maintenir la maintenance.' }
$activeRefresh = Invoke-CrmMysql -Password $newAdmin -Sql 'SELECT COUNT(*) FROM identity_db.refresh_tokens WHERE revoked=0;'
if ($activeRefresh.Trim() -ne '0') { throw 'Sessions anterieures non invalidees.' }
Test-CrmDemoData -Password $newAdmin
$paths = @('/employee/api/employees','/department/api/departments','/hr/api/leaves',
           '/customer/api/customers','/invoice/api/invoices','/fees/api/fees','/payroll/api/payrolls')
foreach ($path in $paths) {
    if ((Invoke-CrmHttp ('http://localhost:8080' + $path)).Status -ne 401) { throw 'Acces anonyme inattendu.' }
}
# Les mots de passe et reponses de login ne sont jamais imprimes.
foreach ($role in @('ADMIN','EMPLOYEE','CLIENT')) {
    $credential = Get-Credential -Message "Compte $role existant (EMPLOYEE associe a un employe)"
    if ($null -eq $credential) { throw 'Validation authentifiee annulee.' }
    $login = Invoke-CrmHttp -Uri 'http://localhost:8080/identity/api/auth/login' -Method POST -Body @{
        username=$credential.UserName; email=$null; password=$credential.GetNetworkCredential().Password
    }
    if ($login.Status -ne 200 -or [string]::IsNullOrWhiteSpace($login.Json.token)) { throw "Login $role invalide." }
    $headers = @{Authorization=('Bearer ' + $login.Json.token)}
    foreach ($path in $paths) {
        $expected = if ($role -eq 'ADMIN' -or ($role -eq 'EMPLOYEE' -and $path -eq '/hr/api/leaves')) { 200 } else { 403 }
        if ((Invoke-CrmHttp -Uri ('http://localhost:8080' + $path) -Headers $headers).Status -ne $expected) {
            throw "Permissions $role non conformes pour $path."
        }
    }
}
Write-Output 'Controles techniques termines. Valider encore le dashboard et les anciens jetons avant reouverture.'
```

Si un ancien access token et un ancien refresh token sont disponibles, les saisir
dans des champs masqués et tester leur rejet, sans les copier dans un fichier :

```powershell
$oldAccess = Read-Host 'Ancien access token (vide si indisponible)' -AsSecureString
$oldRefresh = Read-Host 'Ancien refresh token (vide si indisponible)' -AsSecureString
if ($oldAccess.Length -gt 0) {
    $oldHeaders = @{Authorization=('Bearer ' + (ConvertFrom-CrmSecureString $oldAccess))}
    if ((Invoke-CrmHttp -Uri 'http://localhost:8080/employee/api/employees' -Headers $oldHeaders).Status -ne 401) {
        throw 'Ancien access token encore accepte.'
    }
} else { Write-Output 'NON REALISE : rejet explicite dun ancien access token.' }
if ($oldRefresh.Length -gt 0) {
    $response = Invoke-CrmHttp -Uri 'http://localhost:8080/identity/api/auth/refresh' -Method POST -Body @{
        refreshToken=(ConvertFrom-CrmSecureString $oldRefresh)
    }
    if ($response.Status -notin @(400,401,403)) { throw 'Ancien refresh token non refuse comme attendu.' }
} else { Write-Output 'NON REALISE : rejet explicite dun ancien refresh token.' }
```

## Retour arrière et points d'arrêt

| Moment de l'échec | Action préparée |
|---|---|
| Avant sauvegarde vérifiée | Arrêt total de la procédure ; environnement actif inchangé |
| Pendant CREATE USER/GRANT | Arrêt ; pas de rollback SQL automatique ; inventorier les comptes partiels depuis l'état pending chiffré |
| `.env` modifié mais conteneurs pas recréés | Les anciens conteneurs fonctionnent encore ; préserver l'état et corriger avant bascule |
| ALTER root partiel | Maintenir les applications arrêtées ; identifier quel compte accepte le nouveau mot de passe, puis terminer la rotation explicitement |
| Démarrage applicatif en échec | Maintenir la maintenance ; garder les nouvelles clés et comptes ; réutiliser les IDs d'images précédents, corriger uniquement le câblage |
| Données métier différentes | Ne pas rouvrir l'accès et ne pas importer automatiquement le dump réel ; diagnostic et autorisation distincte de restauration |

Le retour privilégié est **fonctionnel**, avec les anciens IDs d'images mais les
nouveaux secrets et les comptes dédiés. Le bundle donne les anciens IDs et
configurations. Ils sont déjà épinglés pour cette rotation sans rebuild. Le volume
reste inchangé. Ne pas rétablir une clé JWT exposée ni root dans les services comme
solution permanente. Une restauration des anciens secrets pour secours d'urgence
nécessiterait une nouvelle autorisation, avec exposition réseau bloquée et durée
limitée. Les sessions révoquées ne sont pas réactivées lors d'un retour d'image.

Si un changement d'image a finalement été approuvé, revenir aux images précédentes
avec les **nouveaux** identifiants et la **nouvelle** clé, jamais avec les secrets
exposés. Commandes de retour d'images préparées, à exécuter seulement sous
autorisation de reprise et en maintenant les accès utilisateurs bloqués :

```powershell
[void](Invoke-CrmDocker -Arguments ($dc + @('stop','gateway-service')))
$rollbackImages = @{}
foreach ($container in $snapshot.Containers) {
    $service = $container.Config.Labels.'com.docker.compose.service'
    if ($service -eq 'gateway-service') { $prefix='GATEWAY' }
    elseif ($apps -contains $service) { $prefix=$service.Replace('-service','').ToUpperInvariant() }
    else { continue }
    $rollbackImages[$prefix + '_ROTATION_IMAGE'] = $container.Image
}
Set-CrmEnvironment -Updates $rollbackImages -Authorized
[void](Invoke-CrmDocker -Arguments ($dc + @('config','--quiet')))
foreach ($service in $apps) {
    [void](Invoke-CrmDocker -Arguments ($dc + @('up','-d','--no-deps','--no-build','--pull','never','--force-recreate',$service)))
}
# REFAIRE les healthchecks, comptes dedies, checksums et Eureka AVANT Gateway.
# Reprendre le bloc de validation ; ne pas rouvrir si une verification echoue.
```

Ne jamais lancer `docker compose down -v`, supprimer `crm_mysql-data`, importer
le dump sur le serveur réel, utiliser `CREATE DATABASE`/DDL sur les bases réelles,
ou réécrire l'historique dans cette procédure. Les DDL du dump ne sont exécutés
que dans le conteneur de restauration isolé.

Conserver backup, receipt et pending chiffrés jusqu'à validation et délai de
rétention convenu. Fermer ensuite la session PowerShell pour libérer les variables.
La suppression des anciennes valeurs du dépôt n'efface pas l'historique : aucune
publication ne doit précéder l'invalidation effective des valeurs actives exposées.
