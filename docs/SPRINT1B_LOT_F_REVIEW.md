# Sprint 1B — Lot F : régression et validation globale JWT

> Archive documentaire : les constats, chiffres, anciens commits et autorisations ci-dessous décrivent la campagne datée du rapport. Ils ne constituent pas un état courant ni un plan à réappliquer sur `publication-clean`. Toute opération réelle nécessite une nouvelle vérification et l’autorisation adaptée.

Date : 18 octobre 2026, Africa/Tunis. Branche : microservices.

**VERDICT : PASS WITH RESERVATIONS.** Les validations non destructives réussissent. Le parcours E2E réel, le dimensionnement représentatif et la livraison ne sont pas validés. Aucun déploiement n'est autorisé par ce verdict.

## 1. Synthèse et cohérence des lots A à E

Les six documents de référence, plan compris, ont été consultés.

| Lot | État vérifié |
|---|---|
| A | Endpoint interne Identity ; signature, expiration, uid/sub, état courant et rôles ; 204/401/503 ; aucun cache positif ni écriture par la validation |
| B | uid=User.id au Login et Refresh ; sub=username, contrats JSON, durées et rotation conservés |
| C | Client partagé à destination fixe ; authentification seulement après 204 ; refus sûr ; délais 500 ms / 1 s / 1,5 s ; quatre appels simultanés |
| D | Sept consommateurs obligatoires ; RBAC et propriété ; refus interservices et suppression du fallback Department retournant null |
| E | Gardes réactives Gateway avant sélection et après réécriture ; Compose fournit l'origine Identity aux sept services |

Les dix POM concernés déclarent Spring Boot 3.3.6 et Java 21. Les applications Cloud utilisent Spring Cloud 2023.0.2 ; les sept consommateurs déclarent common-security 0.0.1-SNAPSHOT. Identity conserve sa propre politique et Gateway n'utilise pas la bibliothèque servlet.

Le JAR common-security construit et le JAR installé ont le même SHA-256 :
2F65BFB7659FB4E6D16F8A3E6746F3A2F67E386F714CDE6390177D410C74D322.
Les tests de consommateur comptent les appels HTTP et confirment l'utilisation de la nouvelle validation. Aucun code Java de production, POM, règle RBAC ou Compose principal n'est modifié dans ce lot.

## 2. Anomalie reproduite et corrigée

Un 503 reçu directement sur une requête métier était correctement traité. En revanche, après un premier 401 :

- L'échec du refresh avec 503 déclenchait expireSession.
- ensureSession effaçait aussi les identifiants lors d'une panne du refresh pendant la navigation.
- Pour un PDF, la panne du refresh pouvait être reclassée en 401 initial et afficher une expiration trompeuse.

Six nouveaux cas ont été ajoutés à la campagne ciblée : cinq échouaient et un confirmait le comportement direct 503 déjà correct. **Reproduction réellement observée : 78 tests, 5 échecs, 73 succès.**

La correction distingue une indisponibilité HTTP 0/5xx d'un refus définitif :

1. Les identifiants sont conservés sur panne réseau/serveur du refresh ; l'erreur est propagée sans rejeu métier.
2. Avec un JWT expiré, les guards annulent la navigation par false, sans Login forcé ni accès accordé.
3. Les refus définitifs 400/401/403 et l'absence de refresh token conservent le nettoyage et la reconnexion.
4. Un PDF voit la panne de refresh comme 503, avec un message générique de disponibilité.
5. Une nouvelle tentative après rétablissement peut renouveler le JWT ; la transition d'un ancien JWT sans uid est testée côté Angular avec HTTP simulé.

Les deux anciens tests imposant une déconnexion sur refresh 500 ou erreur réseau ont été remplacés par des assertions de conservation des identifiants et d'absence de rejeu. Les tests de refus définitif, boucle, concurrence, génération de session et logout restent exécutés. Aucune règle de rôle ou de propriété n'a été changée.

Une navigation annulée ne crée pas de nouvelle page de panne : l'utilisateur reste sur sa page et peut réessayer. Si Identity a déjà tourné le refresh token mais que sa réponse a été perdue, la conservation des identifiants ne garantit pas la reprise ; un refus ultérieur peut légitimement imposer une reconnexion.

## 3. Commandes réellement exécutées

### Java

Pour common-security, identity-service, les sept services métier et gateway-service :

    mvn -B "-Dmaven.repo.local=C:\Users\ASUS\.m2\repository" -f microservices/<module>/pom.xml test

