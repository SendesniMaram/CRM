# Lot 1C — Associations et espaces personnels

Date : 1er novembre 2026. Branche conservée : `publication-clean`.

## 1. Résumé des fonctionnalités réalisées

Le Lot 1C est implémenté et validé dans le périmètre du code et des tests ciblés. L'espace EMPLOYEE dispose de documents explicitement partagés et téléchargeables ; l'espace CLIENT affiche les paiements de ses factures et mène aux statistiques existantes. La création d'association CLIENT est protégée contre une fusion JPA concurrente. La configuration HTTPS du profil est préparée séparément. Aucune validation intégrée MySQL/Gateway/TLS réel/navigateur connecté n'est déclarée réussie.

## 2. Fonctionnalités existantes réutilisées et audit ciblé

Les rapports Lot 1A, Lot 1B, l'audit fonctionnel et le plan de finalisation ont été consultés, puis confrontés aux services, contrôleurs, dépôts, filtres, routes, menus et tests réels.

| Parcours | Mécanisme conservé | Contrôles constatés |
|---|---|---|
| User ↔ Employee | `EmployeeAssociationService` sur `leave_employee_associations` | UID authentifié, référence canonique, rôle EMPLOYEE, compte actif/non verrouillé, existence distante, unicité UID/employé/username, INSERT et conflit 409, pas de seconde table |
| User ↔ Customer | `CustomerPersonalService` sur `customer_user_associations` | Création ADMIN/SUPER_ADMIN, annuaire Identity, rôle CLIENT actif/non verrouillé, client existant, unicité UID/client ; correction de l'écriture décrite ci-dessous |
| Profil EMPLOYEE | HR `/api/employee-profile/me` et résumé interne Employee | Association UID ; HTTPS dédié, JWT de service RSA à portée limitée, aucune transmission du JWT utilisateur au résumé interne ; états sans association/indisponibilité existants |
| Congés | `LeaveAccess`, `LeaveServiceImpl`, routes mine et écrans existants | Liste paginée filtrée côté serveur, détail/modification/PDF protégés par employé associé, workflow de décision conservé |
| Bulletins | `PersonalPayrollService` et `/my-payroll` | Association authentifiée, liste et détail limités à l'employé et aux bulletins publiés |
| Profil CLIENT et factures | `/api/customers/me`, `PersonalInvoiceService`, `/client` | UID issu du principal, client résolu côté serveur, factures/PDF par `findByIdAndCustomerId`, pas de ciblage par ID client Angular |
| Statistiques | Dashboard et contrat Lot 1A | `byCurrency` objet HTTP adapté et validé par le service Angular existant ; aucune nouvelle agrégation dans l'espace client |
| Sessions | Identity et common-security du Lot 1B | Validation Identity obligatoire, UID typé, sid et révocation conservés ; aucun fichier de sécurité du Lot 1B modifié |

Les erreurs de contrat existantes restent 400/401/403/404/409/502/503 selon la couche : une absence d'association des congés reste notamment un refus 403, et celle du profil a son code dédié. Elles ne sont pas artificiellement uniformisées.

## 3. Corrections effectuées

**Association CLIENT.** Le `saveAndFlush` d'une entité à ID assigné pouvait employer `merge`, puis remplacer une association concurrente entre le contrôle d'absence et l'écriture. Une requête INSERT native dans la transaction existante laisse les contraintes d'unicité refuser cette course. Les violations d'intégrité/concurrence deviennent un 409 après rollback ; une association identique reste idempotente. Le test simule l'échec SQL, sans prétendre prouver la concurrence MySQL.

**Documents.** Ajout d'un partage explicite `employeeVisible`, faux par défaut, y compris pour l'historique grâce à la migration additive préparée. L'administrateur peut partager/retirer dans le dossier existant. Le personnel dispose uniquement d'une liste et d'un téléchargement en lecture : association par UID, propriété et partage contrôlés avant stockage ; document privé/étranger/absent → 404. Les réponses utilisent un DTO sans chemin, clé de stockage ou ID d'un autre employé. Le téléchargement est borné à 10 MiB, refuse les fichiers vides, répertoires, liens symboliques finaux et chemins résolus hors racine ; la réponse impose une pièce jointe et `nosniff`.

**Paiements CLIENT.** Ajout d'une lecture personnelle sous la facture. Le client est résolu via le bearer validé et `/customers/me`, puis la propriété de la facture est vérifiée avant de consulter les paiements. DTO limité à ID, montant décimal textuel, date et méthode ; référence et notes administratives exclues. Aucun paiement n'est créé, modifié ou supprimé par ce parcours. Les règles financières du Lot 1A restent intactes.

