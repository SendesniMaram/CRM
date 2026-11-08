# Lot 3I — Validation globale, stabilisation et commit conditionnel du CRM

Date : 7 novembre 2026. Branche : `publication-clean`.

## 1. Résumé exécutif

**NO-GO pour le commit : le contrôle final de l’index échoue sur du formatage préexistant, dont les huit migrations V1 à préserver. Les tests locaux passent ; validation intégrée réelle NON RÉALISÉE.**

Les suites complètes des onze modules Java demandés réussissent : **1 724 tests dans 165 classes, zéro échec, erreur ou test ignoré**. La suite Angular complète réussit : **1 254/1 254**. Le build production et les vérifications TypeScript app/spec réussissent. Les 51 tests Node des outils de préparation/CI passent également.

Les premiers passages globaux ont révélé des tests anciens non alignés sur les contrats 3B/3E/3F et une incompatibilité de syntaxe des migrations MySQL avec H2. Les corrections sont limitées aux tests et à la configuration H2 de test. Aucun comportement de production, migration originale, secret ou donnée existante n’est modifié par 3I.

Le périmètre proposé couvre les développements accumulés et leurs dépendances, dont les sécurisations et associations antérieures, ainsi que ce rapport. Aucun commit n’a été créé : l’indexation temporaire a été retirée après le contrôle final non passant. Tous les fichiers de travail sont conservés ; b.png reste exclu du périmètre proposé. Aucun push.

## 2. Branche et état Git initial

Contrôles réellement effectués avant modification : `git status --short --branch`, branche active, index, `git diff --stat` et `git diff --cached --stat`.

| Élément initial | Constat |
|---|---|
| Branche | publication-clean, un commit local d’avance sur la référence origin déjà présente |
| Fichiers suivis modifiés | 198 |
| Fichiers non suivis, inventaire détaillé | 217 |
| Suppressions | 0 |
| Index préexistant | Vide |
| Photographie SHA-256 | 1 123 fichiers suivis/non suivis, hors secrets et clés exclus de la lecture |

La photographie et les états initiaux sont conservés sous `target/lot3i`, ignoré par Git. Aucun changement de branche, stash, reset --hard, restore, checkout de fichiers, clean ou suppression de travail préexistant. Après le NO-GO, un reset limité aux seuls chemins temporairement indexés par 3I a rétabli l’index initial vide ; il n’a modifié ni fichier de travail ni historique. Le commit local qui existait avant cette campagne n’est ni amendé ni réécrit.

## 3. Inventaire des changements

Les changements accumulés concernent l’authentification et ses tests, les associations personnelles, les interfaces et services des lots 3B–3G, leurs migrations additives, les rapports et les modèles/scripts de préparation. Les dépendances 1A/1B/1C et les préparations 2B sont incluses lorsqu’elles sont nécessaires au dépôt testé ; il ne s’agit pas d’un diff limité au début de 3G.

Pendant 3I : **huit fichiers préexistants de tests/configuration de test modifiés**, trois auxiliaires Java de copie H2 ajoutés et ce rapport créé. Les **1 115 autres fichiers photographiés sont inchangés**, sans disparition. Les sources de production et les 28 migrations présentes au début de 3I sont inchangées par empreinte.

| Fichier modifié en 3I | Correction |
|---|---|
| CustomerPersonalIntegrationTest | Migrations sur copies explicitement adaptées à H2 |
| CustomerServiceIntegrationTest | Messages 404 conformes au contrat expurgé actuel |
| SalesMigrationTest | Copies H2, assertions des tables 3F et test de l’adaptation |
| FeesServiceIntegrationTest | Montant global sans devise attendu null ; messages 404 actuels |
| QuoteMigrationTest | Copies H2 de tout l’historique et contrôle des nouvelles tables Invoice |
| Invoice src/test/resources/application.yml | Hibernate crée les tables des tests CRUD H2 ; les migrations ont leur test distinct |
| PayrollPublicationMigrationTest | Copies H2 de tout l’historique, conservation des assertions historiques |
| PayrollServiceIntegrationTest | Saisie PENDING, refus de dépublication/édition/suppression après publication, messages 404 actuels |

Les trois nouveaux `H2MigrationCopies.java` sont exclusivement sous `src/test/java` de Customer, Payroll et Invoice. Le document CDC déjà fourni est une spécification source, conservée par empreinte et incluse dans la documentation ; `b.png` est conservé sur disque et exclu du commit.

## 4. Audit architecture

| Périmètre inspecté | Résultat local et limite |
|---|---|
| Maven | Onze modules Java 21 / Spring Boot 3.3.6 ; Spring Cloud 2023.0.2 pour les services concernés ; compilation et tests réussis |
| common-security | Testé puis installé dans le cache local avec ses tests, avant les services qui le consomment |
| Discovery/Gateway | Noms de services, routes explicites, StripPrefix et protection des chemins internes cohérents avec les contrôleurs inspectés ; tests Gateway complets réussis |
| Bases | Huit URL/schémas de domaine distincts ; aucune jointure SQL interservices ajoutée |
| Contrats personnels | UID authentifié, association HR ou Customer puis contrôle de propriété ; absence de repli sur identité choisie par Angular |
| Références distantes | Contrôle de réponse/ID, bearer et erreurs fermées dans les clients inspectés ; délais et refus des redirections pour les nouvelles lectures |
| Fees–Invoice | Sources immuables, client/devise explicites ; registre Invoice et reprises ; aucune transaction distribuée prétendue |
| Configuration | Syntaxe et clés uniques de 14 YAML contrôlées ; six scripts Node et un PowerShell analysés sans exécution opérationnelle |

Les modèles Compose sont analysés comme données YAML, en conservant leurs tags spécifiques tels que `!override`. Ils ne sont pas interpolés avec des secrets ni validés par un moteur Compose en fonctionnement. Les 51 tests Node couvrent notamment isolation prévue, budgets, empreintes et sondes avec faux HTTP.

Le test GatewaySecurityRoutingTest démarre un **Gateway de test embarqué sur port éphémère**, avec annuaire synthétique et petit serveur aval synthétique, Eureka externe désactivé. Il vérifie réellement du routage local et des refus, mais ne connecte aucun Identity/Employee/Customer CRM réel. Aucun service de développement ou candidat n’est lancé comme application autonome.

## 5. Audit sécurité

Contrôles inspectés et exercés : validation du JWT par Identity, sid/révocation, principal UID typé, refus 401/403/503, protection du dernier SUPER_ADMIN, restrictions de promotion et gestion des comptes privilégiés. Les suites Identity et common-security passent intégralement.

Les chaînes administratives et personnelles demeurent distinctes. Les espaces EMPLOYEE/CLIENT n’obtiennent pas d’accès global aux registres RH, Paie, Contacts, Honoraires ou Facturation. Les détails, documents et historiques vérifient le propriétaire côté serveur ; les filtres Angular ne servent pas de preuve d’identité. Les chemins internes sont bloqués par Gateway, y compris les variantes encodées et les réécritures testées. Le canal interne Employee exige les preuves TLS et le jeton de service prévus, sans repli anonyme.