common-security a été relancé après ajout des trois tests de mesure. Aucun install de JAR ni changement de dépendance n'a été nécessaire dans ce lot. Logs : target/lot-f-tests.log de chaque module ; détails : target/surefire-reports/.

### Angular, depuis frontend/

    npm.cmd test -- --watch=false --browsers=ChromeHeadless --include="src/app/core/interceptors/auth.interceptor.spec.ts" --include="src/app/core/services/auth.service.spec.ts" --include="src/app/core/guards/auth.guard.spec.ts" --include="src/app/core/guards/role.guard.spec.ts"
    npm.cmd test -- --watch=false --browsers=ChromeHeadless
    npm.cmd run build -- --configuration production

La première commande reproduit les cinq échecs. La suite complète passe à 721 cas après correction, puis à 732 après ajout des intégrations PDF et du scénario de reprise. Ces exécutions ne s'additionnent pas.

Logs : frontend/tmp/lot-f-reproduction.log, lot-f-angular.log et lot-f-build.log.

### Statique et préparation de fichiers

    docker compose config --quiet
    node --check scripts/e2e/prepare-sprint1b.mjs
    node scripts/e2e/prepare-sprint1b.mjs 20261018-195154
    docker compose --project-name crm-sprint1b-e2e-20261018-195154 --project-directory target/crm-sprint1b-e2e-20261018-195154 -f target/crm-sprint1b-e2e-20261018-195154/compose.json config --quiet

Le générateur ne lance ni Docker, npm, HTTP ni SQL. Il copie des fichiers dans un nouveau répertoire sous target, adapte cette copie et produit le modèle et le script SQL d'initialisation, non exécuté.

La validation du modèle généré emploie des valeurs E2E_* synthétiques dans le seul processus CLI pour satisfaire ses placeholders. Aucun fichier .env ou secret existant n'est modifié ; aucun secret réel n'est affiché.

Les deux validations Compose et le contrôle de syntaxe Node retournent 0. Les versions, empreintes, rapports Surefire, git status et git diff --check ont aussi été examinés. Aucun build d'image, up, stop, start, restart, down ou SQL n'a été exécuté.

## 4. Résultats finaux exacts

| Module | Tests | Échecs | Erreurs | Ignorés | Résultat |
|---|---:|---:|---:|---:|---|
| common-security | 98 | 0 | 0 | 0 | BUILD SUCCESS |
| identity-service | 105 | 0 | 0 | 0 | BUILD SUCCESS |
| employee-service | 77 | 0 | 0 | 0 | BUILD SUCCESS |
| department-service | 44 | 0 | 0 | 0 | BUILD SUCCESS |
| customer-service | 68 | 0 | 0 | 0 | BUILD SUCCESS |
| hr-service | 189 | 0 | 0 | 0 | BUILD SUCCESS |
| payroll-service | 55 | 0 | 0 | 0 | BUILD SUCCESS |
| fees-service | 67 | 0 | 0 | 0 | BUILD SUCCESS |
| invoice-service | 72 | 0 | 0 | 0 | BUILD SUCCESS |
| gateway-service | 93 | 0 | 0 | 0 | BUILD SUCCESS |
| **Total Java** | **868** | **0** | **0** | **0** | **10 succès** |
| Angular Chrome Headless | **732** | **0** | **0** | **0** | **Succès** |

Les chiffres Java viennent des rapports de cette campagne, pas de l'addition des anciens lots. Les trois nouveaux tests common-security couvrent mesures, panne/reprise et saturation.

Le build Angular production réussit avec code 0 et un avertissement : bundle initial **867,77 kB**, budget d'avertissement **500 kB**, dépassement **367,77 kB**. Aucun seuil n'a été augmenté pour le masquer.

Environnement observé : Java 21.0.12.1, Node 24.20.0, Chrome Headless 155, Windows. Discovery et le monolithe, non modifiés et hors mécanisme JWT concerné, ne sont pas inclus dans les 868 cas.

## 5. Scénarios de sécurité vérifiés

