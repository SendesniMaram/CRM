# Lot 3C — Finalisation fonctionnelle du module Ressources humaines

Date : 4 novembre 2026. Branche conservée : publication-clean.

## 1. Résumé exécutif

**LOT 3C PARTIEL — les quatre blocs ont été développés ; 103 cas Java distincts et 217 tests Angular réussis, build Angular de production réussi avec avertissements. La validation intégrée réelle reste NON RÉALISÉE.**

Le module dispose d’un solde technique de congés personnel et administratif, d’un calendrier testé et borné, d’une affectation aux formations vérifiée auprès d’Employee et d’un écran de pilotage RH par période. Le solde est explicitement calendaire et non légal. Lorsque les quotas ne permettent pas une interprétation fiable, le restant est indéterminé, sans zéro de substitution.

Les règles d’acquisition, de jours ouvrés, de report et de ventilation par type restent à définir. Les agrégats ont été vérifiés sur données synthétiques H2 avec une adaptation explicite ; le SQL MySQL, les liaisons JDBC natives et la recette métier restent à valider. Aucun Docker Desktop, MySQL, microservice, Flyway ou E2E connecté lancé.

## 2. État initial et inspection

Lecture des [rapports 3A](LOT_3A_AUDIT_CONFORMITE_FONCTIONNELLE.md), [3B](LOT_3B_FINALISATION_PAIE.md), [1C](LOT_1C_ASSOCIATIONS_ESPACES_PERSONNELS.md), [2B-R](LOT_2B_R_VALIDATION_PROGRESSIVE.md) et du [CDC CRM, p.6 et 8](CDC%20CRM%201.pdf), via son extraction locale conservée au Lot 3A. Aucun document obligatoire absent.

Inspection des entités, DTO, dépôts, services, contrôleurs et sécurités HR ; contrôleurs et droits Employee/Department ; filtre JWT commun ; routes, navigation, congés, calendrier, formulaires HR, dashboard et tests Angular. Les quotas sont exposés par EmployeeDossierController et le calendrier par LeaveController : il n’existe pas de contrôleur autonome portant ces noms.

| Exigence | État initial 3A | Écart traité |
|---|---|---|
| RH-03 | NON VALIDÉE | Tests de composant, bornes, filtres et réponses concurrentes du calendrier |
| RH-04 | PARTIELLE | Calcul des jours et consultation propre, auparavant limitée aux attributions |
| RH-06 | PARTIELLE | Référence Employee non vérifiée et saisie libre |
| RH-12 | PARTIELLE | Compteurs génériques sans période métier explicite |
| RH-13 | PARTIELLE | Absence de synthèses RH par période et statut |

Plan suivi : conserver les workflows existants ; ajouter des modèles de lecture sans migration ; raccorder une sélection Employee et la validation authentifiée ; enrichir les interfaces ; exécuter les tests ciblés et documenter leurs limites. Aucun nouvel audit général ni refonte de la paie.

## 3. Fonctionnalités réalisées

- Solde technique recalculé à la demande, avec période, quotas, jours approuvés, attente, restant, disponibilité après attente et détail des demandes sans motif privé.
- Déduplication des chevauchements et découpage aux bornes de la période ; états explicites pour quota absent, période différente ou types multiples.
- Carte personnelle dans /leaves et consultation administrative dans /leave-calendar. Une carte déjà consultée se recalcule après actualisation des demandes ou changement des quotas depuis cet écran.
- Calendrier existant conservé : chargement initial, mois/années/bissextiles, filtres employé/département/statut, états vide/erreur et protection contre les réponses tardives.
- Création et réaffectation de formation validées via le client interservices existant, avec bearer obligatoire, réponse Employee contrôlée et erreurs 401/403/404/502/503 préservées.
- Sélecteur Employee paginé avec recherche et nom ; référence affichée en lecture seule et valeur historique conservée lors de l’édition.
- Pilotage RH administratif par période, tableaux de statuts et jours d’absence approuvée uniques par employé ; états chargement/vide/erreur et présentation responsive sans bibliothèque graphique supplémentaire.

## 4. Fichiers créés et modifiés

Inventaire relatif à la photographie du début de ce lot, et non au HEAD déjà très modifié : **24 fichiers préexistants modifiés et 25 nouveaux fichiers, rapport compris**.

### Fichiers préexistants modifiés