**HTTPS.** Le code existant vérifie certificats et nom d'hôte, refuse HTTP, impose le signataire dédié et se ferme en cas d'indisponibilité. Le Compose courant n'active pas les trois composants nécessaires : connecteur TLS Employee, validation de service Employee, client/signataire HR. L'overlay optionnel prépare ce raccordement avec clés externes et montages en lecture seule, sans modifier le Compose principal. Aucun incident réel de certificat n'a été reproduit et aucun handshake réseau n'a été effectué. Voir le [protocole intégré](LOT_1C_VALIDATION_INTEGREE.md).

## 4. Fichiers modifiés

| Fichier | Rôle |
|---|---|
| [frontend/src/app/app.routes.ts](../frontend/src/app/app.routes.ts) | Navigation, guards et URL de retour personnelle |
| [frontend/src/app/core/navigation/crm-navigation.ts](../frontend/src/app/core/navigation/crm-navigation.ts) | Navigation, guards et URL de retour personnelle |
| [frontend/src/app/core/services/auth-navigation.ts](../frontend/src/app/core/services/auth-navigation.ts) | Navigation, guards et URL de retour personnelle |
| [frontend/src/app/features/client/client-workspace.component.html](../frontend/src/app/features/client/client-workspace.component.html) | Paiements personnels et états de consultation |
| [frontend/src/app/features/client/client-workspace.component.spec.ts](../frontend/src/app/features/client/client-workspace.component.spec.ts) | Tests Angular ciblés |
| [frontend/src/app/features/client/client-workspace.component.ts](../frontend/src/app/features/client/client-workspace.component.ts) | Paiements personnels et états de consultation |
| [frontend/src/app/features/client/client.service.spec.ts](../frontend/src/app/features/client/client.service.spec.ts) | Tests Angular ciblés |
| [frontend/src/app/features/client/client.service.ts](../frontend/src/app/features/client/client.service.ts) | Paiements personnels et états de consultation |
| [frontend/src/app/features/employees/pages/employee-detail/employee-dossier.component.ts](../frontend/src/app/features/employees/pages/employee-detail/employee-dossier.component.ts) | Partage administratif explicite |
| [microservices/customer-service/src/main/java/com/crm/customer/repository/CustomerAssociationRepository.java](../microservices/customer-service/src/main/java/com/crm/customer/repository/CustomerAssociationRepository.java) | Insertion protégée par les contraintes existantes |
| [microservices/customer-service/src/main/java/com/crm/customer/service/CustomerPersonalService.java](../microservices/customer-service/src/main/java/com/crm/customer/service/CustomerPersonalService.java) | Création d’association sans fusion concurrente |
| [microservices/hr-service/src/main/java/com/crm/hr/config/SecurityConfig.java](../microservices/hr-service/src/main/java/com/crm/hr/config/SecurityConfig.java) | Autorisations documentaires et refus des écritures personnelles |
| [microservices/hr-service/src/main/java/com/crm/hr/controller/EmployeeDossierController.java](../microservices/hr-service/src/main/java/com/crm/hr/controller/EmployeeDossierController.java) | Contrat HTTP et accès personnel |
| [microservices/hr-service/src/main/java/com/crm/hr/entity/EmployeeDocument.java](../microservices/hr-service/src/main/java/com/crm/hr/entity/EmployeeDocument.java) | Visibilité documentaire privée par défaut |
| [microservices/hr-service/src/main/java/com/crm/hr/service/EmployeeDocumentService.java](../microservices/hr-service/src/main/java/com/crm/hr/service/EmployeeDocumentService.java) | Propriété, partage et lecture bornée du stockage |
| [microservices/invoice-service/src/main/java/com/crm/invoice/controller/PersonalInvoiceController.java](../microservices/invoice-service/src/main/java/com/crm/invoice/controller/PersonalInvoiceController.java) | Contrat HTTP et accès personnel |
| [microservices/invoice-service/src/main/java/com/crm/invoice/service/PersonalInvoiceService.java](../microservices/invoice-service/src/main/java/com/crm/invoice/service/PersonalInvoiceService.java) | Paiements filtrés par propriété de facture |

L'inventaire décrit exclusivement ce lot ; les modifications antérieures restent dans Git et ne sont pas attribuées au Lot 1C.