| Scénario | Preuve et portée |
|---|---|
| Login/Refresh avec uid stable | Tests Identity d'émission ; contrats JSON, subject, rôles, durée et rotation |
| État courant | H2 et filtres Identity : disabled/locked, rôle retiré, uid/sub, compte inconnu et suppression/recréation de username |
| JWT non conforme | Parseurs Identity et commun ; signature, expiration, uid/claims ; rejet avant réseau lorsqu'il est certain |
| Ancien JWT sans uid | Rejet strict, refresh conforme ; Angular renouvelle après 401 sans boucle |
| Identity indisponible | Réseau, DNS simulé, connexion refusée, timeout, protocole ; refus 503 sans handler |
| Reprise sans cache positif | Même Bearer accepté/refusé puis réaccepté après réponse synthétique rétablie ; recomptage des appels |
| Sept services et accès directs | MockMvc sans Gateway, chaînes réelles ; suites métier et PDF intégrales |
| RBAC et propriété | Matrices de rôles ; lecture Department légitime pour EMPLOYEE ; séparation des congés entre deux employés |
| Interservices | Feign Employee→Department, annuaires HR, statuts propagés, association inchangée si la seconde dépendance refuse |
| Gateway | Vraie Gateway isolée, discovery synthétique, encodages/réécritures, Bearer, 401/403/503 et CORS |
| Angular 503 direct | Aucun refresh, nettoyage ou navigation injustifiée |
| Refresh indisponible | Identifiants conservés, erreur propagée, aucun rejeu, navigation annulée si JWT expiré |
| PDF refusés | Dix nouveaux tests composant + vrai intercepteur + AuthService ; facture, paie, congé ; aucun Blob URL ni clic sur refus |
| Faux PDF | Tests existants : JSON 200 et PDF vide refusés, diagnostics non affichés |

Les dix intégrations PDF utilisent HttpTestingController, pas un backend réel. Les sept consommateurs utilisent un Identity HTTP synthétique ; les tests Identity relisent des données H2. Ces preuves complémentaires ne constituent pas un parcours navigateur→Gateway→services→Identity→MySQL réel.

## 6. Performances et disponibilité mesurées

AccountValidationPerformanceTest utilise le vrai parseur et le vrai client sur 127.0.0.1, sans cache positif et avec les paramètres inchangés. Dix opérations d'échauffement sont exclues de chaque série.

| Mesure | Échantillons | p50 ms | p95 ms | p99 ms |
|---|---:|---:|---:|---:|
| Parseur JWT strict local | 200 | 0,385 | 0,639 | 0,871 |
| Validation complète, HTTP 204 local | 100 | 1,579 | 2,305 | 2,707 |
| Deux validations séquentielles dans le même client | 50 | 2,493 | 4,194 | 5,545 |

La troisième série ne mesure pas deux vrais services : elle représente deux appels successifs avec le même client. Le serveur reçoit 110 appels pour la série simple, échauffement compris, et 120 pour la série double : aucune réussite antérieure ne dispense d'un appel suivant.

Autres mesures locales :

- Réponse retardée artificiellement : UNAVAILABLE après **1 002,638 ms**, cohérent avec le délai de lecture de 1 s.
- Après rétablissement : VALID en **2,706 ms**, sans succès ancien ni échec figé en cache.
- Quatre validations retenues : cinquième refusée en **0,927 ms**, sans cinquième appel réseau à ce moment.
- Après libération, les quatre appels réussissent et une nouvelle validation réussit. Cinq appels HTTP au total : quatre initiaux et un de reprise.

Sorties locales : common-security/target/lot-f-performance.json, lot-f-recovery.json et lot-f-saturation.json. Elles ne contiennent aucun jeton ou donnée personnelle.

**Limites :** mesures diagnostiques séquentielles en boucle locale, faible population, machine non contrôlée en charge ; certaines durées incluent les assertions. Avec 50 échantillons, le p99 est le maximum observé. Aucun Identity réel, lecture MySQL, réseau Docker ou TLS n'est inclus. Aucun SLA ni dimensionnement de production n'est déduit.

Les limites restent quatre validations par client, refus à saturation, dépendance Identity/base et opérations déjà admises non annulées rétroactivement. Aucun paramètre de concurrence ou de sécurité n'est modifié.

Une résolution DNS native non interruptible peut occuper les workers après expiration du budget applicatif. La reprise HTTP mesurée ne prouve pas la récupération d'un tel blocage système : l'accès reste refusé tant que la capacité n'est pas libérée.

## 7. Plan E2E reproductible — exécution toujours soumise à accord

### 7.1 Préparation livrée

scripts/e2e/prepare-sprint1b.mjs refuse d'écraser un run existant, vérifie que la destination reste dans le workspace et exclut les liens symboliques, .env, node_modules et sorties de build de la copie frontend.

Run préparé et validé statiquement :

    target/crm-sprint1b-e2e-20261018-195154/

Contenu : compose.json autonome, mysql/001-schemas.sql, gateway/application-e2e.yml et copie frontend.

