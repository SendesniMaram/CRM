# Lot 3B — Finalisation fonctionnelle du module Paie

Date : 2 novembre 2026. Branche conservée : publication-clean.

## 1. Résumé exécutif

**LOT 3B PARTIEL — fonctionnalités définies implémentées, 75 tests Java et 101 tests Angular réussis ; décisions métier et intégration réelle restantes.**

La paie comprend désormais des heures supplémentaires à taux applicable explicitement saisi, un registre de règlements avec contre-écritures, des bulletins publiés figés et des brouillons correctifs liés. Les administrateurs disposent d’un CSV technique par période. Les salariés consultent uniquement les règlements de leurs bulletins publiés, avec un DTO réduit.

Aucun taux légal, transfert bancaire, export comptable officiel ou déclaration sociale/fiscale n’a été inventé. Aucun Docker, MySQL, microservice, Flyway ou E2E n’a été lancé. Les tests de servlet utilisent MockMvc ; ChromeHeadless exécute des tests de composants avec services simulés. Ils ne certifient pas un parcours connecté.

## 2. Fonctionnalités développées et inspection préalable

Les rapports [3A](LOT_3A_AUDIT_CONFORMITE_FONCTIONNELLE.md), [1A](LOT_1A_FIABILISATION.md), [1B](LOT_1B_SECURISATION_COMPTES_SESSIONS.md), [1C](LOT_1C_ASSOCIATIONS_ESPACES_PERSONNELS.md), [2B-R](LOT_2B_R_VALIDATION_PROGRESSIVE.md) et le [CDC](CDC%20CRM%201.pdf) ont été consultés. Payroll, ses contrôleurs, DTO, dépôts, PDF, tests et écrans ont été confrontés aux associations HR, au résumé Employee, à common-security et aux routes Angular. Aucun document demandé absent.

Le périmètre retenu réutilise le CRUD, la publication, les PDF, le calcul financier et l’association personnelle existants. Il ajoute :

- Le couple facultatif heures/taux applicable et son montant calculé, affichés dans les formulaires, détails et PDF.
- Les paiements manuels partiels/complets, le total réglé, le solde et l’historique chronologique.
- Une identité de requête pour les reprises, une référence interne unique et une référence de transaction facultative.
- Des contre-écritures motivées, sans modification ni suppression des règlements originaux.
- Le refus d’éditer, supprimer ou dépublier un bulletin publié ; sa correction par un nouveau brouillon lié.
- La lecture personnelle des règlements et un export technique UTF-8 filtré par mois.

## 3. Fichiers créés et modifiés

Inventaire relatif à la photographie de début du Lot 3B, et non au HEAD Git déjà très modifié : **25 fichiers préexistants modifiés et 21 nouveaux fichiers, rapport compris**.

### Fichiers préexistants modifiés

