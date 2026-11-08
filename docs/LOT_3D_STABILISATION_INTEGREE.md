# Lot 3D — Stabilisation fonctionnelle et validation intégrée du CRM

Date : 5 novembre 2026. Branche conservée : publication-clean.

**Verdict : BLOQUÉ pour la validation intégrée réelle.** Les vérifications locales passent après correction d’une attente de test obsolète. Le CRM n’est pas déclaré validé ou prêt à déployer.

## 1. État initial et précontrôles

Les rapports [3C](LOT_3C_FINALISATION_RH.md), [3B](LOT_3B_FINALISATION_PAIE.md), [3A](LOT_3A_AUDIT_CONFORMITE_FONCTIONNELLE.md) et [2B-R](LOT_2B_R_VALIDATION_PROGRESSIVE.md) ont été consultés. Ils décrivent des développements et tests locaux, avec intégration réelle encore non démontrée.

Git a été inspecté avant modification. Une photographie SHA-256 de **1 021 fichiers suivis/non suivis préexistants** a été conservée sous target/lot3d, hors fichiers de secrets et clés exclus de la lecture. Le seul fichier préexistant modifié dans ce lot est le test [home.component.spec.ts](../frontend/src/app/features/home/home.component.spec.ts). Les 1 020 autres fichiers photographiés sont inchangés ; aucune disparition. Aucun changement de branche, staging, commit ou push.

Les mesures opérationnelles suivantes ont été reprises avec l’accès en lecture nécessaire après un refus initial du sandbox. Elles concernent l’instant du précontrôle, pas une réservation de ressources.

| Précontrôle réellement effectué | Résultat | Interprétation |
|---|---|---|
| RAM physique totale | 15,32 Gio | Poste de classe 16 Gio |
| RAM physique libre | **3,06 Gio — BLOQUÉ** | Le groupe A exige 4,25 Gio libres, soit un manque d’environ 1,19 Gio ; B/D exigent 6,5 Gio |
| Disque hôte C: | **289,93 Gio libres — VALIDÉ localement** | Au-dessus de la marge de 20 Gio prévue ; disque interne Docker non inspecté |
| Ports TCP 18080, 18761, 14200 | Aucun listener — VALIDÉ à la mesure | À renouveler avant démarrage |
| Contexte Docker sélectionné | desktop-linux | Aucun changement de contexte effectué |
| Moteur Docker | **BLOQUÉ** | docker info sort avec le code 1 : pipe dockerDesktopLinuxEngine introuvable |
| Mémoire attribuée à Docker | INCONNUE | Le 0 produit lors de cet échec n’est pas une allocation mémoire mesurée |
| CLI Compose | Version 5.5.0 | Présence du CLI, pas preuve de modèle résolu ou de moteur opérationnel |
| Images, conteneurs, volumes et réseaux réels | NON TESTÉS | Le moteur est inaccessible ; aucune ressource existante montée ou supprimée |
| Secrets et matériel HTTPS de validation | NON TESTÉS | Aucun chemin externe approuvé fourni dans cette demande ; aucune recherche large ni modification. Leur absence sur la machine n’est pas affirmée |

Ces deux blocages empêchent le démarrage même du socle A. Docker Desktop, MySQL et les services candidats n’ont pas été lancés. Aucun processus utilisateur n’a été arrêté pour libérer des ressources.

## 2. Tests réellement exécutés

Les tests ont été exécutés séquentiellement, avec cache Maven local et mode hors ligne. Maven est borné à 512 Mio de heap et les JVM de tests à 384 Mio. Les tests Angular utilisent Node limité à 1 536 Mio, deux workers et ChromeHeadless. Les tests de préparation Node sont limités à 512 Mio. Aucun build lourd Java et Angular simultané.

### Backend local — 224 cas réussis

