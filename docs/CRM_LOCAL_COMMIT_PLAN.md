# Plan de stabilisation et de commits locaux CRM

> Archive documentaire : les constats, chiffres, anciens commits et autorisations ci-dessous décrivent la campagne datée du rapport. Ils ne constituent pas un état courant ni un plan à réappliquer sur `publication-clean`. Toute opération réelle nécessite une nouvelle vérification et l’autorisation adaptée.

Date : 20 octobre 2026, Africa/Tunis. Branche `microservices`.

**VERDICT : READY WITH RESERVATIONS pour le plan de commits locaux. Aucun staging ni commit autorisé par ce rapport. Le push GitHub reste BLOCKED.**

> Actualisation documentaire du 22 octobre 2026 : la stabilisation Git est en attente. Le groupe 1 a été commité localement (`16b51887c9c7a1f7ad40c6726602a69d41b39d2a`) ; le groupe 2 n'est pas confirmé comme commité. Les chiffres et états ci-dessous décrivent la campagne initiale du 20 octobre. Les rapports intermédiaires retirés ont été exclus de la liste du groupe 5, qui contient maintenant 15 chemins. Les ajouts 2B et ces changements documentaires imposent une nouvelle revue du manifeste avant tout staging ; ce document n'autorise aucune opération Git.

## 1. État constaté et portée

Documents consultés : `docs/CRM_PRE_COMMIT_AUDIT.md`, `docs/CRM_SECRET_ROTATION_READINESS.md` et rapports des lots concernés. Inventaire et examen statique des sources locales, des différences avec HEAD et des fichiers nouveaux. Aucun changement de code, configuration, secret, base, conteneur ou historique.

- HEAD : `00c2870956826adbb32b9cc22b9506879f5cdfdd`.
- Branche active : `microservices` ; index vide ; aucune suppression suivie détectée.
- Par rapport à la référence locale `origin/microservices` : **13 commits en avance, 0 en retard**. Aucun fetch ni contrôle réseau effectué ici : ce n'est pas une mesure fraîche de GitHub.
- Avant ce rapport : **92 fichiers suivis modifiés et 112 fichiers non suivis**, soit 204 fichiers.
- Répartition : **202 candidats textuels** et les deux binaires exclus. Avec ce rapport : **203 candidats**, 205 fichiers en attente au total.
- **54 568 fichiers ignorés** à l'instant du relevé, notamment dépendances, caches, profils et sorties de tests. Aucun de ces fichiers n'est supprimé.
- Aucun chemin candidat ne relève des répertoires générés ou extensions dump/log/base/clé recherchés.
- `git diff --check` réussit. Des avertissements LF/CRLF existent ; ne pas les traiter par une normalisation globale non autorisée.

Les sources Sprint 1B sont présentes : validation Identity, uid, client commun obligatoire, filtres, consommateurs, protections Gateway et tests. Le Sprint 2A.1 est présent dans le DTO/mapping Identity et la validation DirectoryAccount HR ; le lot 2A.2 est présent dans la feature Angular users, la navigation, les associations/réaffectations et leurs tests. Cette présence statique ne prouve pas l'état des images actives.

Les **915 tests Java, 792 tests Angular et le build Angular production** sont les résultats de l'audit précédent, non relancés ici. L'avertissement de taille du bundle reste ouvert. Aucun préfixe des cinq futurs commits n'a été construit isolément.

## 2. Confidentialité et exclusions

La comparaison exacte en mémoire des deux valeurs sensibles locales avec les **202 candidats textuels actuels** ne retrouve aucune occurrence. Les recherches ciblées de JWT complets, clés privées et certains formats de tokens fournisseur ne remontent pas d'alerte. Ce contrôle ne certifie pas l'absence de tout secret : il ne démontre ni la provenance des fixtures ni le contenu des binaires/profils ignorés.

Les différences de configuration retirent des identifiants ou clés littérales historiques dans `.env.example`, le workflow, README et le backend legacy. Ces retraits sont utiles mais **les lignes supprimées des diffs peuvent elles-mêmes contenir des valeurs sensibles** : ne pas copier un diff brut dans un rapport, une conversation ou des logs publics. Un commit de suppression ne purge pas les objets Git parents.