Pour un nouveau run de préparation, sans Docker :

    $runId = Get-Date -Format 'yyyyMMdd-HHmmss'
    node scripts/e2e/prepare-sprint1b.mjs $runId

Ne jamais fusionner ce modèle avec le Compose principal. Le script ne génère aucun secret et n'exécute aucune initialisation de base.

### 7.2 Architecture isolée

| Élément | Configuration de test |
|---|---|
| MySQL 8.0 | tmpfs /var/lib/mysql, aucun volume réel ou nommé, pas de port hôte |
| Discovery | Registre du seul projet E2E ; aucun registre actif partagé |
| Identity | 127.0.0.1:18082 ; bootstrap local synthétique |
| Sept services | 127.0.0.1:18083 à 18089 |
| Gateway | 127.0.0.1:18080 ; CORS localhost:14200 dans la configuration générée |
| Angular copié | Futur serveur sur 14200 ; origine API et intercepteur adaptées uniquement dans la copie |
| Réseau et images | Bridge et tags propres au nom de projet unique ; aucun réseau externe partagé |

Le SQL généré crée huit schémas vides. Le seul modèle de test utilise ddl-auto=update pour initialiser les tables et garder les comptes lors d'un stop/start Identity. Ce n'est ni une migration ni une procédure de production ; le Compose principal conserve validate.

### 7.3 Préconditions et commandes futures — NON exécutées

Un accord explicite reste nécessaire avant build/start, données ou scénarios de panne. Vérifier ressources, Docker et ports 14200/18080/18082–18089 ; ne jamais arrêter un processus existant pour libérer ces ports.

Depuis la racine CRM, après accord :

    $project = 'crm-sprint1b-e2e-20261018-195154'
    $dir = Join-Path (Get-Location) "target/$project"
    $compose = Join-Path $dir 'compose.json'
    if ($project -notmatch '^crm-sprint1b-e2e-\d{8}-\d{6}$') { throw 'Projet de test requis' }
    $model = Get-Content -LiteralPath $compose -Raw | ConvertFrom-Json
    if ($model.name -ne $project) { throw 'Modèle incorrect' }
    if ($model.services.mysql.tmpfs -notcontains '/var/lib/mysql') { throw 'MySQL temporaire requis' }
    $argsE2E = @('compose', '--project-name', $project, '--project-directory', $dir, '-f', $compose)

Créer des valeurs synthétiques dans le processus, jamais dans .env :

    function New-TestCredential {
      $bytes = New-Object byte[] 32
      $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
      try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
      [Convert]::ToBase64String($bytes)
    }
    $env:E2E_MYSQL_PASSWORD = New-TestCredential
    $env:E2E_JWT_SECRET = New-TestCredential
    $env:E2E_ADMIN_PASSWORD = New-TestCredential

Produire les JAR du workspace courant, modifications locales comprises, puis construire le seul projet E2E :

    mvn -B -f microservices/common-security/pom.xml install
    if ($LASTEXITCODE -ne 0) { throw 'Échec common-security' }
    foreach ($module in 'identity','employee','department','customer','hr','payroll','fees','invoice','gateway','discovery') {
      mvn -B -f "microservices/$module-service/pom.xml" package
      if ($LASTEXITCODE -ne 0) { throw "Échec $module" }
    }
    & docker @argsE2E config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Configuration invalide' }
    & docker @argsE2E build
    if ($LASTEXITCODE -ne 0) { throw 'Construction échouée' }
    & docker @argsE2E up -d
    if ($LASTEXITCODE -ne 0) { throw 'Démarrage échoué' }

Les Dockerfiles copient les JAR de target : packaging indispensable. Régénérer un nouveau run si le frontend change après préparation.

Frontend de test seulement :

    $frontDir = Join-Path $dir 'frontend'
    Push-Location $frontDir
    try {
      npm.cmd ci
      if ($LASTEXITCODE -ne 0) { throw 'Installation échouée' }
    } finally { Pop-Location }
    $front = Start-Process -FilePath (Get-Command node.exe).Source -ArgumentList @('node_modules/@angular/cli/bin/ng.js','serve','--host','127.0.0.1','--port','14200') -WorkingDirectory $frontDir -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $dir 'frontend.stdout.log') -RedirectStandardError (Join-Path $dir 'frontend.stderr.log')
    $frontPid = $front.Id
    $frontStarted = $front.StartTime

