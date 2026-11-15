# CRM V1 — lancement Express contrôlé

Préparation du 11 novembre 2026 ; source métier de référence 390bcee2bd744b0e3255cb94699534bbc0fc41c6. Les scripts Express sont des ajouts non commités à relire avant stabilisation. Aucun déploiement réel encore validé.

## Choisir l’hôte et le périmètre

- A : MySQL, Discovery, Identity, Gateway et frontend. Limites 2 944 Mio, RAM hôte libre requise 4 480 Mio, allocation Docker minimale 3 456 Mio.
- demo : A + Department, Employee, HR, Customer, Invoice. Limites 6 784 Mio, libre requis 8 320 Mio, allocation Docker 7 296 Mio.
- full : demo + Payroll/Fees. Limites 8 320 Mio, libre requis 9 856 Mio, allocation Docker 8 832 Mio.

Budgets candidats, non mesures de consommation réelle. Ajouter la marge du navigateur et surveiller les pointes. Un groupe actif à la fois ; les trois projets sont indépendants et leurs fixtures ne se transfèrent pas implicitement.

Alternative immédiate : poste Windows de test dédié, 32 Gio RAM, au moins 20 Gio disque libres, Docker Desktop Linux/WSL2 et ports libres. Copier le dépôt au commit de référence, les ajouts scripts/deployment/express et aucun secret/document personnel ; régénérer la campagne et les chemins sur ce poste. Utiliser le navigateur sur ce poste (éventuellement en session distante autorisée), sans ouvrir les ports à Internet. Ne pas transporter les anciens modèles JSON avec leurs chemins absolus Windows.

## Préparer sans démarrer

Depuis la racine du dépôt :

    $repo = (Get-Location).Path
    $run = '20261111-120000' # exemple déjà préparé ici : utiliser un identifiant neuf ailleurs
    $group = 'A'
    node --test scripts/deployment/express/prepare.test.mjs

Reconstruire les JAR des seuls services nécessaires, séquentiellement, avant de générer le manifeste : common-security install puis chaque service package, Maven hors ligne, MAVEN_OPTS=-Xmx512m et -DargLine=-Xmx384m, cache local. Ne pas utiliser skipTests ni modifier les migrations pour forcer un passage. common-security, Discovery, Identity et Gateway sont déjà reconstruits et vérifiés par 3J-A sur ce poste ; les autres JAR doivent être qualifiés avant le groupe demo/full.

    $campaign = node scripts/deployment/express/prepare.mjs $run $group
    node scripts/deployment/express/prepare-frontend.mjs $campaign

Préparer des secrets entièrement nouveaux, hors dépôt. La politique Windows peut interdire l’appel direct de fichiers PS1. Les commandes suivantes exécutent explicitement le contenu relu du script local sans modifier de politique système :

    $material = Join-Path $env:USERPROFILE ('.crm-validation/express-' + $run)
    & ([scriptblock]::Create((Get-Content scripts/deployment/express/prepare-secrets.ps1 -Raw))) -Destination $material -RepoRoot $repo

Aucune valeur n’est imprimée. Le dossier doit être neuf ; ACL limitée à l’utilisateur courant et SYSTEM. Fichiers : test.env, certificat/clé Employee, PKCS12 alias employee, mot de passe PKCS12, clés de signature HR. Certificat interne valable 14 jours, SAN employee-service/localhost ; refaire les contrôles avant la soutenance. Ne jamais copier ces fichiers dans Git ou une image. L’accès par Docker aux montages doit encore être validé.

Compiler la copie Angular, sans modifier les sources métier :

    Push-Location (Join-Path $campaign 'frontend-source')
    $env:NODE_OPTIONS='--max-old-space-size=1536'
    $env:NG_BUILD_MAX_WORKERS='2'
    & (Join-Path $repo 'frontend/node_modules/.bin/ng.cmd') build --configuration production --output-path ../frontend-dist
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' }
    Pop-Location
    node scripts/deployment/express/seal-frontend.mjs $campaign
    node scripts/deployment/express/verify.mjs $campaign