| Module | Cas réussis | Suites sélectionnées |
|---|---:|---|
| payroll-service | 75 | PayrollFinancialPolicyTest, PayrollVariableCalculationTest, PayrollSettlementPolicyTest, PayrollTechnicalExportTest, PayrollPdfTest, SqlErrorPrivacyTest, PayrollSettlementSecurityTest, PayrollPersonalOwnershipPolicyTest, PayrollVariablePdfTest |
| hr-service | 103 | LeaveBalanceCalculationTest, LeaveBalanceOwnershipTest, HrStatisticsPolicyTest, TrainingAssignmentPolicyTest, LeaveCalendarPolicyTest, HrInsightsSecurityTest, EmployeeContractServiceTest, EmployeeDossierServiceTest, PersonalEvaluationServiceTest, service.PersonalDocumentPolicyTest, service.DocumentStoragePolicyTest, service.EmployeeProfileServiceTest, SqlErrorPrivacyTest, HrAggregateFixtureTest |
| identity-service | 15 | SessionLifecyclePolicyTest, UsableSuperAdminPolicyTest, SqlErrorPrivacyTest |
| customer-service | 10 | CustomerAssociationCreationPolicyTest, SqlErrorPrivacyTest |
| invoice-service | 21 | InvoiceFinancialPolicyTest, PersonalPaymentPolicyTest, ClientStatisticsContractTest, SqlErrorPrivacyTest |
| **Total distinct** | **224** | Résultats de ce lot, sans addition des décomptes historiques |

Il s’agit de tests unitaires, de services avec dépôts/annuaires simulés, de MockMvc avec filtres réels et de contenu PDF. Le test PDF de paie utilise une tranche WebMvcTest sans base ni serveur réseau. Aucun SpringBootTest avec base lancé.

Les **103 cas RH incluent cinq cas H2** sur de petites tables synthétiques en mémoire. Les requêtes sont lues depuis les annotations du dépôt, mais DATE_ADD est adapté en DATEADD et la CTE récursive utilise des dates de fixture littérales. Cette limite 3C subsiste : ni le SQL MySQL exact, ni ses paramètres JDBC, ni les projections JPA ne sont validés par ces cinq cas.

### Angular local — 1 155 cas réussis après correction

La sélection complète src/app/**/*.spec.ts a été exécutée. Premier passage : **1 152 réussis et trois échecs** dans HomeComponent. Après le correctif décrit au §3 : **1 155/1 155 réussis**, sans échec résiduel dans cette sélection. Les deux passages ne sont pas additionnés.

Cette sélection inclut les modules d’authentification, navigation, employés, départements, clients, RH, paie, facturation, honoraires, documents, espaces personnels et dashboards. Les services HTTP sont simulés ; ChromeHeadless n’était pas connecté à un backend CRM réel.

### Préparation de campagne — 18 tests Node réussis

Les tests de prepare-lot2b.test.mjs et progressive.test.mjs passent : **18/18**. Ils vérifient des invariants de modèles, isolation prévue, budgets, empreintes et sondes avec faux HTTP. Aucune sonde de service réel.

Un nouveau dossier autonome a été généré sans secret ni démarrage : **target/crm-lot2br-20261105-160500/**. Les sept modèles et leurs manifestes actuels passent le checker statique. Les artefacts anciens sont conservés. Les scripts et configurations versionnables du Lot 2B-R n’ont pas été modifiés.

Cette préparation ne valide pas l’existence physique de volumes isolés, les images, les binds HTTPS, les credentials ou la configuration Compose interpolée. Aucun docker compose config résolu avec secrets, build d’image ou up effectué.

### Commandes et preuves

Les sélections Maven du tableau ont été lancées module par module avec le contrat suivant, en remplaçant MODULE et SELECTION par les valeurs du tableau :

    mvn -o -B -ntp -Dmaven.repo.local=C:/Users/ASUS/.m2/repository -f microservices/MODULE/pom.xml -Dtest=SELECTION -DargLine=-Xmx384m test

Depuis frontend, avec CHROME_BIN configuré et NODE_OPTIONS=--max-old-space-size=1536 :

    .\node_modules\.bin\ng.cmd test --no-watch --browsers=ChromeHeadless --karma-config=karma.ci.conf.cjs --include=src/app/**/*.spec.ts

