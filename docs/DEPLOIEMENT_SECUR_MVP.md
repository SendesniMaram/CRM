# Plan de déploiement sécurisé — préparé le 16 octobre 2026

**READY FOR DEPLOYMENT, sous autorisation explicite. Aucune commande de build, tagging, redéploiement ou rollback de ce document n'a été exécutée.** Les contrôles réalisés sont en lecture seule et une simulation Compose `--dry-run`.

## État vérifié et périmètre

Branche `microservices`, HEAD `40b6d0f`. Les fichiers corrigés, Dashboard, scripts et revue sont non commités et préservés. Les huit services à redéployer sont Identity, Employee, Department, Customer, HR, Invoice, Fees et Payroll. Les huit images actuelles ont été retrouvées par leur ID avec `docker image inspect` ; aucune hypothèse de conservation d'image n'est nécessaire au moment de ce contrôle. Il faudra refaire ce contrôle juste avant l'opération.

| Service | Image actuelle vérifiée | Port |
|---|---|---:|
| Identity | crm/identity-service:honoraires-local | 8082 |
| Employee | crm/employee-service:honoraires-local | 8083 |
| Department | crm/department-service:demo-local | 8084 |
| Customer | crm/customer-service:honoraires-local | 8085 |
| HR | crm/hr-service:demo-local | 8086 |
| Invoice | crm/invoice-service:demo-local | 8089 |
| Fees | crm/fees-service:honoraires-local | 8088 |
| Payroll | crm/payroll-service:integration-local | 8087 |

MySQL : conteneur `crm-mysql-1`, ID vérifié `032448ab0112e56c9e1e6a600972b46f60b1e121108e396577f612b92166c5be`, HEALTHY. Volume existant **crm_mysql-data**, pilote local, monté sur `/var/lib/mysql`. Ne pas remplacer ce conteneur, changer le projet Compose, recréer le volume ou nettoyer les images pendant l'opération.

Les huit bases existent avec les 15 tables attendues. Les huit conteneurs ont démarré avec Hibernate validate : EntityManagerFactory initialisée, aucune erreur de validation détectée dans leurs journaux de démarrage. Les corrections ne modifient aucune entité JPA ; les deux colonnes concernées restent VARCHAR(255) NULL. Les bases sont donc compatibles avec les mappings actuels ; la validation du nouveau binaire doit encore être observée après son démarrage, elle n'a pas été exécutée ici.

Lecture actuelle des marqueurs DEMO : départements3, employés6, clients5, congés6, factures8, honoraires6, paies6, soit **40**. La campagne précédente a comparé les 40 objets exactement au plan, confirmé 30583 TND et les relations/calculs.

Dépendances Compose : Identity dépend de Discovery et MySQL ; les autres JPA dépendent de Discovery, Gateway, Identity et MySQL. Les appels métier ajoutent des dépendances fonctionnelles entre Employee/Department, HR/Identity/Employee et les annuaires frontend. **MySQL, Discovery, Gateway et Angular sont exclus du redéploiement.** `--no-deps` est obligatoire, puisque ces dépendances sont déjà opérationnelles.

Le dry-run réalisé avec les références d'images actuelles et les nouvelles surcharges prévoit exactement **huit recréations JPA**, sans MySQL/Discovery/Gateway. Ce résultat n'est pas un redémarrage réel. Les nouvelles images n'existent pas encore.

## Risques et mesures