## 5. Nouveaux fichiers

| Fichier | Rôle |
|---|---|
| [docker-compose.personal-spaces.yml](../docker-compose.personal-spaces.yml) | Configuration HTTPS optionnelle, non démarrée |
| [docs/LOT_1C_ASSOCIATIONS_ESPACES_PERSONNELS.md](../docs/LOT_1C_ASSOCIATIONS_ESPACES_PERSONNELS.md) | Rapport ou protocole de validation différée |
| [docs/LOT_1C_VALIDATION_INTEGREE.md](../docs/LOT_1C_VALIDATION_INTEGREE.md) | Rapport ou protocole de validation différée |
| [frontend/src/app/features/documents/my-documents.component.spec.ts](../frontend/src/app/features/documents/my-documents.component.spec.ts) | Tests Angular ciblés |
| [frontend/src/app/features/documents/my-documents.component.ts](../frontend/src/app/features/documents/my-documents.component.ts) | Espace documentaire personnel sécurisé |
| [frontend/src/app/features/documents/my-documents.service.spec.ts](../frontend/src/app/features/documents/my-documents.service.spec.ts) | Tests Angular ciblés |
| [frontend/src/app/features/documents/my-documents.service.ts](../frontend/src/app/features/documents/my-documents.service.ts) | Espace documentaire personnel sécurisé |
| [frontend/src/app/features/documents/personal-routes.spec.ts](../frontend/src/app/features/documents/personal-routes.spec.ts) | Tests Angular ciblés |
| [frontend/src/app/features/employees/pages/employee-detail/employee-dossier.component.spec.ts](../frontend/src/app/features/employees/pages/employee-detail/employee-dossier.component.spec.ts) | Tests Angular ciblés |
| [microservices/customer-service/src/test/java/com/crm/customer/CustomerAssociationCreationPolicyTest.java](../microservices/customer-service/src/test/java/com/crm/customer/CustomerAssociationCreationPolicyTest.java) | Tests Java ciblés |
| [microservices/hr-service/src/main/java/com/crm/hr/controller/PersonalDocumentController.java](../microservices/hr-service/src/main/java/com/crm/hr/controller/PersonalDocumentController.java) | Contrat HTTP et accès personnel |
| [microservices/hr-service/src/main/java/com/crm/hr/service/PersonalDocumentService.java](../microservices/hr-service/src/main/java/com/crm/hr/service/PersonalDocumentService.java) | Propriété, partage et lecture bornée du stockage |
| [microservices/hr-service/src/main/resources/db/migration/V20261101_02__employee_document_visibility.sql](../microservices/hr-service/src/main/resources/db/migration/V20261101_02__employee_document_visibility.sql) | Migration additive préparée, non exécutée |
| [microservices/hr-service/src/test/java/com/crm/hr/PersonalDocumentHttpContractTest.java](../microservices/hr-service/src/test/java/com/crm/hr/PersonalDocumentHttpContractTest.java) | Tests Java ciblés |
| [microservices/hr-service/src/test/java/com/crm/hr/PersonalDocumentSecurityTest.java](../microservices/hr-service/src/test/java/com/crm/hr/PersonalDocumentSecurityTest.java) | Tests Java ciblés |
| [microservices/hr-service/src/test/java/com/crm/hr/service/DocumentStoragePolicyTest.java](../microservices/hr-service/src/test/java/com/crm/hr/service/DocumentStoragePolicyTest.java) | Tests Java ciblés |
| [microservices/hr-service/src/test/java/com/crm/hr/service/PersonalDocumentPolicyTest.java](../microservices/hr-service/src/test/java/com/crm/hr/service/PersonalDocumentPolicyTest.java) | Tests Java ciblés |
| [microservices/invoice-service/src/test/java/com/crm/invoice/PersonalPaymentPolicyTest.java](../microservices/invoice-service/src/test/java/com/crm/invoice/PersonalPaymentPolicyTest.java) | Tests Java ciblés |

La migration `V20261101_02__employee_document_visibility.sql` ajoute seulement une colonne obligatoire avec valeur par défaut faux. Elle est préparée, **NON exécutée**. Le modèle et cette migration doivent être livrés ensemble. Son comportement MySQL et la validation JPA restent à vérifier ; aucune migration historique n'a changé.

## 6. Endpoints ajoutés ou corrigés