Les réponses sensibles ajoutées utilisent no-store et des DTO réduits. Les nouvelles captures PDF partagées excluent les notes internes. Les erreurs de persistance et réponses distantes restent expurgées. Les tests de disponibilité ne remplacent pas une erreur distante par une autorisation ou un faux résultat vide.

Le contrôle des fichiers candidats ne trouve ni clé privée incorporée, ni jeton GitHub/AWS ou JWT littéral suspect. Les paramètres réels et fichiers de clés restent exclus de la lecture et de l’index. Les valeurs synthétiques explicitement réservées aux tests ne sont pas présentées comme des credentials de production. Le seul signal initial de « clé privée » était une chaîne de vérification du format PEM dans ServiceTokenConfiguration, sans matériel cryptographique incorporé.

Cette inspection et ces recherches ne constituent ni audit de pénétration exhaustif ni recherche de vulnérabilités des dépendances. Les transports et révocations multiservices réels restent à éprouver.

## 6. Audit données et migrations

**28 migrations inventoriées dans huit historiques**, V1 présente partout, versions distinctes par service et empreintes identiques à la photographie initiale et au relevé 3G. Aucun DROP/TRUNCATE/DELETE de données détecté dans les migrations contrôlées. Les évolutions récentes sont additives ; l’ancien élargissement INT de la séquence Invoice reste conservé.

| Historique | Migrations |
|---|---:|
| Customer | 6 |
| Department | 1 |
| Employee | 1 |
| Fees | 2 |
| HR | 6 |
| Identity | 2 |
| Invoice | 7 |
| Payroll | 3 |
| **Total** | **28** |

Inspection particulière des migrations 3B, 3E, 3F et 3G : types DECIMAL, colonnes exactes, flags de publication/archive, contraintes uniques d’UUID/source, FK locales, ordre de création et index. Les modèles JPA, projections, verrous et limites financières ont été confrontés à ces structures et aux tests disponibles. Aucune migration originale n’est modifiée pour satisfaire H2.

**Flyway a été exécuté uniquement sur des bases H2 synthétiques en mémoire.** Pour Customer, Payroll et Invoice, les tests utilisent des copies temporaires qui séparent les actions d’un ALTER TABLE MySQL combiné et expriment ADD INDEX comme CREATE INDEX. Toutes les versions restent sélectionnées ; aucune migration n’est filtrée ou omise. Le BOM UTF-8 et les commentaires de ligne sont traités avant découpage ; les virgules dans les parenthèses et chaînes sont conservées et testées.

Ces copies portent leurs propres checksums H2. Elles ne valident **ni la syntaxe MySQL originale, ni les checksums/historiques Flyway de production, ni les collations et contraintes MySQL réelles**. Les tests CRUD Invoice H2 utilisent désormais explicitement Hibernate create-drop sans Flyway automatique, tandis que QuoteMigrationTest exerce séparément la chaîne complète adaptée. Aucun Flyway, baseline/repair/clean sur MySQL ou base persistante préexistante au lot ; les baselines des seules fixtures H2 synthétiques restent incluses dans les tests. Aucune connexion au MySQL de développement.

Les tests de conservation, idempotence, propriété, précision et rollback local passent. La concurrence et les transactions distribuées ne sont pas déduites des mocks/H2. La suppression interservices des salariés/départements, les historiques incohérents et leur rapprochement restent des décisions de cycle de vie à recetter, sans nouvelle opération destructive exécutée ici.

## 7. Résultats Java par module

Toutes les classes de tests inventoriées du périmètre demandé ont un dernier rapport JUnit réussi. Les relances remplacent les premiers passages ; leurs nombres ne s’additionnent pas.

| Module | Classes | Réussis | Échecs/erreurs | Ignorés |
|---|---:|---:|---:|---:|
| common-security | 8 | 116 | 0 | 0 |
| discovery-service | 1 | 1 | 0 | 0 |
| identity-service | 19 | 159 | 0 | 0 |
| gateway-service | 5 | 111 | 0 | 0 |
| employee-service | 16 | 170 | 0 | 0 |
| department-service | 5 | 45 | 0 | 0 |
| customer-service | 21 | 186 | 0 | 0 |
| hr-service | 40 | 490 | 0 | 0 |
| payroll-service | 16 | 137 | 0 | 0 |
| fees-service | 13 | 141 | 0 | 0 |
| invoice-service | 21 | 168 | 0 | 0 |
| **Total distinct** | **165** | **1 724** | **0** | **0** |

Portées distinctes : unités et mocks ; chaînes de sécurité/MVC ; SpringBootTest en H2/MockMvc ou contexte isolé ; fixtures JPA/H2 ; migrations H2 originales compatibles ou copies adaptées explicitement ; HTTP Identity/annuaires synthétiques ; Gateway embarqué avec dépendances synthétiques. Les SpringBootTest auparavant seulement compilés dans 3B–3G ont ici été exécutés lorsqu’ils appartiennent à ces suites.

Ordre séquentiel conforme à la demande, Maven hors ligne et cache local, heap Maven 512 Mio et JVM de tests 384 Mio. Aucun `skipTests`, filtre de sélection ou exclusion silencieuse ; compilation de production et de tests pour chaque module. Les premières tentatives globales ont totalisé 15 échecs d’assertion et 31 erreurs, détaillés au §11, avant correction.

Commandes de campagne :

    # MAVEN_OPTS=-Xmx512m
    mvn -o -B -ntp -Dmaven.repo.local=C:/Users/ASUS/.m2/repository -f microservices/common-security/pom.xml -DargLine=-Xmx384m install
    # Puis chaque module du tableau, dans l’ordre, et relances intégrales des modules corrigés :
    mvn -o -B -ntp -Dmaven.repo.local=C:/Users/ASUS/.m2/repository -f microservices/MODULE/pom.xml -DargLine=-Xmx384m test

Les comptes proviennent des derniers JUnit de chaque classe, rapprochés de l’inventaire de 165 classes, sans rapport manquant. Les fichiers détaillés et codes de sortie sont sous `target/lot3i/java-results.json` et les journaux de campagne ignorés par Git.

## 8. Résultats Angular

**1 254/1 254 réussis**, ChromeHeadless 157, sélection complète `src/app/**/*.spec.ts`. Tous les modules Paie, RH, Contacts, Honoraires, Facturation, espaces personnels et les guards/routes/navigation sont inclus. Aucun changement Angular n’a été nécessaire pendant 3I.

Les tests contrôlent formulaires, données et filtres transmis, états vide/chargement/erreur, droits des quatre rôles, réponses tardives et purges de session, UUID/reprises et téléchargements. HTTP métier simulé ; aucun navigateur connecté au CRM réel.

