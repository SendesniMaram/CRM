# Lot 1A — Fiabilisation des fonctionnalités CRM

Date : 1er novembre 2026. Branche : publication-clean.

## A. Corrections et contrat

### AUD-02 — Statistiques client

Le contrat HTTP existant de PersonalInvoiceService est conservé : byCurrency est un objet JSON indexé par devise. Chaque entrée contient invoiceCount, total, paid et outstanding. Les trois compteurs globaux sont invoiceCount, paidInvoiceCount et unpaidInvoiceCount.

Angular valide ce contrat puis le transforme en tableau pour les composants. Il vérifie les compteurs entiers non négatifs, leur cohérence, les montants finis non négatifs, le reste dû et la somme des nombres de factures par devise. Une réponse incomplète, un montant null ou un tableau à la place de l'objet est refusé ; aucune valeur zéro n'est inventée. Un montant numérique zéro est accepté. Une erreur de contrat est distinguée d'une erreur de disponibilité dans l'interface.

Le jeu de données partagé frontend/src/app/core/testing/client-invoice-statistics.json est consommé par les tests Angular et vérifié contre la sérialisation Java du vrai DTO : aucune facture, une devise, plusieurs devises et montants zéro. Ce test vérifie le format du DTO, pas les requêtes SQL réelles.

### AUD-04 — Intégrité des factures

- États de saisie conservés : DRAFT, PENDING, UNPAID. Aucun nouveau statut n'est introduit.
- PAID et PARTIALLY_PAID ne peuvent pas être imposés par le CRUD. Ils résultent des paiements enregistrés.
- Les paiements sont acceptés pour PENDING, UNPAID et PARTIALLY_PAID. Un brouillon doit d'abord être passé en attente/non réglé.
- Toute facture avec au moins une ligne de paiement est protégée contre toute édition et toute suppression, même si son statut historique est erroné. Cette politique prudente bloque aussi les notes : une éventuelle édition de métadonnées séparée reste une décision métier.
- Un statut payé historique sans paiement n'est pas réinterprété ou réparé automatiquement : édition/suppression refusées.
- Modification, suppression, paiement et ajout de relance prennent le même verrou pessimiste sur la facture, dans une transaction.
- Une modification ne peut pas changer le sous-total, le client ou la devise d'une facture ayant des lignes issues d'un devis ; les lignes restent la source du sous-total.
- Les factures avec lignes ou relances ne sont pas supprimées par cette API, afin de conserver leur historique.
- Les paiements existants ne sont ni réécrits ni supprimés. Le paiement partiel/complet conserve le calcul existant du solde.
- Angular propose les statuts non réglés, interdit le formulaire d'édition des états payés, explique les conflits et recharge le statut après paiement.

Aucune annulation comptable, aucun avoir, remboursement ou nouveau workflow fiscal n'est ajouté.

### AUD-05 — Validations financières

Factures : sous-total obligatoire strictement positif ; taxe facultative non négative ; montants DECIMAL(12,2), dix chiffres entiers et deux décimales au maximum ; somme calculée bornée ; échéance non antérieure à l'émission lorsque les deux dates sont présentes ; statut connu obligatoire.

Paie : salaire de base obligatoire positif ; primes, retenues et impôt facultatifs mais non négatifs ; même précision DECIMAL(12,2) ; net calculé non négatif et borné ; période AAAA-MM avec mois 01–12 ; statuts existants PENDING ou PAID.

Les règles sont appliquées aux DTO HTTP ET aux appels directs des services, notamment la conversion devis → facture. Les formulaires Angular utilisent les mêmes limites. Les comparaisons des totaux/net sont faites en unités monétaires mineures côté formulaire pour éviter les erreurs binaires telles que 0,3 - 0,1 - 0,2.

Les devises restent facultatives et limitées en longueur, conformément au modèle existant ; aucun catalogue de devises ni règle fiscale n'est inventé. Les anciennes valeurs de statut/période restent lisibles, mais une écriture nouvelle doit respecter le contrat. Aucun historique n'est normalisé automatiquement.

### AUD-14 — Réponses et diagnostics SQL

Les handlers Invoice, Payroll, HR et Fees remplacent les messages de DataIntegrityViolationException par une réponse constante 409. Employee et Department disposent désormais également d'un handler 409 explicite. Customer et Identity avaient déjà une réponse d'intégrité constante : leur comportement est couvert par les nouveaux tests.