- [frontend/src/app/features/payroll/my-payroll-detail.component.html](../frontend/src/app/features/payroll/my-payroll-detail.component.html)
- [frontend/src/app/features/payroll/my-payroll-detail.component.ts](../frontend/src/app/features/payroll/my-payroll-detail.component.ts)
- [frontend/src/app/features/payroll/my-payroll.service.spec.ts](../frontend/src/app/features/payroll/my-payroll.service.spec.ts)
- [frontend/src/app/features/payroll/my-payroll.service.ts](../frontend/src/app/features/payroll/my-payroll.service.ts)
- [frontend/src/app/features/payroll/payroll-detail.component.html](../frontend/src/app/features/payroll/payroll-detail.component.html)
- [frontend/src/app/features/payroll/payroll-detail.component.spec.ts](../frontend/src/app/features/payroll/payroll-detail.component.spec.ts)
- [frontend/src/app/features/payroll/payroll-detail.component.ts](../frontend/src/app/features/payroll/payroll-detail.component.ts)
- [frontend/src/app/features/payroll/payroll-form.component.html](../frontend/src/app/features/payroll/payroll-form.component.html)
- [frontend/src/app/features/payroll/payroll-form.component.spec.ts](../frontend/src/app/features/payroll/payroll-form.component.spec.ts)
- [frontend/src/app/features/payroll/payroll-form.component.ts](../frontend/src/app/features/payroll/payroll-form.component.ts)
- [frontend/src/app/features/payroll/payroll.models.ts](../frontend/src/app/features/payroll/payroll.models.ts)
- [frontend/src/app/features/payroll/payroll.service.spec.ts](../frontend/src/app/features/payroll/payroll.service.spec.ts)
- [frontend/src/app/features/payroll/payroll.service.ts](../frontend/src/app/features/payroll/payroll.service.ts)
- [frontend/src/app/features/payroll/payrolls-page.component.html](../frontend/src/app/features/payroll/payrolls-page.component.html)
- [frontend/src/app/features/payroll/payrolls-page.component.spec.ts](../frontend/src/app/features/payroll/payrolls-page.component.spec.ts)
- [frontend/src/app/features/payroll/payrolls-page.component.ts](../frontend/src/app/features/payroll/payrolls-page.component.ts)
- [microservices/payroll-service/src/main/java/com/crm/payroll/controller/PayrollPdfController.java](../microservices/payroll-service/src/main/java/com/crm/payroll/controller/PayrollPdfController.java)
- [microservices/payroll-service/src/main/java/com/crm/payroll/controller/PersonalPayrollController.java](../microservices/payroll-service/src/main/java/com/crm/payroll/controller/PersonalPayrollController.java)
- [microservices/payroll-service/src/main/java/com/crm/payroll/dto/PayrollRequest.java](../microservices/payroll-service/src/main/java/com/crm/payroll/dto/PayrollRequest.java)
- [microservices/payroll-service/src/main/java/com/crm/payroll/dto/PayrollResponse.java](../microservices/payroll-service/src/main/java/com/crm/payroll/dto/PayrollResponse.java)
- [microservices/payroll-service/src/main/java/com/crm/payroll/entity/Payroll.java](../microservices/payroll-service/src/main/java/com/crm/payroll/entity/Payroll.java)
- [microservices/payroll-service/src/main/java/com/crm/payroll/mapper/PayrollMapper.java](../microservices/payroll-service/src/main/java/com/crm/payroll/mapper/PayrollMapper.java)
- [microservices/payroll-service/src/main/java/com/crm/payroll/repository/PayrollRepository.java](../microservices/payroll-service/src/main/java/com/crm/payroll/repository/PayrollRepository.java)
- [microservices/payroll-service/src/main/java/com/crm/payroll/service/impl/PayrollServiceImpl.java](../microservices/payroll-service/src/main/java/com/crm/payroll/service/impl/PayrollServiceImpl.java)
- [microservices/payroll-service/src/test/java/com/crm/payroll/PayrollFinancialPolicyTest.java](../microservices/payroll-service/src/test/java/com/crm/payroll/PayrollFinancialPolicyTest.java)

### Nouveaux fichiers