La copie réécrit à la fois URLs de services et liste d’origine autorisée de l’intercepteur vers http://localhost:18080. Le navigateur doit être sur l’hôte candidat. Nginx sert la production sur http://localhost:14200, routes SPA comprises. Aucun fallback de sécurité ou token vers une origine tierce.

## Autorisation et précontrôle opérationnel

Le script ne lance pas Docker Desktop. Avant tout démarrage de Desktop ou de conteneur : présenter RAM/ports/isolation à l’opérateur et obtenir son accord explicite. Ensuite démarrer Desktop manuellement, mesurer la RAM restante et l’allocation Docker. Si un seuil échoue, arrêter ; ne pas fermer les applications utilisateur automatiquement.

    $testEnv = Join-Path $material 'test.env'
    & ([scriptblock]::Create((Get-Content scripts/deployment/express/start.ps1 -Raw))) -CampaignDirectory $campaign -TestEnvFile $testEnv -RepoRoot $repo

Sans AuthorizeStart : vérifications seulement, aucun build d’image ni up. Le contrôle requiert le moteur accessible, le bon contexte local, la RAM, 20 Gio libres, config --quiet, fichiers intacts et bundle scellé. Il ne prouve pas à lui seul la validité de chaque certificat, la disponibilité d’images/dépendances ou les migrations MySQL.

Le groupe demo/full nécessite inspection des JAR métier, des SQL embarqués et de leur correspondance au commit avant de donner l’accord de lancement. Enregistrer les digests exacts MySQL/JRE/Nginx lors des builds/pulls ; les tags de base ne sont pas encore figés par digest et aucun téléchargement Docker n’a été effectué ici.

## Lancer après accord seulement

    & ([scriptblock]::Create((Get-Content scripts/deployment/express/start.ps1 -Raw))) -CampaignDirectory $campaign -TestEnvFile $testEnv -RepoRoot $repo -AuthorizeStart

Le script refuse les noms de volumes/réseaux existants non revus, les ports occupés et les empreintes différentes. Il construit puis lance chaque service séparément avec attente healthy bornée à 240 s. Les contrôles Java portent sur les ports TCP, pas sur les invariants métier. Sur échec, ne pas continuer : conserver preuves/volumes et arrêter explicitement les seuls conteneurs de la campagne si nécessaire. Aucun nettoyage automatique. Ne pas lancer de second groupe en parallèle.

## Recette réelle du socle et données fictives

Après contrôles MySQL/Flyway/Hibernate/Eureka dans l’instance neuve :

    node scripts/deployment/express/acceptance-core.mjs $campaign $testEnv --authorized-recette

Ce script, préparé mais NON EXÉCUTÉ ici, vérifie que le port Gateway appartient au projet, exige la première connexion bootstrap, change le mot de passe, crée ADMIN/EMPLOYEE/CLIENT fictifs, teste droits d’accès et révocation access/refresh. Il écrit les mots de passe uniquement dans demo-accounts.json à côté du fichier externe protégé, jamais dans les preuves partagées. Aucun token n’est conservé. Une recette partielle interdit la relance aveugle ; inspecter l’état et le fichier privé sans l’afficher dans les logs.

Les résultats observés seront dans evidence/core-api-results.json ; tant que le script n’a pas été exécuté, il n’y a aucun succès intégré. Aucun compte ou enregistrement métier n’a encore été créé.

Pour les métiers, utiliser uniquement exemples fictifs : département Démonstration, employés Alice Exemple/Bob Exemple, adresses @example.test ; clients Atelier Fictif/Société Exemple ; congé de test, document sans données personnelles, facture 2 × 30,85 = 61,70 avant taxe explicitement saisie. Créer les associations User–Employee/User–Customer via les interfaces prévues. Vérifier avec deux propriétaires différents et les quatre comptes. Les parcours sans dépendance active sont NON TESTÉS.

