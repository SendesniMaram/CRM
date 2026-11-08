# Lot 2A — Préparation sécurisée de la validation intégrée CRM

Préparation : 1er novembre 2026. Finalisation : 2 novembre 2026. Branche conservée : `publication-clean`.

**PRÊT POUR LOT 2B au niveau de la préparation statique.** Un modèle autonome et les scénarios sont préparés. Les précontrôles opérationnels du §7 restent obligatoires avant le premier démarrage : matériel TLS, secrets distincts, mémoire, ports, images et ressources Docker neuves. Leur échec bloque le démarrage. Aucun résultat d'intégration, migration ou déploiement n'est annoncé ici.

## 1. État des configurations examinées

Les rapports des Lots 1A/1B/1C, le protocole intégré Lot 1C et le plan de finalisation ont été consultés. Ont également été examinés : Compose principal, validation, overlay HTTPS et ancien overlay Sprint 6C ; dix Dockerfiles, configurations principales Spring et configurations de test disponibles ; POM, migrations, bootstrap Identity, filtres Gateway et configuration du canal HTTPS.

| Élément | Constat statique et choix Lot 2B |
|---|---|
| Architecture | MySQL 8.0, Discovery, Gateway, Identity et sept services métier ; bibliothèque common-security partagée ; aucune nouvelle architecture |
| Dockerfiles | Java 21 JRE ; copient les JAR `target/*-0.0.1-SNAPSHOT.jar` déjà construits ; aucun build Maven interne ; reconditionner les JAR actuels avant les images |
| Bases | Huit schémas `identity_db`, `employee_db`, `department_db`, `customer_db`, `hr_db`, `payroll_db`, `fees_db`, `invoice_db` ; mêmes noms logiques mais instance MySQL entièrement distincte, sans port hôte ni URL de développement |
| Volumes/réseau | Modèle autonome avec volumes et réseau préfixés automatiquement par le projet de campagne ; pas de nom global, volume externe, container_name ou réseau partagé |
| Dépendances | MySQL sain avant les services de données, Discovery avant les clients, Identity avant les métiers ; graphe sans cycle ; `service_started` ne démontre pas la disponibilité fonctionnelle, à attendre séparément |
| Gateway | Routes existantes conservées ; filtres de refus des namespaces internes inchangés ; validation Identity obligatoire des services métier conservée |
| HTTPS | Propriétés reprises exactement de l'overlay Lot 1C : connecteur interne Employee 8443, clé dédiée HR, `kid=profile`, certificats et hostname verifier ; aucun port TLS interne publié |
| Frontend | Copie isolée sous target : URLs API 8080 remplacées par 18080 ; source de développement inchangée ; CORS du Gateway copié pour l'origine 14200 |
| Identité initiale | Bootstrap local existant explicitement activé sur la base de test vierge, compte synthétique `lot2b-admin` ; rôle initialisé avant bootstrap ; pas de provisionnement production |

Le modèle de campagne est un **fichier Compose autonome en JSON**, format de données YAML compatible avec Compose. Il ne doit être fusionné avec aucun autre Compose. Son acceptation par le vrai CLI Compose reste à vérifier en Lot 2B.

## 2. Anomalies détectées

1. `docker-compose.validation.yml` est un overlay : il n'impose pas à lui seul un projet distinct. Les clés de volume sont celles du Compose principal ; un nom de projet réutilisé peut viser les volumes de développement. L'ancien overlay Sprint 6C impose même un nom physique de volume fixe, réutilisable entre campagnes.
2. Les fichiers existants n'imposent pas de limites de mémoire pour toute la pile et publient de nombreux ports sans adresse loopback explicite. Le canal HTTPS ne fournit ni isolation ni budget mémoire à lui seul.
3. Les tags `sprint6b`/`latest` et les JAR présents sur disque ne prouvent pas qu'une image contient les Lots 1A/1B/1C. Les Dockerfiles nécessitent un packaging préalable explicite.
4. Le générateur historique `scripts/e2e/prepare-sprint1b.mjs` utilise Hibernate `update` et, dans sa variante démo, des initialisations Customer séparées. Il ne convient pas pour prouver une installation actuelle exclusivement par Flyway. Il n'a pas été exécuté ni modifié.
5. Les configurations applicatives autorisent le baseline Flyway automatique et activent souvent `show-sql`. Un schéma neuf doit exercer V1 réellement, sans baseline implicite, ni traces SQL inutiles.
6. Les secrets et le matériel cryptographique ne sont pas provisionnés par les overlays ; un démarrage sans les trois activations HTTPS et les cinq fichiers cohérents échouera ou rendra le profil indisponible.
7. Angular référence le Gateway de développement sur 8080. Utiliser ses sources telles quelles pendant une campagne sur 18080 pourrait envoyer des requêtes au développement.
8. La healthcheck historique MySQL utilise `mysqladmin ping` et le mot de passe en argument. Un ping n'est pas la preuve d'une authentification SELECT 1 réussie.