| Méthode et chemin interne | Accès et contrat |
|---|---|
| GET `/api/documents/me` | EMPLOYEE ; liste de ses documents partagés ; aucun paramètre de ciblage ; no-store |
| GET `/api/documents/me/{id}/content` | EMPLOYEE ; propriété + partage à chaque demande, ID canonique ; pièce jointe, MIME, no-store, nosniff |
| PATCH `/api/documents/{id}/visibility` | ADMIN/SUPER_ADMIN ; corps `{ "employeeVisible": true/false }`, champ obligatoire, 204 |
| GET `/api/invoices/me/{id}/payments` | CLIENT ; propriété serveur de la facture, liste ou vide, aucun paramètre de ciblage, no-store |
| POST `/api/customers/associations` | Contrat HTTP conservé ; écriture INSERT corrigée, conflit 409 |

Les routes Gateway existantes `/hr/api/documents/**` et `/invoice/api/invoices/**` couvrent les ajouts. Aucun élargissement de Gateway ou des API internes. Les chemins documentaires personnels sont déclarés avant les règles administratives, et les autres méthodes du namespace personnel sont refusées.

## 7. Interfaces Angular finalisées

- `/my-documents`, menu EMPLOYEE, guards auth/role et URL de retour interne autorisée. Liste partagée, téléchargement, chargement, liste vide, erreurs fixes et actualisation ; délais bornés, annulation/nettoyage sur changement de session et destruction du composant.
- Dossier employé administratif réutilisé : partage explicite et retrait du partage, documents nouveaux privés, message sur la confidentialité.
- `/client` conserve profil/factures/PDF ; le détail charge les paiements avec états vide/chargement/erreur distincts. Une erreur de paiements conserve la facture visible. Les données tardives d'une ancienne session ne sont pas affichées. Le lien « Mes statistiques financières » mène au dashboard existant.
- `/profile`, `/leaves`, `/my-payroll` conservés et inclus dans les vérifications ciblées pertinentes, sans nouvelle interface ni refonte globale. Aucun nouveau package frontend.

## 8. Mesures de sécurité

Le principal validé et son UID restent la preuve d'identité ; aucun identifiant client/employé Angular ne détermine le propriétaire. Les contrôles restent serveur et les guards sont une protection de navigation supplémentaire. Aucun fallback username/e-mail, aucune nouvelle autorité JWT, aucun cache positif Identity et aucun changement aux règles uid/sid. Le test de la vraie chaîne HR utilise un contexte servlet simulé avec validation Identity mockée : quatre rôles, 401, 503, refus du chemin administratif à EMPLOYEE et refus des écritures personnelles.

Les documents historiques ne sont pas ouverts automatiquement. Les fichiers privés ne sont pas servis comme ressources statiques. Aucune donnée personnelle, clé ou token n'est ajouté aux logs. Le contrôle de chemin limite l'exposition en lecture ; les permissions OS doivent toujours interdire à un tiers de remplacer le stockage pendant une lecture. Une requête déjà autorisée peut terminer si un partage ou une session est retiré ensuite ; aucun mécanisme n'annule rétroactivement les requêtes en cours.

## 9. Tests exécutés et résultats

Toutes les commandes ont été exécutées séquentiellement, en mode Maven hors ligne avec le cache existant, heap de test Java 384 MiB et Node 2048 MiB. Aucun téléchargement ou ajout de dépendance, `@SpringBootTest`, base H2/MySQL, Flyway, migration, conteneur ou serveur applicatif. MockMvc et les contextes de configuration sont en mémoire ; Karma démarre uniquement son serveur de tests et ChromeHeadless.

| Suite | Résultat final distinct |
|---|---:|
| HR : documents, stockage, contrats HTTP, associations, profil, client sécurisé, configuration et éligibilité | 85 réussis |
| HR : vraie chaîne de sécurité documentaire, validation Identity simulée | 8 réussis |
| Customer : politiques personnelles et création/conflits d'association | 17 réussis |
| Invoice : paiements personnels, contrat statistiques et politique financière Lot 1A | 20 réussis |
| **Total Java distinct** | **130 réussis** |
| **Angular ciblé élargi** | **571 réussis** |

Commandes Java depuis la racine :