- [frontend/src/app/app.routes.ts](../frontend/src/app/app.routes.ts)
- [frontend/src/app/core/navigation/crm-navigation.ts](../frontend/src/app/core/navigation/crm-navigation.ts)
- [frontend/src/app/core/services/auth-navigation.ts](../frontend/src/app/core/services/auth-navigation.ts)
- [frontend/src/app/features/dashboard/dashboard.component.html](../frontend/src/app/features/dashboard/dashboard.component.html)
- [frontend/src/app/features/hr-admin/hr-admin-access.spec.ts](../frontend/src/app/features/hr-admin/hr-admin-access.spec.ts)
- [frontend/src/app/features/hr-admin/hr-admin-page.component.html](../frontend/src/app/features/hr-admin/hr-admin-page.component.html)
- [frontend/src/app/features/hr-admin/hr-admin-page.component.spec.ts](../frontend/src/app/features/hr-admin/hr-admin-page.component.spec.ts)
- [frontend/src/app/features/hr-admin/hr-admin-page.component.ts](../frontend/src/app/features/hr-admin/hr-admin-page.component.ts)
- [frontend/src/app/features/leaves/leave-calendar.component.ts](../frontend/src/app/features/leaves/leave-calendar.component.ts)
- [frontend/src/app/features/leaves/leave-calendar.service.ts](../frontend/src/app/features/leaves/leave-calendar.service.ts)
- [frontend/src/app/features/leaves/leaves-page.component.html](../frontend/src/app/features/leaves/leaves-page.component.html)
- [frontend/src/app/features/leaves/leaves-page.component.ts](../frontend/src/app/features/leaves/leaves-page.component.ts)
- [microservices/hr-service/src/main/java/com/crm/hr/config/SecurityConfig.java](../microservices/hr-service/src/main/java/com/crm/hr/config/SecurityConfig.java)
- [microservices/hr-service/src/main/java/com/crm/hr/controller/LeaveController.java](../microservices/hr-service/src/main/java/com/crm/hr/controller/LeaveController.java)
- [microservices/hr-service/src/main/java/com/crm/hr/controller/TrainingController.java](../microservices/hr-service/src/main/java/com/crm/hr/controller/TrainingController.java)
- [microservices/hr-service/src/main/java/com/crm/hr/dto/TrainingRequest.java](../microservices/hr-service/src/main/java/com/crm/hr/dto/TrainingRequest.java)
- [microservices/hr-service/src/main/java/com/crm/hr/exception/GlobalExceptionHandler.java](../microservices/hr-service/src/main/java/com/crm/hr/exception/GlobalExceptionHandler.java)
- [microservices/hr-service/src/main/java/com/crm/hr/repository/LeaveRepository.java](../microservices/hr-service/src/main/java/com/crm/hr/repository/LeaveRepository.java)
- [microservices/hr-service/src/main/java/com/crm/hr/service/EmployeeReferenceValidator.java](../microservices/hr-service/src/main/java/com/crm/hr/service/EmployeeReferenceValidator.java)
- [microservices/hr-service/src/main/java/com/crm/hr/service/ILeaveService.java](../microservices/hr-service/src/main/java/com/crm/hr/service/ILeaveService.java)
- [microservices/hr-service/src/main/java/com/crm/hr/service/ITrainingService.java](../microservices/hr-service/src/main/java/com/crm/hr/service/ITrainingService.java)
- [microservices/hr-service/src/main/java/com/crm/hr/service/impl/LeaveServiceImpl.java](../microservices/hr-service/src/main/java/com/crm/hr/service/impl/LeaveServiceImpl.java)
- [microservices/hr-service/src/main/java/com/crm/hr/service/impl/TrainingServiceImpl.java](../microservices/hr-service/src/main/java/com/crm/hr/service/impl/TrainingServiceImpl.java)
- [microservices/hr-service/src/test/java/com/crm/hr/TrainingServiceIntegrationTest.java](../microservices/hr-service/src/test/java/com/crm/hr/TrainingServiceIntegrationTest.java)

### Nouveaux fichiers