Ces constats ne justifient aucune modification du code métier, des migrations historiques ou des fichiers des Lots 1A/1B/1C.

## 3. Corrections et préparation réalisées

Un générateur distinct prépare, sans Docker ni réseau :

- un projet imposé `crm-lot2b-YYYYMMDD-HHMMSS`, refus d'écrasement d'une campagne existante et refus d'un target redirigé ;
- une instance MySQL neuve avec deux volumes propres au projet (base et documents HR) et un réseau propre ; initialisation limitée à huit `CREATE DATABASE`, aucun import de données ou DDL métier hors Flyway ;
- des images nommées par campagne, des builds pointant vers les Dockerfiles actuels et des dépendances internes explicites ;
- `ddl-auto=validate`, SQL init `never`, Flyway activé, baseline automatique désactivé, validation activée et clean désactivé dans les huit services de données ;
- des limites mémoire/processus, pools JDBC de cinq connexions et SQL/bind logging désactivés ; MySQL borné avec buffer InnoDB 256 MiB et 100 connexions ; healthcheck SELECT 1 authentifiée, sans mot de passe dans les arguments mysql ;
- uniquement Gateway `127.0.0.1:18080` et Eureka `127.0.0.1:18761` publiés ; Angular prévu sur `127.0.0.1:14200` ; MySQL et les huit services de données accessibles seulement dans le réseau candidat ;
- secrets référencés par variables propres `CRM_2B_*`, aucun secret réel généré, lu ou copié ; cinq binds TLS en lecture seule avec refus de création automatique d'un chemin hôte manquant ;
- une copie Angular sans node_modules, sorties de test, caches, fichiers .env ou clés, et une copie de configuration Gateway adaptée au CORS de test ;
- un manifeste des versions et SHA-256 des 24 migrations, un contrôleur statique du modèle généré et un protocole intégré couvrant les trois lots.

Campagne finale préparée : `target/crm-lot2b-20261101-140100/` (ignorée par Git). Elle contient `compose.json`, `migrations.json`, `mysql/001-schemas.sql`, `gateway/application-validation.yml` et `frontend/`. La campagne de préparation intermédiaire `20261101-140000` est conservée sans suppression ; **ne pas l'utiliser**, son modèle est antérieur à l'ajout du port d'inspection Eureka. Régénérer sous un nouvel identifiant si les entrées changent ; le checker signale les dérives de modèle, migrations ou Gateway.

### Compatibilité Flyway examinée sans exécution

| Domaine | Nombre | Chaîne versionnée |
|---|---:|---|
| Identity | 2 | V1 puis V20261101_01 (sid nullable, index) |
| Employee | 1 | V1 |
| Department | 1 | V1 |
| Customer | 5 | V1, association, classification, activités/opportunités, prévision/historique |
| HR | 6 | V1, contrats, évaluations/recrutement, documents/carrière, quotas, visibilité documentaire |
| Payroll | 2 | V1 puis publication |
| Fees | 1 | V1 |
| Invoice | 6 | V1, paiements/relances, séquence, devis, lignes facture, élargissement séquence |

Les huit POM déclarent Flyway core et son module MySQL ; le parent Spring Boot 3.3.6 gère Flyway 10.10.0. Les noms/versions sont distincts par domaine, V1 existe partout, les tables précèdent leurs ALTER et les références principales. Le manifeste est un contrôle de fichiers, pas un parseur SQL ni un substitut aux checksums Flyway. La syntaxe MySQL effective, les types reconnus par Hibernate (notamment BOOLEAN/BIT et TIMESTAMP), index, clés étrangères, rollbacks et upgrades restent non validés. Les DDL MySQL ne garantissent pas un rollback global après erreur. Aucune migration corrective nouvelle n'a été inventée sans preuve d'erreur réelle.