- Les JAR existants sont anciens : `mvn test` n'a pas produit de nouveaux packages. Il faut `package` avant de construire les images, sinon les corrections n'entreront pas dans les images.
- L'image de base `eclipse-temurin:21-jre-jammy` n'est pas présente sous ce tag local. Le plan ci-dessous reconstruit depuis chaque image courante vérifiée, avec remplacement du JAR ; aucun téléchargement de base n'est nécessaire. Cela conserve aussi l'ancien runtime Java et ses éventuelles vulnérabilités : sa mise à jour n'est pas incluse ici.
- Une recréation JPA cause une brève indisponibilité et un délai de convergence Eureka/Gateway. Redéployer un service à la fois, attendre sa santé puis contrôler Eureka et les GET.
- Compose emploie les identifiants de `.env`. Ne pas afficher `docker compose config` complet ni les environnements Docker. Utiliser seulement les sorties filtrées ci-dessous.
- Identity doit garder **APP_BOOTSTRAP_ADMIN_ENABLED=false**, au déploiement comme au rollback. Aucun bootstrap de compte, reset de mot de passe ou login automatique n'est prévu.
- Identity possède aussi un initialiseur de rôles indépendant du bootstrap administrateur. Une lecture SQL a confirmé exactement un rôle ADMIN, CLIENT, EMPLOYEE et SUPER_ADMIN : cet initialiseur n'a donc rien à créer dans l'état observé. Recontrôler leur présence avant le déploiement ; si un rôle manque, arrêter, sans laisser le démarrage le recréer implicitement dans cette mission sans écriture autorisée.
- Aucun schéma n'est créé ni modifié. Toute erreur Hibernate validate impose un arrêt et un diagnostic, jamais un ALTER ou un passage à update.
- Les tags rollback doivent être créés avant tout build ; aucun image prune/volume prune. Sans les fichiers de suivi et images sauvegardées localement, ne pas démarrer le déploiement.
- Les formulaires Angular corrigés ne sont pas déployés par ce plan backend. Le serveur Angular de développement utilise les sources locales ; un éventuel hébergement frontend de production demanderait une opération distincte.

## Commandes PowerShell proposées — après autorisation uniquement

Exécuter les blocs dans **la même session PowerShell 5.1**, depuis la racine du projet. Aucune commande de ces blocs n'a été lancée pendant la préparation.

### 1. Précontrôle et conservation des références rollback

```powershell
Set-Location -LiteralPath 'C:\Users\ASUS\Documents\CRM'
$ErrorActionPreference = 'Stop'
$services = @('identity','department','employee','customer','hr','invoice','fees','payroll')
$ports = @{identity=8082;department=8084;employee=8083;customer=8085;hr=8086;invoice=8089;fees=8088;payroll=8087}
if ((git branch --show-current) -ne 'microservices') { throw 'Branche inattendue' }
git diff --check
if ($LASTEXITCODE -ne 0) { throw 'Diff invalide' }
docker compose config --quiet
if ($LASTEXITCODE -ne 0) { throw 'Compose invalide' }
docker info --format '{{.ServerVersion}}'
if ($LASTEXITCODE -ne 0) { throw 'Docker inaccessible' }
$mysql = (docker inspect crm-mysql-1 | ConvertFrom-Json)[0]
if ($mysql.State.Health.Status -ne 'healthy') { throw 'MySQL non HEALTHY' }
$mount = @($mysql.Mounts | Where-Object { $_.Name -eq 'crm_mysql-data' -and $_.Destination -eq '/var/lib/mysql' })
if ($mount.Count -ne 1) { throw 'Volume MySQL inattendu' }

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$deployDir = Join-Path $env:TEMP ('crm-deploy-' + $stamp)
$null = New-Item -ItemType Directory -Path $deployDir
$protected = @{}
foreach ($name in @('crm-mysql-1','crm-discovery-service-1','crm-gateway-service-1')) {
    $container = (docker inspect $name | ConvertFrom-Json)[0]
    if ($container.State.Status -ne 'running') { throw 'Dependance arretee' }
    $protected[$name] = @{id=$container.Id;startedAt=$container.State.StartedAt}
}
$oldImages = @{}
foreach ($service in $services) {
    $container = (docker inspect "crm-$service-service-1" | ConvertFrom-Json)[0]
    docker image inspect $container.Image --format '{{.Id}}'
    if ($LASTEXITCODE -ne 0) { throw 'Image rollback absente' }
    $tag = "crm/${service}-service:rollback-$stamp"
    docker tag $container.Image $tag
    if ($LASTEXITCODE -ne 0) { throw 'Conservation image impossible' }
    $oldImages[$service] = @{id=$container.Image;tag=$tag}
}
# Uniquement IDs et tags non sensibles, jamais de snapshot d'environnement.
@{protected=$protected;oldImages=$oldImages} | ConvertTo-Json -Depth 6 |
    Set-Content -LiteralPath (Join-Path $deployDir 'baseline.json') -Encoding UTF8
Write-Host "Dossier de suivi non sensible : $deployDir"
```