- [frontend/src/app/features/payroll/payroll-calculation.spec.ts](../frontend/src/app/features/payroll/payroll-calculation.spec.ts)
- [frontend/src/app/features/payroll/payroll-calculation.ts](../frontend/src/app/features/payroll/payroll-calculation.ts)
- [frontend/src/app/features/payroll/payroll-settlements.component.spec.ts](../frontend/src/app/features/payroll/payroll-settlements.component.spec.ts)
- [frontend/src/app/features/payroll/payroll-settlements.component.ts](../frontend/src/app/features/payroll/payroll-settlements.component.ts)
- [frontend/src/app/features/payroll/payroll-settlements.models.ts](../frontend/src/app/features/payroll/payroll-settlements.models.ts)
- [microservices/payroll-service/src/main/java/com/crm/payroll/controller/PayrollSettlementController.java](../microservices/payroll-service/src/main/java/com/crm/payroll/controller/PayrollSettlementController.java)
- [microservices/payroll-service/src/main/java/com/crm/payroll/entity/PayrollSettlement.java](../microservices/payroll-service/src/main/java/com/crm/payroll/entity/PayrollSettlement.java)
- [microservices/payroll-service/src/main/java/com/crm/payroll/repository/PayrollSettlementRepository.java](../microservices/payroll-service/src/main/java/com/crm/payroll/repository/PayrollSettlementRepository.java)
- [microservices/payroll-service/src/main/java/com/crm/payroll/service/PayrollAuthorization.java](../microservices/payroll-service/src/main/java/com/crm/payroll/service/PayrollAuthorization.java)
- [microservices/payroll-service/src/main/java/com/crm/payroll/service/PayrollCalculation.java](../microservices/payroll-service/src/main/java/com/crm/payroll/service/PayrollCalculation.java)
- [microservices/payroll-service/src/main/java/com/crm/payroll/service/PayrollLedger.java](../microservices/payroll-service/src/main/java/com/crm/payroll/service/PayrollLedger.java)
- [microservices/payroll-service/src/main/java/com/crm/payroll/service/PayrollSettlementService.java](../microservices/payroll-service/src/main/java/com/crm/payroll/service/PayrollSettlementService.java)
- [microservices/payroll-service/src/main/java/com/crm/payroll/service/PayrollTechnicalExport.java](../microservices/payroll-service/src/main/java/com/crm/payroll/service/PayrollTechnicalExport.java)
- [microservices/payroll-service/src/main/resources/db/migration/V20261102_01__payroll_variables_and_settlements.sql](../microservices/payroll-service/src/main/resources/db/migration/V20261102_01__payroll_variables_and_settlements.sql)
- [microservices/payroll-service/src/test/java/com/crm/payroll/PayrollPersonalOwnershipPolicyTest.java](../microservices/payroll-service/src/test/java/com/crm/payroll/PayrollPersonalOwnershipPolicyTest.java)
- [microservices/payroll-service/src/test/java/com/crm/payroll/PayrollSettlementPolicyTest.java](../microservices/payroll-service/src/test/java/com/crm/payroll/PayrollSettlementPolicyTest.java)
- [microservices/payroll-service/src/test/java/com/crm/payroll/PayrollSettlementSecurityTest.java](../microservices/payroll-service/src/test/java/com/crm/payroll/PayrollSettlementSecurityTest.java)
- [microservices/payroll-service/src/test/java/com/crm/payroll/PayrollTechnicalExportTest.java](../microservices/payroll-service/src/test/java/com/crm/payroll/PayrollTechnicalExportTest.java)
- [microservices/payroll-service/src/test/java/com/crm/payroll/PayrollVariableCalculationTest.java](../microservices/payroll-service/src/test/java/com/crm/payroll/PayrollVariableCalculationTest.java)
- [microservices/payroll-service/src/test/java/com/crm/payroll/PayrollVariablePdfTest.java](../microservices/payroll-service/src/test/java/com/crm/payroll/PayrollVariablePdfTest.java)
- [docs/LOT_3B_FINALISATION_PAIE.md](LOT_3B_FINALISATION_PAIE.md) — ce rapport.

Les 975 fichiers de la photographie initiale ont été comparés par SHA-256 : aucune disparition ; seules les 25 modifications de paie inventoriées ci-dessus. Les 950 autres fichiers photographiés sont inchangés, dont les 24 migrations historiques et tous les fichiers du Lot 2B-R. Les fichiers de secrets étaient exclus de la lecture et des empreintes. Aucun accès aux secrets, staging, commit, push, changement de branche ou nettoyage.

## 4. Architecture des nouvelles fonctionnalités

PayrollCalculation centralise le calcul BigDecimal et réutilise FinancialValidation aux frontières HTTP et service. PayrollSettlementService porte les écritures du registre dans une transaction, avec verrou pessimiste de la ligne Payroll et rafraîchissement sous verrou. Mise à jour, suppression, publication et correction passent par ce même verrou.

PayrollSettlementRepository expose la lecture et saveAndFlush, sans API de suppression ou de mise à jour du registre. Les colonnes d’une écriture sont non modifiables par JPA. Les contraintes SQL prévues constituent la seconde protection contre les doublons. Les contraintes et les verrous doivent encore être éprouvés sur MySQL.