Exclusions absolues de tout ajout futur : `.env`, variantes locales, secrets privés, fichiers temporaires de rotation, bundles DPAPI, états pending/receipts opérationnels, dumps réels, logs, profils navigateur, cookies, bases cache et artefacts `target/`, `node_modules/`, `dist/`, `.angular/`, `frontend/tmp/`. L'exemple `.env.example` est une exception admissible uniquement avec champs sensibles vides.

Exclusions par défaut, à conserver localement :
- `b.png` ;
- `docs/CDC CRM 1.pdf`.

Leur publication nécessite une décision distincte de confidentialité, provenance et droits. Aucun renommage, suppression ni déplacement proposé.

### Fixtures et documents nécessitant une décision

Le commit déjà créé mais non poussé `65cf538` inclut :
- `scripts/demo/Import-TechNova.ps1`
- `scripts/demo/README.md`
- `scripts/demo/Test-CRM-MVP.Local.ps1`
- `scripts/demo/Test-CRM-MVP.ps1`
- `scripts/demo/Test-TechNova.Local.ps1`
- `scripts/demo/technova.json`

Ces six fichiers sont déjà dans HEAD : ils ne font pas partie des nouvelles listes de staging. Le test local contient des identifiants de fixture ; le contexte est simulé et les documents parlent de démonstration. Aucune correspondance avec les deux secrets locaux n'est détectée. **Leur non-réutilisation ailleurs et la provenance synthétique des noms, coordonnées et montants doivent être confirmées par l'utilisateur avant publication.** Aucun login ou script demo exécuté.

Les documents `docs/SPRINT1B_E2E_REVIEW.md`, `docs/SPRINT1B_LOT_F_REVIEW.md` et `docs/SPRINT2A_LOT_1_REVIEW.md` contiennent des occurrences d'adresses ou exemples d'identité : revue du contexte avant publication, sans présumer qu'il s'agit de personnes réelles. Les autres rapports contiennent aussi des chemins locaux et des observations techniques ; leur caractère publiable reste à approuver. Aucun contenu n'est expurgé automatiquement.

Les helpers de rotation et leurs procédures sont admissibles comme préparation explicitement réservée ; ils ne sont pas validés pour exécution. Le runbook contient notamment une ancienne exception de retour aux secrets exposés et une attente Department EMPLOYEE obsolète, détaillées dans le rapport de rotation. Leur future révision est une tâche séparée, pas une modification silencieuse pour faciliter les commits.

## 3. Cinq groupes proposés

Ordre : **1 → 2 → 3 → 4 → 5**. Snapshots de fichiers complets, sans prétendre reconstruire un commit par sprint historique. Les changements antérieurs au Sprint 1B restent identifiés dans le groupe 1 ; tous les fichiers non committés ne sont pas attribués aux sprints récents.

Chaque groupe exclut tous les chemins des autres groupes et toutes les exclusions globales. Les listes ci-dessous sont exactes, sans joker ni ajout récursif. Elles constituent un manifeste de préparation, pas une commande de staging.

### Groupe 1 — Configuration et monolithe

Message proposé : `fix(config): externaliser les secrets et isoler les tests legacy`.

Conserver ensemble POM legacy, configuration H2, validation JWT et tests correspondants. Les procédures de rotation sont des préparations historiques : leurs réserves restent explicites et elles ne doivent pas être exécutées telles quelles. Aucune rotation autorisée par un commit de ces fichiers. Les liens README vers des éléments des groupes suivants peuvent rester temporairement non résolus. Le script Test-RotationPlan peut dépendre du Compose final du groupe 3 : son succès sur ce préfixe n'est pas établi.

Chemins exacts : **14 fichiers**.