Ouvrir un profil navigateur de test à http://localhost:14200. Ne pas utiliser le frontend actif ni enregistrer des Bearer dans les captures.

service_started ne garantit pas la readiness : attendre avec délais bornés les healthchecks sur les ports de test, puis un Login synthétique et une validation interne directe 204 sur 18082. L'accès interne via 18080 doit être refusé. Aucun seed si ces préconditions échouent.

### 7.4 Données synthétiques et identifiants

Tenir un registre des IDs réellement retournés ; ne jamais supposer User.id=Employee.id ou Customer.id.

| Donnée | Préparation |
|---|---|
| SUPER_ADMIN | Bootstrap sprint1b-admin@example.test, mot de passe E2E_ADMIN_PASSWORD |
| ADMIN | e2e-admin ; attribution explicite dans la seule base de test, faute d'API générale de changement de rôle |
| Employés utilisateurs A/B | e2e-employee-a/b via POST /identity/api/users avec Super Admin |
| CLIENT | e2e-client via inscription publique ; rôle CLIENT imposé |
| Employés métier A/B | employeeCode E2E-A/B, salary=1000, ID de réponse sauvegardé |
| Département/client métier | Fiches dédiées ; leurs vrais IDs sont réutilisés |
| Associations congés | User A→Employee A et User B→Employee B via API, IDs explicites |
| Congés | Une demande future par employé ; accès propres/croisés |
| Paie | employeeId réel en chaîne, baseSalary=1000, payPeriod=E2E, status=PAID |
| Facture | invoiceNumber E2E-<run>, customerId réel en chaîne, subtotal=100, status=DRAFT |

Les inscriptions fournissent firstName, lastName, username, email example.test et mot de passe synthétique. Aucune association approximative à partir d'un nom ou email.

Pour modifier enabled/account_non_locked/user_roles : SQL uniquement sur le MySQL du projet, transaction commitée, IDs enregistrés et username e2e-* exact, nombre de lignes affectées vérifié. Aucun endpoint de statut n'est ajouté.

Avant toute commande SQL future :

    $dbId = (& docker @argsE2E ps -q mysql).Trim()
    if (-not $dbId) { throw 'MySQL de test absent' }
    $meta = (& docker inspect $dbId | ConvertFrom-Json)[0]
    if ($meta.Config.Labels.'com.docker.compose.project' -ne $project -or $meta.Config.Labels.'com.docker.compose.service' -ne 'mysql') { throw 'Conteneur hors périmètre' }
    # SQL de fixture seulement après ce contrôle, via stdin sur $dbId.
    # Tables : identity_db.users et identity_db.user_roles.

Un décalage d'AUTO_INCREMENT peut rendre les IDs volontairement distincts, uniquement dans la base de test. Ne jamais employer le secret JWT actif pour fabriquer les tokens de test.

### 7.5 Scénarios E2E et critères de réussite

| Scénario | Critère |
|---|---|
| Nominal | Login navigateur, uid égal au User enregistré ; droits admin normaux sur les sept domaines |
| Refresh | Une rotation et uid correct ; ancien refresh refusé ; aucune boucle |
| Legacy | JWT signé avec la seule clé E2E sans uid ; 401 puis upgrade par refresh ou reconnexion |
| Falsifié/expiré | Accès direct et Gateway refusés |
| Disabled/locked | Même JWT conservé ; statut commité en test ; prochain accès, Login et Refresh refusés |
| Rôle retiré | Ancien JWT refusé ; ajout d'un rôle courant ne promeut pas les autorités signées |
| Réactivation | Ancien JWT non expiré peut redevenir utilisable : limite acceptée |
| Propriété | Employé A lit son congé ; congé B et PDF B refusés |
| RBAC existant | CLIENT/EMPLOYEE restent refusés sur CRUD Paie/Facture ; aucun futur portail personnel attendu |
| Interservices | Employee→Department et HR→Identity/Employee ; refus propagés et absence de sauvegarde après refus obligatoire |
| Panne Identity | Seul Identity du projet de test arrêté ; services métier 503, pas d'écriture ; session Angular conservée |
| Reprise | Readiness puis nouvelle validation réelle ; aucun succès périmé |
| Saturation | Charge bornée de test ; refus sûr, limite quatre inchangée |
| Gateway | Endpoint interne direct/préfixé/discovery/encodé bloqué ; Login/Refresh passent |
| PDF | Refus 401/403/503 sans fichier ; succès admin donnant un PDF non vide |