PayrollLedger calcule les montants depuis les écritures, sans stocker un solde indépendant. PayrollTechnicalExport lit les bulletins d’une période dans une transaction en lecture. La nouvelle lecture EMPLOYEE délègue d’abord à PersonalPayrollService : UID authentifié → association HR → salarié résolu côté serveur → bulletin propre et publié → registre réduit.

## 5. Endpoints REST ajoutés ou modifiés

Les chemins ci-dessous sont locaux au service ; le Gateway existant les préfixe par /payroll. Aucun routage ni filtre de sécurité transversal modifié.

| Méthode et chemin | Contrat / autorisation |
|---|---|
| GET /api/payrolls/{id}/settlements | Total réglé, solde, état, écritures et trace de correction ; ADMIN/SUPER_ADMIN |
| POST /api/payrolls/{id}/settlements | requestId UUID, amount, paidAt, method et transactionReference facultatifs ; ADMIN/SUPER_ADMIN |
| POST /api/payrolls/{id}/settlements/{reference}/reversal | requestId UUID et motif obligatoire ; ADMIN/SUPER_ADMIN |
| POST /api/payrolls/{id}/corrections | Motif obligatoire ; retourne le brouillon correctif ; ADMIN/SUPER_ADMIN |
| GET /api/payrolls/me/{id}/settlements | EMPLOYEE propriétaire, bulletin publié ; aucun paramètre de ciblage accepté |
| GET /api/payrolls/export/technical.csv?period=AAAA-MM | CSV technique, mois obligatoire ; ADMIN/SUPER_ADMIN |
| POST/PUT /api/payrolls et /{id} | Heures/taux facultatifs ; seul PENDING est autorisé en saisie CRUD |
| PATCH /api/payrolls/{id}/publication | Publication conservée ; retrait d’un bulletin déjà publié refusé en 409 |
| DELETE /api/payrolls/{id} | Seulement brouillon PENDING sans registre ni lien de correction |
| GET PDF admin et personnel existants | Ajout des variables et de la mention d’annulation/remplacement ; contrôles antérieurs conservés |

Les nouvelles réponses imposent no-store. Les montants du registre sont des chaînes décimales ; l’auteur est un UID textuel. L’espace personnel ne reçoit ni référence de transaction, motif interne, auteur ni trace administrative de correction. Erreurs 400/401/403/404/409/502/503 selon validation, droits, ressource, conflit et disponibilité ; aucune erreur SQL détaillée n’est renvoyée.

## 6. Interfaces Angular

Le formulaire admin affiche heures, taux saisi, net prévisionnel et erreurs de précision, de paire incomplète ou de net hors limites. Le calcul de prévisualisation utilise des entiers BigInt au centime pour éviter les erreurs binaires aux limites. La sauvegarde reste soumise au calcul serveur.

Le composant PayrollSettlementsComponent est intégré aux détails admin et personnel. Le chargement est explicite, avec état occupé, vide, erreur, total, solde et liste chronologique. L’administrateur saisit un règlement, un motif de contre-écriture ou ouvre le brouillon correctif. EMPLOYEE n’a que la consultation. Une réponse tardive d’une autre session ou d’un autre bulletin est ignorée ; les données et saisies du registre sont effacées au changement de contexte. Une reprise du même règlement après erreur conserve son requestId.

La liste admin propose un mois et un bouton CSV clairement technique. Une réponse d’export arrivant après changement de session n’est pas téléchargée. Les liens d’édition et actions de suppression/retrait sont bloqués après publication ; le serveur applique indépendamment les mêmes protections. Aucun nouvel écran déclaratif fictif.

## 7. Migration Flyway préparée

[V20261102_01__payroll_variables_and_settlements.sql](../microservices/payroll-service/src/main/resources/db/migration/V20261102_01__payroll_variables_and_settlements.sql) ajoute les trois variables facultatives, l’annulation et les liens/métadonnées de correction, puis la table payroll_settlements.

Contrôle statique : version nouvelle après V1 et publication ; payrolls existe avant ALTER ; liens de correction vers son identifiant BIGINT ; table de règlements créée avant sa clé étrangère réflexive ; montants DECIMAL(12,2), heures DECIMAL(8,2), références uniques, index chronologique et clés étrangères. Aucun UPDATE/DELETE historique, DROP, baseline, repair ou clean. Les anciens champs d’heures restent NULL et cancelled vaut faux par défaut. Les 24 fichiers de migration antérieurs sont inchangés par empreinte.

