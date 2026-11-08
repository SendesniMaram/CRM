# Lot 1B — Sécurisation des comptes et des sessions CRM

Date : 1er novembre 2026. Branche conservée : publication-clean.

## A. Résumé des corrections

Les anomalies AUD-06, AUD-07 et AUD-08 sont corrigées dans le périmètre du code et des tests ciblés. La validation intégrée MySQL/Gateway/navigateur reste différée.

- Ajout de POST /api/auth/logout, exposé par la route Gateway existante /identity/api/auth/logout. Une requête valide retourne 204, sans contenu et avec Cache-Control: no-store, y compris si le token présenté est déjà révoqué ou inconnu. Les entrées vides ou trop longues retournent 400.
- Chaque session possède un UUID sid. Le JWT contient ce sid ; les refresh tokens successifs de cette session conservent le même identifiant.
- Déconnexion, désactivation et changement sensible de rôle révoquent les sessions concernées côté serveur. La réactivation ne remet aucun token à disposition.
- Le filtre public Identity et son endpoint interne utilisent désormais le même AccessTokenValidationService. Aucun ancien JWT n'obtient un rôle nouvellement ajouté en base.
- Les modifications administratives prennent un verrou commun sur la ligne de rôle SUPER_ADMIN, puis les lignes des utilisateurs. Le dernier SUPER_ADMIN activé et non verrouillé ne peut pas perdre son rôle. L'auto-désactivation reste interdite.
- Angular supprime immédiatement ses credentials à la déconnexion, effectue la révocation serveur et distingue une confirmation serveur d'une simple déconnexion locale. Une réponse tardive ne restaure pas une session ; les credentials reçus tardivement sont également présentés au logout.
- Une modification de ses propres rôles ferme immédiatement la session de l'administrateur dans l'interface. Les erreurs 401/403 et le renouvellement partagé conservent leurs protections contre les boucles.
- Aucun fichier du Lot 1A n'a été modifié. Aucune migration historique n'a été modifiée ou exécutée.

## B. Politique de session retenue

| Événement | Access token | Refresh token et session | Comportement Angular |
|---|---|---|---|
| Connexion réussie | Nouveau JWT, UID persisté et sid de session | Révocation des anciennes sessions du compte ; création d'une session neuve | Stockage de la nouvelle session |
| Renouvellement | Nouveau JWT avec le même sid ; les anciens access tokens restent valides jusqu'à leur expiration tant que la session reste active | Token présenté révoqué, nouveau refresh token à usage unique avec le même sid | Un renouvellement partagé pour les requêtes simultanées |
| Déconnexion | Refusé dès que la révocation serveur est validée | Révocation de toute la famille du sid concerné, sans toucher une connexion ultérieure avec un autre sid | Effacement local immédiat et requête de logout bornée à 5 secondes |
| Backend indisponible au logout | Révocation distante non démontrable | Aucun succès serveur annoncé ; la connexion suivante révoquera les anciennes sessions | Effacement local maintenu, message fixe sans corps technique ni token |
| Désactivation | Anciennes sessions refusées après commit | Tous les refresh tokens du compte révoqués dans la même transaction | Le prochain refus définitif conduit à une reconnexion |
| Réactivation | Les anciennes sessions restent invalides | Aucune restauration ; une connexion neuve est nécessaire | Pas de rétablissement artificiel |
| Ajout/retrait sensible de rôle via l'administration | Ancien JWT invalidé, même si le rôle a été ajouté | Tous les refresh tokens du compte révoqués ; connexion neuve nécessaire | Fermeture immédiate pour sa propre modification ; sinon refus au prochain appel |
| Remplacement par exactement les mêmes rôles | Inchangé | Pas de révocation inutile | Session conservée |
| Expiration | Une heure après émission | Sept jours après création/rotation, selon le fonctionnement existant | Renouvellement si possible ; refus définitif si le refresh est invalide |
| Réutilisation d'un refresh révoqué | Aucun nouvel access token émis | 401 ; aucune nouvelle session créée | Pas de boucle de renouvellement |