Préparation locale uniquement :

    node --test --test-concurrency=1 scripts/validation/prepare-lot2b.test.mjs scripts/validation/progressive.test.mjs
    node scripts/validation/prepare-progressive.mjs 20261105-160500
    node scripts/validation/prepare-progressive.mjs --check target/crm-lot2br-20261105-160500

Preuves expurgées : target/lot3d/preflight.json, results-sanitized.json, migrations.json et inventaire de préservation. Les sorties brutes de frameworks restent des auxiliaires ignorés par Git et ne sont pas destinées à être publiées. git diff --check réussi.

## 3. Anomalies identifiées et corrigées

**Corrigé : attente de navigation obsolète dans le test d’accueil.** Le test attendait encore 20 cartes ADMIN/SUPER_ADMIN et cinq cartes EMPLOYEE. Le code courant affiche 21 et six cartes, avec Pilotage RH et Mes documents déjà introduits. Les trois échecs ont été reproduits dans la suite Angular complète.

Le test vérifie désormais les **routes exactes autorisées par rôle**, incluant /hr-insights pour les administrateurs et /my-documents pour EMPLOYEE. Il continue à exclure les routes non prévues pour CLIENT et EMPLOYEE. Cette assertion est plus précise qu’un simple nombre de cartes. Aucun droit de navigation ou contrôle serveur n’a été affaibli.

Le correctif porte exclusivement sur [home.component.spec.ts](../frontend/src/app/features/home/home.component.spec.ts). Aucun comportement métier de production n’a été modifié pour faire passer les tests. Les 1 155 cas passent après correction.

**Identifiés, non corrigés automatiquement :** RAM insuffisante et moteur Docker inaccessible. Leur correction exige une évolution réelle de l’environnement ; aucun succès réseau n’est déduit des tests locaux.

Aucune anomalie MySQL, migration ou communication interservices n’a pu être reproduite pendant ce lot. Les anomalies/risques métiers déjà signalés par 3A, notamment bornes Fees et suppression de contacts avec historique, ne sont pas considérés comme résolus par cette campagne locale.

## 4. Résultats des parcours métier

Tous les parcours réels ci-dessous restent **NON VALIDÉS EN INTÉGRATION**, avec exécution BLOQUÉE par les précontrôles. La colonne locale décrit uniquement ce qui a effectivement été testé.

| Parcours demandé | Preuve locale obtenue | Résultat avec backend/base réels |
|---|---|---|
| Authentification et autorisations | Sessions/révocations/dernier SUPER_ADMIN avec dépôts simulés ; filtres et tests Angular de rôles | NON TESTÉ : login/refresh/logout, Identity et révocation réseau |
| Employés et départements | Tests Angular de formulaires/services/pages et permissions ; aucune nouvelle recette CRUD backend réelle | NON TESTÉ : persistance et références interservices |
| Clients | Neuf cas de création d’association, confidentialité d’erreurs et tests Angular | NON TESTÉ : CRUD/association avec Identity et MySQL |
| Congés et calendrier | Calculs, propriété, limites, mois/filtres, cas historiques et affichage Angular | NON TESTÉ : requêtes/JPA MySQL, décisions concurrentes et navigateur connecté |
| Formations et affectations | Références et erreurs distantes simulées, bearer, conservation historique, formulaire/sélecteur | NON TESTÉ : Employee/Identity réels et sauvegarde MySQL |
| Paie, règlements et PDF | 75 cas du Lot 3B réexécutés ; tests Angular inclus dans la sélection complète | NON TESTÉ : verrous, ledger, corrections, PDF via Gateway et base réelle |
| Facturation et paiements CLIENT | Règles financières, propriété personnelle et contrat statistique testés localement | NON TESTÉ : séquence, paiements concurrents, agrégats et routage MySQL/Gateway |
| Honoraires / frais | Tests Angular du module existant ; pas de nouvelle recette financière SQL Fees | NON TESTÉ : calcul et stockage réels, statistiques, références |
| Dashboards et navigation | Suite Angular complète, routes exactes d’accueil par rôle, agrégats RH H2 adaptés et contrat CLIENT local | NON TESTÉ : données multiservice réelles et continuité des quatre rôles |