- `.env.example`
- `.github/workflows/build.yml`
- `.gitignore`
- `README.md`
- `backend/pom.xml`
- `backend/src/main/java/com/crm/security/jwt/JwtService.java`
- `backend/src/main/resources/application.properties`
- `backend/src/test/java/com/crm/security/jwt/JwtConfigurationTest.java`
- `backend/src/test/resources/application.properties`
- `docker-compose.rotation.yml`
- `docs/ROTATION_EXECUTION.md`
- `docs/ROTATION_SECRETS.md`
- `scripts/rotation/Rotation-Helpers.ps1`
- `scripts/rotation/Test-RotationPlan.ps1`

### Groupe 2 — Identity

Message proposé : `fix(identity): valider les sessions et exposer un annuaire multi-roles`.

Conserver dans un seul snapshot émission uid Login/Refresh, filtre, politique de validation, endpoint interne, annuaire enrichi et tests. Le DTO conserve roleType et ajoute roles/accountNonLocked ; ne pas isoler les consommateurs HR dans un déploiement utilisant un Identity ancien. Dépendance de séquence : groupe 1 ; aucune dépendance de compilation à la nouvelle common-security identifiée dans ce groupe. Vérifier néanmoins le préfixe réel avant de le déclarer vert.

Chemins exacts : **24 fichiers**.

- `microservices/identity-service/src/main/java/com/crm/identity/config/SecurityConfig.java`
- `microservices/identity-service/src/main/java/com/crm/identity/controller/AccessTokenValidationController.java`
- `microservices/identity-service/src/main/java/com/crm/identity/controller/AuthenticationController.java`
- `microservices/identity-service/src/main/java/com/crm/identity/dto/LoginResponse.java`
- `microservices/identity-service/src/main/java/com/crm/identity/dto/UserResponse.java`
- `microservices/identity-service/src/main/java/com/crm/identity/repository/RefreshTokenRepository.java`
- `microservices/identity-service/src/main/java/com/crm/identity/security/AccessTokenValidationFilter.java`
- `microservices/identity-service/src/main/java/com/crm/identity/security/AccessTokenValidationService.java`
- `microservices/identity-service/src/main/java/com/crm/identity/security/CustomUserDetailsService.java`
- `microservices/identity-service/src/main/java/com/crm/identity/security/JwtAuthenticationFilter.java`
- `microservices/identity-service/src/main/java/com/crm/identity/security/JwtService.java`
- `microservices/identity-service/src/main/java/com/crm/identity/service/impl/AuthenticationServiceImpl.java`
- `microservices/identity-service/src/main/java/com/crm/identity/service/impl/IdentityServiceImpl.java`
- `microservices/identity-service/src/test/java/com/crm/identity/AccessTokenValidationAvailabilityTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/AccessTokenValidationIntegrationTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/AdminEmployeeCreationTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/SessionSecurityIntegrationTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/UidIssuanceIntegrationTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/security/AccessTokenClaimsTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/security/AccessTokenValidationServiceTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/security/JwtServiceTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/security/UidIssuanceTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/service/impl/DirectoryRolesTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/service/impl/RefreshTokenSecurityTest.java`

### Groupe 3 — Sécurité distribuée et services métier

Message proposé : `feat(security): integrer la validation Identity et proteger les services`.

Groupe atomique : common-security stricte, sept configurations consommatrices, doubles Identity de test, adaptations interservices, HR multi-rôles, PDF/POM, Gateway réactive et Compose. Dépend du contrat Identity du groupe 2 à l'exécution. La nouvelle auto-configuration impose une destination Identity : publier la bibliothèque sans les propriétés et fixtures peut faire échouer les contextes Spring. Les PDF requièrent leurs dépendances Maven. Aucun découpage de hunks proposé pour contourner ces liens.

Chemins exacts : **99 fichiers**.