**Migration NON EXÉCUTÉE.** Syntaxe MySQL effective, DDL, correspondance Hibernate, collations, contraintes, rollback et upgrade restent non validés. Les DDL MySQL ne garantissent pas de rollback global. Aucune modification des configurations Flyway existantes.

## 8. Règles de calcul

Montant des heures supplémentaires = heures × taux applicable saisi, arrondi technique au centime par HALF_UP. Net = base + primes + montant des heures supplémentaires − déductions − taxes déjà saisies.

Exemple de test : base 100,00, heures 2,50, taux 12,34 → supplément 30,85 → net 130,85, sans autres variables. Une multiplication donnant un demi-centime est arrondie vers le centime supérieur. Ce choix technique n’est pas une règle légale de paie.

Base strictement positive ; variables non négatives ; montants au plus 9 999 999 999,99 et deux décimales ; heures au plus 999 999,99 avec deux décimales. Le couple heures/taux est facultatif mais doit être complet ; des heures positives nécessitent un taux positif. Produit arrondi et net doivent respecter DECIMAL(12,2), net non négatif. Le serveur valide les appels directs autant que les DTO HTTP.

Un couple absent conserve exactement l’ancien calcul. Aucun recalcul automatique des bulletins historiques à la migration ou en lecture. Le taux est propre au bulletin ; aucun coefficient de majoration ou calcul fiscal/social automatique n’est supposé.

## 9. Règles de règlement

Un règlement est une déclaration manuelle, pas un transfert bancaire. Il est positif, daté et rattaché à un bulletin publié non annulé. Le total net des PAYMENT moins REVERSAL ne peut dépasser son net. Une même requête UUID et un même contenu retournent le résultat sans nouvelle écriture ; un contenu différent avec ce UUID provoque un conflit. Une référence de transaction renseignée ne peut être réutilisée sur le même bulletin.

Chaque écriture reçoit une référence interne UUID, un auteur UID et une date de création. La contre-écriture inverse la totalité d’un règlement PAYMENT, exige un motif et conserve l’original. Une seule contre-écriture par règlement, pas de suppression, modification ou contre-écriture d’une contre-écriture. En cas de montant erroné : contre-écrire puis saisir le bon règlement. Une contre-écriture ne prétend pas prouver un remboursement bancaire.

| Situation | Statut Payroll / état du registre |
|---|---|
| Aucun paiement ou paiement partiel | PENDING / UNPAID ou PARTIAL |
| Net positif entièrement réglé | PAID / SETTLED ; date = dernière date des paiements non contre-écrits |
| Contre-écriture rouvrant un solde | PENDING ; date de paiement effacée ; historique conservé |
| Net nul | PENDING / NO_AMOUNT_DUE ; aucun paiement fictif |
| Bulletin remplacé | Annulé / CANCELLED ; net original conservé, restant dû zéro |
| Historique PAID sans registre | LEGACY_UNRECONCILED ; solde inconnu, nouveau paiement et correction bloqués |

Pour un PAID historique sans registre, le total du registre est zéro car il n’y a aucune écriture connue ; cela n’affirme pas qu’aucun argent n’a été versé. Le solde reste NULL, l’interface dit « À rapprocher » et le CSV laisse ce solde vide. Aucun outil de rapprochement historique n’est encore implémenté.

## 10. Politique des bulletins publiés

Publication et paiement sont séparés. Après publication, les montants, le salarié et la devise ne sont plus éditables par le CRUD ; suppression et dépublication sont refusées. Les transitions de règlement restent autorisées et tracées.

Une correction motivée exige un original publié, non annulé, sans montant net déjà réglé et sans ambiguïté historique. Elle crée atomiquement un nouveau brouillon lié, marque l’ancien annulé et conserve ses montants, ses écritures, son auteur/motif/date de correction et le lien de remplacement. Le salarié conserve la consultation/PDF de l’original publié avec mention d’annulation ; le nouveau brouillon reste invisible avant publication.