Aucun parcours connecté SUPER_ADMIN, ADMIN, EMPLOYEE ou CLIENT n’a été exécuté. Les erreurs, chargements et permissions vérifiés dans les specs Angular ne prouvent pas le comportement avec les services en réseau.

## 5. État des migrations et de MySQL

Inventaire de **25 migrations dans huit domaines**, avec contrôle des versions distinctes, présence de V1 et SHA-256. Aucune dérive par rapport aux migrations présentes au début du Lot 3C ; aucune migration historique modifiée.

| Domaine | Nombre | Contrôle statique |
|---|---:|---|
| customer-service | 5 | V1 présente ; versions distinctes et empreintes contrôlées |
| department-service | 1 | V1 présente ; versions distinctes et empreintes contrôlées |
| employee-service | 1 | V1 présente ; versions distinctes et empreintes contrôlées |
| fees-service | 1 | V1 présente ; versions distinctes et empreintes contrôlées |
| hr-service | 6 | V1 présente ; versions distinctes et empreintes contrôlées |
| identity-service | 2 | V1 présente ; versions distinctes et empreintes contrôlées |
| invoice-service | 6 | V1 présente ; versions distinctes et empreintes contrôlées |
| payroll-service | 3 | V1 présente ; versions distinctes et empreintes contrôlées |

La migration additive Payroll V20261102_01__payroll_variables_and_settlements.sql est incluse dans ce total et dans le nouveau plan de campagne. Les anciennes préparations antérieures au Lot 3B ne doivent pas être utilisées comme preuve de contenu actuel.

**MySQL 8 : NON TESTÉ.** Aucune instance isolée n’a été créée ou jointe. Aucun historique flyway_schema_history consulté, aucun DDL métier ou Flyway exécuté, aucun baseline/repair/clean/update employé. Les empreintes de fichiers ne remplacent pas les checksums et historiques Flyway.

Restent non éprouvés : migration neuve et upgrade, BOOLEAN/TIMESTAMP et types Hibernate, collations, index/clé étrangère/contraintes, séquences, paramètres JDBC, projections JPA, CTE native RH, isolation et rollback/concurrence. H2 adapté n’est pas une preuve de compatibilité MySQL.

## 6. État des microservices et de l’environnement candidat

| Composant | État de validation réelle en 3D |
|---|---|
| MySQL candidat | Non créé/démarré ; moteur Docker inaccessible |
| Discovery/Eureka | Aucun démarrage candidat ni appel réel ; inscriptions non inspectées |
| Gateway | Aucun routage réel, CORS ou refus de namespace testé en réseau |
| Identity | Tests locaux réussis ; aucun service candidat ni authentification réseau |
| Department et Employee | Aucun service candidat ni lecture interservices réelle |
| Customer | Tests locaux ciblés ; aucune instance candidate ou recette SQL |
| HR | Tests locaux ciblés/H2 ; aucune instance candidate ou liaison HTTPS réelle |
| Payroll | Tests locaux ciblés ; aucune instance candidate ou migration réelle |
| Fees | Aucune instance candidate ; parcours réel non exécuté |
| Invoice | Tests locaux ciblés ; aucune instance candidate ou transaction SQL réelle |
| Frontend connecté | Non démarré sur 14200 ; tests unitaires ChromeHeadless uniquement |

Ces lignes décrivent les actions du Lot 3D, pas l’état supposé d’éventuels services de développement. Les ressources du moteur et les services externes n’ont pas été inspectés. Aucune connexion au MySQL de développement et aucune réutilisation de ses volumes.

Le modèle statique du groupe A cible le projet crm-lot2br-20261105-160500-a et des volumes/réseau préfixés par projet, sans base de développement ni MySQL publié. Avant toute création réelle, il faudra prouver l’absence de collision de ressources et l’isolation du Compose résolu. Aucun nom de projet n’a été créé dans Docker pendant ce lot.