- [frontend/src/app/features/hr-admin/hr-employee-picker.component.html](../frontend/src/app/features/hr-admin/hr-employee-picker.component.html)
- [frontend/src/app/features/hr-admin/hr-employee-picker.component.ts](../frontend/src/app/features/hr-admin/hr-employee-picker.component.ts)
- [frontend/src/app/features/hr-admin/hr-insights.component.html](../frontend/src/app/features/hr-admin/hr-insights.component.html)
- [frontend/src/app/features/hr-admin/hr-insights.component.scss](../frontend/src/app/features/hr-admin/hr-insights.component.scss)
- [frontend/src/app/features/hr-admin/hr-insights.component.ts](../frontend/src/app/features/hr-admin/hr-insights.component.ts)
- [frontend/src/app/features/hr-admin/hr-insights.service.ts](../frontend/src/app/features/hr-admin/hr-insights.service.ts)
- [frontend/src/app/features/hr-admin/hr-insights.spec.ts](../frontend/src/app/features/hr-admin/hr-insights.spec.ts)
- [frontend/src/app/features/leaves/leave-balance.component.html](../frontend/src/app/features/leaves/leave-balance.component.html)
- [frontend/src/app/features/leaves/leave-balance.component.ts](../frontend/src/app/features/leaves/leave-balance.component.ts)
- [frontend/src/app/features/leaves/leave-calendar.component.spec.ts](../frontend/src/app/features/leaves/leave-calendar.component.spec.ts)
- [microservices/hr-service/src/main/java/com/crm/hr/controller/HrInsightsController.java](../microservices/hr-service/src/main/java/com/crm/hr/controller/HrInsightsController.java)
- [microservices/hr-service/src/main/java/com/crm/hr/repository/HrStatisticsRepository.java](../microservices/hr-service/src/main/java/com/crm/hr/repository/HrStatisticsRepository.java)
- [microservices/hr-service/src/main/java/com/crm/hr/repository/LeaveBalanceRepository.java](../microservices/hr-service/src/main/java/com/crm/hr/repository/LeaveBalanceRepository.java)
- [microservices/hr-service/src/main/java/com/crm/hr/service/HrPeriod.java](../microservices/hr-service/src/main/java/com/crm/hr/service/HrPeriod.java)
- [microservices/hr-service/src/main/java/com/crm/hr/service/HrStatisticsService.java](../microservices/hr-service/src/main/java/com/crm/hr/service/HrStatisticsService.java)
- [microservices/hr-service/src/main/java/com/crm/hr/service/LeaveBalanceCalculator.java](../microservices/hr-service/src/main/java/com/crm/hr/service/LeaveBalanceCalculator.java)
- [microservices/hr-service/src/main/java/com/crm/hr/service/LeaveBalanceService.java](../microservices/hr-service/src/main/java/com/crm/hr/service/LeaveBalanceService.java)
- [microservices/hr-service/src/test/java/com/crm/hr/HrAggregateFixtureTest.java](../microservices/hr-service/src/test/java/com/crm/hr/HrAggregateFixtureTest.java)
- [microservices/hr-service/src/test/java/com/crm/hr/HrInsightsSecurityTest.java](../microservices/hr-service/src/test/java/com/crm/hr/HrInsightsSecurityTest.java)
- [microservices/hr-service/src/test/java/com/crm/hr/HrStatisticsPolicyTest.java](../microservices/hr-service/src/test/java/com/crm/hr/HrStatisticsPolicyTest.java)
- [microservices/hr-service/src/test/java/com/crm/hr/LeaveBalanceCalculationTest.java](../microservices/hr-service/src/test/java/com/crm/hr/LeaveBalanceCalculationTest.java)
- [microservices/hr-service/src/test/java/com/crm/hr/LeaveBalanceOwnershipTest.java](../microservices/hr-service/src/test/java/com/crm/hr/LeaveBalanceOwnershipTest.java)
- [microservices/hr-service/src/test/java/com/crm/hr/LeaveCalendarPolicyTest.java](../microservices/hr-service/src/test/java/com/crm/hr/LeaveCalendarPolicyTest.java)
- [microservices/hr-service/src/test/java/com/crm/hr/TrainingAssignmentPolicyTest.java](../microservices/hr-service/src/test/java/com/crm/hr/TrainingAssignmentPolicyTest.java)
- [docs/LOT_3C_FINALISATION_RH.md](LOT_3C_FINALISATION_RH.md) — ce rapport.

Photographie SHA-256 de 996 fichiers suivis/non suivis, hors fichiers de secrets et clés exclus de la lecture : aucune disparition ; les 972 autres fichiers photographiés sont inchangés. Tous les fichiers Payroll du Lot 3B, les 25 migrations historiques, les rapports antérieurs et les configurations/scripts du Lot 2B-R sont préservés. Employee, Department et common-security ne sont pas modifiés.

Les changements partagés de navigation se limitent à la route administrative /hr-insights, son menu et son URL de retour autorisée. Les mécanismes de session 1B restent inchangés. Aucun staging, commit, push, changement de branche, suppression de données ou volume, ni accès à un secret.

## 5. Endpoints REST

Préfixe Gateway existant : /hr. Les chemins ci-dessous sont locaux à HR.

| Endpoint | Comportement et droits |
|---|---|
| GET /api/leaves/balance/me?from=AAAA-MM-JJ&to=AAAA-MM-JJ | EMPLOYEE ; UID et association résolus côté serveur ; aucun paramètre de ciblage supplémentaire accepté |
| GET /api/leaves/balance/employees/{employee}?from=…&to=… | ADMIN/SUPER_ADMIN ; référence Employee vérifiée avec le bearer de l’appelant |
| GET /api/hr-insights?from=…&to=… | ADMIN/SUPER_ADMIN ; agrégats RH sans données individuelles |
| GET /api/leaves/calendar?from=…&to=…&employeeId=… | Endpoint conservé, ADMIN/SUPER_ADMIN ; filtrage côté dépôt, au plus 5 000 lignes, sans troncature silencieuse |
| POST /api/trainings | ADMIN/SUPER_ADMIN ; Authorization requis, validation du nouvel employé avant écriture |
| PUT /api/trainings/{id} | ADMIN/SUPER_ADMIN ; nouvelle référence vérifiée ; référence historique inchangée conservable |