- `docker-compose.yml`
- `microservices/common-security/src/main/java/com/crm/security/AccountValidationClient.java`
- `microservices/common-security/src/main/java/com/crm/security/AccountValidationProperties.java`
- `microservices/common-security/src/main/java/com/crm/security/AccountValidationResult.java`
- `microservices/common-security/src/main/java/com/crm/security/JwtAuthenticationFilter.java`
- `microservices/common-security/src/main/java/com/crm/security/JwtSecurityAutoConfiguration.java`
- `microservices/common-security/src/main/java/com/crm/security/JwtService.java`
- `microservices/common-security/src/test/java/com/crm/security/AccountValidationClientTest.java`
- `microservices/common-security/src/test/java/com/crm/security/AccountValidationConfigurationTest.java`
- `microservices/common-security/src/test/java/com/crm/security/AccountValidationHttpTest.java`
- `microservices/common-security/src/test/java/com/crm/security/AccountValidationPerformanceTest.java`
- `microservices/common-security/src/test/java/com/crm/security/JwtAuthenticationFilterTest.java`
- `microservices/common-security/src/test/java/com/crm/security/JwtServiceTest.java`
- `microservices/customer-service/src/main/resources/application.yml`
- `microservices/customer-service/src/test/java/com/crm/customer/CentralizedJwtSecurityTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/CustomerReadSecurityTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/CustomerServiceIntegrationTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/CustomerServiceSmokeTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/security/AccountValidationConfigurationTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/security/TestIdentitySupport.java`
- `microservices/department-service/src/main/resources/application.yml`
- `microservices/department-service/src/test/java/com/crm/department/CentralizedJwtSecurityTest.java`
- `microservices/department-service/src/test/java/com/crm/department/DepartmentServiceIntegrationTest.java`
- `microservices/department-service/src/test/java/com/crm/department/DepartmentServiceSmokeTest.java`
- `microservices/department-service/src/test/java/com/crm/department/security/AccountValidationConfigurationTest.java`
- `microservices/department-service/src/test/java/com/crm/department/security/TestIdentitySupport.java`
- `microservices/employee-service/src/main/java/com/crm/employee/client/DepartmentClientConfiguration.java`
- `microservices/employee-service/src/main/java/com/crm/employee/client/DepartmentClientFallback.java`
- `microservices/employee-service/src/main/java/com/crm/employee/client/DepartmentClientFallbackFactory.java`
- `microservices/employee-service/src/main/java/com/crm/employee/exception/GlobalExceptionHandler.java`
- `microservices/employee-service/src/main/java/com/crm/employee/service/impl/EmployeeServiceImpl.java`
- `microservices/employee-service/src/main/resources/application.yml`
- `microservices/employee-service/src/test/java/com/crm/employee/CentralizedJwtSecurityTest.java`
- `microservices/employee-service/src/test/java/com/crm/employee/EmployeeReadSecurityTest.java`
- `microservices/employee-service/src/test/java/com/crm/employee/EmployeeServiceIntegrationTest.java`
- `microservices/employee-service/src/test/java/com/crm/employee/EmployeeServiceSmokeTest.java`
- `microservices/employee-service/src/test/java/com/crm/employee/client/DepartmentClientAuthorizationTest.java`
- `microservices/employee-service/src/test/java/com/crm/employee/client/DepartmentClientFallbackTest.java`
- `microservices/employee-service/src/test/java/com/crm/employee/security/AccountValidationConfigurationTest.java`
- `microservices/employee-service/src/test/java/com/crm/employee/security/TestIdentitySupport.java`
- `microservices/fees-service/src/main/resources/application.yml`
- `microservices/fees-service/src/test/java/com/crm/fees/CentralizedJwtSecurityTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/FeeReadSecurityTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/FeesServiceIntegrationTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/FeesServiceSmokeTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/security/AccountValidationConfigurationTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/security/TestIdentitySupport.java`
- `microservices/gateway-service/src/main/java/com/crm/gateway/security/InternalPathPolicy.java`
- `microservices/gateway-service/src/main/java/com/crm/gateway/security/InternalPathRoutingFilter.java`
- `microservices/gateway-service/src/main/java/com/crm/gateway/security/InternalPathWebFilter.java`
- `microservices/gateway-service/src/test/java/com/crm/gateway/ComposeSecurityConfigurationTest.java`
- `microservices/gateway-service/src/test/java/com/crm/gateway/GatewaySecurityRoutingTest.java`
- `microservices/gateway-service/src/test/java/com/crm/gateway/security/InternalPathPolicyTest.java`
- `microservices/hr-service/pom.xml`
- `microservices/hr-service/src/main/java/com/crm/hr/client/DirectoryAccount.java`
- `microservices/hr-service/src/main/java/com/crm/hr/client/LeaveDirectoryClient.java`
- `microservices/hr-service/src/main/java/com/crm/hr/controller/LeaveAssociationController.java`
- `microservices/hr-service/src/main/java/com/crm/hr/controller/LeavePdfController.java`
- `microservices/hr-service/src/main/java/com/crm/hr/exception/GlobalExceptionHandler.java`
- `microservices/hr-service/src/main/java/com/crm/hr/pdf/PdfReport.java`
- `microservices/hr-service/src/main/resources/application.yml`
- `microservices/hr-service/src/test/java/com/crm/hr/AttendanceServiceIntegrationTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/CentralizedJwtSecurityTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/HrAdministrativeReadSecurityTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/HrServiceIntegrationTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/HrServiceSmokeTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/LeaveAssociationReassignmentTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/LeavePdfTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/LeaveSecurityIntegrationTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/LeaveServiceIntegrationTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/TrainingServiceIntegrationTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/client/DirectoryAccountTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/client/LeaveDirectoryClientTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/security/AccountValidationConfigurationTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/security/TestIdentitySupport.java`
- `microservices/invoice-service/pom.xml`
- `microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoicePdfController.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/pdf/PdfReport.java`
- `microservices/invoice-service/src/main/resources/application.yml`
- `microservices/invoice-service/src/test/java/com/crm/invoice/CentralizedJwtSecurityTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/InvoicePdfTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceReadSecurityTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceServiceIntegrationTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceServiceSmokeTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/PdfPaginationTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/security/AccountValidationConfigurationTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/security/TestIdentitySupport.java`
- `microservices/payroll-service/pom.xml`
- `microservices/payroll-service/src/main/java/com/crm/payroll/controller/PayrollPdfController.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/pdf/PdfReport.java`
- `microservices/payroll-service/src/main/resources/application.yml`
- `microservices/payroll-service/src/test/java/com/crm/payroll/CentralizedJwtSecurityTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollPdfTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollReadSecurityTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollServiceIntegrationTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollServiceSmokeTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/security/AccountValidationConfigurationTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/security/TestIdentitySupport.java`
- `scripts/Test-SecureConfiguration.ps1`