Les diagnostics d'intégrité ne journalisent que les types d'exception/cause. Le handler générique Identity ne journalise plus les messages et causes bruts, susceptibles de contenir des valeurs sensibles. Les réponses publiques ne contiennent ni SQL, table, contrainte ou valeur pilote. Les autres erreurs de persistance restent une erreur 500 générique.

## B. Fichiers du lot

Les modifications préexistantes sont exclues de cet inventaire. Ce rapport est lui-même un nouveau fichier de documentation.

| Fichier | Rôle |
|---|---|
| [frontend/src/app/core/testing/client-invoice-statistics.json](../frontend/src/app/core/testing/client-invoice-statistics.json) | Jeu de contrat partagé Java/Angular |
| [frontend/src/app/features/dashboard/dashboard-data.service.spec.ts](../frontend/src/app/features/dashboard/dashboard-data.service.spec.ts) | Tests Angular, contrat ou fixtures alignées sur les statuts API |
| [frontend/src/app/features/dashboard/dashboard-data.service.ts](../frontend/src/app/features/dashboard/dashboard-data.service.ts) | Adaptation et contrôle du contrat des statistiques client |
| [frontend/src/app/features/invoices/invoice-detail.component.html](../frontend/src/app/features/invoices/invoice-detail.component.html) | Formulaire ou interface financière harmonisée |
| [frontend/src/app/features/invoices/invoice-detail.component.spec.ts](../frontend/src/app/features/invoices/invoice-detail.component.spec.ts) | Tests Angular, contrat ou fixtures alignées sur les statuts API |
| [frontend/src/app/features/invoices/invoice-detail.component.ts](../frontend/src/app/features/invoices/invoice-detail.component.ts) | Formulaire ou interface financière harmonisée |
| [frontend/src/app/features/invoices/invoice-form.component.html](../frontend/src/app/features/invoices/invoice-form.component.html) | Formulaire ou interface financière harmonisée |
| [frontend/src/app/features/invoices/invoice-form.component.spec.ts](../frontend/src/app/features/invoices/invoice-form.component.spec.ts) | Tests Angular, contrat ou fixtures alignées sur les statuts API |
| [frontend/src/app/features/invoices/invoice-form.component.ts](../frontend/src/app/features/invoices/invoice-form.component.ts) | Formulaire ou interface financière harmonisée |
| [frontend/src/app/features/invoices/invoice.service.spec.ts](../frontend/src/app/features/invoices/invoice.service.spec.ts) | Tests Angular, contrat ou fixtures alignées sur les statuts API |
| [frontend/src/app/features/invoices/invoices-page.component.spec.ts](../frontend/src/app/features/invoices/invoices-page.component.spec.ts) | Tests Angular, contrat ou fixtures alignées sur les statuts API |
| [frontend/src/app/features/invoices/invoices.routes.spec.ts](../frontend/src/app/features/invoices/invoices.routes.spec.ts) | Tests Angular, contrat ou fixtures alignées sur les statuts API |
| [frontend/src/app/features/payroll/payroll-detail.component.spec.ts](../frontend/src/app/features/payroll/payroll-detail.component.spec.ts) | Tests Angular, contrat ou fixtures alignées sur les statuts API |
| [frontend/src/app/features/payroll/payroll-form.component.html](../frontend/src/app/features/payroll/payroll-form.component.html) | Formulaire ou interface financière harmonisée |
| [frontend/src/app/features/payroll/payroll-form.component.spec.ts](../frontend/src/app/features/payroll/payroll-form.component.spec.ts) | Tests Angular, contrat ou fixtures alignées sur les statuts API |
| [frontend/src/app/features/payroll/payroll-form.component.ts](../frontend/src/app/features/payroll/payroll-form.component.ts) | Formulaire ou interface financière harmonisée |
| [frontend/src/app/features/payroll/payroll.routes.spec.ts](../frontend/src/app/features/payroll/payroll.routes.spec.ts) | Tests Angular, contrat ou fixtures alignées sur les statuts API |
| [frontend/src/app/features/payroll/payroll.service.spec.ts](../frontend/src/app/features/payroll/payroll.service.spec.ts) | Tests Angular, contrat ou fixtures alignées sur les statuts API |
| [frontend/src/app/features/payroll/payrolls-page.component.spec.ts](../frontend/src/app/features/payroll/payrolls-page.component.spec.ts) | Tests Angular, contrat ou fixtures alignées sur les statuts API |
| [frontend/src/app/shared/financial-validators.spec.ts](../frontend/src/app/shared/financial-validators.spec.ts) | Tests Angular, contrat ou fixtures alignées sur les statuts API |
| [frontend/src/app/shared/financial-validators.ts](../frontend/src/app/shared/financial-validators.ts) | Validation monétaire partagée et calculs en unités mineures |
| [microservices/customer-service/src/test/java/com/crm/customer/SqlErrorPrivacyTest.java](../microservices/customer-service/src/test/java/com/crm/customer/SqlErrorPrivacyTest.java) | Test MVC autonome de non-divulgation SQL |
| [microservices/department-service/src/main/java/com/crm/department/exception/GlobalExceptionHandler.java](../microservices/department-service/src/main/java/com/crm/department/exception/GlobalExceptionHandler.java) | Réponses d’erreur et diagnostics sûrs |
| [microservices/department-service/src/test/java/com/crm/department/SqlErrorPrivacyTest.java](../microservices/department-service/src/test/java/com/crm/department/SqlErrorPrivacyTest.java) | Test MVC autonome de non-divulgation SQL |
| [microservices/employee-service/src/main/java/com/crm/employee/exception/GlobalExceptionHandler.java](../microservices/employee-service/src/main/java/com/crm/employee/exception/GlobalExceptionHandler.java) | Réponses d’erreur et diagnostics sûrs |
| [microservices/employee-service/src/test/java/com/crm/employee/SqlErrorPrivacyTest.java](../microservices/employee-service/src/test/java/com/crm/employee/SqlErrorPrivacyTest.java) | Test MVC autonome de non-divulgation SQL |
| [microservices/fees-service/src/main/java/com/crm/fees/exception/GlobalExceptionHandler.java](../microservices/fees-service/src/main/java/com/crm/fees/exception/GlobalExceptionHandler.java) | Réponses d’erreur et diagnostics sûrs |
| [microservices/fees-service/src/test/java/com/crm/fees/SqlErrorPrivacyTest.java](../microservices/fees-service/src/test/java/com/crm/fees/SqlErrorPrivacyTest.java) | Test MVC autonome de non-divulgation SQL |
| [microservices/hr-service/src/main/java/com/crm/hr/exception/GlobalExceptionHandler.java](../microservices/hr-service/src/main/java/com/crm/hr/exception/GlobalExceptionHandler.java) | Réponses d’erreur et diagnostics sûrs |
| [microservices/hr-service/src/test/java/com/crm/hr/SqlErrorPrivacyTest.java](../microservices/hr-service/src/test/java/com/crm/hr/SqlErrorPrivacyTest.java) | Test MVC autonome de non-divulgation SQL |
| [microservices/identity-service/src/main/java/com/crm/identity/exception/GlobalExceptionHandler.java](../microservices/identity-service/src/main/java/com/crm/identity/exception/GlobalExceptionHandler.java) | Réponses d’erreur et diagnostics sûrs |
| [microservices/identity-service/src/test/java/com/crm/identity/SqlErrorPrivacyTest.java](../microservices/identity-service/src/test/java/com/crm/identity/SqlErrorPrivacyTest.java) | Test MVC autonome de non-divulgation SQL |
| [microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoiceController.java](../microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoiceController.java) | Paiements, statuts et verrou de relance |
| [microservices/invoice-service/src/main/java/com/crm/invoice/dto/InvoiceRequest.java](../microservices/invoice-service/src/main/java/com/crm/invoice/dto/InvoiceRequest.java) | Contraintes de saisie financière |
| [microservices/invoice-service/src/main/java/com/crm/invoice/exception/GlobalExceptionHandler.java](../microservices/invoice-service/src/main/java/com/crm/invoice/exception/GlobalExceptionHandler.java) | Réponses d’erreur et diagnostics sûrs |
| [microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoicePaymentRepository.java](../microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoicePaymentRepository.java) | Détection des paiements conservés |
| [microservices/invoice-service/src/main/java/com/crm/invoice/service/FinancialValidation.java](../microservices/invoice-service/src/main/java/com/crm/invoice/service/FinancialValidation.java) | Validation des appels directs au service et des montants calculés |
| [microservices/invoice-service/src/main/java/com/crm/invoice/service/impl/InvoiceServiceImpl.java](../microservices/invoice-service/src/main/java/com/crm/invoice/service/impl/InvoiceServiceImpl.java) | Application des règles métier et protections transactionnelles |
| [microservices/invoice-service/src/test/java/com/crm/invoice/ClientStatisticsContractTest.java](../microservices/invoice-service/src/test/java/com/crm/invoice/ClientStatisticsContractTest.java) | Tests unitaires de contrat ou de règles financières |
| [microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceFinancialPolicyTest.java](../microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceFinancialPolicyTest.java) | Tests unitaires de contrat ou de règles financières |
| [microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceServiceIntegrationTest.java](../microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceServiceIntegrationTest.java) | Anciennes attentes de statut ajustées ; suite avec base NON exécutée |
| [microservices/invoice-service/src/test/java/com/crm/invoice/SqlErrorPrivacyTest.java](../microservices/invoice-service/src/test/java/com/crm/invoice/SqlErrorPrivacyTest.java) | Test MVC autonome de non-divulgation SQL |
| [microservices/payroll-service/src/main/java/com/crm/payroll/dto/PayrollRequest.java](../microservices/payroll-service/src/main/java/com/crm/payroll/dto/PayrollRequest.java) | Contraintes de saisie financière |
| [microservices/payroll-service/src/main/java/com/crm/payroll/exception/GlobalExceptionHandler.java](../microservices/payroll-service/src/main/java/com/crm/payroll/exception/GlobalExceptionHandler.java) | Réponses d’erreur et diagnostics sûrs |
| [microservices/payroll-service/src/main/java/com/crm/payroll/service/FinancialValidation.java](../microservices/payroll-service/src/main/java/com/crm/payroll/service/FinancialValidation.java) | Validation des appels directs au service et des montants calculés |
| [microservices/payroll-service/src/main/java/com/crm/payroll/service/impl/PayrollServiceImpl.java](../microservices/payroll-service/src/main/java/com/crm/payroll/service/impl/PayrollServiceImpl.java) | Application des règles métier et protections transactionnelles |
| [microservices/payroll-service/src/test/java/com/crm/payroll/PayrollFinancialPolicyTest.java](../microservices/payroll-service/src/test/java/com/crm/payroll/PayrollFinancialPolicyTest.java) | Tests unitaires de contrat ou de règles financières |
| [microservices/payroll-service/src/test/java/com/crm/payroll/SqlErrorPrivacyTest.java](../microservices/payroll-service/src/test/java/com/crm/payroll/SqlErrorPrivacyTest.java) | Test MVC autonome de non-divulgation SQL |