Le brouillon correctif conserve le salarié et la devise ; il ne peut être supprimé ni réaffecté. Ses variables et sa période sont rectifiables avant nouvelle publication. Aucun recalcul rétroactif silencieux. Si un paiement existe, sa contre-écriture doit d’abord ramener le total réglé à zéro ; un historique PAID sans registre nécessite un rapprochement métier préalable.

## 11. Sécurité et autorisations

Les SecurityConfig et common-security existants sont conservés. ADMIN/SUPER_ADMIN administrent règlements, corrections et export. CLIENT est refusé. EMPLOYEE lit seulement son bulletin publié après résolution de son association HR ; employeeId n’est jamais accepté comme preuve de propriété. Les contrôles de service complètent le RBAC HTTP.

Les tests passent par les filtres de sécurité réels avec validation Identity simulée : rôles, 401 et indisponibilité 503. Les tests de propriété emploient le vrai PersonalPayrollService avec annuaire/dépôt simulés ; le registre n’est pas consulté si cette validation échoue. Cela ne prouve pas un JWT ou un service Identity réel en réseau.

Le CSV est UTF-8 avec BOM, en-têtes explicites, guillemets doublés et cellules commençant par =, +, -, @ ou espace de tête préfixées par une apostrophe. Il impose un mois, au plus 5 000 bulletins, lectures par pages de 250, sans total implicite entre devises. Chaque ligne porte TECHNICAL_NON_REGULATORY, les liens de correction et l’annulation. Ce n’est pas un journal comptable ; les lignes annulées ne doivent pas être additionnées comme paies courantes.

## 12. Tests exécutés

Exécutions séquentielles, Maven hors ligne et dépendances locales ; MAVEN_OPTS -Xmx512m, JVM de tests -Xmx384m, Node Angular limité à 2048 Mio. Aucun build Java et Angular simultané.

| Sélection Java | Réussis |
|---|---:|
| PayrollFinancialPolicyTest | 11 |
| PayrollVariableCalculationTest | 10 |
| PayrollSettlementPolicyTest | 22 |
| PayrollTechnicalExportTest | 6 |
| PayrollPersonalOwnershipPolicyTest | 4 |
| PayrollSettlementSecurityTest | 14 |
| PayrollPdfTest | 5 |
| PayrollVariablePdfTest | 2 |
| SqlErrorPrivacyTest | 1 |
| **Total Java ciblé** | **75** |

La sélection principale contient 73 tests ; les deux nouveaux tests de contenu PDF ont ensuite été exécutés séparément avec succès. Ils ne se recouvrent pas. Calcul nominal/historique, primes, retenues, arrondis, dépassements, paires invalides, règlements partiels/complets, surpaiement, doublons, contre-écritures, correction liée, refus de modification publiée, CSV/formules, rôles et erreurs sont couverts.

La sélection Angular finale couvre tous les fichiers spec de features/payroll : **101/101 réussis**, ChromeHeadless 156. Elle comprend services HTTP simulés, calcul exact, heures/taux, formulaire et net prévisionnel, registre vide/partiel, solde, reprise UUID, contre-écriture et motif, correction, lecture EMPLOYEE, erreurs, réponses tardives, export, liste, détails et routes protégées.