Recontrôler également les tables et données par les lectures de la campagne avant toute action réelle. Si les données ou fichiers ont évolué, examiner l'écart avant de poursuivre. Ne pas stocker `.env` ou l'environnement complet des conteneurs dans ce dossier.

### 2. Package Maven et images locales distinctes

```powershell
foreach ($service in $services) {
    mvn.cmd -o '-Dmaven.repo.local=C:/Users/ASUS/.m2/repository' `
        -f "microservices/$service-service/pom.xml" -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "Package echoue : $service" }
    $jar = "microservices/$service-service/target/$service-service-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path -LiteralPath $jar)) { throw 'JAR absent' }
    $base = $oldImages[$service].tag
    $dockerfile = "FROM $base`nCOPY target/$service-service-0.0.1-SNAPSHOT.jar /app/$service-service-0.0.1-SNAPSHOT.jar`n"
    $dockerfile | docker build --pull=false -t "crm/${service}-service:safe-$stamp" `
        -f - "microservices/$service-service"
    if ($LASTEXITCODE -ne 0) { throw "Build echoue : $service" }
}
```

Les 403 tests backend ont déjà réussi ; `-DskipTests` ne constitue pas une nouvelle exécution de tests. Aucun service n'est encore remplacé pendant ces builds. Ne pas lancer `docker compose up --build` globalement.

### 3. Créer les deux surcharges non sensibles et examiner le plan

```powershell
$next = @{}; $previous = @{}
foreach ($service in $services) {
    $environment = @{SPRING_JPA_HIBERNATE_DDL_AUTO='validate';SPRING_SQL_INIT_MODE='never'}
    if ($service -eq 'identity') { $environment.APP_BOOTSTRAP_ADMIN_ENABLED='false' }
    $next[($service+'-service')] = @{image="crm/${service}-service:safe-$stamp";environment=$environment}
    $previous[($service+'-service')] = @{image=$oldImages[$service].tag;environment=$environment}
}
$nextFile = Join-Path $deployDir 'next.json'
$previousFile = Join-Path $deployDir 'previous.json'
@{services=$next} | ConvertTo-Json -Depth 6 | Set-Content $nextFile -Encoding UTF8
@{services=$previous} | ConvertTo-Json -Depth 6 | Set-Content $previousFile -Encoding UTF8
$targets = @($services | ForEach-Object { $_+'-service' })
docker compose -f docker-compose.yml -f $nextFile config --quiet
if ($LASTEXITCODE -ne 0) { throw 'Surcharge invalide' }
docker compose --dry-run -f docker-compose.yml -f $nextFile up -d `
    --no-deps --no-build --pull never --force-recreate $targets