## 4. Fichiers modifiés et nouveaux fichiers

**Aucun fichier préexistant modifié.** Les 153 fichiers déjà modifiés/non suivis au début du lot ont été photographiés puis comparés par SHA-256, sans changement ni disparition. Les Lots 1A/1B/1C et leurs migrations, y compris non suivies, sont préservés. Les fichiers tracked auparavant propres n'ont pas été modifiés non plus.

| Nouveau fichier versionnable | Rôle |
|---|---|
| [scripts/validation/prepare-lot2b.mjs](../scripts/validation/prepare-lot2b.mjs) | Génération autonome sans démarrage ni secret |
| [scripts/validation/check-lot2b.mjs](../scripts/validation/check-lot2b.mjs) | Contrôle statique de la campagne et des empreintes |
| [scripts/validation/prepare-lot2b.test.mjs](../scripts/validation/prepare-lot2b.test.mjs) | Neuf vérifications locales des invariants de configuration |
| [docs/LOT_2B_SCENARIOS_VALIDATION.md](LOT_2B_SCENARIOS_VALIDATION.md) | Scénarios, fixtures, résultats attendus et preuves |
| [docs/LOT_2A_PREPARATION_VALIDATION.md](LOT_2A_PREPARATION_VALIDATION.md) | Ce rapport et les commandes futures |

Les artefacts ignorés sous target sont reproductibles, sans credentials. Aucun staging, commit, push, changement de branche, accès aux volumes ou fichier .env lu/modifié. Aucun Docker, MySQL, microservice, Flyway ou E2E n'a été lancé.

## 5. Risques identifiés et préconditions bloquantes pour le démarrage

| Point | État / mesure obligatoire en Lot 2B |
|---|---|
| Certificats et clés | Non fournis/non inspectés pendant 2A ; exiger les cinq fichiers de test hors dépôt, SAN `employee-service`, validité/chaîne, alias `employee`, paire RSA correspondante et ACL ; détails dans le protocole Lot 1C |
| Secrets | Exiger trois valeurs dédiées et indépendantes, distinctes du développement : MySQL, JWT (au moins 32 octets selon le contrat), bootstrap (12 à 72 octets UTF-8) ; fichier externe protégé, jamais rendu dans un log |
| Docker/contexte | Non interrogé ; vérifier la cible locale et l'absence des ressources du nouveau projet avant première création ; ne pas utiliser de contexte distant par accident |
| Compose | Le modèle JSON est contrôlé localement, pas encore validé par Compose ; vérifier `config --quiet` puis la configuration résolue en mémoire, sans afficher les secrets |
| Mémoire, disque, ports | Non mesurés ; appliquer le §6 et refuser les ports occupés ; ne pas arrêter une application ou pile existante pour libérer une ressource |
| Images et JAR | Non reconstruits ; tags de base mutables ; packager séquentiellement, capturer SHA-256 JAR et digests images en Lot 2B ; ne jamais utiliser les images historiques par défaut |
| SQL/upgrade | Validation réelle différée ; ne pas réactiver baseline/update/repair pour contourner une erreur ; upgrade uniquement sur copie autorisée |
| Credentials Docker | Les variables sont visibles à un administrateur Docker ; `config`, inspect complet et logs bruts peuvent les révéler. Ne publier que les contrôles expurgés |
| Bootstrap local | Réservé à ce réseau et ces données synthétiques ; ni le profil local ni le modèle ne sont une configuration de production |

## 6. Besoins en mémoire et ports

| Processus | Limite conteneur | Heap / autre borne |
|---|---:|---|
| MySQL | 1024 MiB | Buffer InnoDB 256 MiB, 100 connexions |
| Discovery | 512 MiB | Xmx256m, metaspace128m |
| Gateway | 512 MiB | Xmx256m, metaspace128m |
| Identity et sept métiers | 8 × 768 MiB | Xmx384m chacun, metaspace192m, pool JDBC 5 |
| **Total maximal des limites** | **8192 MiB (8 GiB)** | N'inclut pas Docker VM, OS, navigateur ou builds |

