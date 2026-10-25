# Sprint 1B — Validation E2E : contrôle préalable

> Archive documentaire : les constats, chiffres, anciens commits et autorisations ci-dessous décrivent la campagne datée du rapport. Ils ne constituent pas un état courant ni un plan à réappliquer sur `publication-clean`. Toute opération réelle nécessite une nouvelle vérification et l’autorisation adaptée.

Date : 19 octobre 2026, Africa/Tunis. Branche : `microservices`.

**VERDICT : BLOCKED — campagne E2E non démarrée.** L'isolation du réseau et des données est prévue correctement par le générateur, mais la préservation de la disponibilité de l'environnement actif n'est pas démontrée avec la capacité mémoire actuelle. Ce verdict ne remet pas en cause les résultats isolés du lot F et ne valide aucun parcours E2E.

## 1. Environnement examiné

Documents consultés : `docs/ACCESS_TOKEN_SECURITY_PLAN.md`, `docs/SPRINT1B_LOT_F_REVIEW.md` et intégralité de `scripts/e2e/prepare-sprint1b.mjs`.

Le générateur prévoit un nouveau projet `crm-sprint1b-e2e-YYYYMMDD-HHMMSS`, un répertoire propre sous `target/`, des tags d'images propres au projet et un réseau bridge privé `isolated`. Il refuse d'écraser un répertoire existant. Il n'exécute aucune commande Docker, SQL ou HTTP.

| Ressource prévue | Isolation constatée dans le générateur |
|---|---|
| MySQL 8.0 | Données dans `/var/lib/mysql` en tmpfs ; aucun volume existant, aucun port hôte ; SQL d'initialisation monté en lecture seule |
| Données | Huit schémas initialement vides ; bootstrap `sprint1b-admin@example.test` ; identifiants E2E obligatoires, distincts des secrets réels |
| Réseau | Bridge propre au projet ; aucun réseau externe déclaré ; Eureka propre au test |
| Identity | Port prévu `127.0.0.1:18082` |
| Services métier | Ports prévus `127.0.0.1:18083` à `18089` |
| Gateway | Port prévu `127.0.0.1:18080` ; configuration copiée, CORS adapté à 14200 |
| Angular | Copie distincte, origine API 18080 ; `.env`, liens symboliques, dépendances et sorties de build exclus de la copie |
| Validation | Destination fixe `http://identity-service:8082` pour les sept consommateurs ; aucune valeur de secours ajoutée |

La vérification des ports ne retourne aucun listener sur 14200, 18080 ou 18082–18089 au moment du contrôle. Cela ne réserve pas ces ports pour une campagne ultérieure. Aucun nouveau run n'est généré : ses noms devront être revérifiés avant toute construction ou création.

Les données prévues sont synthétiques ; aucune donnée n'a été créée et aucun secret de test n'a été généré pendant cette vérification. L'image de base MySQL peut être partagée sans partager les données ; les conteneurs et stockages actifs restent exclus.

## 2. Blocage de capacité et disponibilité

Docker annonce **7 965 650 944 octets, soit 7,42 Gio**, et 12 CPU logiques. Les onze conteneurs CRM préexistants sont en cours d'exécution et utilisent environ **4 773,2 Mio, soit 4,66 Gio**, selon l'instantané `docker stats`.

| Conteneur préexistant | Mémoire observée, Mio |
|---|---:|
| crm-fees-service-1 | 454,8 |
| crm-customer-service-1 | 421,4 |
| crm-department-service-1 | 449,5 |
| crm-employee-service-1 | 506,9 |
| crm-identity-service-1 | 432,2 |
| crm-mysql-1 | 420,9 |
| crm-invoice-service-1 | 476,1 |
| crm-hr-service-1 | 454,2 |
| crm-payroll-service-1 | 425,4 |
| crm-gateway-service-1 | 382,4 |
| crm-discovery-service-1 | 349,4 |