if ($LASTEXITCODE -ne 0) { throw 'Plan invalide' }
```

Examiner la sortie : seuls les huit services ciblés doivent être recréés. MySQL, son volume, Discovery et Gateway doivent être absents des opérations. Le plan réel emploie les nouveaux tags safe, alors que la simulation de préparation a utilisé les tags courants.

### 4. Redéployer progressivement — action réelle à autoriser

```powershell
function Wait-CrmHealth([int]$Port) {
    for ($attempt=0; $attempt -lt 40; $attempt++) {
        try {
            $health = Invoke-RestMethod -Uri "http://localhost:$Port/actuator/health" -TimeoutSec 3
            if ($health.status -eq 'UP') { return }
        } catch { }
        Start-Sleep -Seconds 2
    }
    throw "Service non UP sur $Port : arreter et diagnostiquer"
}
foreach ($service in $services) {
    docker compose -f docker-compose.yml -f $nextFile up -d `
        --no-deps --no-build --pull never --force-recreate ($service+'-service')
    if ($LASTEXITCODE -ne 0) { throw "Redeploiement echoue : $service" }
    Wait-CrmHealth $ports[$service]
    $container = (docker inspect "crm-$service-service-1" | ConvertFrom-Json)[0]
    $expected = (docker image inspect "crm/${service}-service:safe-$stamp" | ConvertFrom-Json)[0]
    if ($container.Image -ne $expected.Id) { throw 'Image effective inattendue' }
}
```

Cette boucle ne doit être lancée qu'après approbation du plan. Une erreur arrête la suite ; ne pas compenser par une modification SQL. Les services déjà remplacés ne sont pas automatiquement restaurés : appliquer le rollback explicite ci-dessous si nécessaire.

### 5. Vérifications après déploiement

```powershell
foreach ($name in $protected.Keys) {
    $now = (docker inspect $name | ConvertFrom-Json)[0]
    if ($now.Id -ne $protected[$name].id -or $now.State.StartedAt -ne $protected[$name].startedAt) {
        throw 'Une dependance protegee a ete recreee ou redemarree'
    }
}
docker inspect crm-mysql-1 --format '{{.State.Health.Status}}|{{range .Mounts}}{{.Name}}:{{.Destination}} {{end}}'
foreach ($service in $services) {
    Wait-CrmHealth $ports[$service]
    $container = (docker inspect "crm-$service-service-1" | ConvertFrom-Json)[0]
    $safe = @($container.Config.Env | Where-Object { $_ -match '^(SPRING_JPA_HIBERNATE_DDL_AUTO|SPRING_SQL_INIT_MODE|APP_BOOTSTRAP_ADMIN_ENABLED)=' })
    $safe
    if (@($container.Config.Env | Where-Object { $_ -like 'SPRING_DATASOURCE_URL=*createDatabaseIfNotExist=true*' }).Count) {
        throw 'Creation automatique de base reactivee'
    }
    # Inspecter les logs localement ; ne jamais publier les corps ou environnements complets.
    $log = @(docker logs "crm-$service-service-1" 2>&1)
    if ($log -match 'Schema-validation|SchemaManagementException|Application run failed') {
        throw "Erreur de demarrage ou schema : $service"
    }
    if (-not ($log -match 'Initialized JPA EntityManagerFactory')) { throw 'Validation JPA non confirmee' }
}
$apps = Invoke-RestMethod -Uri http://localhost:8761/eureka/apps -Headers @{Accept='application/json'}
foreach ($app in $apps.applications.application) {
    foreach ($instance in $app.instance) { Write-Host "$($app.name) | $($instance.status)" }
}
foreach ($service in $services) {
    $expectedName = $service.ToUpperInvariant()+'-SERVICE'
    $app = @($apps.applications.application | Where-Object name -CEQ $expectedName)
    if ($app.Count -ne 1 -or @($app[0].instance | Where-Object status -CEQ 'UP').Count -lt 1) {
        throw "Eureka non conforme : $expectedName"
    }
}
$paths = @('/employee/api/employees','/department/api/departments','/hr/api/leaves',
    '/customer/api/customers','/invoice/api/invoices','/fees/api/fees','/payroll/api/payrolls')
foreach ($path in $paths) {
    $status=0
    try { $r=Invoke-WebRequest -UseBasicParsing -Uri ('http://localhost:8080'+$path) -TimeoutSec 5; $status=[int]$r.StatusCode }
    catch { if ($_.Exception.Response) { $status=[int]$_.Exception.Response.StatusCode } }
    if ($status -ne 401) { throw "Gateway non converge ou refus incorrect : $path (HTTP $status)" }
}
```

Eureka et le cache Gateway peuvent nécessiter 30–120 secondes de convergence. Attendre puis refaire les contrôles en lecture seule ; ne pas redémarrer Gateway. Vérifier que les nouvelles instances sont UP et laisser expirer les anciennes inscriptions. Une inscription ancienne seule ne prouve pas la disponibilité du nouveau service.

Puis lancer **manuellement**, si autorisé, le vérificateur existant :

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\demo\Test-CRM-MVP.ps1
```

Il exige confirmation et Get-Credential, ne stocke pas le JWT et n'émet que des GET métier. Le login peut écrire les jetons Identity habituels : ne pas le lancer si ces écritures de session ne sont pas autorisées. Les 40 fiches, relations, sommes, statuts, recherches et paginations doivent être confirmés ; les API doivent répondre 200 avec la session autorisée.

Sans session autorisée : refaire uniquement les SELECT de contrôle via la méthode locale déjà utilisée, sans les remplacer par des écritures ou un jeton forgé. Confirmer 40 DEMO (3/6/5/6/8/6/6), comptes globaux 3/8/5/7/8/6/6, PENDING2, répartition DEMO 2/2/2 et factures30583 TND. La comparaison SQL seule ne valide pas les API authentifiées.

Dans le navigateur connecté : actualiser le Dashboard et les sept modules. KPI attendus : employés8, clients5, congés7, PENDING2, factures8, honoraires6, paies6 ; factures30583 TND ; honoraires sans total monétaire faute de devise. Examiner console/réseau et états vides/erreurs sans faire de CRUD. Ne pas annoncer une validation visuelle si elle n'est pas réalisée.

## Retour arrière sans toucher aux données

Le rollback restaure les anciens binaires, **pas les données ni le schéma** : aucune migration n'est prévue, donc aucune restauration SQL n'est requise. Les anciennes validations DTO à 500 réapparaîtront ; ce retour arrière réintroduit l'anomalie applicative correspondante. Les paramètres runtime sûrs validate/never et bootstrap désactivé sont conservés même sur les anciennes images.

Dans la même session, ou après avoir retrouvé le dossier de suivi, employer les références figées et vérifier chaque image avant de remplacer un conteneur :

```powershell
# Si nouvelle session : définir $deployDir vers le dossier exact annoncé lors du déploiement.
# $deployDir = 'CHEMIN_DU_DOSSIER_DE_SUIVI'
$baseline = Get-Content (Join-Path $deployDir 'baseline.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$previousFile = Join-Path $deployDir 'previous.json'
if (-not (Test-Path -LiteralPath $previousFile)) { throw 'Surcharge rollback absente' }
$services = @('identity','department','employee','customer','hr','invoice','fees','payroll')
foreach ($service in $services) {
    $saved = $baseline.oldImages.$service
    $image = (docker image inspect $saved.tag | ConvertFrom-Json)[0]
    if ($LASTEXITCODE -ne 0 -or $image.Id -ne $saved.id) { throw 'Image rollback absente ou remplacee' }
}
$targets = @($services | ForEach-Object { $_+'-service' })
docker compose --dry-run -f docker-compose.yml -f $previousFile up -d `
    --no-deps --no-build --pull never --force-recreate $targets
if ($LASTEXITCODE -ne 0) { throw 'Plan rollback invalide' }
# Apres examen : ne choisir que les services effectivement remplaces.
$rollbackServices = @('identity','department','employee','customer','hr','invoice','fees','payroll')
foreach ($service in $rollbackServices) {
    docker compose -f docker-compose.yml -f $previousFile up -d `
        --no-deps --no-build --pull never --force-recreate ($service+'-service')
    if ($LASTEXITCODE -ne 0) { throw 'Rollback echoue : ne pas modifier la base' }
    # Reprendre Wait-CrmHealth du bloc precedent si nouvelle session.
    Wait-CrmHealth $ports[$service]
    $container = (docker inspect "crm-$service-service-1" | ConvertFrom-Json)[0]
    if ($container.Image -ne $baseline.oldImages.$service.id) { throw 'Mauvaise image apres rollback' }
}
```

Dans une nouvelle session, redéfinir également `$ports` et la fonction `Wait-CrmHealth` à partir des blocs précédents. Vérifier de nouveau les IDs/StartedAt des dépendances protégées dans `baseline.protected`, MySQL HEALTHY, le volume, Hibernate, Eureka, Gateway et les 40 DEMO. Ne pas lancer un rollback aveugle si une image manque. Ne jamais exécuter `docker compose down`, `down -v`, `docker rm`, `volume rm`, `prune`, une migration ou un changement de port pour ce plan.

## Décision

**READY FOR DEPLOYMENT** : plan, dépendances, images rollback disponibles et compatibilité des mappings vérifiés. Une approbation explicite est encore requise avant tagging, packaging/build et redéploiement. La préparation ne constitue ni un déploiement réussi ni une validation E2E authentifiée. Seul ce document a été ajouté pendant cette mission ; tous les changements existants sont préservés.