### Groupe 4 — Frontend Angular

Message proposé : `feat(frontend): fiabiliser les sessions et administrer les comptes employes`.

Inclure routes lazy, modèles/services utilisateurs, composants, navigation, returnUrl, tests, session/intercepteur et module PDF partagé ensemble. Une route users sans son composant, ou un import PDF sans son module, crée un état potentiellement non compilable. Dépendances d'exécution : Identity enrichi et HR des groupes 2/3. Les tests HTTP simulés Angular ne prouvent pas ces interactions réelles. Aucun nouveau droit serveur ne découle du menu.

Chemins exacts : **39 fichiers**.

- `frontend/src/app/app.routes.ts`
- `frontend/src/app/core/guards/auth.guard.spec.ts`
- `frontend/src/app/core/guards/auth.guard.ts`
- `frontend/src/app/core/guards/role.guard.spec.ts`
- `frontend/src/app/core/guards/role.guard.ts`
- `frontend/src/app/core/http/pdf-download-context.ts`
- `frontend/src/app/core/interceptors/auth.interceptor.spec.ts`
- `frontend/src/app/core/interceptors/auth.interceptor.ts`
- `frontend/src/app/core/navigation/crm-navigation.ts`
- `frontend/src/app/core/services/auth-navigation.ts`
- `frontend/src/app/core/services/auth.service.spec.ts`
- `frontend/src/app/core/services/auth.service.ts`
- `frontend/src/app/core/services/session-unavailable.ts`
- `frontend/src/app/features/home/home.component.spec.ts`
- `frontend/src/app/features/invoices/invoice-detail.component.html`
- `frontend/src/app/features/invoices/invoice-detail.component.ts`
- `frontend/src/app/features/invoices/invoices.scss`
- `frontend/src/app/features/leaves/leave.service.spec.ts`
- `frontend/src/app/features/leaves/leave.service.ts`
- `frontend/src/app/features/leaves/leaves-page.component.html`
- `frontend/src/app/features/leaves/leaves-page.component.spec.ts`
- `frontend/src/app/features/leaves/leaves-page.component.ts`
- `frontend/src/app/features/payroll/payroll-detail.component.html`
- `frontend/src/app/features/payroll/payroll-detail.component.ts`
- `frontend/src/app/features/payroll/payroll.scss`
- `frontend/src/app/features/users/admin-account-error.ts`
- `frontend/src/app/features/users/onboarding.integration.spec.ts`
- `frontend/src/app/features/users/user-directory.service.spec.ts`
- `frontend/src/app/features/users/user-directory.service.ts`
- `frontend/src/app/features/users/users-access.spec.ts`
- `frontend/src/app/features/users/users-page.component.html`
- `frontend/src/app/features/users/users-page.component.spec.ts`
- `frontend/src/app/features/users/users-page.component.ts`
- `frontend/src/app/navigation-availability.spec.ts`
- `frontend/src/app/shared/pdf-download/_pdf-detail.scss`
- `frontend/src/app/shared/pdf-download/pdf-download.component.spec.ts`
- `frontend/src/app/shared/pdf-download/pdf-download.component.ts`
- `frontend/src/app/shared/pdf-download/pdf-download.service.ts`
- `frontend/src/app/shared/pdf-download/pdf-security.integration.spec.ts`

