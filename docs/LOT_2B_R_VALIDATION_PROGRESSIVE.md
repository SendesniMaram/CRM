# Lot 2B-R — Validation progressive adaptée aux ressources

Date : 2 novembre 2026. Branche conservée : `publication-clean`.

**PRÊT POUR VALIDATION PROGRESSIVE au niveau de la préparation.** Le découpage et les fichiers sont préparés ; aucun groupe n'a été démarré ou validé réellement. Le dernier précontrôle opérationnel reste celui du Lot 2B : 3,99 Gio libres et moteur Docker inaccessible. Cet état n'a pas été remesuré ici et ne constitue pas un feu vert de démarrage. Même le groupe A exige un nouveau précontrôle positif.

## 1. Ressources : ce qui est connu, estimé et inconnu

Les rapports [Lot 2A](LOT_2A_PREPARATION_VALIDATION.md) et [Lot 2B](LOT_2B_VALIDATION_INTEGREE.md) ont été lus. Les dépendances ont été confrontées à `EmployeeAssociationService`, `EmployeeProfileClient`, `DepartmentClient`, `CustomerIdentityDirectory`, `PersonalCustomerClient`, `PayrollEmployeeDirectoryClient`, aux services de paie/frais et au générateur existant.

La seule mesure physique disponible est **historique** : 15,32 Gio au total, 3,99 Gio libres, disque C: libre 289,47 Gio. Aucune consommation MySQL/JVM ou disponibilité Docker nouvelle n'a été mesurée. Il n'est donc pas possible d'affirmer un minimum de RSS réaliste et validé. Les budgets ci-dessous sont des limites candidates conservées du Lot 2A, pas des mesures et pas des promesses de démarrage.

| Composant | Limite candidate | Paramètres et interprétation |
|---|---:|---|
| MySQL | 1024 Mio | Buffer InnoDB 256 Mio, 100 connexions ; le buffer ne représente pas toute la mémoire du serveur. Aucune réduction arbitraire à 256/512 Mio. Minimum fonctionnel à mesurer sur migrations et fixtures réelles. |
| Discovery | 512 Mio | Heap initiale 64 Mio, maximale 256 Mio, metaspace maximale 128 Mio ; mémoire native/threads supplémentaires. |
| Gateway | 512 Mio | Mêmes bornes ; prévoir buffers Netty et mémoire native, pas uniquement la heap. |
| Identity | 768 Mio | Heap initiale 64 Mio, maximale 384 Mio, metaspace maximale 192 Mio, pool JDBC 5. |
| Chaque métier | 768 Mio | Mêmes bornes conservées ; HR peut aussi gérer fichiers, TLS et workers. Le coût réel diffère selon le service et les scénarios. |

La réduction vient du **nombre de services simultanés**, sans changer les heaps, pools, paramètres MySQL, JWT, validation Identity ou HTTPS du candidat Lot 2A. Les pointes de migration, chargement de classes et fichiers doivent être observées avant toute optimisation ultérieure. Les builds Maven/images, l'API et Angular ne doivent pas être lancés simultanément.

## 2. Groupes et dépendances réelles

Socle commun : **MySQL isolé + Discovery + Identity + Gateway**, soit quatre conteneurs. Tous les services métier conservent la validation Identity distante obligatoire. Le socle est recréé dans un projet distinct pour chaque groupe ; aucune base n'est partagée avec le développement.

| Étape | Services ajoutés au socle | Justification et périmètre |
|---|---|---|
| A — Authentification | Aucun | Login/refresh/logout, sessions, administration et dernier SUPER_ADMIN ; aucun métier simulé comme validé. |
| B — Employés | Department, Employee, HR | Association stockée dans HR ; HR vérifie Identity puis Employee. Department est conservé pour le parcours réaliste d'affectation et le nom du département, même si toutes les lectures Employee ne l'appellent pas. |
| C — Clients/factures | Customer, Invoice | Association CLIENT validée contre Identity ; Invoice résout le client propre auprès de Customer avant factures/paiements. Les annuaires Employee de fonctions CRM secondaires ne sont pas inclus : opportunités/attributions ne font pas partie des scénarios C. |
| D — RH/profil/documents | Department, Employee, HR | Même topologie que B, scénario distinct : création de fixture, propriété des documents, partage et vrai canal HTTPS HR → Employee. Aucune désactivation du TLS pour alléger le groupe. |
| E-bootstrap — Préparer la paie | Department, Employee, HR | Créer par API les comptes, employés et associations nécessaires. Le schéma payroll est créé vide à cette étape, sans service Payroll et donc sans ses migrations encore. |
| E-payroll — Bulletins | HR, Payroll | Après arrêt complet E-bootstrap, réutiliser **uniquement le même projet E**. Payroll lit l'association existante via `/api/employee-associations/me` sur HR ; Employee et Department ne sont pas requis pour cette lecture. Pas de création/réaffectation d'association ni test de profil dans cette étape. |
| E-fees — Frais | Fees | Sous-campagne indépendante. Le service ne fait pas d'appel Employee/HR dans son CRUD actuel ; un champ de référence n'est pas une dépendance de lecture personnelle. Pas de promesse de rapprochement paie/frais. |