Prévoir au moins **10 GiB attribués à Docker** et **12 GiB libres côté hôte pour la campagne API**, en vérifiant l'absence de surengagement de la VM Docker/WSL. Pour Angular + Chrome ajouter environ 3 GiB de marge : Node borné à 2048 MiB et navigateur. Ce sont des budgets de préparation, pas des mesures ou une garantie d'absence d'OOM. Ne pas exécuter Maven, builds d'images, Angular et toute la pile simultanément. Si les ressources sont insuffisantes, bloquer la campagne complète et convenir d'un découpage ; ne pas réduire les services silencieusement tout en déclarant la pile complète validée.

Prévoir également de la place pour les dix images, les JAR et les volumes ; viser 20 GiB de marge disque et mesurer la disponibilité réelle avant build. Les seules réservations hôte sont TCP 18080 et 18761, puis 14200 pour Angular ; aucun 3307/13307, 8080 ou 8443 publié par la campagne.

## 7. Commandes prévues pour le Lot 2B — non exécutées

**Ces commandes sont un protocole futur.** La première commande `up` de chaque service de données déclenchera automatiquement Flyway. L'exécution du Lot 2B doit être explicitement demandée ; aucune commande Docker de ce paragraphe n'a été appelée pendant 2A.

### Préparation reproductible et contrôles avant Docker

Depuis la racine, réutiliser la campagne finale non démarrée ou préparer un nouvel identifiant libre :

```powershell
$env:NODE_OPTIONS='--max-old-space-size=512'
node --test scripts/validation/prepare-lot2b.test.mjs
# Seulement si une nouvelle campagne est nécessaire, avec un identifiant libre :
# node scripts/validation/prepare-lot2b.mjs YYYYMMDD-HHMMSS
node scripts/validation/check-lot2b.mjs target/crm-lot2b-20261101-140100
if ($LASTEXITCODE -ne 0) { throw 'Préparation incohérente : arrêt' }
$lot2bProject='crm-lot2b-20261101-140100'
$lot2bCompose=(Resolve-Path "target/$lot2bProject/compose.json").Path
$lot2bEnv=Read-Host 'Chemin absolu du fichier de secrets de test hors dépôt'
$lot2bKeys=Read-Host 'Répertoire absolu du matériel TLS de test hors dépôt'
# Contrôler manuellement chemins absolus externes, ACL et cinq fichiers TLS.
# Le fichier externe contient CRM_2B_MYSQL_PASSWORD, CRM_2B_JWT_SECRET,
# CRM_2B_ADMIN_PASSWORD ; aucune valeur réelle n'est présente dans ce rapport.
$env:CRM_PROFILE_MATERIAL_DIR=$lot2bKeys
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
  Where-Object { $_.LocalPort -in 18080,18761,14200 } |
  Select-Object LocalAddress,LocalPort
# Si un port est occupé : arrêter la préparation, pas le processus utilisateur.
```

### Précontrôles Docker autorisés seulement en Lot 2B

Vérifier mémoire/disque/certificats avant de poursuivre. Contrôler le contexte local et les ressources de campagne ; toute ressource existante lors d'une **première** création exige un nouvel identifiant, sans suppression de l'ancienne :

```powershell
docker context show
docker info --format '{{.MemTotal}}'
docker ps -a --filter "label=com.docker.compose.project=$lot2bProject" --format '{{.ID}}'
docker volume ls --filter "label=com.docker.compose.project=$lot2bProject" --format '{{.Name}}'
docker network ls --filter "label=com.docker.compose.project=$lot2bProject" --format '{{.Name}}'
docker compose -p $lot2bProject --env-file $lot2bEnv -f $lot2bCompose config --quiet
if ($LASTEXITCODE -ne 0) { throw 'Configuration Compose refusée : arrêt' }
```

Avant build/up, inspecter **en mémoire et sans impression** le JSON résolu (`config --format json`, capturé dans une variable), puis libérer cette variable. Vérifier : projet exact, volumes physiques `${lot2bProject}_mysql-data` et `${lot2bProject}_hr-documents`, réseau `${lot2bProject}_isolated`, aucun external/name imposé différent, binds de clés seulement dans le répertoire externe approuvé en lecture seule, deux binds de configuration uniquement dans la campagne, ports loopback précités, URLs MySQL vers `mysql:3306`, limites et paramètres Flyway stricts. Aucun champ de configuration résolue ne doit être partagé dans une sortie brute.