### Groupe 5 — Documentation et préparation E2E

Message proposé : `docs(crm): documenter les validations et preparer les campagnes E2E`.

Inclure les rapports datés et la préparation E2E sans les confondre avec une exécution. Dépendances documentaires : groupes précédents. Le script de préparation écrit des artefacts lorsqu'il est lancé ; il n'est pas exécuté ici. Les anciens rapports peuvent garder leurs résultats historiques et réserves, mais leur publication exige une revue des exemples, observations d'exploitation et droits de diffusion.

Chemins encore présents après nettoyage documentaire : **15 fichiers**. Périmètre à revalider avant toute reprise Git.

- `RAPPORT_TESTS_GLOBAUX_CRM_2026-10-17.md`
- `REVUE_FINALE_CRM.md`
- `docs/ACCESS_TOKEN_SECURITY_PLAN.md`
- `docs/AUDIT_CONFORMITE_CDC.md`
- `docs/CRM_ROLE_BASED_SPACES_ROADMAP.md`
- `docs/PDF_ANGULAR_MANUAL_VALIDATION.md`
- `docs/PDF_DOCUMENTS.md`
- `docs/SPRINT1B_E2E_OPTIMIZATION_PLAN.md`
- `docs/SPRINT1B_E2E_REVIEW.md`
- `docs/SPRINT1B_LOT_F_REVIEW.md`
- `docs/SPRINT2A_LOT_1_REVIEW.md`
- `scripts/e2e/prepare-sprint1b.mjs`
- `docs/CRM_PRE_COMMIT_AUDIT.md`
- `docs/CRM_SECRET_ROTATION_READINESS.md`
- `docs/CRM_LOCAL_COMMIT_PLAN.md`

## 4. États intermédiaires et validations

| Préfixe | Risque à contrôler | Validation future sur le snapshot exact |
|---|---|---|
| Groupe 1 | POM/config/tests legacy indissociables ; scripts et liens pouvant anticiper les groupes suivants | Suite backend H2, syntaxe PowerShell ; noter les dépendances des validations Compose plutôt que lancer un script d'exploitation |
| Groupes 1–2 | Identity strict émet uid, ancien contrat côté autres services ; cohérence globale encore incomplète | Suite Identity complète ; pas de livraison de ce préfixe |
| Groupes 1–3 | Nouvelle bibliothèque exige propriétés et clients de test ; cache SNAPSHOT peut masquer un oubli | Construire common-security depuis le snapshot puis les huit services concernés et Gateway/Discovery, séquentiellement, avec H2/doubles |
| Groupes 1–4 | Imports Angular, session, nouvelle route et PDF doivent être présents ensemble | Suite Angular complète et build production ; vérifier les contrats HTTP simulés et les quatre rôles |
| Groupes 1–5 | Documentation peut annoncer des résultats historiques ou référencer un environnement non validé | Revue documentaire, syntaxe du préparateur E2E, manifeste complet et état Git final |