Les consultations de solde et d’indicateurs imposent no-store. Périodes inclusives de 366 jours maximum, from et to obligatoires. Le détail d’un solde est borné à 2 000 demandes et 2 000 quotas ; au-delà, une erreur 400 demande de réduire la période. Les erreurs de paramètres et dates HTTP deviennent 400, avec message fixe sans valeur reçue.

## 6. Modèles et services

Aucune entité persistante ajoutée. LeaveBalanceRepository projette seulement les identifiants, dates et statuts des demandes d’un employé, et lit ses quotas chevauchant la période. Les lectures sont filtrées en base et bornées. Le DTO public expose des identifiants de demande/quota textuels ; il ne contient ni motif de congé, username, UID tiers ou document.

LeaveBalanceCalculator effectue une union d’intervalles triés sans développer chaque journée en mémoire. LeaveBalanceService assure période, propriété et limites dans une transaction de lecture. Le champ leaveType des quotas n’a pas d’équivalent dans LeaveRequest ; aucune ventilation automatique n’est inventée.

HrStatisticsRepository exécute six requêtes agrégées, indépendantes du nombre de lignes : statuts des congés, formations, évaluations, offres et candidatures, puis nombre de jours approuvés uniques par employé. HrStatisticsService complète uniquement les statuts absents d’un résultat réussi par zéro ; un échec de service ou un contrat invalide ne devient pas un zéro.

TrainingServiceImpl réutilise EmployeeReferenceValidator et LeaveDirectoryClient. Le contrôleur transmet explicitement le bearer aux créations/modifications. La validation du DTO est aussi appliquée aux appels directs du service. Le chemin Employee utilisé lit une fiche simple et n’appelle pas HR : aucune boucle d’appel n’est créée.

## 7. Interfaces Angular

Le composant LeaveBalanceComponent est partagé entre la page personnelle des congés et le calendrier administratif. Les dates, quotas et demandes de référence sont visibles ; les soldes inconnus restent affichés comme indéterminés. La première consultation est explicite. Les relectures déclenchées après action utilisent la période sélectionnée, sans recalcul local d’un solde supposé.

HrEmployeePickerComponent utilise uniquement l’annuaire Employee paginé, avec recherche par nom/code, sélection autorisée parmi les résultats, pagination, chargement, vide et erreur. Il conserve la référence existante, même historique, sans l’écraser lors d’une recherche. L’API reste la protection définitive. Le sélecteur suit le contrat numérique de l’annuaire actuel et refuse des identifiants JavaScript non sûrs.

Le formulaire Training garde ses champs et ses règles de statut/certificat. La case certificat est sortie du mat-form-field incompatible ; l’écran d’édition est réellement rendu dans les tests. Une formation terminée reste figée. Les dates historiques inchangées d’une formation non terminée peuvent être conservées lors de sa clôture ; un nouveau début placé dans le passé est refusé.

La route /hr-insights est lazy et protégée par authGuard/roleGuard, visible dans le menu RH et reliée depuis le dashboard administratif. Elle propose des dates, cartes/tableaux de statuts, période calculée, vide explicite et messages d’erreur. Les styles réutilisent une structure flexible sans nouvelle dépendance graphique.

Les nouvelles vues, le sélecteur et le calendrier invalident leurs réponses et effacent les données lors d’un changement de session/rôle. Le calendrier annule la requête remplacée lors d’un changement de mois. Un filtre Angular ne remplace jamais la protection serveur.

## 8. Règles de calcul des congés

**Politique technique : jours calendaires inclusifs, toutes les demandes confondues, sans qualification légale.** Les dates sont des LocalDate : aucune conversion en instant ou déplacement lié au fuseau n’est appliqué aux jours de congé.

| Élément | Règle retenue |
|---|---|
| Consommé | Union des dates APPROVED, bornées à la période, y compris jours futurs déjà approuvés |
| En attente | Union des dates PENDING, hors dates déjà approuvées ; doublons d’attente dédupliqués |
| Refusé | REJECTED visible dans le détail mais exclu du consommé et de l’attente |
| Restant | Quota technique explicite moins consommé ; un résultat négatif reste visible |
| Disponible après attente | Restant moins attente supplémentaire |
| Chevauchement | Une journée ne compte qu’une fois pour le même employé ; signal d’alerte et détails conservés |
| Plusieurs périodes | Intersection de chaque demande avec la période interrogée ; aucune attribution proratisée |