Commandes de sélection :

    mvn -o -B -ntp -Dmaven.repo.local=C:/Users/ASUS/.m2/repository -f microservices/payroll-service/pom.xml -Dtest=PayrollFinancialPolicyTest,PayrollVariableCalculationTest,PayrollSettlementPolicyTest,PayrollTechnicalExportTest,PayrollPdfTest,SqlErrorPrivacyTest,PayrollSettlementSecurityTest,PayrollPersonalOwnershipPolicyTest -DargLine=-Xmx384m test
    mvn -o -B -ntp -Dmaven.repo.local=C:/Users/ASUS/.m2/repository -f microservices/payroll-service/pom.xml -Dtest=PayrollVariablePdfTest -DargLine=-Xmx384m test
    # Depuis frontend, CHROME_BIN configuré et NODE_OPTIONS=--max-old-space-size=2048 :
    .\node_modules\.bin\ng.cmd test --no-watch --browsers=ChromeHeadless --karma-config=karma.ci.conf.cjs --include=src/app/features/payroll/*.spec.ts

Les premières tentatives ont révélé des erreurs de compilation/configuration de mocks et un test ancien conservant PAID comme statut de saisie, puis la garde Angular de retrait de publication manquante. Elles ont été corrigées avant les résultats ci-dessus. Une tentative Maven a aussi été refusée par le contrôle automatique pour limite d’usage, puis la reprise a été autorisée. Aucun résultat échoué ou non exécuté n’est compté comme un succès.

Les auxiliaires et résultats expurgés sont sous target/lot3b, ignoré par Git. Aucun credential réel utilisé. Les sorties brutes de frameworks ne constituent pas des preuves à publier. git diff --check standard réussit ; une tentative avec autocrlf désactivé a signalé les CRLF existants comme espaces finaux, sans modification de ces fichiers.

## 13. Tests non exécutés

**NON EXÉCUTÉS** : migrations et validation Hibernate sur MySQL neuf/upgrade ; verrous et transactions simultanées réelles ; contraintes d’unicité, clés étrangères et collations ; rollback après erreur SQL ; routing Gateway, vrai Identity/JWT révoqué, association HR réseau, TLS, campagne Docker, navigateur connecté, comptabilité cible et déclarations.

Les tests Mockito vérifient l’ordre d’acquisition du verrou, les écritures et les deux ordres paiement/correction. Ils ne sont pas des tests de concurrence SQL, de deadlock ou de rollback transactionnel. Le mock EntityManagerFactory du test de sécurité n’ouvre aucune base. Aucun test SpringBootTest avec base ni campagne E2E lancé ; le test PDF existant utilise uniquement une tranche WebMvcTest.

Les suites complètes des autres modules et les anciens décomptes 1A/1B/1C ne sont pas réexécutés ni additionnés à ceux-ci. Les ressources hôte et Docker n’ont pas été remesurées ; leur dernier état bloquant reste historique.

## 14. Anomalies corrigées

- CRUD pouvant altérer/supprimer un bulletin publié : refus serveur et interface, verrou commun avec règlements.
- PAID directement imposable par saisie admin : refus pour les nouvelles écritures CRUD ; statut dérivé des règlements.
- Absence de registre et de solde explicable : écritures positives, contre-écritures et calcul depuis l’historique.
- Absence d’heures supplémentaires : variables explicites, validations aux deux couches et PDF adaptés.
- Réponse tardive pouvant restaurer le registre d’une ancienne session ou d’un autre bulletin : contexte contrôlé et purge de l’état.

Les anomalies Fees/Customer AN-01/AN-02/AN-03 du Lot 3A ne sont pas corrigées dans ce lot de paie.

## 15. Limites et décisions métier en attente

À définir : juridiction et organismes, règles salariales contractuelles et légales, taux/majorations des heures supplémentaires, cotisations, barèmes fiscaux, périodes et formulaires déclaratifs ; validation métier des exemples de référence et des arrondis.

Pour PAY-07 : format, colonnes officielles, plan comptable, comptes/contreparties, logiciel cible, gestion des devises, règles des corrections et validation d’import. Le CSV actuel transmet les valeurs enregistrées ; aucun compte ni écriture comptable n’est inventé.

Pour les anciennes paies PAID sans registre : procédure de rapprochement contrôlée à décider et réaliser, avec pièces/acteurs et prévention du double paiement. Aucun backfill automatique autorisé. L’auteur/date/motif des nouvelles corrections sont tracés, mais aucun audit transversal ou preuve bancaire n’est ajouté.

La contre-écriture annule entièrement un paiement et la correction remplace entièrement un bulletin après total net réglé nul. Les corrections sur paie déjà versée nécessitant un rappel/avoir différentiel sont hors de ce mécanisme simple et demandent un contrat métier. L’export est borné et le registre n’a pas encore de pagination ; performances et longues périodes restent à éprouver.

## 16. État actualisé des exigences PAY-01 à PAY-09

COMPLETE ci-dessous reste une appréciation de périmètre de code avec tests ciblés, comme dans 3A. **Toutes les intégrations réelles restent NON VALIDÉES** ; le rapport 3A historique n’est pas réécrit.

| Exigence | Avant Lot 3B | Après Lot 3B | Preuves | Reste à faire |
|---|---|---|---|---|
| PAY-01 Calcul automatisé | PARTIELLE | PARTIELLE | PayrollCalculation, FinancialPolicy/VariableCalculation, formulaire et prévisualisation | Moteur de règles salariales et légales contractualisées ; intégration |
| PAY-02 Primes | COMPLETE | COMPLETE conservée | Validation BigDecimal, net, formulaire et PDF | Recette intégrée |
| PAY-03 Heures supplémentaires | ABSENTE | COMPLETE pour heures × taux applicable explicitement saisi | DTO/service, migration additive, formulaire/détails/PDF, tests de calcul/limites | Acceptation des taux saisis/arrondis, règles légales éventuelles et MySQL |
| PAY-04 Déductions | COMPLETE | COMPLETE conservée | Montants saisis bornés, net non négatif, tests financiers | Règles automatiques éventuelles et recette intégrée |
| PAY-05 PDF | COMPLETE | COMPLETE enrichie | 5 tests PDF existants et 2 tests variables/annulation admin/personnel | Gateway connecté, propriété en réseau et recette visuelle métier |
| PAY-06 Historique des paiements | PARTIELLE | PARTIELLE, nouveau registre fonctionnel complet dans son périmètre | 22 tests de règlements/corrections, sécurité/propriété, widget admin/personnel | Rapprochement des PAID historiques, concurrence/rollback MySQL et recette |
| PAY-07 Export comptable | ABSENTE | PARTIELLE | CSV technique UTF-8 par période, protection formules/RBAC, tests Java/Angular | Format comptable standard et réconciliation approuvés |
| PAY-08 Déclarations sociales | ABSENTE | ABSENTE | Aucune déclaration officielle fictive | Juridiction, organismes, règles, formats et validation métier |
| PAY-09 Déclarations fiscales | ABSENTE | ABSENTE | Aucune déclaration officielle fictive | Barèmes, formulaires, périodes, format et validation métier |

## 17. Préparation à la validation intégrée ultérieure

Conserver les configurations du Lot 2B-R. Le nouveau fichier de migration rend leurs anciens manifestes antérieurs au Lot 3B : régénérer une nouvelle campagne sous un identifiant libre avant toute exécution, puis revalider les modèles et empreintes, sans modifier ni réutiliser silencieusement un artefact ancien. Aucun démarrage autorisé par ce rapport.

Après précontrôles positifs et autorisation opérationnelle distincte, reprendre E-bootstrap puis E-payroll selon le [protocole progressif](LOT_2B_R_VALIDATION_PROGRESSIVE.md), sur bases et volumes isolés. Préparer les JAR/images contenant la nouvelle migration et vérifier leurs empreintes. Tester successivement :

1. Installation neuve V1 → publication → variables/registre, schéma validé par Hibernate ; puis upgrade sur copie sauvegardée et autorisée, avec historiques inchangés.
2. Deux salariés synthétiques et quatre rôles ; association serveur, publication, PDF/registre propres et refus croisés.
3. Heures, arrondis/limites, paies historiques, paiement partiel/complet, reprise UUID et référence transactionnelle.
4. Deux paiements concurrents, doubles contre-écritures, paiement contre correction/édition/publication, contraintes et rollback ; preuves attendues sans surpaiement ni doublon.
5. Correction : original inchangé et marqué annulé, brouillon invisible au salarié, nouvelle publication et CSV sans double comptabilisation implicite.
6. Parcours Angular connecté et erreurs Identity/HR, fichiers CSV/PDF, mémoire et absence d’OOM.

Cette campagne progressive ne démontrera toujours pas toute l’architecture simultanée. Les exigences sociales/fiscales et la comptabilité cible exigent leur propre recette après décision métier.

**LOT 3B PARTIEL — développements définis réalisés et tests ciblés réussis ; intégration, rapprochement historique et décisions métier restent à mener.**