L'hôte Windows annonce 16 067 356 Kio de mémoire physique totale et **2 388 284 Kio libres, soit 2,28 Gio**. La différence entre la capacité Docker et la somme des conteneurs ne constitue pas une mesure de mémoire réellement disponible : le moteur, les caches et les autres processus consomment aussi de la mémoire.

Le modèle E2E ajouterait dix JVM et MySQL, ainsi qu'Angular et le navigateur sur l'hôte. Il ne définit aucune limite mémoire pour ses conteneurs. Les conteneurs actifs n'ont pas non plus de plafond individuel (`HostConfig.Memory=0`, `MemorySwap=0`). Les consommations observées de la pile active ne prédisent pas exactement celles de la pile de test, mais ne permettent pas de justifier le démarrage simultané d'une seconde pile dans la marge actuelle.

**Risque concret :** contention mémoire, pagination et éventuellement arrêt par manque de mémoire dans le moteur partagé, pouvant affecter les conteneurs actifs malgré la séparation des réseaux et des bases. Aucun incident de ce type n'a été provoqué ni observé pendant ce contrôle. Le démarrage a été suspendu avant construction et création de ressources.

## 3. Commandes réellement exécutées

Contrôles de lecture et de syntaxe uniquement :

```powershell
git status --short
git -c core.quotepath=false ls-files --modified --others --exclude-standard
Get-FileHash -LiteralPath <chaque fichier local préexistant> -Algorithm SHA256
Get-Content scripts/e2e/prepare-sprint1b.mjs
Get-Content docs/SPRINT1B_LOT_F_REVIEW.md
Get-Content docs/ACCESS_TOKEN_SECURITY_PLAN.md -Encoding UTF8
Get-Content microservices/identity-service/Dockerfile
Get-Content microservices/common-security/src/main/java/com/crm/security/AccountValidationProperties.java
rg -n 'IDENTITY_VALIDATION_BASE_URL|base-url' microservices -g application.yml
node --check scripts/e2e/prepare-sprint1b.mjs
git diff --check
docker info --format '{{.MemTotal}} {{.NCPU}}'
docker ps -a --format '{{.ID}} {{.Names}} {{.Status}}'
docker network ls
docker volume ls
docker stats --no-stream --format '{{.Name}} {{.MemUsage}}'
docker inspect --format '{{.Id}}|{{.Name}}|{{.State.StartedAt}}|{{.RestartCount}}|{{.HostConfig.Memory}}|{{.HostConfig.MemorySwap}}' $(docker ps -aq)
Get-NetTCPConnection -State Listen
Get-CimInstance Win32_OperatingSystem
```

Les deux dernières commandes sont filtrées aux ports et champs décrits ci-dessus dans l'exécution. Les premières tentatives CIM dans le bac à sable ont été refusées ; les mêmes lectures ont ensuite réussi avec l'autorisation de l'outil. Aucun refus automatique d'approbation n'a été reçu. Une recherche `rg` avec un glob de chemin incompatible avec Windows a échoué ; la recherche récursive avec `-g application.yml` a été utilisée ensuite.

Le contrôle de syntaxe Node réussit. `git diff --check` retourne 0, avec des avertissements de conversion LF/CRLF préexistants. Les sept services déclarent la propriété obligatoire depuis `IDENTITY_VALIDATION_BASE_URL`. Aucun test Maven, Angular ou E2E n'est lancé dans cette campagne ; aucune validation Compose d'un nouveau run n'est revendiquée.

## 4. Images et services réellement démarrés

**Aucun.** Aucun packaging, build d'image, pull, `up`, `start`, `stop`, `pause`, `restart`, `down`, appel métier ou SQL n'a été exécuté. Les onze conteneurs énumérés sont préexistants ; ils ne sont pas des services de cette campagne.

## 5. Scénarios et résultats exacts