La panne de la route Login/Refresh peut apparaître en erreur réseau/5xx selon transport/découverte ; Angular doit conserver les identifiants dans ces cas. Le contrat strict 503 à vérifier concerne les services métier incapables de confirmer la validité auprès d'Identity.

Comparer les données/horodatages des seules tables de test avant et après les refus sensibles. Une vue déjà chargée ne constitue pas une nouvelle lecture autorisée.

### 7.6 Panne, arrêt et retour arrière — NON exécutés

Après vérification du même projet/modèle et des labels :

    & docker @argsE2E stop identity-service
    # Scénarios de panne autorisés dans ce seul projet.
    & docker @argsE2E start identity-service
    # Attendre readiness et refaire un contrôle.

Fin de campagne :

    $runningFront = Get-Process -Id $frontPid -ErrorAction SilentlyContinue
    if ($runningFront -and $runningFront.StartTime -eq $frontStarted -and $runningFront.ProcessName -eq 'node') {
      Stop-Process -Id $frontPid
    }
    & docker @argsE2E down
    Remove-Item Env:E2E_MYSQL_PASSWORD, Env:E2E_JWT_SECRET, Env:E2E_ADMIN_PASSWORD -ErrorAction SilentlyContinue

Aucun -v ni suppression de volume. L'arrêt de MySQL perd ses seules données synthétiques en tmpfs ; ne pas l'arrêter pour le scénario de reprise Identity. Ne jamais employer un nom de projet générique ou le Compose principal.

Retour arrière : aucun déploiement à annuler. Fermer le navigateur de test, arrêter le seul frontend copié, retirer les seuls conteneurs/réseaux du projet unique et conserver des logs expurgés. Ne pas réinitialiser Git : les fichiers comportent des modifications antérieures à préserver.

## 8. Fichiers du lot et préservation

Production Angular, sous frontend/src/app/ :

- core/interceptors/auth.interceptor.ts
- core/services/auth.service.ts
- core/guards/auth.guard.ts
- core/guards/role.guard.ts
- shared/pdf-download/pdf-download.component.ts
- Nouveau core/services/session-unavailable.ts

Tests adaptés : auth.interceptor.spec.ts, auth.service.spec.ts, auth.guard.spec.ts, role.guard.spec.ts, pdf-download.component.spec.ts.
Ajout : shared/pdf-download/pdf-security.integration.spec.ts.

Autres créations : microservices/common-security/src/test/java/com/crm/security/AccountValidationPerformanceTest.java, scripts/e2e/prepare-sprint1b.mjs et ce rapport.

Sur 162 fichiers déjà modifiés/non suivis, quatre sont étendus : les deux fichiers d'intercepteur et les deux fichiers du composant PDF. Leurs modifications antérieures sont conservées ; les 158 autres gardent leur empreinte. Les autres changements touchent des fichiers auparavant propres ou nouveaux. Aucun fichier supprimé.

Les sources Java de production, Identity, les sept services, Gateway, POM, Compose principal, .env et secrets existants restent inchangés. target/dist/tmp contiennent seulement des sorties de test/build/préparation. Aucun conteneur ni donnée réelle n'est modifié.

## 9. Réserves et conditions de clôture

Les validations de code et de régression autorisées sont terminées ; aucun échec final n'est masqué.

Pour valider le Sprint 1B de bout en bout, il reste à :

1. Autoriser puis exécuter le plan E2E isolé avec comptes synthétiques et artefacts A–F cohérents.
2. Vérifier MySQL réel de test, Eureka/DNS Docker, readiness et reprise ; bases actives toujours exclues.
3. Mesurer Identity/base et les percentiles sous charge représentative ; tout changement de capacité exige justification et validation.
4. Accepter les limites : réactivation d'anciens JWT, requêtes en vol, dépendance Identity, clé HMAC partagée et HTTP interne de confiance.
5. Préparer et approuver une livraison coordonnée ; aucune image active n'a été remplacée.

Le modèle de test ddl-auto=update ne résout pas l'absence de migrations reproductibles de production. L'avertissement Angular et l'absence de readiness Identity restent à examiner avant livraison.

L'absence de commande atomique de changement de statut/révocation et les courses avec Login/Refresh, déjà signalées dans le plan, restent hors correction de ce lot. Le contrôle courant ne devient pas un registre de révocation définitive des anciennes sessions.

**VERDICT FINAL : PASS WITH RESERVATIONS. Régression isolée réussie ; E2E réel, dimensionnement et livraison restent à autoriser et valider. Aucun déploiement ni E2E sur les services actifs.**