B et D ont volontairement la même topologie : leur fusion technique ne supprimerait pas une dépendance pour les parcours retenus. Ils ont des projets/fixtures distincts pour conserver des preuves indépendantes. E a été scindé pour éviter de garder Employee, Department, Payroll et Fees tous ensemble.

## 3. Budgets estimés et critères d'arrêt

La marge hôte de **1536 Mio** et la marge d'allocation Docker de **512 Mio** sont des seuils de préparation choisis, non issus de mesures. Le seuil hôte est vérifié **avant** de commencer chaque groupe, après arrêt du précédent. L'allocation totale Docker ne prouve pas sa mémoire encore disponible : contrôler aussi les autres charges et `docker stats` lors de la future exécution.

| Étape | Conteneurs | Somme des limites | RAM hôte libre exigée avant groupe | Allocation Docker minimale candidate |
|---|---:|---:|---:|---:|
| A | 4 | 2816 Mio / 2,75 Gio | 4352 Mio / 4,25 Gio | 3328 Mio / 3,25 Gio |
| B | 7 | 5120 Mio / 5 Gio | 6656 Mio / 6,5 Gio | 5632 Mio / 5,5 Gio |
| C | 6 | 4352 Mio / 4,25 Gio | 5888 Mio / 5,75 Gio | 4864 Mio / 4,75 Gio |
| D | 7 | 5120 Mio / 5 Gio | 6656 Mio / 6,5 Gio | 5632 Mio / 5,5 Gio |
| E-bootstrap | 7 | 5120 Mio / 5 Gio | 6656 Mio / 6,5 Gio | 5632 Mio / 5,5 Gio |
| E-payroll | 6 | 4352 Mio / 4,25 Gio | 5888 Mio / 5,75 Gio | 4864 Mio / 4,75 Gio |
| E-fees | 5 | 3584 Mio / 3,5 Gio | 5120 Mio / 5 Gio | 4096 Mio / 4 Gio |

Cela peut permettre une campagne sur une machine de 16 Gio après libération volontaire de ressources, sans prétendre que 3,99 Gio libres suffisent. Ne pas arrêter automatiquement les applications utilisateur. Les critères ne reposent pas sur l'utilisation du swap comme substitut à la RAM.

Arrêter la progression si : mémoire sous le seuil avant groupe ; Docker inaccessible/insuffisant ; port occupé ; isolation douteuse ; secrets/certificats invalides ; migration/JPA échouée ; délai de disponibilité dépassé ; OOMKilled, redémarrage inattendu, pression mémoire/swap durable, ou usage d'un conteneur proche de 90 % de sa limite. Le seuil 90 % est une alerte conservatrice, pas une mesure d'OOM garantie. Réexaminer les pointes et conserver les preuves avant ajustement.

Entre deux services d'un groupe déjà partiellement démarré, surveiller la mémoire restante en tenant compte **des services restant à lancer + la marge**, plutôt que d'exiger à nouveau toute la somme initiale. Les précontrôles de début de groupe ne sont pas un monitoring continu.

Pour Angular/navigateur, ajouter environ 3 Gio de marge (Node borné à 2 Gio et navigateur) ou préparer/build le frontend séparément avant la campagne API. Ne pas annoncer les parcours navigateur validés sur la seule base des API. Utiliser une copie de sources récentes adaptée au port 18080, comme prévu au Lot 2A, jamais le frontend de développement pointant sur 8080.

## 4. Configurations et scripts préparés