**0 scénario E2E exécuté, 0 réussite E2E constatée.** Les seize scénarios demandés restent non exécutés : Login synthétique, émission uid, accès autorisé, rôle insuffisant, désactivation avec ancien JWT, verrouillage, retrait de rôle, rotation refresh, panne Identity, refus 503 sans exécution métier, rétablissement, accès directs, blocage Gateway, propagation interservices, Angular 401/403/503 et PDF refusé.

Les 868 tests Java, 732 tests Angular et le build production mentionnés au lot F sont des résultats antérieurs. Ils ne sont ni réexécutés ni assimilés à des preuves E2E dans ce rapport.

## 6. Anomalies et corrections

Anomalie de préparation identifiée : absence de budget mémoire démontré pour exécuter la pile temporaire en parallèle de l'environnement actif. Aucune correction de code, configuration, concurrence, RBAC ou règle de sécurité n'est appliquée. Réduire les limites de validation, partager une base active ou arrêter des services préexistants ne constitue pas une solution utilisée ici.

## 7. Performances

Aucune mesure E2E de p50/p95/p99, de temps Identity, de nombre de validations par parcours, de saturation à quatre appels, de timeout ou de reprise n'est disponible. Les statistiques mémoire ci-dessus sont des mesures de préparation, pas un benchmark JWT. Les mesures loopback du lot F ne remplacent pas ces résultats manquants.

## 8. Ressources et nettoyage

Aucun conteneur, réseau, volume ou image temporaire n'a été créé : aucun nettoyage Docker n'est nécessaire et aucune commande de suppression n'est exécutée. Les cinq réseaux et les quatre volumes inventoriés restent préexistants, notamment `crm_crm-network`, `crm_mysql-data` et `crm-ci_mysql-data`. Le run préparé au lot F sous `target/crm-sprint1b-e2e-20261018-195154` n'est ni utilisé ni supprimé.

Le seul fichier créé par cette campagne est ce rapport. La comparaison SHA-256 finale confirme que les **173 fichiers déjà modifiés ou non suivis sont inchangés**. Le nouvel inventaire Docker retrouve les mêmes onze conteneurs en exécution, les cinq mêmes réseaux et les quatre mêmes volumes, avec les mêmes identifiants disponibles dans les inventaires. Aucun `.env`, secret, source, Compose principal ou donnée réelle n'est modifié. Aucun commit, push ou déploiement.

## 9. Conditions de reprise et risques restants

1. Fournir une capacité distincte pour la campagne, idéalement un moteur Docker ou une machine dédiée, ou démontrer un budget mémoire sûr avec des limites propres aux seuls conteneurs E2E. La mémoire libre actuelle de l'hôte ne justifie pas une simple augmentation automatique de l'allocation Docker.
2. Si une exécution séquentielle par sous-ensembles est retenue pour réduire la consommation, préparer et vérifier ce protocole avant démarrage ; documenter que ses mesures ne représentent pas une pile complète simultanée.
3. Refaire l'inventaire des ressources et ports, générer un nouveau run, vérifier tous les mounts et labels, puis construire les images depuis le code courant avec des identifiants exclusivement synthétiques.
4. Attendre une vraie readiness Identity : Login synthétique et validation interne 204, pas seulement `service_started`. Exécuter ensuite les scénarios du plan du lot F, comparer les tables synthétiques avant/après les refus et mesurer les parcours réels.
5. Pour panne et nettoyage, vérifier les labels du projet unique avant chaque action ; préserver les ressources préexistantes. N'effectuer aucun déploiement principal.

Les réserves fonctionnelles du plan restent applicables : réactivation possible d'un ancien JWT, requêtes déjà admises, dépendance Identity/base, secret HMAC partagé et transport interne HTTP. Cette campagne bloquée n'apporte aucune nouvelle garantie sur ces points.

**VERDICT FINAL : BLOCKED. Les contrôles préalables sont effectués ; l'E2E reste à exécuter dans des conditions de capacité permettant de préserver l'environnement actif. Aucun déploiement effectué.**