## C. Tests exécutés

Toutes les exécutions ont été séquentielles. Aucun test @SpringBootTest, Flyway, migration H2/MySQL, conteneur, base réelle ou microservice n'a été exécuté. MockMvc standalone crée uniquement un servlet de test en mémoire, sans serveur applicatif.

Commandes Maven depuis la racine, avec cache existant, mode hors ligne et heap de test bornée :

```powershell
mvn -o -B -ntp '-Dmaven.repo.local=C:\Users\ASUS\.m2\repository' -f microservices/invoice-service/pom.xml '-Dtest=InvoiceFinancialPolicyTest,ClientStatisticsContractTest,SqlErrorPrivacyTest' '-DargLine=-Xmx384m' test
mvn -o -B -ntp '-Dmaven.repo.local=C:\Users\ASUS\.m2\repository' -f microservices/payroll-service/pom.xml '-Dtest=PayrollFinancialPolicyTest,SqlErrorPrivacyTest' '-DargLine=-Xmx384m' test

# Un seul module à la fois :
foreach ($module in @('hr','fees','identity','employee','department','customer')) {
  mvn -o -B -ntp '-Dmaven.repo.local=C:\Users\ASUS\.m2\repository' -f "microservices/$module-service/pom.xml" '-Dtest=SqlErrorPrivacyTest' '-DargLine=-Xmx384m' test
  if ($LASTEXITCODE -ne 0) { break }
}
```