```powershell
mvn -o -B -ntp '-Dmaven.repo.local=C:\Users\ASUS\.m2\repository' -f microservices/hr-service/pom.xml '-Dtest=PersonalDocumentPolicyTest,DocumentStoragePolicyTest,PersonalDocumentHttpContractTest,EmployeeAssociationServiceTest,EmployeeProfileServiceTest,EmployeeProfileClientTest,EmployeeProfileClientConfigurationTest,DirectoryAccountTest' '-DargLine=-Xmx384m' test
mvn -o -B -ntp '-Dmaven.repo.local=C:\Users\ASUS\.m2\repository' -f microservices/hr-service/pom.xml '-Dtest=PersonalDocumentSecurityTest' '-DargLine=-Xmx384m' test
mvn -o -B -ntp '-Dmaven.repo.local=C:\Users\ASUS\.m2\repository' -f microservices/customer-service/pom.xml '-Dtest=CustomerPersonalPolicyTest,CustomerAssociationCreationPolicyTest' '-DargLine=-Xmx384m' test
mvn -o -B -ntp '-Dmaven.repo.local=C:\Users\ASUS\.m2\repository' -f microservices/invoice-service/pom.xml '-Dtest=PersonalPaymentPolicyTest,ClientStatisticsContractTest,InvoiceFinancialPolicyTest' '-DargLine=-Xmx384m' test
```

Commande Angular finale depuis `frontend` :

```powershell
$env:CHROME_BIN='C:\Program Files\Google\Chrome\Application\chrome.exe'
$env:NODE_OPTIONS='--max-old-space-size=2048'
.\node_modules\.bin\ng.cmd test --no-watch --browsers=ChromeHeadless --karma-config=karma.ci.conf.cjs --include='src/app/features/documents/*.spec.ts' --include='src/app/features/client/*.spec.ts' --include='src/app/features/profile/*.spec.ts' --include='src/app/features/employees/pages/employee-detail/employee-dossier.component.spec.ts' --include='src/app/core/navigation/crm-navigation.spec.ts' --include='src/app/app.routes.spec.ts' --include='src/app/features/leaves/*.spec.ts' --include='src/app/features/payroll/*.spec.ts' --include='src/app/core/services/auth.service.spec.ts' --include='src/app/core/interceptors/auth.interceptor.spec.ts' --include='src/app/core/guards/*.spec.ts' --include='src/app/features/auth/login/*.spec.ts' --include='src/app/features/users/*.spec.ts' --include='src/app/features/dashboard/*.spec.ts' --include='src/app/features/invoices/*.spec.ts' --include='src/app/shared/financial-validators.spec.ts'
```

Les 390 tests Angular sélectionnés au Lot 1B, dont les 158 du Lot 1A, sont inclus dans les 571 : ne pas additionner les résultats historiques. Les premières passes HR 58 et Angular 181 sont également incluses dans les suites élargies. La première passe Angular a échoué sur neuf tests sans provider zoneless et sur l'ordre des catégories du menu ; corrections appliquées et passe finale entièrement réussie. Les tests SQL sont simulés ; le test de stockage utilise uniquement un répertoire temporaire de test. Les cas de liens symboliques ne sont pas validés sur un système de fichiers réel Windows/MySQL.

Les tests Maven compilent les sources de test des modules, y compris les suites d'intégration existantes, sans les exécuter. Aucun build Angular de production ou test global n'a été lancé. `git diff --check` : réussi. L'overlay a été inspecté statiquement ; Docker Compose et son interpolation réelle n'ont pas été exécutés.

## 10. Tests différés

Le [protocole intégré préparé](LOT_1C_VALIDATION_INTEGREE.md) détaille les données et résultats attendus : migrations neuve/upgrade, contraintes et concurrence MySQL, rollback, vrai HTTPS et certificats négatifs, routage Gateway, refus cross-account, publication/retrait documentaire, disponibilité interservices, quatre rôles, sessions révoquées et réponses tardives. Les suites d'intégration existantes restent différées ; aucun scénario réel n'est déclaré passé.

## 11. Risques restants

- Migration et modèle HR indissociables ; pas de démarrage de cette version sur l'ancien schéma sans migration autorisée.
- Provisionnement externe des certificats/clés, chaîne/SAN/DNS, horloges et droits des fichiers à vérifier. L'overlay n'apporte ni base isolée, ni limite mémoire, ni procédure de sauvegarde à lui seul.
- La politique explicite de partage évite une exposition historique ; les administrateurs doivent sélectionner les pièces destinées au salarié. Le retrait ne retire pas les copies déjà téléchargées.
- Stockage de fichiers et transaction SQL ne constituent pas une transaction distribuée. La rétention, le nettoyage des fichiers orphelins et la résilience restent des sujets d'exploitation distincts.
- La concurrence d'association n'est pas certifiée par les mocks. Les contraintes réelles et les éventuels interblocages doivent être vérifiés sur MySQL isolé.
- Aucun changement rétroactif des données, aucun workflow de remboursement, aucune correction comptable ou publication immuable de la paie n'est ajouté.