Exemple : 20,50 jours attribués ; approuvé du 1 au 3 janvier ; attente du 3 au 5 janvier → consommé 3, attente supplémentaire 2, restant 17,50, disponible après attente 15,50.

Le restant n’est calculé que pour un quota unique dont les bornes correspondent exactement à la période. Ce quota sert d’enveloppe technique pour toutes les demandes, sans allocation légale par type.

| État | Interprétation |
|---|---|
| NO_QUOTA | Aucun quota chevauchant ; attribution et restant inconnus |
| QUOTA_PERIOD_MISMATCH | Bornes différentes ; quotas affichés à titre informatif, sans proratisation ni restant |
| QUOTA_TYPE_AMBIGUOUS | Plusieurs quotas/types ; le modèle des demandes ne permet pas leur ventilation, restant inconnu |
| TECHNICAL_BALANCE | Un quota exact ; calcul technique possible, sous la réserve non légale affichée |

Le modèle actuel ne possède que PENDING, APPROVED et REJECTED. Il n’existe pas d’état de congé annulé : seules les demandes en attente sont supprimables, donc elles cessent d’apparaître dans le calcul. Les demandes approuvées/refusées restent non modifiables ; aucune annulation après approbation n’est ajoutée implicitement. Une évolution de ce workflow nécessite une règle métier explicite et traçable.

Les demi-journées, jours ouvrés/fériés, acquisition, reports et régularisations ne sont pas représentés par ces demandes datées. Des quotas décimaux ne suffisent pas à inventer ces règles.

## 9. Statistiques et indicateurs RH

| Famille | Période et sens des chiffres |
|---|---|
| Congés | Nombre de demandes chevauchant la période, par statut actuel |
| Absence approuvée | Nombre de couples distincts employé/date dans la période ; chevauchements du même employé dédupliqués |
| Formations | Demandes de formation chevauchant la période, par statut actuel PLANNED/IN_PROGRESS/COMPLETED/CANCELLED |
| Évaluations | Périodes d’évaluation chevauchantes, par état DRAFT/FINALIZED |
| Offres | Offres créées dans la période, classées selon leur statut actuel OPEN/CLOSED |
| Candidatures | Candidatures créées dans la période, classées selon leur statut actuel |

Les bornes de recrutement utilisent début à minuit inclus, lendemain de fin exclu. Elles s’appliquent aux horodatages exposés par la session JDBC ; aucun historique de statut à une date passée n’est reconstruit. Le fuseau d’exploitation/JDBC et les cas proches de minuit doivent être vérifiés en intégration, notamment parce que les colonnes historiques sont TIMESTAMP.

La requête MySQL des jours approuvés génère au plus 366 dates par CTE récursive, joint les demandes puis déduplique employé/date en base ; aucune liste de salariés ou de demandes de toute la table n’est chargée par le service d’indicateurs. Les autres requêtes GROUP BY renvoient seulement les statuts et comptes. Index et plans d’exécution restent à mesurer sur MySQL.

L’effectif total appartient à Employee et reste lu par le dashboard existant via une page de taille 1 et totalElements. Il n’est ni copié dans HR ni remplacé par un nombre de contrats/formations. Aucun taux d’absentéisme, score de performance ou dénominateur de travail fictif. Le calendrier conserve son annuaire administratif existant pour noms/départements ; ce chargement n’appartient pas au calcul des statistiques.

## 10. Contrôles de sécurité

ADMIN/SUPER_ADMIN accèdent aux vues administratives, aux formations et aux indicateurs. EMPLOYEE consulte seulement son solde personnel après résolution de l’association UID ; il ne peut ni choisir un employeeId personnel ni accéder au calendrier/aux statistiques. CLIENT est refusé.

Le filtre JWT et la validation Identity communs ne sont pas modifiés. Les namespaces de solde sont explicitement protégés avant la règle générale des congés. Les services ajoutent leurs vérifications de rôle/propriété. La consultation administrative valide la référence distante ; la consultation propre utilise exclusivement l’association existante côté HR.

EmployeeReferenceValidator exige un bearer, une référence canonique et un objet distant contenant l’identifiant entier attendu. Null, tableau, texte, décimal ou autre employé sont refusés en 502. Les erreurs distantes 401/403/404/503 sont propagées sans corps distant ni SQL. Aucune transmission anonyme, clé nouvelle, désactivation de HTTPS ou changement de common-security.

Les tests de sécurité utilisent les filtres réels et une réponse Identity simulée. Ils couvrent les quatre rôles, refus personnel ciblé, 401, 503, no-store, paramètres invalides et transmission du bearer lors d’une écriture Training. Ils ne constituent pas une validation JWT/Identity réseau.