### Contrat JWT final

L'authentification vérifie la signature, l'expiration, le subject non vide, l'UID positif canonique sous forme de chaîne, les rôles connus, enabled=true, le sid UUID canonique, l'existence de l'utilisateur, la concordance UID/username, l'état activé/non verrouillé, l'égalité des rôles signés et courants, et la présence d'une session active non expirée.

Les helpers JWT d'inspection locale ne suffisent pas à authentifier une requête. Les filtres utilisent exclusivement la validation finale. Les erreurs d'identité/session produisent 401 ; une indisponibilité de validation produit 503 et ne donne aucun accès au contrôleur métier.

common-security conserve son contrôle local signature/UID/claims et sa confirmation Identity obligatoire, sans cache positif. Le sid supplémentaire est compatible avec les sept consommateurs. C'est Identity qui tranche la validité de la session. Les réponses 401/503 d'Identity sont refusées par les filtres partagés. Aucun assouplissement des services métier n'est ajouté.

### Concurrence et transactions

Ordre des verrous administratifs : rôle SUPER_ADMIN commun, utilisateur acteur, utilisateur cible, lignes de refresh. Les écritures de session prennent l'utilisateur avant le refresh token. Après une attente de verrou, EntityManager.refresh avec verrou pessimiste recharge les objets pour éviter une décision sur une entité périmée dans le cache JPA/OSIV.

Le comptage des SUPER_ADMIN utilisables inclut uniquement enabled=true et accountNonLocked=true. Un compte désactivé ou verrouillé ne protège pas artificiellement le dernier compte utilisable. Les restrictions ADMIN sur les comptes privilégiés sont conservées et l'acteur est relu avant mutation.

La mutation des rôles/statuts et la révocation forment une seule transaction. Un échec de persistance n'est pas transformé en succès. Les révocations liées à un refresh rejeté pour compte inactif ou token expiré sont conçues pour être validées malgré le 401, comme dans le mécanisme préexistant.

Les tests unitaires vérifient les décisions et l'ordre des opérations ; ils ne prouvent pas les attentes de verrous, deadlocks ou rollbacks réels MySQL. Une requête déjà autorisée avant un commit de révocation peut terminer ; ce mécanisme n'annule pas rétroactivement les requêtes en cours.

### Migration additive préparée, non exécutée

Fichier : microservices/identity-service/src/main/resources/db/migration/V20261101_01__refresh_session_identity.sql.

Il ajoute refresh_tokens.session_id VARCHAR(36) nullable et un index (user_id, session_id, revoked, expiry_date). Il n'efface ni ne réécrit les lignes historiques. Les sessions historiques sans sid ne peuvent pas être renouvelées : les utilisateurs devront se reconnecter.

Cette migration et le nouveau modèle JPA doivent être livrés ensemble dans une phase autorisée. Avec Flyway activé, un démarrage ultérieur peut appliquer automatiquement ce fichier : aucun démarrage n'a été réalisé pendant le lot. La compatibilité MySQL de cette migration n'est pas déclarée validée.

## C. Fichiers modifiés

L'inventaire ci-dessous contient les fichiers du Lot 1B, y compris les corrections reprises de la tentative interrompue. Il exclut les fichiers préexistants sans rapport et les fichiers du Lot 1A.