## Arrêt et redémarrage

    $manifest = Get-Content (Join-Path $campaign 'manifest.json') -Raw | ConvertFrom-Json
    docker compose -p $manifest.project --env-file $testEnv -f (Join-Path $campaign 'compose.json') stop
    docker compose -p $manifest.project --env-file $testEnv -f (Join-Path $campaign 'compose.json') ps -a

Conserver volumes et preuve d’arrêt. Pour redémarrer une campagne déjà revue, tous ses services doivent être arrêtés, ports libres, sources/JAR inchangés :

    & ([scriptblock]::Create((Get-Content scripts/deployment/express/start.ps1 -Raw))) -CampaignDirectory $campaign -TestEnvFile $testEnv -RepoRoot $repo -AuthorizeStart -RestartValidatedCampaign

Aucune suppression de volume, force, repair, baseline ou down -v. Si changement de source ou doute d’isolation, campagne neuve plutôt que réutilisation silencieuse.

## Sauvegarde de démonstration

Suspendre toute écriture et arrêter tous les conteneurs applicatifs/front/discovery/gateway du seul projet ; laisser son MySQL actif. Aucun autre projet ne doit être arrêté. Choisir un fichier neuf dans le dossier privé externe :

    node scripts/deployment/express/backup.mjs $campaign $testEnv (Join-Path $material 'demo-backup.sql') --authorized-backup

Le script refuse l’écrasement et les applications encore actives, écrit les octets SQL sans les afficher. Copie incluant les données fictives et les tables MySQL : traiter le fichier comme sensible, restreindre les ACL et chiffrer lors du transfert. Faire un SHA-256 et conserver le commit/digests/migrations.

Pour HR : archiver aussi le volume hr-documents du seul projet, en lecture seule, applications arrêtées, vers une archive neuve privée ; vérifier labels et empreinte. Ne pas confondre sauvegarde SQL seule et sauvegarde complète RH. La procédure existante locale de sauvegarde/restauration isolée détaille cette cohérence. Restauration uniquement dans un projet/volumes neufs avec autorisation séparée ; aucun restore exécuté ici.

## Limites et critères GO

Le réseau de démonstration est local/isolé, avec HTTP Gateway et JDBC sans TLS ; le canal HR–Employee garde HTTPS/signature. Cette préparation n’est ni un déploiement Internet ni une validation HTTPS globale. Ne pas désactiver une protection ou ouvrir des ports pour contourner un échec.

GO seulement après MySQL réel, migrations/validate, tests API/RBAC réels, navigateur connecté et répétition du scénario. GO DÉMO LIMITÉE seulement pour les parcours réellement déployés et vérifiés. Sinon BLOCKED. Ne pas présenter des tests H2, quatre tests Node ou des conteneurs healthy comme une recette intégrée réussie.

## Complément — préparation sans démarrage, choix opérateur confirmé

L’opérateur a choisi de poursuivre sans démarrer Docker ni les services. Les commandes comportant AuthorizeStart, --authorized-recette ou --authorized-stop ci-dessous sont des commandes FUTURES, pas exécutées pendant cette préparation.

### Ordre impératif du lancement futur

start.ps1 utilise désormais Stage=core par défaut : seuls MySQL, Discovery, Identity, Gateway et frontend peuvent démarrer, même dans un modèle demo/full. La vérification des autres artefacts et le budget du groupe restent conservateurs.

Après autorisation et succès des précontrôles, lancer le socle de la campagne, puis exécuter acceptance-core. La preuve réussie contient le projet, le commit et le SHA-256 du manifeste ; Stage=all refuse une preuve absente, échouée ou périmée. Pour poursuivre après recette du socle : arrêter la campagne, conserver ses volumes, puis redémarrer avec RestartValidatedCampaign et Stage=all. Aucun métier n’est validé par cette preuve de socle.

    # FUTUR seulement, après recette du socle réussie et arrêt confirmé :
    & ([scriptblock]::Create((Get-Content scripts/deployment/express/start.ps1 -Raw))) -CampaignDirectory $campaign -TestEnvFile $testEnv -RepoRoot $repo -AuthorizeStart -RestartValidatedCampaign -Stage all