**TypeScript app et spec : PASS**, deux commandes `tsc --noEmit`, sans sortie générée. La compilation Angular contrôle aussi les templates.

    # Depuis frontend : NODE_OPTIONS=--max-old-space-size=1536, NG_BUILD_MAX_WORKERS=2, CHROME_BIN configuré
    .\node_modules\.bin\ng.cmd test --no-watch --browsers=ChromeHeadless --karma-config=karma.ci.conf.cjs --include=src/app/**/*.spec.ts
    .\node_modules\.bin\tsc.cmd --noEmit -p tsconfig.app.json
    .\node_modules\.bin\tsc.cmd --noEmit -p tsconfig.spec.json

## 9. Build production

**PASS**, bundle initial **889,30 kB**, estimation transférée 217,48 kB, sous le seuil bloquant existant de 1 MB. Sortie ignorée : `target/lot3i/frontend-production`.

Avertissements conservés : initial au-dessus du seuil indicatif 500 kB ; styles Payroll 4,17 kB et Invoices 5,33 kB au-dessus du seuil indicatif 4 kB ; NG8011 pour la projection d’icônes dans CustomersPageComponent. Aucun budget ni style n’est modifié pour masquer ces avertissements.

    .\node_modules\.bin\ng.cmd build --configuration production --output-path ../target/lot3i/frontend-production

Le build est une compilation, pas un déploiement ni une preuve de parcours connecté.

## 10. Vérifications transversales

| Parcours | Preuves obtenues et portée |
|---|---|
| SUPER_ADMIN | Création/administration, restrictions de rôles et dernier compte utilisable ; Identity H2/MockMvc et navigation Angular |
| ADMIN | Employés/départements, clients, RH, honoraires et factures ; suites complètes locales et guards |
| EMPLOYEE | Authentification/session, association UID, congés, profil/documents/bulletins propres et contacts affectés ; refus croisés testés |
| CLIENT | Authentification/session, demandes et messages propres, factures/PDF archivés et historique de paiements ; refus d’un autre client |
| Paiements | Précision/limites, solde, refus de surpaiement et états dérivés ; tests locaux avec H2 et mocks selon les suites |
| Documents | Publication/partage/propriété, captures PDF et conservation après archive ; aucun élargissement de données privées |
| Références | Bearer, identité canonique, réponse distante invalide/absente et 401/403/404/409/502/503 selon les contrats |
| Fees–Invoice | Sources figées, réservations uniques, UUID, échec après réservation, reprise/libération et transaction finale locale |

Les écritures de paiements restent administratives ; CLIENT consulte les paiements de ses seules factures. Aucun nouveau droit de paiement CLIENT n’est créé pour satisfaire une formulation générale de parcours.

Les suites de préparation et CI Node passent **51/51**, avec faux temps/HTTP et modèles en mémoire. Elles ne sont pas des sondes d’une architecture démarrée. Aucun de ces parcours n’est qualifié d’E2E CRM réel.

## 11. Anomalies détectées et corrigées

| Classement | Constat reproduit | Correction minimale et résultat |
|---|---|---|
| BLOQUANTE pour la validation locale, corrigée | H2 refuse les ALTER TABLE MySQL combinés de 3B/3F/3G ; chargement des contextes Invoice affecté | Copies de test H2 explicites, aucune version exclue ni migration originale modifiée ; test de transformation et nouvelles tables vérifiées |
| BLOQUANTE pour la validation locale, corrigée | Flyway automatique et Hibernate create-drop mêlés dans les tests CRUD Invoice H2 | Flyway désactivé uniquement dans cette configuration de test ; chaîne de migration adaptée exercée séparément |
| MAJEURE pour la pertinence des tests, corrigée | PayrollServiceIntegrationTest impose PAID et autorise encore la dépublication, contrairement à 3B | Fixture PENDING et assertions de refus 409 après publication ; conservation des montants et publication vérifiée |
| MAJEURE pour la pertinence des tests, corrigée | FeesServiceIntegrationTest attend encore un montant total sans devise | Attente explicite null conforme à 3E ; compte/heures/distributions toujours vérifiés |
| MINEURE, corrigée | Six attentes 404 Customer/Fees/Payroll utilisent les anciens libellés | Assertions des messages expurgés actuels, sans exposition d’identifiants ni SQL |
| MINEURE dans l’auxiliaire ajouté, corrigée | BOM et point-virgule d’un commentaire V1 mal traités par la première copie H2 | Traitement avant découpage ; test avec BOM, commentaire, virgule citée et contrainte composite |
| MINEURE, conservée | Avertissements de budgets et projection Angular | Recensés, sans modification des seuils ou refonte |
| BLOQUANTE pour le commit, non levée | git diff --cached --check retourne 2 après indexation complète | Lignes finales dans les huit V1 historiques, ancien test PayrollSettlementPolicy et espaces Markdown dans PLAN_FINALISATION_CRM ; aucune modification des migrations pour masquer ce résultat |

Le premier contrôle d’index comporte 13 diagnostics : trois lignes finales surnuméraires des auxiliaires H2 ajoutés ont été retirées, sans changement exécutable ; les dix diagnostics préexistants restants comprennent les huit V1, le test PayrollSettlementPolicy et le Markdown PLAN_FINALISATION_CRM. Les migrations restent intactes et aucun réglage de contrôle n’est modifié.

Les premiers passages Customer/Payroll/Fees/Invoice échoués sont conservés comme preuves auxiliaires ; ils ne sont pas comptés comme réussis. Aucun test n’est supprimé ou ignoré. Les suites concernées ont été relancées intégralement après correction. Aucune anomalie bloquante de production ni régression majeure démontrée ne reste ouverte dans les tests locaux exécutés. Le contrôle de formatage de l’index constitue toutefois un NO-GO de campagne distinct, sans prétendre révéler une anomalie métier.

## 12. Tests non exécutés et limites

**NON EXÉCUTÉS :** MySQL 8 neuf/upgrade, Flyway sur MySQL et historique réel, Hibernate validate sur candidat migré, collations/FK/plans MySQL, concurrence/deadlocks/rollback MySQL, architecture multiservice réelle, annuaire Eureka réel, HTTPS candidat, révocation multiservice connectée, navigateur connecté quatre comptes, charge et déploiement Jenkins/Docker.

Les anciennes preuves H2 adaptées RH et Fees gardent leurs limites ; le succès global ne les transforme pas en SQL/JDBC MySQL validé. Les contextes et bases en mémoire des tests ne touchent aucune base ou volume existant. Aucun téléchargement de dépendance nécessaire : Maven hors ligne et outils locaux.

**Aucune classe Java locale disponible des onze modules n’est exclue de cette campagne.** Le monolithe historique `backend/`, hors liste des modules demandés et sans changement dans cette campagne, n’est pas intégré au total. Les sondes opérationnelles, scripts d’import et campagnes E2E ne sont pas exécutés contre des services réels.