| Fichier | Rôle |
|---|---|
| [frontend/src/app/core/interceptors/auth.interceptor.spec.ts](../frontend/src/app/core/interceptors/auth.interceptor.spec.ts) | Tests Angular de session, intercepteur ou administration |
| [frontend/src/app/core/services/auth.service.spec.ts](../frontend/src/app/core/services/auth.service.spec.ts) | Tests Angular de session, intercepteur ou administration |
| [frontend/src/app/core/services/auth.service.ts](../frontend/src/app/core/services/auth.service.ts) | Logout distant, effacement local, notifications et réponses tardives |
| [frontend/src/app/features/auth/login/login.component.html](../frontend/src/app/features/auth/login/login.component.html) | Affichage de messages de session fixes sur la connexion |
| [frontend/src/app/features/auth/login/login.component.ts](../frontend/src/app/features/auth/login/login.component.ts) | Affichage de messages de session fixes sur la connexion |
| [frontend/src/app/features/users/users-page.component.spec.ts](../frontend/src/app/features/users/users-page.component.spec.ts) | Tests Angular de session, intercepteur ou administration |
| [frontend/src/app/features/users/users-page.component.ts](../frontend/src/app/features/users/users-page.component.ts) | Fermeture de sa propre session après modification des rôles |
| [microservices/common-security/src/test/java/com/crm/security/SessionBoundTokenCompatibilityTest.java](../microservices/common-security/src/test/java/com/crm/security/SessionBoundTokenCompatibilityTest.java) | Test unitaire ou MVC autonome de sécurité |
| [microservices/identity-service/src/main/java/com/crm/identity/config/SecurityConfig.java](../microservices/identity-service/src/main/java/com/crm/identity/config/SecurityConfig.java) | Câblage du validateur unique et placement du filtre |
| [microservices/identity-service/src/main/java/com/crm/identity/controller/AuthenticationController.java](../microservices/identity-service/src/main/java/com/crm/identity/controller/AuthenticationController.java) | Contrat HTTP de connexion, refresh et logout |
| [microservices/identity-service/src/main/java/com/crm/identity/dto/RefreshTokenRequest.java](../microservices/identity-service/src/main/java/com/crm/identity/dto/RefreshTokenRequest.java) | Validation et borne de taille du token présenté |
| [microservices/identity-service/src/main/java/com/crm/identity/entity/RefreshToken.java](../microservices/identity-service/src/main/java/com/crm/identity/entity/RefreshToken.java) | Identifiant UUID de session persisté |
| [microservices/identity-service/src/main/java/com/crm/identity/repository/RefreshTokenRepository.java](../microservices/identity-service/src/main/java/com/crm/identity/repository/RefreshTokenRepository.java) | Verrous, sessions actives, révocation et administrateurs utilisables |
| [microservices/identity-service/src/main/java/com/crm/identity/repository/UserRepository.java](../microservices/identity-service/src/main/java/com/crm/identity/repository/UserRepository.java) | Verrous, sessions actives, révocation et administrateurs utilisables |
| [microservices/identity-service/src/main/java/com/crm/identity/security/AccessTokenValidationFilter.java](../microservices/identity-service/src/main/java/com/crm/identity/security/AccessTokenValidationFilter.java) | Contrat JWT et validation cohérente des comptes/sessions |
| [microservices/identity-service/src/main/java/com/crm/identity/security/AccessTokenValidationService.java](../microservices/identity-service/src/main/java/com/crm/identity/security/AccessTokenValidationService.java) | Contrat JWT et validation cohérente des comptes/sessions |
| [microservices/identity-service/src/main/java/com/crm/identity/security/JwtAuthenticationFilter.java](../microservices/identity-service/src/main/java/com/crm/identity/security/JwtAuthenticationFilter.java) | Contrat JWT et validation cohérente des comptes/sessions |
| [microservices/identity-service/src/main/java/com/crm/identity/security/JwtService.java](../microservices/identity-service/src/main/java/com/crm/identity/security/JwtService.java) | Contrat JWT et validation cohérente des comptes/sessions |
| [microservices/identity-service/src/main/java/com/crm/identity/service/IAuthenticationService.java](../microservices/identity-service/src/main/java/com/crm/identity/service/IAuthenticationService.java) | Contrat HTTP de connexion, refresh et logout |
| [microservices/identity-service/src/main/java/com/crm/identity/service/impl/AuthenticationServiceImpl.java](../microservices/identity-service/src/main/java/com/crm/identity/service/impl/AuthenticationServiceImpl.java) | Cycle de session, rotation, émission et logout transactionnels |
| [microservices/identity-service/src/main/java/com/crm/identity/service/impl/IdentityServiceImpl.java](../microservices/identity-service/src/main/java/com/crm/identity/service/impl/IdentityServiceImpl.java) | Révocation sensible et protection du dernier SUPER_ADMIN |
| [microservices/identity-service/src/main/resources/db/migration/V20261101_01__refresh_session_identity.sql](../microservices/identity-service/src/main/resources/db/migration/V20261101_01__refresh_session_identity.sql) | Migration additive préparée, NON exécutée |
| [microservices/identity-service/src/test/java/com/crm/identity/AccessTokenValidationAvailabilityTest.java](../microservices/identity-service/src/test/java/com/crm/identity/AccessTokenValidationAvailabilityTest.java) | Fixture d’intégration adaptée ; compilée, NON exécutée |
| [microservices/identity-service/src/test/java/com/crm/identity/AccessTokenValidationIntegrationTest.java](../microservices/identity-service/src/test/java/com/crm/identity/AccessTokenValidationIntegrationTest.java) | Fixture d’intégration adaptée ; compilée, NON exécutée |
| [microservices/identity-service/src/test/java/com/crm/identity/AdminEmployeeCreationTest.java](../microservices/identity-service/src/test/java/com/crm/identity/AdminEmployeeCreationTest.java) | Fixture d’intégration adaptée ; compilée, NON exécutée |
| [microservices/identity-service/src/test/java/com/crm/identity/LogoutHttpContractTest.java](../microservices/identity-service/src/test/java/com/crm/identity/LogoutHttpContractTest.java) | Test unitaire ou MVC autonome de sécurité |
| [microservices/identity-service/src/test/java/com/crm/identity/SessionLifecyclePolicyTest.java](../microservices/identity-service/src/test/java/com/crm/identity/SessionLifecyclePolicyTest.java) | Test unitaire ou MVC autonome de sécurité |
| [microservices/identity-service/src/test/java/com/crm/identity/SessionSecurityIntegrationTest.java](../microservices/identity-service/src/test/java/com/crm/identity/SessionSecurityIntegrationTest.java) | Fixture d’intégration adaptée ; compilée, NON exécutée |
| [microservices/identity-service/src/test/java/com/crm/identity/UidIssuanceIntegrationTest.java](../microservices/identity-service/src/test/java/com/crm/identity/UidIssuanceIntegrationTest.java) | Fixture d’intégration adaptée ; compilée, NON exécutée |
| [microservices/identity-service/src/test/java/com/crm/identity/UsableSuperAdminPolicyTest.java](../microservices/identity-service/src/test/java/com/crm/identity/UsableSuperAdminPolicyTest.java) | Test unitaire ou MVC autonome de sécurité |
| [microservices/identity-service/src/test/java/com/crm/identity/security/AccessTokenClaimsTest.java](../microservices/identity-service/src/test/java/com/crm/identity/security/AccessTokenClaimsTest.java) | Test unitaire ou MVC autonome de sécurité |
| [microservices/identity-service/src/test/java/com/crm/identity/security/AccessTokenValidationServiceTest.java](../microservices/identity-service/src/test/java/com/crm/identity/security/AccessTokenValidationServiceTest.java) | Test unitaire ou MVC autonome de sécurité |
| [microservices/identity-service/src/test/java/com/crm/identity/security/UidIssuanceTest.java](../microservices/identity-service/src/test/java/com/crm/identity/security/UidIssuanceTest.java) | Test unitaire ou MVC autonome de sécurité |
| [microservices/identity-service/src/test/java/com/crm/identity/security/UnifiedAuthenticationPolicyTest.java](../microservices/identity-service/src/test/java/com/crm/identity/security/UnifiedAuthenticationPolicyTest.java) | Test unitaire ou MVC autonome de sécurité |
| [microservices/identity-service/src/test/java/com/crm/identity/service/impl/AuthenticationServiceImplTest.java](../microservices/identity-service/src/test/java/com/crm/identity/service/impl/AuthenticationServiceImplTest.java) | Test unitaire ou MVC autonome de sécurité |
| [microservices/identity-service/src/test/java/com/crm/identity/service/impl/DirectoryRolesTest.java](../microservices/identity-service/src/test/java/com/crm/identity/service/impl/DirectoryRolesTest.java) | Test unitaire ou MVC autonome de sécurité |
| [microservices/identity-service/src/test/java/com/crm/identity/service/impl/RefreshTokenSecurityTest.java](../microservices/identity-service/src/test/java/com/crm/identity/service/impl/RefreshTokenSecurityTest.java) | Test unitaire ou MVC autonome de sécurité |