Aucun état intermédiaire non compilable n'est démontré par une exécution dans cette mission. Les risques ci-dessus sont identifiés statiquement. Une compilation du working tree complet pendant qu'un sous-ensemble seulement est indexé ne valide pas ce sous-ensemble : elle pourrait utiliser des fichiers futurs non indexés.

Après autorisation de commit, valider les snapshots dans un répertoire de contrôle isolé contenant exactement les fichiers de l'index ou du commit retenu, sans .env, sans secrets d'exploitation ni accès MySQL/Docker. Autoriser explicitement cette préparation si elle requiert de nouvelles écritures. Construire common-security depuis les sources du snapshot avec un cache Maven maîtrisé ; ne pas se fier au seul JAR SNAPSHOT préexistant. Ne pas utiliser stash, reset ou changement de branche pour fabriquer les préfixes dans le répertoire de travail actuel.

Si un préfixe échoue, arrêter la séquence et proposer une adaptation des groupes avec ses preuves ; ne pas modifier le code, les tests ou la sécurité pour obtenir artificiellement cinq commits verts.

## 5. Vérifications avant chaque commit

Étapes futures, après autorisation utilisateur distincte de staging/commit :

1. Relever branche, HEAD et état Git ; vérifier qu'aucun autre outil/utilisateur n'a changé les candidats depuis la revue. Comparer les contenus, pas seulement les dates.
2. Vérifier que l'index est vide avant le premier groupe, puis ne contient que les chemins explicitement approuvés du groupe courant. Arrêter en présence d'un fichier inattendu ; aucune correction destructive automatique.
3. Lire le contenu complet des nouveaux fichiers et le diff des fichiers suivis, avec masquage des valeurs sensibles historiques. Contrôler notamment .env.example vide, absence de défaut secret, isolation H2 et profils de test.
4. Après staging autorisé, contrôler localement :
   - `git diff --cached --name-status`
   - `git diff --cached --stat`
   - `git diff --cached --check`
   - revue expurgée de `git diff --cached`, dans un outil local maîtrisé ;
   - scan des blobs réellement indexés et du message de commit, pas uniquement du working tree.
5. Comparer la liste indexée au manifeste du groupe : zéro chemin supplémentaire, aucune suppression imprévue, aucune dépendance oubliée ; vérifier fichiers ignorés et binaires exclus.
6. Valider le snapshot selon le tableau précédent, sans services actifs ; consigner commande, version, résultats et limites. Les chiffres historiques ne sont pas une preuve du nouvel état.
7. Confirmer le message et le périmètre avant l'opération autorisée. Ne pas utiliser un ajout global ni une option de commit qui inclurait automatiquement tous les fichiers modifiés.

Ces commandes de lecture de l'index sont proposées pour la phase suivante ; aucun index candidat n'a été créé aujourd'hui.

### Après le dernier commit local

Vérifier que la différence cumulée avec le HEAD initial ne contient que les 203 candidats approuvés, ou le sous-ensemble explicitement retenu si une décision a changé le manifeste. Contrôler l'ordre des commits et l'absence de secrets dans leurs blobs/messages. Les deux binaires exclus doivent rester non suivis ; les fichiers ignorés restent locaux. Un working tree « propre » n'est donc pas un objectif à atteindre par suppression.

Effectuer une validation finale sur l'ensemble exact committé, avec common-security reconstruit, tests Java/Angular séquentiels, build production et vérifications statiques sans données réelles. Ne lancer aucune commande Docker dans cette phase sans autorisation : la validation Compose peut être préparée ou différée. Relever les échecs et tests non exécutés explicitement. Aucun push automatique après succès.

## 6. Treize commits existants et limites du découpage

Plage locale à revoir avant tout push :