Résultats finaux : Invoice 15, Payroll 12 et six autres tests de non-divulgation, soit **33 tests Java réussis**. Les tests Invoice vérifient les protections avant/après paiements partiel/complet, l'ordre verrou/contrôle/écriture, le contrat des annotations transactionnelles, les deux ordres séquentiels édition/paiement, les validations HTTP et service, le solde et la conservation de l'historique. Ils ne prouvent pas une attente de verrou MySQL réelle.

Les essais initiaux de Maven dans le sandbox ont rencontré des accès refusés au cache ; la validation a abouti avec accès aux dépendances existantes. Une ambiguïté de surcharge Mockito dans un nouveau test a été corrigée avant la relance réussie. Aucun téléchargement réseau n'était nécessaire.

Commande Angular finale depuis frontend :

```powershell
$env:CHROME_BIN='C:\Program Files\Google\Chrome\Application\chrome.exe'
$env:NODE_OPTIONS='--max-old-space-size=2048'
.\node_modules\.bin\ng.cmd test --no-watch --browsers=ChromeHeadless --karma-config=karma.ci.conf.cjs --include='src/app/features/dashboard/*.spec.ts' --include='src/app/features/invoices/*.spec.ts' --include='src/app/features/payroll/*.spec.ts' --include='src/app/shared/financial-validators.spec.ts'
```