| Nouveau fichier | Fonction |
|---|---|
| [prepare-progressive.mjs](../scripts/validation/prepare-progressive.mjs) | Réutilise le modèle sécurisé Lot 2A, sélectionne les services et initialise les seuls schémas utiles ; génération et contrôle statique `--check`. |
| [progressive-preflight.ps1](../scripts/validation/progressive-preflight.ps1) | Futur contrôle en lecture : modèle, RAM, disque, ports, contexte local, allocation Docker et `config --quiet`. Jamais de lancement Docker Desktop ni de conteneur. Non exécuté ici. |
| [progressive-readiness.mjs](../scripts/validation/progressive-readiness.mjs) | Futures sondes explicites `--authorized-probes`, attente bornée de l'inscription UP Eureka et route Identity pour Gateway ; sans credential ni journalisation de corps. Non exécuté contre des services réels. |
| [progressive.test.mjs](../scripts/validation/progressive.test.mjs) | Tests locaux des topologies, budgets, protections, isolation et attentes bornées avec faux HTTP injecté. |
| [LOT_2B_R_VALIDATION_PROGRESSIVE.md](LOT_2B_R_VALIDATION_PROGRESSIVE.md) | Ce rapport et protocole futur. |

Artefacts préparés sous `target/crm-lot2br-20261102-100000/`, ignorés par Git : sept fichiers `*.compose.json`, `plan.json` (projets, services, seuils et empreintes de migrations), six dossiers d'initialisation MySQL, configuration Gateway et dossier `evidence/`. Les seules écritures SQL préparées sont des créations de schémas vides ; elles n'ont pas été exécutées. Les migrations métier restent dans les JAR et seront appliquées par Flyway lors d'un futur démarrage autorisé.

Projets : suffixes `-a`, `-b`, `-c`, `-d`, `-e`, `-e-fees`. E-bootstrap et E-payroll utilisent intentionnellement le même projet `-e`, les mêmes schémas et volumes pour conserver les associations. **Il faut arrêter tout E-bootstrap avant E-payroll**, puis redémarrer le socle avec l'autre modèle. Les scripts ne démarrent pas cette transition automatiquement.

## 5. Isolation et précontrôles nécessaires

- Utiliser uniquement le Compose autonome de l'étape, avec son nom `-p` exact de `plan.json`, jamais fusionné aux Compose principaux/anciens. Aucun `container_name`, volume/réseau externe ou nom physique global ; images propres à cette campagne, communes aux groupes pour éviter des builds redondants, à condition d'en vérifier les JAR et digests récents.
- Les volumes MySQL/HR et réseaux sont préfixés par chaque projet. Aucune URL de base de développement ; MySQL non publié. Seuls 18080 et 18761 sont publiés sur loopback ; 14200 réservé au frontend éventuel. Un seul groupe actif à la fois.
- Conserver les secrets `CRM_2B_*` dédiés dans un fichier externe protégé. La présence de variables ne prouve pas leur robustesse ou leur indépendance ; vérifier leur contrat sans impression. Aucun secret créé/lu ici.
- Les étapes contenant Employee/HR gardent les paramètres HTTPS et montages de clés du Lot 1C. Exiger les fichiers réellement utilisés par chaque étape, certificats valides, SAN, alias et paire RSA cohérente. E-payroll garde son client HTTPS configuré mais ne l'appelle pas pour lire l'association ; le profil doit échouer fermé si Employee est absent.
- Vérifier le contexte local, l'espace disque Windows **et Docker**, les autres charges, les ports et les labels des conteneurs qui portent ces ports. Les sondes localhost ne prouvent pas à elles seules le projet auquel elles parlent : ce contrôle de labels est obligatoire avant les sondes.
- Vérifier le modèle Compose résolu **en mémoire** sans afficher de secrets : noms physiques préfixés, binds approuvés en lecture seule, limites, URLs MySQL, Identity et HTTPS. `config --quiet` ne vérifie pas les dates/SAN, les ACL, la présence effective de chaque clé, ni l'isolation déjà présente dans le moteur.
- À la première création d'un projet, refuser une ressource préexistante de même nom. Pour E-payroll, les ressources du seul projet E sont attendues : contrôler leur provenance E-bootstrap plutôt que les supprimer. Conserver historique Flyway/empreintes, ne pas employer baseline automatique, update, repair ou clean.

Le script de précontrôle ne remplace pas ces vérifications de certificats, secrets, labels, sauvegardes, images et charge réelle. Aucun téléchargement, build ou test E2E n'a été effectué pour cette préparation.

## 6. Démarrage futur progressif, preuves et scénarios

### Commandes de préparation déjà exécutées sans service