Utiliser exclusivement `-p`, `--env-file` et `-f` ci-dessus, sans autre Compose, et arrêter sur tout code de sortie non nul. Ne pas exécuter le runner historique `run-minimal.mjs` : il cible une autre campagne et d'autres ports/services.

### Packaging et build séquentiels, avant démarrage

```powershell
$env:MAVEN_OPTS='-Xmx512m'
$lot2bModules='common-security','discovery-service','identity-service','gateway-service',
  'department-service','employee-service','customer-service','hr-service',
  'payroll-service','fees-service','invoice-service'
foreach ($lot2bModule in $lot2bModules) {
  $lot2bGoal=if ($lot2bModule -eq 'common-security') { 'install' } else { 'package' }
  mvn -o -B -ntp '-Dmaven.repo.local=C:/Users/ASUS/.m2/repository' `
    -f "microservices/$lot2bModule/pom.xml" '-Dmaven.test.skip=true' $lot2bGoal
  if ($LASTEXITCODE -ne 0) { throw "Packaging refusé : $lot2bModule" }
}
foreach ($lot2bModule in ($lot2bModules | Where-Object { $_ -ne 'common-security' })) {
  Get-FileHash "microservices/$lot2bModule/target/$lot2bModule-0.0.1-SNAPSHOT.jar" -Algorithm SHA256
  docker compose -p $lot2bProject --env-file $lot2bEnv -f $lot2bCompose build $lot2bModule
  if ($LASTEXITCODE -ne 0) { throw "Build refusé : $lot2bModule" }
}
```

Vérifier l'inclusion des migrations courantes dans les JAR et enregistrer les identifiants/digests des images réellement utilisées, MySQL inclus. Si le cache Maven/images manque, arrêter et traiter séparément le besoin de téléchargement ; ne pas substituer un ancien JAR pour continuer.

### Démarrage par étapes et attentes bornées

```powershell
docker compose -p $lot2bProject --env-file $lot2bEnv -f $lot2bCompose up -d --no-deps --wait --wait-timeout 180 mysql
if ($LASTEXITCODE -ne 0) { throw 'MySQL candidat non sain : arrêt' }
docker compose -p $lot2bProject --env-file $lot2bEnv -f $lot2bCompose up -d --no-deps discovery-service
if ($LASTEXITCODE -ne 0) { throw 'Discovery candidat indisponible : arrêt' }
# Attendre Eureka sur http://localhost:18761/eureka/apps, Accept: application/json.
# Puis, une commande à la fois, attendre démarrage et inscription avant la suivante :
foreach ($lot2bService in @('identity-service','gateway-service','department-service',
  'employee-service','customer-service','hr-service','payroll-service','fees-service','invoice-service')) {
  docker compose -p $lot2bProject --env-file $lot2bEnv -f $lot2bCompose up -d --no-deps $lot2bService
  if ($LASTEXITCODE -ne 0) { throw "Démarrage refusé : $lot2bService" }
  # POINT D'ARRÊT : disponibilité/inscription et migration de ce service à contrôler.
  # Attente max 180 s par service, requêtes bornées à 2 s ; pas de logs bruts.
  # Ne passer au suivant qu'après verdict positif, sinon interrompre la boucle.
  Read-Host "Confirmer le contrôle positif de $lot2bService (sinon Ctrl+C pour arrêter)" | Out-Null
}
docker compose -p $lot2bProject --env-file $lot2bEnv -f $lot2bCompose ps
```

Le `Read-Host` est une pause opérateur, pas un contrôle automatique de disponibilité. Aucun démarrage successif ne doit être confirmé sans preuve positive. Le statut running ne remplace pas les historiques Flyway, les inscriptions Eureka et les appels fonctionnels. Ne pas coller cette séquence sans suivre les étapes.

### Validation API puis navigateur

Rejouer le [protocole de scénarios](LOT_2B_SCENARIOS_VALIDATION.md) sur Gateway 18080, avec jeux synthétiques. Lire les historiques de migration depuis MySQL candidat, sans connexion au MySQL de développement. Pour Angular, ouvrir un second terminal dans la **copie** de campagne :

```powershell
Set-Location 'C:/Users/ASUS/Documents/CRM/target/crm-lot2b-20261101-140100/frontend'
$env:NODE_OPTIONS='--max-old-space-size=2048'
npm ci --offline
if ($LASTEXITCODE -ne 0) { throw 'Dépendances frontend indisponibles : arrêt' }
.\node_modules\.bin\ng.cmd serve --host 127.0.0.1 --port 14200
```

Aucun `npm ci`, build ou navigateur connecté n'a été exécuté en 2A. Si le cache est incomplet, traiter le téléchargement séparément. Ne pas lancer le frontend original sur 4200 pour cette campagne. Après les scénarios, arrêt non destructif limité au candidat, sous le contrôle de l'opérateur :

```powershell
docker compose -p $lot2bProject --env-file $lot2bEnv -f $lot2bCompose stop
```

Pas de `down -v`, prune, suppression ou nettoyage global prévu. Conserver les preuves et volumes candidats tant que leur archivage n'est pas décidé.

## 8. Précautions de sauvegarde

L'installation neuve n'utilise aucune copie de développement : elle ne nécessite ni montage ni export automatique des volumes existants. Avant 2B, vérifier qu'une sauvegarde indépendante des données importantes existe selon la procédure d'exploitation approuvée ; ce lot n'en a réalisé aucune et n'a pas inspecté les volumes.

L'upgrade constitue une **seconde campagne distincte**, sur copie sauvegardée et autorisée, idéalement anonymisée. Sauvegarder ensemble les huit schémas, leurs historiques Flyway et les fichiers/métadonnées HR cohérents ; stocker les sauvegardes hors dépôt avec accès restreint, contrôler empreintes et restaurabilité. Ne jamais faire pointer un service candidat vers l'instance de développement. Pas de baseline/repair/import automatique sur schéma existant ; sur doute de version, bloquer et examiner une copie. Ne pas annoncer de rollback DDL MySQL garanti.

## 9. Critères d'acceptation et contrôles exécutés

**Exécuté en 2A uniquement :** neuf tests locaux Node du modèle (9/9), génération sans secret de la campagne finale, checker statique réussi, inventaire et empreintes des 24 migrations, examen syntaxique de 22 YAML via SnakeYAML 2.2 déjà en cache (Java Xmx128m), contrôle des URLs de la copie Angular (aucune référence restante à `http://localhost:8080` dans ses fichiers TS), comparaison des fichiers préexistants et `git diff --check`.