### Arrêt sécurisé dédié

    # Plan uniquement : aucune commande Docker exécutée
    node scripts/deployment/express/stop.mjs $campaign $testEnv
    # FUTUR après autorisation d’arrêt : labels du projet et contexte local contrôlés
    node scripts/deployment/express/stop.mjs $campaign $testEnv --authorized-stop

Le script ne fait que stop --timeout 30 sur le projet exact, sans down, rm, prune ni suppression de volumes. Il vérifie ensuite zéro conteneur de ce projet encore actif. Aucun arrêt n’a été réalisé pendant la préparation.

### Jeu de données prêt à charger ultérieurement

demo-fixtures.json est un modèle fictif versionnable, sans mot de passe. Préparer ses dates et un document neutre sur disque :

    node scripts/deployment/express/materialize-fixtures.mjs $campaign

Le fichier demo-data.json ne constitue pas un import. Les quatre comptes principaux sont créés seulement par la recette API future. Les deux comptes de contrôle EMPLOYEE_B/CLIENT_B sont prévus séparément pour les refus croisés. Ne pas afficher leurs futurs mots de passe ; pas d’ID deviné.

Ordre manuel de chargement après autorisation, dans le projet isolé uniquement :
1. Recette socle : comptes, changement initial, rôles, révocation.
2. Département D1 via écran ADMIN.
3. Employés E1/E2 : résoudre departmentRef en ID renvoyé par l’API.
4. Associations UID–Employee des comptes personnels : IDs textuels exacts via écrans prévus.
5. Clients C1/C2 et affectations E1/E2 ; résoudre les références symboliques, puis associations User–Customer.
6. Congé L1 depuis EMPLOYEE ; ses dates sont générées à J+7/J+8, sans déduction de durée légale.
7. Document neutre : charger et partager explicitement avec le seul employé destinataire.
8. Facture I1 de C1 : saisir 61,70 EUR, taxe 0 choisie pour la fixture ; dates J/J+30. Vérifier le registre et le PDF avant paiement. Aucun honoraire ne crée une facture automatiquement.

Les références ref, accountRef, departmentRef, customerRef et assignedEmployeeRef sont des métadonnées de préparation : ne pas les envoyer comme champs DTO ni comme identifiants. Répertorier les IDs effectivement créés dans une preuve expurgée propre à la campagne. Sur conflit d’existence, arrêter et examiner ; pas d’écrasement, purge ou insertion SQL.

### Machine alternative recommandée

Poste Windows dédié 32 Gio, Docker Desktop Linux/WSL2, disque libre >=20 Gio, Java 21/Maven/Node/npm/Git, navigateur local. Exiger 8 320 Mio libres pour demo après prise en compte de Desktop ; 9 856 Mio pour full, plus le navigateur. Les outils installés ne prouvent pas leur configuration effective.

Cloner origin/publication-clean sur la machine de test puis vérifier HEAD égal à 390bcee2bd744b0e3255cb94699534bbc0fc41c6. Si différent, arrêter et revoir le candidat ; ne pas réécrire l’historique. Copier uniquement les ajouts scripts/deployment/express de la préparation, vérifier l’empreinte de l’archive d’appoint et ne transférer ni docs/ ni secrets. Les fichiers générés contenant des chemins Windows de l’ancien poste ne sont pas portables : reconstruire les JAR, régénérer campagne/frontend et générer de NOUVEAUX secrets sur le poste de test.

La configuration ne doit pas exposer 18080/14200 à Internet. Utiliser le navigateur du poste candidat ou une session distante explicitement autorisée. Aucune connexion ni action sur une machine distante n’a été effectuée ici.