## 13. Risques résiduels

L’intégration réelle reste obligatoire avant déploiement : les tests locaux ne démontrent pas l’isolation/configuration Compose résolue, les secrets dédiés, les certificats, la disponibilité de RAM, le stockage ou les communications de vrais services. Les ressources et le moteur Docker ne sont pas remesurés, aucun démarrage n’étant demandé ici.

Restent les décisions métier déjà documentées : congés légaux, règles de paie et déclarations, rapprochements historiques, avoirs/corrections et relances effectivement envoyées, projets CLIENT, cycles de vie de références interservices. Ce sont des limites explicites, pas des fonctionnalités déclarées acquises par cette campagne.

Les anciens contrats numériques JavaScript, les réponses partielles d’import, NEEDS_ATTENTION sur un protocole de rapprochement réussi au niveau HTTP, les changements DELETE→archive et statistics.amount nullable exigent une recette des consommateurs réels. Les avertissements Angular sont non bloquants pour le build courant mais restent à traiter ultérieurement.

Les configurations de validation sont préparées/testées statiquement, pas déployées. Aucun ancien manifeste de campagne ne doit être utilisé comme preuve du contenu actuel.

## 14. Décision GO/NO-GO pour le commit

**NO-GO — aucun commit créé.** Les onze compilations/suites, Angular, TypeScript et contrôles de migrations passent dans leur portée locale, mais le dernier git diff --cached --check retourne 2. Les huit V1 étaient non suivies : leur première indexation rend visibles leurs lignes finales historiques, que le contrôle du seul diff suivi n’avait pas signalées. Modifier ces migrations contredirait leur préservation demandée. Aucun seuil ou réglage Git n’est changé pour neutraliser le contrôle.

Le statut NO-GO ne remet pas en cause les résultats de tests, mais interdit le commit selon la consigne de contrôles obligatoires réussis. Le message demandé reste réservé à une reprise autorisée après résolution du contrôle :

    chore(stabilization): valider localement les modules CRM des lots 3B a 3G

L’autorisation de commit conditionnel provient de la consigne du Lot 3I ; aucune autorisation de push, de changement de branche ou de modification de configuration Git n’en est déduite.

## 15. Fichiers effectivement indexés

La liste ci-dessous recense les 424 fichiers temporairement indexés, puis désindexés après le NO-GO. Leurs 424 objets ont été lus et contrôlés : concordance avec le manifeste, absence de suppression, type/contenu et recherche de secrets. Aucun secret ou artefact généré détecté. L’index initial vide est rétabli. Aucun git add global utilisé.

**424 fichiers proposés, temporairement indexés ; zéro fichier commité.**

<details>
<summary>Liste exhaustive du périmètre temporairement indexé</summary>