Les JAR de déploiement et images de campagne n’ont pas été packagés/construits. Maven a compilé ou vérifié les classes utiles aux tests des cinq modules concernés ; cela n’atteste pas le contenu d’une image historique. Le build Angular de production 3C reste un résultat historique, non relancé dans 3D ; aucune publication de frontend.

## 7. Fonctionnalités validées et non validées

**PASS local, dans la portée indiquée :** 224 tests Java sélectionnés, 1 155 tests Angular, 18 tests Node, checker statique du nouveau plan, inventaire des migrations et préservation des sources.

**NON VALIDÉES en intégration :** l’ensemble des parcours du §4, les huit schémas MySQL/Flyway, les dix services Spring simultanés ou par groupes réels, les paramètres SQL/JPA natifs, HTTPS et les quatre rôles connectés.

Les 75 cas Payroll et 103 cas HR ont été réexécutés dans ce lot ; ils ne sont pas additionnés une seconde fois au total 224. Les 1 155 tests Angular incluent les specs des Lots 3B/3C ; les anciens totaux 101 et 217 ne s’ajoutent pas à ce nombre.

Le rapport courant ne requalifie pas les matrices de conformité des Lots 3A/3B/3C. Le solde calendaire n’est toujours pas légal, le CSV Payroll n’est toujours pas un journal comptable officiel et aucune déclaration sociale/fiscale n’est inventée.

## 8. Risques restants et conditions de reprise

1. **Ressources :** rendre disponibles au moins 4,25 Gio libres pour commencer A ; 6,5 Gio avant B/D, selon les limites existantes. Vérifier aussi mémoire effectivement attribuée/disponible à Docker et disque de sa VM. La marge navigateur prévue par 2B-R reste nécessaire pour une campagne connectée. Ne pas abaisser silencieusement les seuils ni arrêter les applications utilisateur.
2. **Docker :** rendre le moteur local accessible puis refaire les contrôles de contexte, allocation, charge, ports et collisions de ressources. La valeur libre de 3,06 Gio est la mesure initiale du lot et doit être renouvelée.
3. **Matériel de test :** disposer des chemins externes approuvés des secrets dédiés et des certificats HTTPS. Vérifier présence, contrats, ACL, validité/SAN/chaîne et correspondance des clés sans afficher leurs valeurs. Aucun défaut de certificat ou absence physique de secret n’est affirmé ici.
4. **Candidat réel :** résoudre et vérifier Compose en mémoire sans publier de secrets ; packager les JAR actuels, contrôler les migrations embarquées, construire les images séquentiellement et relever leurs identifiants. Vérifier chaque service avant le suivant selon 2B-R.
5. **Base et sécurité :** exercer MySQL neuf, verrous/concurrence/rollbacks, Identity/JWT et communications réelles. Réserver l’upgrade à une copie sauvegardée et autorisée ; les DDL MySQL ne garantissent pas un rollback global. Aucun essai sur les données de développement.
6. **Métier et navigateur :** rejouer les neuf familles du §4, avec données synthétiques et quatre comptes, puis vérifier formulaires, erreurs, chargements, refus croisés et cohérence des tableaux de bord. Les risques 3A et décisions légales/métier non résolus demeurent dans la feuille de route.

Les fichiers et empreintes des Lots 3B/3C et les scripts/configurations 2B-R sont conservés. Le seul correctif versionnable est le spec d’accueil ; le seul nouveau fichier versionnable est ce rapport. La préparation candidate et les preuves restent sous target, ignoré par Git.

## 9. Verdict final

**BLOQUÉ — validation intégrée réelle empêchée par la RAM libre insuffisante au précontrôle et le moteur Docker local inaccessible.**

La non-régression locale est réussie après correction du test d’accueil. Aucun échec résiduel n’est observé dans les sélections finales exécutées. Cela ne remplit pas les critères de PASS intégré : MySQL, Gateway, Identity/services en réseau, HTTPS et navigateur connecté ne sont pas validés. Aucune version candidate n’est déclarée certifiée ou déployée.