## 11. Migrations et compatibilité

**Aucune migration nécessaire ni créée.** Les nouvelles lectures utilisent les tables et colonnes existantes. Les 25 migrations antérieures, dont celle de la paie 3B, sont inchangées par empreinte. Flyway n’a pas été exécuté.

Le test de fixtures crée uniquement cinq petites tables synthétiques dans des bases H2 éphémères en mémoire. Il ne charge aucune migration du dépôt et n’accède à aucune instance MySQL.

Les formations historiques restent lisibles. Lors d’une modification, une référence inchangée n’est pas résolue à nouveau : ce choix conserve les références historiques et évite de bloquer leur suivi sur une indisponibilité Employee. Toute création ou nouvelle référence doit être vérifiée. Les dates anciennes inchangées sont conservables, sans autoriser un nouveau début antérieur à aujourd’hui. Les formations terminées et les règles de certificat restent protégées.

## 12. Tests exécutés et résultats

Exécutions séquentielles et hors ligne : Maven avec heap 512 Mio, JVM de tests 384 Mio ; fixtures H2 avec JVM 256 Mio ; Node Angular limité à 2 048 Mio. Build de production avec deux workers. Aucun build Java et Angular simultané.

| Suite Java | Cas réussis |
|---|---:|
| EmployeeContractServiceTest | 3 |
| EmployeeDossierServiceTest | 4 |
| HrInsightsSecurityTest | 23 |
| HrStatisticsPolicyTest | 6 |
| LeaveBalanceCalculationTest | 13 |
| LeaveBalanceOwnershipTest | 6 |
| LeaveCalendarPolicyTest | 6 |
| PersonalEvaluationServiceTest | 2 |
| service.DocumentStoragePolicyTest | 7 |
| service.EmployeeProfileServiceTest | 2 |
| service.PersonalDocumentPolicyTest | 10 |
| SqlErrorPrivacyTest | 1 |
| TrainingAssignmentPolicyTest | 15 |
| HrAggregateFixtureTest | 5 |
| **Total distinct** | **103** |

Les décomptes sont ceux des derniers rapports JUnit de chaque suite, pas la somme des relances : sélection initiale finale de 93 cas, cinq nouveaux cas de sécurité ensuite ajoutés à sa suite, puis cinq cas de fixtures H2. Les 23 cas de HrInsightsSecurityTest remplacent son précédent décompte de 18.

Les tests couvrent périodes et bissextiles, dates inclusives, unions et chevauchements, quotas absents/ambigus, accès propre, limites de détail, référence Training et erreurs distantes, conservation historique, immutabilité après approbation/achèvement, requêtes filtrées, statuts, SQL d’agrégation sur fixtures, RBAC et confidentialité des erreurs. Les tests existants ciblés des contrats, documents, évaluations et profil sont inclus.

**Portée H2 explicite :** les requêtes sont lues depuis les annotations du dépôt. DATE_ADD est adapté en DATEADD. La CTE récursive utilise des dates littérales issues des fixtures LocalDate car ses paramètres liés renvoyaient zéro ligne avec H2 2.2.224 ; la même logique avec ces dates littérales produit les quatre couples employé/date attendus, puis un pour la journée chevauchante. Les autres requêtes gardent leurs paramètres liés. Cela valide des comptes et bornes sur données connues, pas le dialecte MySQL, sa liaison JDBC, ni le mapping des projections JPA.

**Angular : 217/217 réussis**, ChromeHeadless 156. Sélection : tous les specs leaves, hr-admin, dashboard et le spec de navigation CRM. Sont couverts services HTTP simulés, période/contrat, cartes de solde, données inconnues, recalcul après actualisation, sélection/recherche/pagination Employee, formulaire réellement rendu, calendrier/mois/année/filtres, chargement/vide/erreurs, session/réponse tardive, routes et menus selon les quatre rôles.

**Build production : réussi**, bundle initial 887,86 kB, sous le seuil bloquant existant de 1 MB. Avertissements : seuil indicatif initial de 500 kB dépassé, styles Payroll/Invoices au-dessus de leur seuil indicatif et projection d’icônes NG8011 dans CustomersPageComponent. Aucun budget, style Payroll/Invoices ou composant Customer n’a été modifié pour les masquer. Le build est une compilation, pas un lancement connecté.