- `docker-compose.personal-spaces.yml`
- `docker-compose.validation.sprint6c.yml`
- `docker-compose.validation.yml`
- `docker-compose.yml`
- `docker/validation/mysql-init/01-databases.sql`
- `docs/AUDIT_FONCTIONNEL_CRM.md`
- `docs/CDC CRM 1.pdf`
- `docs/LOT_1A_FIABILISATION.md`
- `docs/LOT_1B_SECURISATION_COMPTES_SESSIONS.md`
- `docs/LOT_1C_ASSOCIATIONS_ESPACES_PERSONNELS.md`
- `docs/LOT_1C_VALIDATION_INTEGREE.md`
- `docs/LOT_2A_PREPARATION_VALIDATION.md`
- `docs/LOT_2B_R_VALIDATION_PROGRESSIVE.md`
- `docs/LOT_2B_SCENARIOS_VALIDATION.md`
- `docs/LOT_2B_VALIDATION_INTEGREE.md`
- `docs/LOT_3A_AUDIT_CONFORMITE_FONCTIONNELLE.md`
- `docs/LOT_3B_FINALISATION_PAIE.md`
- `docs/LOT_3C_FINALISATION_RH.md`
- `docs/LOT_3D_STABILISATION_INTEGREE.md`
- `docs/LOT_3E_FINALISATION_HONORAIRES.md`
- `docs/LOT_3F_FINALISATION_CRM_COMMERCIAL.md`
- `docs/LOT_3G_FINALISATION_FACTURATION.md`
- `docs/LOT_3I_VALIDATION_GLOBALE_ET_COMMIT.md`
- `docs/PLAN_FINALISATION_CRM.md`
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/core/interceptors/auth.interceptor.spec.ts`
- `frontend/src/app/core/navigation/crm-navigation.ts`
- `frontend/src/app/core/services/auth-navigation.ts`
- `frontend/src/app/core/services/auth.service.spec.ts`
- `frontend/src/app/core/services/auth.service.ts`
- `frontend/src/app/core/testing/client-invoice-statistics.json`
- `frontend/src/app/features/auth/login/login.component.html`
- `frontend/src/app/features/auth/login/login.component.ts`
- `frontend/src/app/features/client/client-workspace.component.html`
- `frontend/src/app/features/client/client-workspace.component.spec.ts`
- `frontend/src/app/features/client/client-workspace.component.ts`
- `frontend/src/app/features/client/client.service.spec.ts`
- `frontend/src/app/features/client/client.service.ts`
- `frontend/src/app/features/customers/assigned-contacts.component.html`
- `frontend/src/app/features/customers/assigned-contacts.component.spec.ts`
- `frontend/src/app/features/customers/assigned-contacts.component.ts`
- `frontend/src/app/features/customers/client-requests.component.html`
- `frontend/src/app/features/customers/client-requests.component.spec.ts`
- `frontend/src/app/features/customers/client-requests.component.ts`
- `frontend/src/app/features/customers/commercial-routes.spec.ts`
- `frontend/src/app/features/customers/commercial-work.scss`
- `frontend/src/app/features/customers/contact-work.service.spec.ts`
- `frontend/src/app/features/customers/contact-work.service.ts`
- `frontend/src/app/features/customers/customer-detail.component.html`
- `frontend/src/app/features/customers/customer-detail.component.ts`
- `frontend/src/app/features/customers/customer-form.component.html`
- `frontend/src/app/features/customers/customer-form.component.spec.ts`
- `frontend/src/app/features/customers/customer-form.component.ts`
- `frontend/src/app/features/customers/customer.models.ts`
- `frontend/src/app/features/customers/customer.service.spec.ts`
- `frontend/src/app/features/customers/customer.service.ts`
- `frontend/src/app/features/customers/customers-page.component.html`
- `frontend/src/app/features/customers/customers-page.component.spec.ts`
- `frontend/src/app/features/customers/customers-page.component.ts`
- `frontend/src/app/features/customers/customers.routes.ts`
- `frontend/src/app/features/dashboard/dashboard-data.service.spec.ts`
- `frontend/src/app/features/dashboard/dashboard-data.service.ts`
- `frontend/src/app/features/dashboard/dashboard.component.html`
- `frontend/src/app/features/documents/my-documents.component.spec.ts`
- `frontend/src/app/features/documents/my-documents.component.ts`
- `frontend/src/app/features/documents/my-documents.service.spec.ts`
- `frontend/src/app/features/documents/my-documents.service.ts`
- `frontend/src/app/features/documents/personal-routes.spec.ts`
- `frontend/src/app/features/employees/pages/employee-detail/employee-dossier.component.spec.ts`
- `frontend/src/app/features/employees/pages/employee-detail/employee-dossier.component.ts`
- `frontend/src/app/features/fees/fee-cycle.service.spec.ts`
- `frontend/src/app/features/fees/fee-cycle.service.ts`
- `frontend/src/app/features/fees/fee-detail.component.html`
- `frontend/src/app/features/fees/fee-detail.component.ts`
- `frontend/src/app/features/fees/fee-finance.spec.ts`
- `frontend/src/app/features/fees/fee-finance.ts`
- `frontend/src/app/features/fees/fee-form.component.html`
- `frontend/src/app/features/fees/fee-form.component.spec.ts`
- `frontend/src/app/features/fees/fee-form.component.ts`
- `frontend/src/app/features/fees/fee-reference-picker.component.ts`
- `frontend/src/app/features/fees/fee-workbench.component.html`
- `frontend/src/app/features/fees/fee-workbench.component.scss`
- `frontend/src/app/features/fees/fee-workbench.component.spec.ts`
- `frontend/src/app/features/fees/fee-workbench.component.ts`
- `frontend/src/app/features/fees/fee.models.ts`
- `frontend/src/app/features/fees/fees-page.component.html`
- `frontend/src/app/features/fees/fees-page.component.ts`
- `frontend/src/app/features/fees/fees.routes.spec.ts`
- `frontend/src/app/features/fees/fees.routes.ts`
- `frontend/src/app/features/home/home.component.spec.ts`
- `frontend/src/app/features/hr-admin/hr-admin-access.spec.ts`
- `frontend/src/app/features/hr-admin/hr-admin-page.component.html`
- `frontend/src/app/features/hr-admin/hr-admin-page.component.spec.ts`
- `frontend/src/app/features/hr-admin/hr-admin-page.component.ts`
- `frontend/src/app/features/hr-admin/hr-employee-picker.component.html`
- `frontend/src/app/features/hr-admin/hr-employee-picker.component.ts`
- `frontend/src/app/features/hr-admin/hr-insights.component.html`
- `frontend/src/app/features/hr-admin/hr-insights.component.scss`
- `frontend/src/app/features/hr-admin/hr-insights.component.ts`
- `frontend/src/app/features/hr-admin/hr-insights.service.ts`
- `frontend/src/app/features/hr-admin/hr-insights.spec.ts`
- `frontend/src/app/features/invoices/invoice-detail.component.html`
- `frontend/src/app/features/invoices/invoice-detail.component.spec.ts`
- `frontend/src/app/features/invoices/invoice-detail.component.ts`
- `frontend/src/app/features/invoices/invoice-form.component.html`
- `frontend/src/app/features/invoices/invoice-form.component.spec.ts`
- `frontend/src/app/features/invoices/invoice-form.component.ts`
- `frontend/src/app/features/invoices/invoice-work.service.spec.ts`
- `frontend/src/app/features/invoices/invoice-work.service.ts`
- `frontend/src/app/features/invoices/invoice-workbench.component.html`
- `frontend/src/app/features/invoices/invoice-workbench.component.scss`
- `frontend/src/app/features/invoices/invoice-workbench.component.spec.ts`
- `frontend/src/app/features/invoices/invoice-workbench.component.ts`
- `frontend/src/app/features/invoices/invoice.models.ts`
- `frontend/src/app/features/invoices/invoice.service.spec.ts`
- `frontend/src/app/features/invoices/invoices-page.component.html`
- `frontend/src/app/features/invoices/invoices-page.component.spec.ts`
- `frontend/src/app/features/invoices/invoices-page.component.ts`
- `frontend/src/app/features/invoices/invoices.routes.spec.ts`
- `frontend/src/app/features/invoices/invoices.routes.ts`
- `frontend/src/app/features/leaves/leave-balance.component.html`
- `frontend/src/app/features/leaves/leave-balance.component.ts`
- `frontend/src/app/features/leaves/leave-calendar.component.spec.ts`
- `frontend/src/app/features/leaves/leave-calendar.component.ts`
- `frontend/src/app/features/leaves/leave-calendar.service.ts`
- `frontend/src/app/features/leaves/leaves-page.component.html`
- `frontend/src/app/features/leaves/leaves-page.component.ts`
- `frontend/src/app/features/payroll/my-payroll-detail.component.html`
- `frontend/src/app/features/payroll/my-payroll-detail.component.ts`
- `frontend/src/app/features/payroll/my-payroll.service.spec.ts`
- `frontend/src/app/features/payroll/my-payroll.service.ts`
- `frontend/src/app/features/payroll/payroll-calculation.spec.ts`
- `frontend/src/app/features/payroll/payroll-calculation.ts`
- `frontend/src/app/features/payroll/payroll-detail.component.html`
- `frontend/src/app/features/payroll/payroll-detail.component.spec.ts`
- `frontend/src/app/features/payroll/payroll-detail.component.ts`
- `frontend/src/app/features/payroll/payroll-form.component.html`
- `frontend/src/app/features/payroll/payroll-form.component.spec.ts`
- `frontend/src/app/features/payroll/payroll-form.component.ts`
- `frontend/src/app/features/payroll/payroll-settlements.component.spec.ts`
- `frontend/src/app/features/payroll/payroll-settlements.component.ts`
- `frontend/src/app/features/payroll/payroll-settlements.models.ts`
- `frontend/src/app/features/payroll/payroll.models.ts`
- `frontend/src/app/features/payroll/payroll.routes.spec.ts`
- `frontend/src/app/features/payroll/payroll.service.spec.ts`
- `frontend/src/app/features/payroll/payroll.service.ts`
- `frontend/src/app/features/payroll/payrolls-page.component.html`
- `frontend/src/app/features/payroll/payrolls-page.component.spec.ts`
- `frontend/src/app/features/payroll/payrolls-page.component.ts`
- `frontend/src/app/features/users/users-page.component.spec.ts`
- `frontend/src/app/features/users/users-page.component.ts`
- `frontend/src/app/shared/financial-validators.spec.ts`
- `frontend/src/app/shared/financial-validators.ts`
- `microservices/common-security/src/test/java/com/crm/security/SessionBoundTokenCompatibilityTest.java`
- `microservices/customer-service/src/main/java/com/crm/customer/client/AssignedEmployeeDirectory.java`
- `microservices/customer-service/src/main/java/com/crm/customer/client/CustomerEmployeeDirectory.java`
- `microservices/customer-service/src/main/java/com/crm/customer/config/SecurityConfig.java`
- `microservices/customer-service/src/main/java/com/crm/customer/controller/ContactWorkController.java`
- `microservices/customer-service/src/main/java/com/crm/customer/controller/CustomerController.java`
- `microservices/customer-service/src/main/java/com/crm/customer/controller/SalesController.java`
- `microservices/customer-service/src/main/java/com/crm/customer/dto/CustomerRequest.java`
- `microservices/customer-service/src/main/java/com/crm/customer/dto/CustomerResponse.java`
- `microservices/customer-service/src/main/java/com/crm/customer/entity/ClientCase.java`
- `microservices/customer-service/src/main/java/com/crm/customer/entity/ClientCaseEvent.java`
- `microservices/customer-service/src/main/java/com/crm/customer/entity/ClientCaseMessage.java`
- `microservices/customer-service/src/main/java/com/crm/customer/entity/Customer.java`
- `microservices/customer-service/src/main/java/com/crm/customer/entity/CustomerInteraction.java`
- `microservices/customer-service/src/main/java/com/crm/customer/exception/GlobalExceptionHandler.java`
- `microservices/customer-service/src/main/java/com/crm/customer/mapper/CustomerMapper.java`
- `microservices/customer-service/src/main/java/com/crm/customer/repository/ClientCaseEventRepository.java`
- `microservices/customer-service/src/main/java/com/crm/customer/repository/ClientCaseMessageRepository.java`
- `microservices/customer-service/src/main/java/com/crm/customer/repository/ClientCaseRepository.java`
- `microservices/customer-service/src/main/java/com/crm/customer/repository/CustomerAssociationRepository.java`
- `microservices/customer-service/src/main/java/com/crm/customer/repository/CustomerInteractionRepository.java`
- `microservices/customer-service/src/main/java/com/crm/customer/repository/CustomerRepository.java`
- `microservices/customer-service/src/main/java/com/crm/customer/service/AssignedContactService.java`
- `microservices/customer-service/src/main/java/com/crm/customer/service/ClientCaseService.java`
- `microservices/customer-service/src/main/java/com/crm/customer/service/ContactAccess.java`
- `microservices/customer-service/src/main/java/com/crm/customer/service/CustomerCsvService.java`
- `microservices/customer-service/src/main/java/com/crm/customer/service/CustomerPersonalService.java`
- `microservices/customer-service/src/main/java/com/crm/customer/service/CustomerXlsxService.java`
- `microservices/customer-service/src/main/java/com/crm/customer/service/ICustomerService.java`
- `microservices/customer-service/src/main/java/com/crm/customer/service/impl/CustomerServiceImpl.java`
- `microservices/customer-service/src/main/java/com/crm/customer/util/CustomerSpecification.java`
- `microservices/customer-service/src/main/resources/application.yml`
- `microservices/customer-service/src/main/resources/db/migration/V1__initial_schema.sql`
- `microservices/customer-service/src/main/resources/db/migration/V20261105_02__commercial_archive_and_client_cases.sql`
- `microservices/customer-service/src/test/java/com/crm/customer/AssignedContactPolicyTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/AssignedEmployeeDirectoryTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/ClientCasePolicyTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/CommercialIntegrityPolicyTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/CommercialJpaFixtureTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/CommercialSecurityTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/CommercialUploadConfigurationTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/CustomerAssociationCreationPolicyTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/CustomerPersonalIntegrationTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/CustomerReadSecurityTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/CustomerServiceIntegrationTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/CustomerXlsxHttpTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/CustomerXlsxImportTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/H2MigrationCopies.java`
- `microservices/customer-service/src/test/java/com/crm/customer/SalesMigrationTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/SqlErrorPrivacyTest.java`
- `microservices/department-service/pom.xml`
- `microservices/department-service/src/main/java/com/crm/department/exception/GlobalExceptionHandler.java`
- `microservices/department-service/src/main/resources/application.yml`
- `microservices/department-service/src/main/resources/db/migration/V1__initial_schema.sql`
- `microservices/department-service/src/test/java/com/crm/department/SqlErrorPrivacyTest.java`
- `microservices/department-service/src/test/resources/application.yml`
- `microservices/employee-service/pom.xml`
- `microservices/employee-service/src/main/java/com/crm/employee/exception/GlobalExceptionHandler.java`
- `microservices/employee-service/src/main/resources/application.yml`
- `microservices/employee-service/src/main/resources/db/migration/V1__initial_schema.sql`
- `microservices/employee-service/src/test/java/com/crm/employee/SqlErrorPrivacyTest.java`
- `microservices/employee-service/src/test/resources/application.yml`
- `microservices/fees-service/pom.xml`
- `microservices/fees-service/src/main/java/com/crm/fees/config/SecurityConfig.java`
- `microservices/fees-service/src/main/java/com/crm/fees/controller/FeeBillingController.java`
- `microservices/fees-service/src/main/java/com/crm/fees/controller/FeeController.java`
- `microservices/fees-service/src/main/java/com/crm/fees/controller/FeeCycleController.java`
- `microservices/fees-service/src/main/java/com/crm/fees/controller/FeeStatisticsController.java`
- `microservices/fees-service/src/main/java/com/crm/fees/dto/FeeCycleContracts.java`
- `microservices/fees-service/src/main/java/com/crm/fees/dto/FeeRequest.java`
- `microservices/fees-service/src/main/java/com/crm/fees/dto/FeeResponse.java`
- `microservices/fees-service/src/main/java/com/crm/fees/entity/Fee.java`
- `microservices/fees-service/src/main/java/com/crm/fees/entity/FeeOffering.java`
- `microservices/fees-service/src/main/java/com/crm/fees/entity/FeeTariff.java`
- `microservices/fees-service/src/main/java/com/crm/fees/entity/FeeTimeEntry.java`
- `microservices/fees-service/src/main/java/com/crm/fees/exception/GlobalExceptionHandler.java`
- `microservices/fees-service/src/main/java/com/crm/fees/mapper/FeeMapper.java`
- `microservices/fees-service/src/main/java/com/crm/fees/repository/FeeOfferingRepository.java`
- `microservices/fees-service/src/main/java/com/crm/fees/repository/FeeReportRepository.java`
- `microservices/fees-service/src/main/java/com/crm/fees/repository/FeeRepository.java`
- `microservices/fees-service/src/main/java/com/crm/fees/repository/FeeTariffRepository.java`
- `microservices/fees-service/src/main/java/com/crm/fees/repository/FeeTimeRepository.java`
- `microservices/fees-service/src/main/java/com/crm/fees/service/FeeAuthorization.java`
- `microservices/fees-service/src/main/java/com/crm/fees/service/FeeBillingCandidates.java`
- `microservices/fees-service/src/main/java/com/crm/fees/service/FeeCycleService.java`
- `microservices/fees-service/src/main/java/com/crm/fees/service/FeeFinance.java`
- `microservices/fees-service/src/main/java/com/crm/fees/service/FeeReferenceDirectory.java`
- `microservices/fees-service/src/main/java/com/crm/fees/service/FeeReportService.java`
- `microservices/fees-service/src/main/java/com/crm/fees/service/IFeeService.java`
- `microservices/fees-service/src/main/java/com/crm/fees/service/impl/FeeServiceImpl.java`
- `microservices/fees-service/src/main/resources/application.yml`
- `microservices/fees-service/src/main/resources/db/migration/V1__initial_schema.sql`
- `microservices/fees-service/src/main/resources/db/migration/V20261105_01__fee_tariffs_time_and_exact_hours.sql`
- `microservices/fees-service/src/test/java/com/crm/fees/FeeBillingCandidatesTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/FeeCyclePolicyTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/FeeCycleSecurityTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/FeeFinancialPolicyTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/FeeReadSecurityTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/FeeReferenceDirectoryTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/FeeReportFixtureTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/FeeReportPolicyTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/FeesServiceIntegrationTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/SqlErrorPrivacyTest.java`
- `microservices/fees-service/src/test/resources/application.yml`
- `microservices/gateway-service/src/main/resources/application.yml`
- `microservices/gateway-service/src/test/java/com/crm/gateway/CommercialRouteConfigurationTest.java`
- `microservices/gateway-service/src/test/java/com/crm/gateway/GatewaySecurityRoutingTest.java`
- `microservices/gateway-service/src/test/java/com/crm/gateway/GatewayServiceSmokeTest.java`
- `microservices/hr-service/src/main/java/com/crm/hr/config/SecurityConfig.java`
- `microservices/hr-service/src/main/java/com/crm/hr/controller/EmployeeDossierController.java`
- `microservices/hr-service/src/main/java/com/crm/hr/controller/HrInsightsController.java`
- `microservices/hr-service/src/main/java/com/crm/hr/controller/LeaveController.java`
- `microservices/hr-service/src/main/java/com/crm/hr/controller/PersonalDocumentController.java`
- `microservices/hr-service/src/main/java/com/crm/hr/controller/TrainingController.java`
- `microservices/hr-service/src/main/java/com/crm/hr/dto/TrainingRequest.java`
- `microservices/hr-service/src/main/java/com/crm/hr/entity/EmployeeDocument.java`
- `microservices/hr-service/src/main/java/com/crm/hr/exception/GlobalExceptionHandler.java`
- `microservices/hr-service/src/main/java/com/crm/hr/repository/HrStatisticsRepository.java`
- `microservices/hr-service/src/main/java/com/crm/hr/repository/LeaveBalanceRepository.java`
- `microservices/hr-service/src/main/java/com/crm/hr/repository/LeaveRepository.java`
- `microservices/hr-service/src/main/java/com/crm/hr/service/EmployeeDocumentService.java`
- `microservices/hr-service/src/main/java/com/crm/hr/service/EmployeeReferenceValidator.java`
- `microservices/hr-service/src/main/java/com/crm/hr/service/HrPeriod.java`
- `microservices/hr-service/src/main/java/com/crm/hr/service/HrStatisticsService.java`
- `microservices/hr-service/src/main/java/com/crm/hr/service/ILeaveService.java`
- `microservices/hr-service/src/main/java/com/crm/hr/service/ITrainingService.java`
- `microservices/hr-service/src/main/java/com/crm/hr/service/LeaveBalanceCalculator.java`
- `microservices/hr-service/src/main/java/com/crm/hr/service/LeaveBalanceService.java`
- `microservices/hr-service/src/main/java/com/crm/hr/service/PersonalDocumentService.java`
- `microservices/hr-service/src/main/java/com/crm/hr/service/impl/LeaveServiceImpl.java`
- `microservices/hr-service/src/main/java/com/crm/hr/service/impl/TrainingServiceImpl.java`
- `microservices/hr-service/src/main/resources/db/migration/V1__initial_schema.sql`
- `microservices/hr-service/src/main/resources/db/migration/V20261101_02__employee_document_visibility.sql`
- `microservices/hr-service/src/test/java/com/crm/hr/HrAggregateFixtureTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/HrInsightsSecurityTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/HrStatisticsPolicyTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/LeaveBalanceCalculationTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/LeaveBalanceOwnershipTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/LeaveCalendarPolicyTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/PersonalDocumentHttpContractTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/PersonalDocumentSecurityTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/SqlErrorPrivacyTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/TrainingAssignmentPolicyTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/TrainingServiceIntegrationTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/service/DocumentStoragePolicyTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/service/PersonalDocumentPolicyTest.java`
- `microservices/identity-service/pom.xml`
- `microservices/identity-service/src/main/java/com/crm/identity/config/SecurityConfig.java`
- `microservices/identity-service/src/main/java/com/crm/identity/controller/AuthenticationController.java`
- `microservices/identity-service/src/main/java/com/crm/identity/dto/RefreshTokenRequest.java`
- `microservices/identity-service/src/main/java/com/crm/identity/entity/RefreshToken.java`
- `microservices/identity-service/src/main/java/com/crm/identity/exception/GlobalExceptionHandler.java`
- `microservices/identity-service/src/main/java/com/crm/identity/repository/RefreshTokenRepository.java`
- `microservices/identity-service/src/main/java/com/crm/identity/repository/UserRepository.java`
- `microservices/identity-service/src/main/java/com/crm/identity/security/AccessTokenValidationFilter.java`
- `microservices/identity-service/src/main/java/com/crm/identity/security/AccessTokenValidationService.java`
- `microservices/identity-service/src/main/java/com/crm/identity/security/JwtAuthenticationFilter.java`
- `microservices/identity-service/src/main/java/com/crm/identity/security/JwtService.java`
- `microservices/identity-service/src/main/java/com/crm/identity/service/IAuthenticationService.java`
- `microservices/identity-service/src/main/java/com/crm/identity/service/impl/AuthenticationServiceImpl.java`
- `microservices/identity-service/src/main/java/com/crm/identity/service/impl/IdentityServiceImpl.java`
- `microservices/identity-service/src/main/resources/application.yml`
- `microservices/identity-service/src/main/resources/db/migration/V1__initial_schema.sql`
- `microservices/identity-service/src/main/resources/db/migration/V20261101_01__refresh_session_identity.sql`
- `microservices/identity-service/src/test/java/com/crm/identity/AccessTokenValidationAvailabilityTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/AccessTokenValidationIntegrationTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/AdminEmployeeCreationTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/LogoutHttpContractTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/SessionLifecyclePolicyTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/SessionSecurityIntegrationTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/SqlErrorPrivacyTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/UidIssuanceIntegrationTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/UsableSuperAdminPolicyTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/security/AccessTokenClaimsTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/security/AccessTokenValidationServiceTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/security/UidIssuanceTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/security/UnifiedAuthenticationPolicyTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/service/impl/AuthenticationServiceImplTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/service/impl/DirectoryRolesTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/service/impl/RefreshTokenSecurityTest.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/client/InvoiceSourceClient.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/config/InvoiceClockConfiguration.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/config/SecurityConfig.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoiceController.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoicePdfController.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoiceWorkController.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/controller/PersonalInvoiceController.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/controller/QuoteController.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/dto/InvoiceRequest.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/dto/InvoiceResponse.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/entity/Invoice.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoiceDocument.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoiceFeeClaim.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoiceLine.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoicePresentation.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoiceReconciliation.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/entity/ReminderPolicy.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/entity/ReminderTask.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/exception/GlobalExceptionHandler.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/mapper/InvoiceMapper.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/pdf/PdfReport.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoiceDocumentRepository.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoiceFeeClaimRepository.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoicePaymentRepository.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoicePresentationRepository.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoiceReconciliationRepository.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/repository/ReminderPolicyRepository.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/repository/ReminderTaskRepository.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/service/FinancialValidation.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/service/InvoiceFollowupService.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/service/InvoiceLifecycle.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/service/InvoicePresentationService.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/service/InvoiceReconciliationService.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/service/InvoiceWorkAccess.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/service/PersonalInvoiceService.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/service/impl/InvoiceServiceImpl.java`
- `microservices/invoice-service/src/main/resources/db/migration/V1__initial_schema.sql`
- `microservices/invoice-service/src/main/resources/db/migration/V20261031_01__widen_invoice_sequence_id.sql`
- `microservices/invoice-service/src/main/resources/db/migration/V20261107_01__invoice_documents_reminders_and_fee_claims.sql`
- `microservices/invoice-service/src/test/java/com/crm/invoice/ClientStatisticsContractTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/H2MigrationCopies.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceFinancialPolicyTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceFollowupPolicyTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceFrozenOwnershipTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/InvoicePresentationPolicyTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceReconciliationFixtureTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceServiceIntegrationTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceSourceContractTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceWorkSecurityTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/PersonalPaymentPolicyTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/QuoteMigrationTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/SqlErrorPrivacyTest.java`
- `microservices/invoice-service/src/test/resources/application.yml`
- `microservices/payroll-service/src/main/java/com/crm/payroll/controller/PayrollPdfController.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/controller/PayrollSettlementController.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/controller/PersonalPayrollController.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/dto/PayrollRequest.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/dto/PayrollResponse.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/entity/Payroll.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/entity/PayrollSettlement.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/exception/GlobalExceptionHandler.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/mapper/PayrollMapper.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/repository/PayrollRepository.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/repository/PayrollSettlementRepository.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/service/FinancialValidation.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/service/PayrollAuthorization.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/service/PayrollCalculation.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/service/PayrollLedger.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/service/PayrollSettlementService.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/service/PayrollTechnicalExport.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/service/impl/PayrollServiceImpl.java`
- `microservices/payroll-service/src/main/resources/db/migration/V1__initial_schema.sql`
- `microservices/payroll-service/src/main/resources/db/migration/V20261102_01__payroll_variables_and_settlements.sql`
- `microservices/payroll-service/src/test/java/com/crm/payroll/H2MigrationCopies.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollFinancialPolicyTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollPersonalOwnershipPolicyTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollPublicationMigrationTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollServiceIntegrationTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollSettlementPolicyTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollSettlementSecurityTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollTechnicalExportTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollVariableCalculationTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollVariablePdfTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/SqlErrorPrivacyTest.java`
- `scripts/validation/check-lot2b.mjs`
- `scripts/validation/prepare-lot2b.mjs`
- `scripts/validation/prepare-lot2b.test.mjs`
- `scripts/validation/prepare-progressive.mjs`
- `scripts/validation/progressive-preflight.ps1`
- `scripts/validation/progressive-readiness.mjs`
- `scripts/validation/progressive.test.mjs`

</details>

`b.png` reste non suivi et inchangé. Les auxiliaires `target/lot3i`, caches, JAR, logs, copies SQL et build ne sont pas indexés. Le document CDC est une spécification fournie, pas une sortie générée. Les vérifications finales comportent `git diff --cached --check`, le stat, les noms/objets de chaque entrée et le contenu des fichiers sensibles.

## 16. Hash du commit

**Aucun hash : aucun commit créé.** Le contrôle final de l’index n’étant pas passant, l’autorisation conditionnelle ne s’applique pas. Aucun commit existant amendé ou réécrit.

## 17. État Git final

Branche publication-clean, toujours un commit d’avance sur la référence origin présente localement. Index vide, aucun commit/push 3I. Les 424 fichiers proposés demeurent modifiés/non suivis dans le répertoire de travail ; b.png est également conservé et non suivi. Aucune disparition de fichier préexistant.

Le retrait de l’indexation temporaire a été limité à la liste explicite de cette campagne. Aucun reset --hard, suppression de données, changement de branche ou modification de configuration Git. Le statut final détaillé est conservé sous target/lot3i/git-status-final.txt.

## 18. Prochaines étapes pour la validation intégrée

0. Lever d’abord la contradiction entre le contrôle de formatage non passant et la préservation octet pour octet des migrations V1 : les diagnostics exacts sont dans target/lot3i/git-index-check.txt. Ne pas réécrire une migration historique ni désactiver silencieusement les contrôles ; une décision explicite sur ce périmètre est nécessaire avant une nouvelle tentative de commit. Les espaces du Markdown et du test historique peuvent être traités séparément.
1. Reprendre les précontrôles de ressources et d’isolation du [Lot 2B-R](LOT_2B_R_VALIDATION_PROGRESSIVE.md) après autorisation opérationnelle, avec les secrets/certificats dédiés ; ne monter aucune base de développement.
2. Produire un nouveau manifeste et les artefacts correspondant au commit courant ; contrôler leurs empreintes et migrations embarquées.
3. Installer MySQL 8 neuf, puis tester l’upgrade uniquement sur copie sauvegardée et autorisée ; valider schémas Hibernate, contraintes, paramètres/projections et requêtes natives exactes.
4. Rejouer les quatre rôles via Gateway/HTTPS, associations et refus croisés, révocation réelle, documents, références et erreurs distantes.
5. Exercer paiements/publication/corrections, archives, tarifs/temps, UUID et rapprochements concurrents ; observer rollback, deadlocks, plans et consommation mémoire.
6. Terminer les parcours Angular connectés et la recette métier/visuelle, puis seulement statuer sur un candidat intégré.

Preuves locales expurgées sous `target/lot3i` : inventaire initial, préservation, migrations, résultats Java/Angular/Node, contrôles statiques et audit de l’index. Les journaux bruts restent des auxiliaires ignorés. **Tests locaux validés ; commit NO-GO ; intégration réelle non validée.**