```powershell
$env:NODE_OPTIONS='--max-old-space-size=512'
node --test scripts/validation/progressive.test.mjs
node scripts/validation/prepare-progressive.mjs 20261102-100000
node scripts/validation/prepare-progressive.mjs --check target/crm-lot2br-20261102-100000
```

Le générateur refuse d'écraser ce dossier : choisir un nouvel identifiant libre pour une nouvelle préparation. Les fichiers préexistants ne sont pas modifiés.

### Protocole futur — aucune commande Docker ci-dessous exécutée ici

Après autorisation distincte de la campagne opérationnelle et contrôle des secrets/certificats, choisir l'étape. Packager ses modules et common-security, puis construire seulement ses images, séquentiellement, avec les limites Maven du Lot 2A. Ne pas réutiliser les images historiques. Ne pas builder pendant que le groupe tourne.

```powershell
$campaign=(Resolve-Path 'target/crm-lot2br-20261102-100000').Path
$stage='A' # B, C, D, E-bootstrap, E-payroll ou E-fees : une seule étape
$plan=Get-Content "$campaign/plan.json" -Raw | ConvertFrom-Json
$project=$plan.stages.$stage.project
$compose=Join-Path $campaign "$stage.compose.json"
# $testEnv : chemin absolu externe approuvé ; clés via CRM_PROFILE_MATERIAL_DIR.
# Ce script interrogera le moteur existant, sans démarrer Docker Desktop :
.\scripts\validation\progressive-preflight.ps1 -CampaignDirectory $campaign -Stage $stage -TestEnvFile $testEnv
if (-not $?) { throw 'Précontrôle refusé' }
# Compléter les vérifications manuelles du §5 avant toute écriture.

docker compose -p $project --env-file $testEnv -f $compose up -d --no-deps --wait --wait-timeout 180 mysql
if ($LASTEXITCODE -ne 0) { throw 'MySQL non sain : arrêter la progression' }
foreach ($service in ($plan.stages.$stage.services | Where-Object { $_ -ne 'mysql' })) {
  # Avant chaque étape : mémoire restante/charge et verdict positif de la précédente.
  docker compose -p $project --env-file $testEnv -f $compose up -d --no-deps $service
  if ($LASTEXITCODE -ne 0) { throw "Démarrage refusé : $service" }
  node scripts/validation/progressive-readiness.mjs --authorized-probes $campaign $stage $service
  if ($LASTEXITCODE -ne 0) { throw "Disponibilité refusée : $service" }
  # Examiner migration/JPA et santé/mémoire de ce service avant de continuer.
  # Inscription Eureka et route publique ne prouvent pas les scénarios métier.
}
```

L'ordre est MySQL, Discovery, Identity, Gateway puis les métiers déclarés. MySQL est contrôlé par SELECT 1 authentifié, Discovery par sa réponse Eureka, chaque autre service par inscription UP (et Gateway par `/identity/api/test`). Les sondes ont une attente de 180 secondes, requêtes de deux secondes maximum. Il faut compléter par les historiques Flyway, état/OOM des conteneurs et essais métier avant de considérer un service validé. La boucle est un protocole opérateur : ne pas la lancer sans ces points d'arrêt.

| Groupe | Scénarios exécutables après disponibilité et fixtures synthétiques |
|---|---|
| A | Login/refresh/logout idempotent, UID/sid, révocation sur rôle/statut, ancien JWT, dernier SUPER_ADMIN, transactions administratives concurrentes ; refus 401/403/503. Pas de preuve de révocation dans les sept métiers à cette étape. |
| B | CRUD employé/département, références, association EMPLOYEE unique et idempotente, mauvais rôle/compte inactif, conflits concurrents, association propre UID, refus transversal et erreurs d'annuaire. |
| C | Association CLIENT, profil, factures/PDF/paiements propres, absence de notes internes, statistiques Lot 1A, validations financières Invoice, paiements partiel/complet, édition interdite et concurrence de verrouillage. |
| D | Profil personnel et vrai TLS HR→Employee avec contre-tests de certificat/clé/hôte, propriété congés/documents, partage/retrait, aucun accès aux pièces privées/étrangères et absence de fuite de stockage. |
| E-bootstrap | Préparer E1/E2, associations et département par API ; conserver IDs et données synthétiques, sans secrets dans les preuves. Vérifier les associations avant arrêt. |
| E-payroll | Première migration Payroll, bulletins publiés propres, refus cross-account/non publiés, validations financières et périodes, erreurs HR/Identity ; aucune preuve de création d'association dans cette étape seule. |
| E-fees | CRUD/statistiques/filtres Frais et RBAC existants, disponibilité Identity et confidentialité des erreurs ; pas de prétendu rapprochement avec la paie. |