La vérification YAML a neutralisé le tag Compose `!override` pour son seul parsing syntaxique : elle ne prouve **pas** la fusion/interpolation Compose. Les tests Node portent sur des objets de configuration et fichiers locaux : aucun test métier, SQL, HTTP ou E2E exécuté. Node de préparation limité à 512 MiB ; aucun package téléchargé. Aucun test Java/Angular des lots précédents n'a été relancé ; leurs résultats restent historiques.

Acceptation future 2B : isolation confirmée avant écriture, huit historiques Flyway/JPA valides, neuf clients inscrits Eureka, routage normal et refus interne, connexions/logout/révocations corrects, dernier SUPER_ADMIN préservé sous concurrence, associations uniques, espaces personnels sans accès transversal, partage documentaire explicite, paiements et statistiques cohérents, règles financières et verrous confirmés, vrai HTTPS validé avec contre-tests, quatre rôles navigateur, mémoire/OOM et preuves expurgées consignés. L'installation neuve et l'upgrade doivent être distingués dans les résultats.

## 10. Validations restant à effectuer et bilan

Restent tous les contrôles opérationnels : disponibilité Docker/versions CLI/images, certificats et secrets externes, ressources physiques, schéma Compose résolu, packaging, démarrage et enregistrement, migrations MySQL neuve/upgrade, contraintes de types et index, transactions concurrentes/rollbacks, TLS réel, routage et parcours API/navigateur. Aucun de ces résultats n'est inféré des tests statiques.

**PRÊT POUR LOT 2B** : les fichiers, l'isolation prévue, les commandes et critères d'arrêt sont préparés et les contrôles locaux passent. Cette conclusion autorise à proposer la phase contrôlée suivante, pas à démarrer maintenant ni à déclarer le CRM validé ou déployé. Un précontrôle négatif en Lot 2B impose l'état BLOQUÉ avant tout démarrage affecté.