## D. Tests exécutés

### Java Identity : 92 réussis

Depuis la racine :

```powershell
mvn -o -B -ntp '-Dmaven.repo.local=C:\Users\ASUS\.m2\repository' -f microservices/identity-service/pom.xml '-Dtest=SessionLifecyclePolicyTest,UsableSuperAdminPolicyTest,UnifiedAuthenticationPolicyTest,LogoutHttpContractTest,AccessTokenClaimsTest,AccessTokenValidationServiceTest,RefreshTokenSecurityTest,AuthenticationServiceImplTest,DirectoryRolesTest,UidIssuanceTest,JwtServiceTest' '-DargLine=-Xmx384m' test
```

Tests unitaires et MVC autonome : aucun @SpringBootTest, aucune base, migration ou application serveur. Les 60 tests de la première passe sont inclus dans ces 92 et ne s'additionnent pas au résultat final.

Couverture : logout idempotent ; ancienne session versus nouvelle connexion ; logout concurrent à une rotation simulée ; rejet du refresh réutilisé ; désactivation/réactivation ; changements de rôles ; absence d'élévation sur un ancien JWT ; UID/sid absent, incohérent, signature ou expiration invalide ; utilisateur supprimé ; compte verrouillé ; indisponibilité ; dernier SUPER_ADMIN ; ordre des verrous et propagation des échecs.