Réutiliser les assertions détaillées de [LOT_2B_SCENARIOS_VALIDATION.md](LOT_2B_SCENARIOS_VALIDATION.md) en marquant explicitement les scénarios hors groupe NON TESTÉS. Chaque groupe doit tester les rôles réellement autorisés/interdits ; ne pas déduire une révocation d'un 403 qui existait déjà avant l'opération.

### Arrêt non destructif et preuves

```powershell
# À exécuter pour le groupe courant avant tout groupe suivant :
docker compose -p $project --env-file $testEnv -f $compose stop
if ($LASTEXITCODE -ne 0) { throw 'Arrêt non confirmé : ne pas démarrer le groupe suivant' }
docker compose -p $project --env-file $testEnv -f $compose ps -a
docker ps --filter "label=com.docker.compose.project=$project" --format '{{.Names}}'
```

Vérifier zéro conteneur candidat encore actif et libération des ports. Ne pas lancer deux groupes en parallèle. Aucun down -v, prune ou suppression prévu. Volumes, réseaux et données synthétiques restent conservés, notamment E-bootstrap → E-payroll. Un arrêt ne rend pas les anciennes instances Eureka instantanément absentes : après redémarrage, confirmer les seuls services attendus et éviter les anciennes inscriptions transitoires.

Conserver sous `evidence/<étape>/` : chronologie, contrôles mémoire avant/après chaque démarrage et scénario, pics `docker stats`, OOM/restarts, version/digest des images et JAR, empreintes et historiques Flyway, cas/attendu/observé/verdict. Le dossier est préparé mais aucun résultat d'intégration n'y est inventé. Les statuts sont VALIDÉ, ÉCHEC, BLOQUÉ ou NON TESTÉ, par cas et par groupe. Pas de bearer, refresh token, secret, corps personnel, clé privée, HAR ou logs bruts dans les preuves partagées.

## 7. Limites et validations nécessitant encore une pile complète

La somme des succès isolés ne constitue pas une validation simultanée de l'architecture. Les projets séparés n'ont pas de fixture commune implicite ; même A/B/C/D réussis séparément ne prouvent pas un parcours global sur la même installation.

Restent nécessaires sur pile complète, ou sur un environnement plus doté : concurrence et charge entre tous les services, limites globales MySQL/pools et mémoire, neuf inscriptions Eureka simultanées, révocation d'une même session sur les sept métiers ensemble, routage/CORS complets, dashboard multi-domaine et navigation continue des quatre rôles avec tous les modules actifs, résilience en cascade, durée et pression de stockage, et upgrade coordonné de l'ensemble. Un upgrade se teste toujours sur copie autorisée/sauvegardée, jamais sur développement ; les DDL MySQL ne garantissent pas un rollback global.

Les résultats TLS ne sont acquis que dans D (ou un autre groupe où le trajet est réellement testé), pas parce que la configuration est présente. E-payroll n'établit pas la disponibilité Employee, volontairement absent. Les économies mémoire de ce découpage ne sont pas des mesures de production ni une recommandation de déploiement.

## 8. Contrôles réalisés et bilan

Exécutés uniquement : **9/9 tests locaux Node**, génération des sept modèles, contrôle statique des budgets/modèles/schémas/empreintes de migrations, analyse syntaxique PowerShell sans exécution et contrôle Git. Les tests de readiness utilisent des faux appels HTTP injectés, sans socket ni E2E. Les configurations reprennent les protections du Lot 2A ; aucun fichier préexistant, migration, secret ou donnée métier modifié. Aucun Docker CLI, Docker Desktop, MySQL, microservice, Flyway ou E2E lancé pendant ce lot.

Les **159 fichiers modifiés/non suivis préexistants** ont été comparés par SHA-256 : aucun changement ni disparition. Les cinq nouveaux fichiers sont ceux de l'inventaire du §4. `git diff --check` réussi, branche `publication-clean` conservée, aucun staging, commit ou push.

La mémoire et l'accessibilité Docker restent à recontrôler : leur dernier état est bloquant. Les seuils choisis peuvent encore devoir être ajustés sur preuves réelles, sans annoncer de succès si une dépendance/scénario est retiré.

**PRÊT POUR VALIDATION PROGRESSIVE — préparation terminée ; exécution conditionnée aux précontrôles positifs et à l'autorisation opérationnelle. Aucun succès d'intégration annoncé.**