## 12. État Git et préservation

Branche `publication-clean`, aucun staging, commit, push, reset, changement de branche ou déploiement. Les **118 fichiers déjà modifiés/non suivis** au début du lot ont été comparés par SHA-256 : tous inchangés, aucun disparu. Cela inclut les Lots 1A/1B, leurs rapports, configurations et migrations préexistantes. Le Compose principal est inchangé ; seul un overlay optionnel est nouveau. Aucun fichier `.env` lu ou modifié, aucun volume accédé et aucune donnée supprimée.

État Git complet, incluant les modifications antérieures :

```text
 M docker-compose.yml
 M docs/AUDIT_FONCTIONNEL_CRM.md
 M frontend/src/app/app.routes.ts
 M frontend/src/app/core/interceptors/auth.interceptor.spec.ts
 M frontend/src/app/core/navigation/crm-navigation.ts
 M frontend/src/app/core/services/auth-navigation.ts
 M frontend/src/app/core/services/auth.service.spec.ts
 M frontend/src/app/core/services/auth.service.ts
 M frontend/src/app/features/auth/login/login.component.html
 M frontend/src/app/features/auth/login/login.component.ts
 M frontend/src/app/features/client/client-workspace.component.html
 M frontend/src/app/features/client/client-workspace.component.spec.ts
 M frontend/src/app/features/client/client-workspace.component.ts
 M frontend/src/app/features/client/client.service.spec.ts
 M frontend/src/app/features/client/client.service.ts
 M frontend/src/app/features/dashboard/dashboard-data.service.spec.ts
 M frontend/src/app/features/dashboard/dashboard-data.service.ts
 M frontend/src/app/features/employees/pages/employee-detail/employee-dossier.component.ts
 M frontend/src/app/features/invoices/invoice-detail.component.html
 M frontend/src/app/features/invoices/invoice-detail.component.spec.ts
 M frontend/src/app/features/invoices/invoice-detail.component.ts
 M frontend/src/app/features/invoices/invoice-form.component.html
 M frontend/src/app/features/invoices/invoice-form.component.spec.ts
 M frontend/src/app/features/invoices/invoice-form.component.ts
 M frontend/src/app/features/invoices/invoice.service.spec.ts
 M frontend/src/app/features/invoices/invoices-page.component.spec.ts
 M frontend/src/app/features/invoices/invoices.routes.spec.ts
 M frontend/src/app/features/payroll/payroll-detail.component.spec.ts
 M frontend/src/app/features/payroll/payroll-form.component.html
 M frontend/src/app/features/payroll/payroll-form.component.spec.ts
 M frontend/src/app/features/payroll/payroll-form.component.ts
 M frontend/src/app/features/payroll/payroll.routes.spec.ts
 M frontend/src/app/features/payroll/payroll.service.spec.ts
 M frontend/src/app/features/payroll/payrolls-page.component.spec.ts
 M frontend/src/app/features/users/users-page.component.spec.ts
 M frontend/src/app/features/users/users-page.component.ts
 M microservices/customer-service/src/main/java/com/crm/customer/repository/CustomerAssociationRepository.java
 M microservices/customer-service/src/main/java/com/crm/customer/service/CustomerPersonalService.java
 M microservices/department-service/pom.xml
 M microservices/department-service/src/main/java/com/crm/department/exception/GlobalExceptionHandler.java
 M microservices/department-service/src/main/resources/application.yml
 M microservices/department-service/src/test/resources/application.yml
 M microservices/employee-service/pom.xml
 M microservices/employee-service/src/main/java/com/crm/employee/exception/GlobalExceptionHandler.java
 M microservices/employee-service/src/main/resources/application.yml
 M microservices/employee-service/src/test/resources/application.yml
 M microservices/fees-service/pom.xml
 M microservices/fees-service/src/main/java/com/crm/fees/exception/GlobalExceptionHandler.java
 M microservices/fees-service/src/main/resources/application.yml
 M microservices/fees-service/src/test/resources/application.yml
 M microservices/gateway-service/src/main/resources/application.yml
 M microservices/gateway-service/src/test/java/com/crm/gateway/GatewaySecurityRoutingTest.java
 M microservices/gateway-service/src/test/java/com/crm/gateway/GatewayServiceSmokeTest.java
 M microservices/hr-service/src/main/java/com/crm/hr/config/SecurityConfig.java
 M microservices/hr-service/src/main/java/com/crm/hr/controller/EmployeeDossierController.java
 M microservices/hr-service/src/main/java/com/crm/hr/entity/EmployeeDocument.java
 M microservices/hr-service/src/main/java/com/crm/hr/exception/GlobalExceptionHandler.java
 M microservices/hr-service/src/main/java/com/crm/hr/service/EmployeeDocumentService.java
 M microservices/identity-service/pom.xml
 M microservices/identity-service/src/main/java/com/crm/identity/config/SecurityConfig.java
 M microservices/identity-service/src/main/java/com/crm/identity/controller/AuthenticationController.java
 M microservices/identity-service/src/main/java/com/crm/identity/dto/RefreshTokenRequest.java
 M microservices/identity-service/src/main/java/com/crm/identity/entity/RefreshToken.java
 M microservices/identity-service/src/main/java/com/crm/identity/exception/GlobalExceptionHandler.java
 M microservices/identity-service/src/main/java/com/crm/identity/repository/RefreshTokenRepository.java
 M microservices/identity-service/src/main/java/com/crm/identity/repository/UserRepository.java
 M microservices/identity-service/src/main/java/com/crm/identity/security/AccessTokenValidationFilter.java
 M microservices/identity-service/src/main/java/com/crm/identity/security/AccessTokenValidationService.java
 M microservices/identity-service/src/main/java/com/crm/identity/security/JwtAuthenticationFilter.java
 M microservices/identity-service/src/main/java/com/crm/identity/security/JwtService.java
 M microservices/identity-service/src/main/java/com/crm/identity/service/IAuthenticationService.java
 M microservices/identity-service/src/main/java/com/crm/identity/service/impl/AuthenticationServiceImpl.java
 M microservices/identity-service/src/main/java/com/crm/identity/service/impl/IdentityServiceImpl.java
 M microservices/identity-service/src/main/resources/application.yml
 M microservices/identity-service/src/test/java/com/crm/identity/AccessTokenValidationAvailabilityTest.java
 M microservices/identity-service/src/test/java/com/crm/identity/AccessTokenValidationIntegrationTest.java
 M microservices/identity-service/src/test/java/com/crm/identity/AdminEmployeeCreationTest.java
 M microservices/identity-service/src/test/java/com/crm/identity/SessionSecurityIntegrationTest.java
 M microservices/identity-service/src/test/java/com/crm/identity/UidIssuanceIntegrationTest.java
 M microservices/identity-service/src/test/java/com/crm/identity/security/AccessTokenClaimsTest.java
 M microservices/identity-service/src/test/java/com/crm/identity/security/AccessTokenValidationServiceTest.java
 M microservices/identity-service/src/test/java/com/crm/identity/security/UidIssuanceTest.java
 M microservices/identity-service/src/test/java/com/crm/identity/service/impl/AuthenticationServiceImplTest.java
 M microservices/identity-service/src/test/java/com/crm/identity/service/impl/DirectoryRolesTest.java
 M microservices/identity-service/src/test/java/com/crm/identity/service/impl/RefreshTokenSecurityTest.java
 M microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoiceController.java
 M microservices/invoice-service/src/main/java/com/crm/invoice/controller/PersonalInvoiceController.java
 M microservices/invoice-service/src/main/java/com/crm/invoice/dto/InvoiceRequest.java
 M microservices/invoice-service/src/main/java/com/crm/invoice/exception/GlobalExceptionHandler.java
 M microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoicePaymentRepository.java
 M microservices/invoice-service/src/main/java/com/crm/invoice/service/PersonalInvoiceService.java
 M microservices/invoice-service/src/main/java/com/crm/invoice/service/impl/InvoiceServiceImpl.java
 M microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceServiceIntegrationTest.java
 M microservices/payroll-service/src/main/java/com/crm/payroll/dto/PayrollRequest.java
 M microservices/payroll-service/src/main/java/com/crm/payroll/exception/GlobalExceptionHandler.java
 M microservices/payroll-service/src/main/java/com/crm/payroll/service/impl/PayrollServiceImpl.java
?? b.png
?? docker-compose.personal-spaces.yml
?? docker-compose.validation.sprint6c.yml
?? docker-compose.validation.yml
?? docker/
?? "docs/CDC CRM 1.pdf"
?? docs/LOT_1A_FIABILISATION.md
?? docs/LOT_1B_SECURISATION_COMPTES_SESSIONS.md
?? docs/LOT_1C_ASSOCIATIONS_ESPACES_PERSONNELS.md
?? docs/LOT_1C_VALIDATION_INTEGREE.md
?? docs/PLAN_FINALISATION_CRM.md
?? frontend/src/app/core/testing/client-invoice-statistics.json
?? frontend/src/app/features/documents/
?? frontend/src/app/features/employees/pages/employee-detail/employee-dossier.component.spec.ts
?? frontend/src/app/shared/financial-validators.spec.ts
?? frontend/src/app/shared/financial-validators.ts
?? microservices/common-security/src/test/java/com/crm/security/SessionBoundTokenCompatibilityTest.java
?? microservices/customer-service/src/main/resources/db/migration/V1__initial_schema.sql
?? microservices/customer-service/src/test/java/com/crm/customer/CustomerAssociationCreationPolicyTest.java
?? microservices/customer-service/src/test/java/com/crm/customer/SqlErrorPrivacyTest.java
?? microservices/department-service/src/main/resources/db/
?? microservices/department-service/src/test/java/com/crm/department/SqlErrorPrivacyTest.java
?? microservices/employee-service/src/main/resources/db/
?? microservices/employee-service/src/test/java/com/crm/employee/SqlErrorPrivacyTest.java
?? microservices/fees-service/src/main/resources/db/
?? microservices/fees-service/src/test/java/com/crm/fees/SqlErrorPrivacyTest.java
?? microservices/hr-service/src/main/java/com/crm/hr/controller/PersonalDocumentController.java
?? microservices/hr-service/src/main/java/com/crm/hr/service/PersonalDocumentService.java
?? microservices/hr-service/src/main/resources/db/migration/V1__initial_schema.sql
?? microservices/hr-service/src/main/resources/db/migration/V20261101_02__employee_document_visibility.sql
?? microservices/hr-service/src/test/java/com/crm/hr/PersonalDocumentHttpContractTest.java
?? microservices/hr-service/src/test/java/com/crm/hr/PersonalDocumentSecurityTest.java
?? microservices/hr-service/src/test/java/com/crm/hr/SqlErrorPrivacyTest.java
?? microservices/hr-service/src/test/java/com/crm/hr/service/DocumentStoragePolicyTest.java
?? microservices/hr-service/src/test/java/com/crm/hr/service/PersonalDocumentPolicyTest.java
?? microservices/identity-service/src/main/resources/db/
?? microservices/identity-service/src/test/java/com/crm/identity/LogoutHttpContractTest.java
?? microservices/identity-service/src/test/java/com/crm/identity/SessionLifecyclePolicyTest.java
?? microservices/identity-service/src/test/java/com/crm/identity/SqlErrorPrivacyTest.java
?? microservices/identity-service/src/test/java/com/crm/identity/UsableSuperAdminPolicyTest.java
?? microservices/identity-service/src/test/java/com/crm/identity/security/UnifiedAuthenticationPolicyTest.java
?? microservices/invoice-service/src/main/java/com/crm/invoice/service/FinancialValidation.java
?? microservices/invoice-service/src/main/resources/db/migration/V1__initial_schema.sql
?? microservices/invoice-service/src/main/resources/db/migration/V20261031_01__widen_invoice_sequence_id.sql
?? microservices/invoice-service/src/test/java/com/crm/invoice/ClientStatisticsContractTest.java
?? microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceFinancialPolicyTest.java
?? microservices/invoice-service/src/test/java/com/crm/invoice/PersonalPaymentPolicyTest.java
?? microservices/invoice-service/src/test/java/com/crm/invoice/SqlErrorPrivacyTest.java
?? microservices/payroll-service/src/main/java/com/crm/payroll/service/FinancialValidation.java
?? microservices/payroll-service/src/main/resources/db/migration/V1__initial_schema.sql
?? microservices/payroll-service/src/test/java/com/crm/payroll/PayrollFinancialPolicyTest.java
?? microservices/payroll-service/src/test/java/com/crm/payroll/SqlErrorPrivacyTest.java
```

## 13. Prochaine étape recommandée

Revoir le partage documentaire et l'overlay, puis exécuter le protocole intégré sur une pile isolée et sauvegardée, bornée en mémoire, pour valider ensemble les Lots 1A, 1B et 1C. Le code et les tests ciblés sont prêts pour cette étape ; le déploiement n'est pas certifié.