```text
a376731 fix(securite): réserver les données administratives aux administrateurs
34d7788 fix(clients): aligner la longueur des adresses sur le contrat API
986f444 feat(frontend): intégrer la gestion des clients et des factures
a3dd488 fix(honoraires): aligner la description sur le contrat API
f469e7c feat(frontend): intégrer la gestion des honoraires
40b6d0f feat(frontend): intégrer la gestion de la paie
e84ed88 feat(frontend): finaliser le dashboard dynamique
40c0fb9 fix(config): securiser les configurations des microservices
61f3961 fix(validation): aligner les limites des formulaires et DTO
65cf538 test(demo): ajouter les donnees et scripts de validation du MVP
814a0ef docs(mvp): documenter les tests et les limites connues
2293cca feat(frontend): moderniser l'accueil et la navigation CRM
00c2870 feat(frontend): finaliser l'interface CRM et l'expérience utilisateur
```

Le scan du rapport de rotation ne retrouvait aucune des deux valeurs locales dans les diffs de cette plage. Elles restent présentes dans des ancêtres déjà publiés. Ni le nouveau plan ni un nouveau commit ne les effacent.

Les cinq nouveaux groupes s'ajouteraient aux treize commits ; ils ne les remplacent pas. Un fichier déjà présent dans ces commits ne peut être exclu d'un futur push en l'omettant simplement du prochain staging. Si les fixtures ou un ancien contenu sont refusés à publication, suspendre le push et soumettre une décision de traitement de l'historique ; aucune réécriture autorisée ici.

## 7. Autorisation des commits locaux et autorisation du push

### Conditions pour les commits locaux

- Autorisation explicite de l'utilisateur pour les groupes et messages retenus, avec les exclusions maintenues.
- Accord sur les documents et fixtures dont la provenance reste à confirmer, ou maintien d'une réserve de publication clairement documentée. Aucun nouveau secret réel ou contenu non approuvé ne doit être ajouté.
- Revue et scan de l'index réel avant chaque commit ; vérification des dépendances et du snapshot exact.
- Préservation des fichiers locaux, de la branche, des secrets et des services ; aucune opération d'exploitation implicite.
- Compréhension que les procédures de rotation sont des préparations à réviser et ne seront pas exécutées par cette séquence.

La présence de secrets compromis dans un .env ignoré est un risque opérationnel à traiter, mais leur rotation n'est pas techniquement requise pour enregistrer localement des modifications qui ne les contiennent pas. Un commit local ne publie pas la branche et ne vaut pas approbation du push. Cette distinction ne lève aucune réserve du rapport de rotation.

### Conditions distinctes pour le push GitHub

- Rotation des valeurs compromises encore actives, ou preuve de leur non-utilisation, suivant un plan autorisé et validé ; la mission de rotation reste NEEDS CLARIFICATION.
- Confirmation explicite de la provenance et de la non-réutilisation des identifiants demo et du caractère publiable des données/documentations.
- Revue de toute la plage effectivement envoyée, incluant les treize commits existants et les futurs commits ; scan de secrets sur les blobs et l'historique concernés.
- Relecture fraîche du distant, avance/retard, remote de destination origin/microservices, permissions et protections ; arrêter si une divergence apparaît, sans rebase ou force-push automatique.
- Validation finale du code réellement committé, contrôles CI et réserves documentées ; autorisation explicite et séparée de push.

Un push de code n'est pas un déploiement. Le Sprint 1B E2E reste en attente de capacité dédiée ; aucun succès isolé ne permet de le déclarer totalement validé, ni d'autoriser une livraison ou de commencer le Sprint 2B.

## 8. Bilan de la préparation

Seul `docs/CRM_LOCAL_COMMIT_PLAN.md` est créé. Aucun git add, commit, push, reset, stash, rebase, changement de branche, suppression de fichier, accès MySQL, commande Docker ou changement de secret. Les contenus préexistants sont conservés.

**VERDICT FINAL : READY WITH RESERVATIONS.** Le manifeste local est complet et les cinq groupes sont préparés ; leur exécution attend l'autorisation utilisateur et la vérification de chaque index/snapshot. **Le push GitHub demeure BLOCKED**, indépendamment de cette préparation locale.