Commandes de sélection exécutées avec le cache Maven local, sans téléchargement :

    # MAVEN_OPTS=-Xmx512m ; JVM de tests -Xmx384m
    mvn -o -B -ntp -Dmaven.repo.local=C:/Users/ASUS/.m2/repository -f microservices/hr-service/pom.xml -Dtest=LeaveBalanceCalculationTest,LeaveBalanceOwnershipTest,HrStatisticsPolicyTest,TrainingAssignmentPolicyTest,LeaveCalendarPolicyTest,HrInsightsSecurityTest,EmployeeContractServiceTest,EmployeeDossierServiceTest,PersonalEvaluationServiceTest,PersonalDocumentPolicyTest,DocumentStoragePolicyTest,EmployeeProfileServiceTest,SqlErrorPrivacyTest -DargLine=-Xmx384m test
    # Relances ciblées : HrInsightsSecurityTest ; HrAggregateFixtureTest avec -Xmx256m
    # Depuis frontend, NODE_OPTIONS=--max-old-space-size=2048, CHROME_BIN configuré :
    .\node_modules\.bin\ng.cmd test --no-watch --browsers=ChromeHeadless --karma-config=karma.ci.conf.cjs --include=src/app/features/leaves/*.spec.ts --include=src/app/features/hr-admin/*.spec.ts --include=src/app/features/dashboard/*.spec.ts --include=src/app/core/navigation/crm-navigation.spec.ts
    # NG_BUILD_MAX_WORKERS=2 :
    .\node_modules\.bin\ng.cmd build --configuration production --output-path ../target/lot3c/frontend-production

Échecs intermédiaires, ensuite corrigés : compilation de l’adaptation du test Training au bearer ; date HTTP invalide renvoyant 500 ; champ certificat placé dans un mat-form-field incompatible ; sélecteur DOM de test d’un contrôle dynamique ; paramètres de CTE récursive H2. Aucun de ces premiers résultats n’est compté comme un succès. Les résultats finaux ci-dessus ne comportent aucun échec résiduel.

Preuves expurgées et inventaire sous target/lot3c, ignoré par Git ; sorties de build dans target/lot3c/frontend-production. git diff --check réussi. Les sorties brutes de frameworks ne sont pas des documents à publier.

## 13. Tests non exécutés

**NON EXÉCUTÉS :** MySQL neuf/upgrade, SQL natif MySQL/JPA et paramètres de la CTE, schéma/indices/plans réels, concurrence et cohérence transactionnelle entre écritures/lectures, Gateway, Identity réel, JWT révoqué multiservice, annuaire Employee réseau, véritable HTTPS, navigateur connecté, performances/charge et campagne E2E.

La suite TrainingServiceIntegrationTest a été adaptée au bearer, à une référence canonique et à un validateur simulé ; elle a été compilée, pas exécutée. Aucun SpringBootTest avec base ni migration lancé. Le test H2 autonome n’est pas une exécution de cette suite.

Les 75 Java/101 Angular du Lot 3B restent historiques et ne sont pas additionnés aux résultats 3C. Leur code, leurs tests et leurs migrations sont inchangés. La RAM libre et l’accès Docker n’ont pas été remesurés ; leur dernier état bloquant 2B reste une observation historique.

## 14. Anomalies corrigées

- Calendrier sans chargement initial, sans filtre de statut et susceptible d’afficher la réponse d’un ancien mois : initialisation, filtre, annulation et garde de contexte.
- Calendrier backend chargeant toutes les demandes avant filtrage : requête de recouvrement en base, limite explicite et refus de dépassement.
- Formations nouvellement affectées sans preuve d’existence Employee : résolution authentifiée et contrôle strict de la réponse avant écriture.
- Impossible de clôturer une formation passée avec ses dates inchangées : validation différenciée création/modification, en conservant le refus des nouveaux débuts passés et des modifications de formations terminées.
- Formulaire de formation contenant une case certificat dans un conteneur Material incompatible : rendu corrigé et testé.
- Dates ou paramètres HTTP invalides traités comme erreur interne : réponse centralisée 400 sans détail sensible.

Les protections et comportements hors dépendance directe de ce lot sont conservés ; aucune anomalie Fees/Customer ou fonctionnalité fiscale/paie n’est traitée ici.

## 15. Décisions métier restantes

Pour un solde légal : unité de décompte, jours ouvrés/fériés, demi-journées, acquisition, prorata, report, régularisation, types des demandes et correspondance aux quotas. Définir également une annulation/correction approuvée et sa trace avant de rendre ce workflow possible.

Le solde technique ne bloque pas une approbation en cas de dépassement : aucune règle d’interdiction par quota n’existe dans le contrat courant. Les dépassements sont visibles. Une telle règle devrait traiter la concurrence des approbations et les quotas dans une transaction appropriée, puis être éprouvée sur MySQL.

Les indicateurs sont les comptes opérationnels définis au §9. Restent à accepter métier les périodes et cohortes, le fuseau des horodatages de recrutement, les dénominateurs d’éventuels taux et les analyses temporelles avancées. Aucun score ou taux n’est inventé. L’effectif reste dans son service propriétaire.

Les références Training inchangées peuvent être historiques et ne prouvent pas une existence actuelle. Les nouvelles affectations, elles, sont vérifiées. Une politique de nettoyage/réconciliation historique ou de suppression interservices reste à décider, sans effacer ces formations.

## 16. État actualisé des exigences RH

COMPLETE concerne ci-dessous le périmètre implémenté et testé localement. Toutes les intégrations réelles restent NON VALIDÉES. Le rapport 3A historique n’est pas réécrit.

| Exigence | Avant 3C | Après 3C | Preuves | Reste à faire |
|---|---|---|---|---|
| RH-03 | NON VALIDÉE | COMPLETE dans le périmètre calendrier existant | Tests du composant et du service, requête filtrée/bornée, mois/années/bissextile, statuts et confidentialité | Recette MySQL/Gateway/navigateur et annuaires réels |
| RH-04 | PARTIELLE | PARTIELLE ; solde technique disponible et testé | Calcul d’union, quotas/ambiguïtés, API propre/admin, carte et actualisation | Règles légales et types/demi-journées, décisions d’annulation, recette réelle |
| RH-06 | PARTIELLE | COMPLETE pour création/réaffectation vérifiée et conservation historique | 15 tests de service, tests HTTP/RBAC et sélecteur/formulaire Angular | Employee/Identity réels, recette des anciennes références |
| RH-12 | PARTIELLE | PARTIELLE ; pilotage opérationnel par période livré | Six agrégats, cartes/statuts, fixtures connues et tests Angular | Acceptation métier des KPI, fuseaux et validation MySQL réelle |
| RH-13 | PARTIELLE | PARTIELLE ; synthèses période/statuts et jours uniques livrées | SQL testé sur H2 adapté, bornes/vides/chevauchements et vues testées | SQL/JPA/paramètres MySQL, performances et analyses métier supplémentaires si retenues |

Les autres workflows RH ne sont pas requalifiés par une simple somme de tests et ne sont pas reconstruits.

## 17. Limites de la validation

Les mocks ne prouvent ni transaction concurrente, ni données réelles, ni indisponibilité d’un vrai annuaire. Les fixtures H2 ne prouvent ni compatibilité de migration, ni typage Hibernate, ni SQL/JDBC MySQL. ChromeHeadless a exécuté des unités/composants avec services simulés, pas les parcours connectés de quatre comptes.

Les seuils de 366 jours, 2 000 détails et 5 000 lignes calendrier sont des bornes techniques explicites, sans troncature ni modification des droits acquis. Ils n’expriment aucune politique légale. Les quotas de périodes plus longues ou multiples restent consultables comme données, sans solde extrapolé.

Les plans de requête, la mémoire/RSS et les index doivent être observés en exploitation candidate. Les limites de heap et le succès du build ne mesurent pas la RAM disponible de la machine. Les avertissements du build sont conservés. Aucun CRM complet, déploiement ou conformité légale déclaré validé.

## 18. Recommandations pour la suite

Après précontrôles positifs et autorisation opérationnelle, reprendre les groupes B/D du [Lot 2B-R](LOT_2B_R_VALIDATION_PROGRESSIVE.md) sur MySQL et volumes isolés. Conserver les configurations existantes ; préparer un candidat neuf avec les JAR actuels. Les anciennes campagnes antérieures à la migration 3B nécessitent toujours une régénération de manifeste ; 3C n’ajoute pas de migration.

Vérifier successivement :

1. Démarrage et schéma validés ; projections JPA, CTE et liaison des dates sur MySQL 8, plans et indices.
2. Deux salariés, quatre rôles et associations UID ; accès propre, refus croisés et refus du calendrier/indicateurs pour EMPLOYEE/CLIENT.
3. Quota exact, absent, périodes différentes et types multiples ; dates inclusives, bissextile, demande multi-période et chevauchements historiques sans double compte.
4. Mise à jour du solde après demandes/décisions ; refus de modifier/supprimer une demande approuvée ; cohérence sous concurrence.
5. Création/réaffectation Training avec Employee valide, absent, interdit, indisponible ou réponse invalide ; maintien des références/dates historiques et certificat à achèvement.
6. Réconciliation des KPI avec un jeu connu, créations de recrutement proches de minuit et fuseau JDBC ; navigation/affichage mobile et erreurs dans un vrai navigateur connecté.

Une validation progressive par groupes ne prouve toujours pas le fonctionnement simultané de toute l’architecture. Les décisions de congés et de pilotage doivent être acceptées séparément, puis testées sur le candidat approprié.

**LOT 3C PARTIEL — fonctionnalités définies développées et tests ciblés réussis ; validation intégrée réelle non réalisée et décisions métier restantes.**