### Java common-security : 86 réussis

```powershell
mvn -o -B -ntp '-Dmaven.repo.local=C:\Users\ASUS\.m2\repository' -f microservices/common-security/pom.xml '-Dtest=SessionBoundTokenCompatibilityTest,JwtServiceTest,JwtAuthenticationFilterTest,AccountValidationClientTest,AuthenticatedUserPrincipalTest' '-DargLine=-Xmx384m' test
```

Le nouveau test exerce les quatre rôles avec un JWT contenant sid, puis une révocation et une indisponibilité Identity simulées. Les tests existants conservent les protections de signature, principal typé, claims et absence de cache positif.

**Total Java distinct : 178 tests réussis.** Les 33 tests Java du Lot 1A restent un résultat historique distinct et ne sont pas additionnés ; leurs fichiers sont préservés.

### Angular : 390 réussis

Depuis frontend :

```powershell
$env:CHROME_BIN='C:\Program Files\Google\Chrome\Application\chrome.exe'
$env:NODE_OPTIONS='--max-old-space-size=2048'
.\node_modules\.bin\ng.cmd test --no-watch --browsers=ChromeHeadless --karma-config=karma.ci.conf.cjs --include='src/app/core/services/auth.service.spec.ts' --include='src/app/core/interceptors/auth.interceptor.spec.ts' --include='src/app/core/guards/*.spec.ts' --include='src/app/features/auth/login/*.spec.ts' --include='src/app/features/users/*.spec.ts' --include='src/app/features/dashboard/*.spec.ts' --include='src/app/features/invoices/*.spec.ts' --include='src/app/features/payroll/*.spec.ts' --include='src/app/shared/financial-validators.spec.ts'
```

Inclut les 158 tests Angular du Lot 1A, sans modifier leurs attentes ni leurs sources. Les 230 tests de la première passe sont inclus dans cette dernière passe et ne s'additionnent pas.

Couverture supplémentaire : logout serveur, backend indisponible, délai dépassé, réponse tardive après nouvelle connexion, nettoyage des tokens tardifs, renouvellements simultanés/refusés, 401/403, modification de ses propres rôles et erreurs d'administration.