**158 tests Angular réussis**, couvrant tableaux de bord, formulaires, pages, routes, services, paiements et validateurs partagés. La passe initiale de 49 tests est incluse dans cette validation élargie et n'est pas additionnée au total.

git diff --check : réussi.

Pas de build Angular de production ou suite globale relancés. La compilation ciblée Maven compile les classes du module et ses sources de test, mais seuls les tests explicitement sélectionnés sont exécutés.

## D. Limites et décisions métier

- Corrections AUD-02, AUD-04, AUD-05 et AUD-14 implémentées et validées dans le périmètre ciblé.
- Validation d'intégration MySQL NON exécutée : verrous simultanés, premier paiement concurrent à une édition/suppression, rollback, deadlock, précision SQL et persistance restent à vérifier.
- Aucun audit/correctif des données historiques : les statuts payés sans paiement et les anciens statuts libres nécessiteront un diagnostic autorisé, pas une réécriture automatique.
- Décider du parcours avoir/remboursement/annulation et de l'édition des seules notes après paiement.
- Décider des statuts supplémentaires, règles de correction rétroactive et transitions propres à la paie. PAID reste une déclaration manuelle existante, sans preuve bancaire.
- Le net négatif est refusé prudemment ; un éventuel mécanisme de dette/report doit faire l'objet d'une décision métier distincte.
- Les devises restent libres ; les règles de devise obligatoire et de précision par devise ne sont pas définies par ce lot.
- Publication immuable de la paie, sessions/JWT, profils HTTPS et déploiement restent hors Lot 1A.
- Les tests d'intégration existants n'ont pas été exécutés car ils chargent des contextes applicatifs et peuvent activer Flyway. Leurs attentes de paiement direct PAID ont été adaptées là où le contrat change.

Scénario de concurrence à exécuter plus tard sur MySQL isolé : transaction A verrouille une facture ; transaction B tente un paiement ou une modification/suppression ; vérifier l'attente puis, après commit A, soit le paiement sur le nouveau total, soit le conflit 409 sans mutation. Rejouer en inversant les transactions, avec paiement partiel/complet et rollback A. Aucun de ces scénarios n'est déclaré passé ici.

## E. Préservation et état Git

Les empreintes SHA-256 des **33 fichiers modifiés/non suivis préexistants** ont été comparées : aucun changement ni disparition. Ceci inclut les V1 non suivies, la migration corrective Invoice et les configurations Compose déjà présentes.

Branche conservée : publication-clean. Aucun commit, push, changement de branche, changement de migration, démarrage Docker ou accès aux volumes. Aucun fichier .env lu ou modifié.

## F. Recommandation

Le Lot 1A est prêt pour une validation d'intégration ultérieure. Le Lot 1B peut être préparé sur ce socle, sans considérer la livraison comme certifiée : les essais MySQL et E2E différés restent obligatoires avant déploiement.

### État Git final (sans staging)

```text
 M docker-compose.yml
 M docs/AUDIT_FONCTIONNEL_CRM.md
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
 M microservices/identity-service/src/main/java/com/crm/identity/exception/GlobalExceptionHandler.java
 M microservices/identity-service/src/main/resources/application.yml
 M microservices/identity-service/src/test/java/com/crm/identity/AdminEmployeeCreationTest.java
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
?? docs/PLAN_FINALISATION_CRM.md
?? frontend/src/app/core/testing/client-invoice-statistics.json
?? frontend/src/app/shared/financial-validators.spec.ts
?? frontend/src/app/shared/financial-validators.ts
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
?? microservices/identity-service/src/test/java/com/crm/identity/SqlErrorPrivacyTest.java
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