### Compilation et contrôles statiques

```powershell
mvn -o -B -ntp '-Dmaven.repo.local=C:\Users\ASUS\.m2\repository' -f microservices/identity-service/pom.xml -DskipTests test-compile
git diff --check
```

Compilation réussie de toutes les sources de test Identity, dont les fixtures d'intégration adaptées au nouveau contrat. Les suites d'intégration n'ont pas été exécutées. Pas de build global, installation de dépendances, démarrage Docker, MySQL ou microservice. Les commandes de tests ont été exécutées séquentiellement.

La tentative antérieure avait été interrompue par un échec de vérification automatique d'autorisation dû à une limite d'usage. À la reprise, l'autorisation a abouti et les commandes ci-dessus ont effectivement terminé avec succès.

### Compatibilité inspectée

| Composant | Vérification | Limite |
|---|---|---|
| Gateway | Route /identity/api/auth/** existante couvrant logout ; blocage des routes internes conservé | Aucun routage réel testé |
| Identity | Même validateur pour filtre public et endpoint interne ; statuts et session vérifiés | Pas de contexte applicatif ou MySQL |
| common-security | 86 tests ciblés, dont sid et révocation distante | Réponses Identity simulées |
| Employee, Department, Customer, HR, Payroll, Fees, Invoice | Dépendance common-security et configuration de validation Identity présentes ; aucun code métier changé | Aucun service réel lancé |
| Angular Lot 1A | 158 tests inclus dans les 390 ; fichiers inchangés | Pas de navigateur connecté aux services réels |

## E. Risques et validations différées

- Migration MySQL neuve et upgrade sur copie sauvegardée ; types/index, historique Flyway et validation JPA.
- Deux opérations administratives simultanées visant les derniers SUPER_ADMIN ; vérifier qu'une seule peut réduire le nombre à un, jamais à zéro.
- Concurrence login/refresh/logout et changement de rôle/désactivation, avec commit et rollback. Vérifier l'ordre de verrouillage et les éventuels deadlocks sur MySQL.
- Révocation observée à travers le vrai Gateway et chacun des sept services, pas uniquement les filtres simulés.
- Parcours navigateur des quatre rôles, perte réseau au logout, reconnexion et traitement des sessions historiques sans sid.
- Les changements sensibles doivent passer par les services d'administration. Les écritures SQL manuelles contournent leurs hooks de révocation ; aucune politique de modification directe de la base n'est introduite.
- Une panne réseau ne permet pas de garantir une révocation serveur : l'interface ne l'annonce pas comme réussie. Une nouvelle connexion révoque les sessions antérieures.
- L'invariant du dernier administrateur préserve un état initial sain ; il ne crée pas automatiquement un SUPER_ADMIN si la base n'en contient déjà aucun.
- Les refresh tokens révoqués sont conservés plutôt que supprimés ; une éventuelle politique de rétention reste à définir séparément.

Les tests unitaires ne certifient ni la migration, ni le rollback SQL réel, ni les performances ou les parcours E2E.

## F. État Git et préservation

Branche : publication-clean. Aucun staging, commit, push, changement de branche, reset ou suppression de données.

Les 49 fichiers de l'inventaire Lot 1A, rapport compris, ont été comparés par SHA-256 à la reprise : aucun changement. Les 24 fichiers de configuration, POM et migrations historiques déjà modifiés/non suivis lors de cette reprise sont également inchangés. Les autres migrations suivies ne figurent dans aucun diff.

SecurityConfig.java et AdminEmployeeCreationTest.java avaient des modifications locales antérieures à ce lot. Les extensions ont été limitées aux points nécessaires : les réponses 401, le test anonyme et les réglages de test préexistants sont conservés. Les fichiers Docker/Compose, volumes et .env n'ont pas été modifiés.

L'état Git complet est consigné en annexe ; il contient aussi les modifications des lots précédents.

## G. Prochaine étape

Le Lot 1B est prêt pour revue et validation intégrée ultérieure ; il n'est pas certifié pour déploiement tant que la migration et les scénarios MySQL/E2E n'ont pas été validés.

La suite fonctionnelle cohérente est la fiabilisation des associations et espaces personnels, notamment le canal HTTPS du profil salarié et l'accès aux documents personnels, tout en préparant l'environnement isolé et borné en mémoire pour valider ensemble les Lots 1A et 1B. Aucune nouvelle correction hors Lot 1B n'a été commencée.

## Annexe — État Git final sans staging

```text
 M docker-compose.yml
 M docs/AUDIT_FONCTIONNEL_CRM.md
 M frontend/src/app/core/interceptors/auth.interceptor.spec.ts
 M frontend/src/app/core/services/auth.service.spec.ts
 M frontend/src/app/core/services/auth.service.ts
 M frontend/src/app/features/auth/login/login.component.html
 M frontend/src/app/features/auth/login/login.component.ts
 M frontend/src/app/features/dashboard/dashboard-data.service.spec.ts
 M frontend/src/app/features/dashboard/dashboard-data.service.ts
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
 M microservices/hr-service/src/main/java/com/crm/hr/exception/GlobalExceptionHandler.java
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
 M microservices/invoice-service/src/main/java/com/crm/invoice/dto/InvoiceRequest.java
 M microservices/invoice-service/src/main/java/com/crm/invoice/exception/GlobalExceptionHandler.java
 M microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoicePaymentRepository.java
 M microservices/invoice-service/src/main/java/com/crm/invoice/service/impl/InvoiceServiceImpl.java
 M microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceServiceIntegrationTest.java
 M microservices/payroll-service/src/main/java/com/crm/payroll/dto/PayrollRequest.java
 M microservices/payroll-service/src/main/java/com/crm/payroll/exception/GlobalExceptionHandler.java
 M microservices/payroll-service/src/main/java/com/crm/payroll/service/impl/PayrollServiceImpl.java
?? b.png
?? docker-compose.validation.sprint6c.yml
?? docker-compose.validation.yml
?? docker/
?? "docs/CDC CRM 1.pdf"
?? docs/LOT_1A_FIABILISATION.md
?? docs/LOT_1B_SECURISATION_COMPTES_SESSIONS.md
?? docs/PLAN_FINALISATION_CRM.md
?? frontend/src/app/core/testing/client-invoice-statistics.json
?? frontend/src/app/shared/financial-validators.spec.ts
?? frontend/src/app/shared/financial-validators.ts
?? microservices/common-security/src/test/java/com/crm/security/SessionBoundTokenCompatibilityTest.java
?? microservices/customer-service/src/main/resources/db/migration/V1__initial_schema.sql
?? microservices/customer-service/src/test/java/com/crm/customer/SqlErrorPrivacyTest.java
?? microservices/department-service/src/main/resources/db/
?? microservices/department-service/src/test/java/com/crm/department/SqlErrorPrivacyTest.java
?? microservices/employee-service/src/main/resources/db/
?? microservices/employee-service/src/test/java/com/crm/employee/SqlErrorPrivacyTest.java
?? microservices/fees-service/src/main/resources/db/
?? microservices/fees-service/src/test/java/com/crm/fees/SqlErrorPrivacyTest.java
?? microservices/hr-service/src/main/resources/db/migration/V1__initial_schema.sql
?? microservices/hr-service/src/test/java/com/crm/hr/SqlErrorPrivacyTest.java
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
?? microservices/invoice-service/src/test/java/com/crm/invoice/SqlErrorPrivacyTest.java
?? microservices/payroll-service/src/main/java/com/crm/payroll/service/FinancialValidation.java
?? microservices/payroll-service/src/main/resources/db/migration/V1__initial_schema.sql
?? microservices/payroll-service/src/test/java/com/crm/payroll/PayrollFinancialPolicyTest.java
?? microservices/payroll-service/src/test/java/com/crm/payroll/SqlErrorPrivacyTest.java
```
