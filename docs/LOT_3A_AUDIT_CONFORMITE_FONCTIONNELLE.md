# Lot 3A — Audit de conformité fonctionnelle du CRM

Date : 2 novembre 2026. Branche : `publication-clean`. Référence : [CDC CRM 1.pdf](CDC%20CRM%201.pdf), dix pages, lu directement après extraction locale. Audit du dépôt de travail, incluant ses modifications et fichiers non suivis ; aucune modification applicative.

## 1. Résumé exécutif

**CONFORMITÉ FONCTIONNELLE PARTIELLE.** Le CRM contient des parcours réels et reliés : administration employés/contacts, associations, congés, registres RH, paie simple/PDF, honoraires simples, devis/factures/paiements manuels, espaces personnels protégés. Les Lots 1A/1B/1C renforcent ce socle. Les déclarations sociales/fiscales, export comptable, heures supplémentaires, grilles tarifaires, modèles de facture, archivage, import Excel et plusieurs privilèges du CDC restent absents ou incomplets.

La présence d'un champ ne vaut pas un workflow. Inversement, un registre manuel ne devient pas absent parce qu'il ne possède pas une intégration externe que le CDC n'impose pas. Les communications de contacts sont bien journalisées manuellement ; cela ne démontre pas un envoi SMTP, qui n'est pas exigé explicitement pour ce suivi.

| Module | Complètes | Partielles | Absentes | Non validées |
|---|---:|---:|---:|---:|
| Employés | 4 | 1 | 0 | 1 |
| RH | 8 | 4 | 0 | 1 |
| Paie | 3 | 2 | 4 | 0 |
| Honoraires | 2 | 3 | 1 | 0 |
| Factures | 8 | 1 | 2 | 0 |
| Contacts | 14 | 3 | 1 | 0 |
| **Total métier** | **39** | **14** | **8** | **2** |

Aucune ligne métier n’utilise BLOQUÉE : le blocage de validation est transversal, TR-13, et n’est pas recompté dans chaque module.

Le tableau porte uniquement sur les **63 lignes métier** du §3. Les exigences transversales supplémentaires ont leur propre décompte. Aucun pourcentage de conformité ou couverture de code n'est déduit de ces comptes. **Aucun parcours n'est certifié en intégration réelle** : MySQL, Gateway, HTTPS et navigateur connecté restent non validés. Le dernier précontrôle opérationnel était bloqué ; il n'a pas été rejoué ici.

## 2. Méthodologie et preuves

Lecture obligatoire des huit rapports demandés : [plan](PLAN_FINALISATION_CRM.md), [audit antérieur](AUDIT_FONCTIONNEL_CRM.md), [1A](LOT_1A_FIABILISATION.md), [1B](LOT_1B_SECURISATION_COMPTES_SESSIONS.md), [1C](LOT_1C_ASSOCIATIONS_ESPACES_PERSONNELS.md), [2A](LOT_2A_PREPARATION_VALIDATION.md), [2B](LOT_2B_VALIDATION_INTEGREE.md), [2B-R](LOT_2B_R_VALIDATION_PROGRESSIVE.md). Aucun de ces documents ne remplace le code inspecté.

Le PDF a été extrait avec PDFBox déjà en cache, heap 192 Mio, sans réseau, serveur ni tests applicatifs. Pages 5 : acteurs ; 6–7 : six modules ; 8 : exigences techniques/sécurité ; 9 : initialisation/livrables. La première extraction a nécessité de corriger le classpath local ; un avertissement de cache de polices non inscriptible n'a pas empêché la lecture des dix pages. Aucun contenu manquant n'est inventé.

Inspection des entités, repositories, DTO/validations, services, contrôleurs, routes API, SecurityConfig de chaque domaine, composants/services/templates Angular, guards, menus, tests et Jenkinsfile. Les noms de tests cités attestent leur présence, **pas leur réussite pendant 3A**. Aucun test lourd, E2E, Docker, MySQL ou Flyway lancé ; aucune sonde métier réelle.

Statuts appliqués : **COMPLETE** = périmètre fonctionnel décrit relié backend/Angular avec tests pertinents identifiés ; **PARTIELLE** = manque fonctionnel identifié ; **ABSENTE** = aucun parcours correspondant identifié dans le code actif ; **NON VALIDÉE** = parcours présent mais preuves de test insuffisantes pour conclure ; **BLOQUÉE** = contrainte identifiée empêchant validation/utilisation. COMPLETE n'est jamais synonyme de certification fiscale, production ou intégration. Pour chaque ligne métier : backend et Angular décrivent la couverture ; Tests décrit les preuves disponibles ; **I:NV** signifie intégration réelle NON VALIDÉE, **I:—** qu'aucun parcours à intégrer n'existe pour la fonction absente. Le blocage opérationnel commun est décrit séparément en TR-13.

### Catalogue des preuves de code

Les références de la matrice renvoient aux fichiers ci-dessous. Chaque chaîne précise entités/dépôts, services/contrôleurs, endpoints, interface et tests ; les chemins sont relatifs au dépôt. Les restrictions API sont confirmées dans les SecurityConfig, pas seulement dans les guards.

<a id="e"></a>

**E — Employés**

- Backend : [Employee](../microservices/employee-service/src/main/java/com/crm/employee/entity/Employee.java), [EmployeeRepository](../microservices/employee-service/src/main/java/com/crm/employee/repository/EmployeeRepository.java), [EmployeeServiceImpl](../microservices/employee-service/src/main/java/com/crm/employee/service/impl/EmployeeServiceImpl.java), [EmployeeController](../microservices/employee-service/src/main/java/com/crm/employee/controller/EmployeeController.java), [EmployeeSpecification](../microservices/employee-service/src/main/java/com/crm/employee/util/EmployeeSpecification.java).
- Angular : [employee.service.ts](../frontend/src/app/features/employees/services/employee.service.ts), [employee-form.component.ts](../frontend/src/app/features/employees/pages/employee-form/employee-form.component.ts), [employee-detail.component.ts](../frontend/src/app/features/employees/pages/employee-detail/employee-detail.component.ts), [employees-page.component.ts](../frontend/src/app/features/employees/pages/employees-page/employees-page.component.ts), [employees.routes.ts](../frontend/src/app/features/employees/employees.routes.ts).
- Tests identifiés : [EmployeeServiceIntegrationTest](../microservices/employee-service/src/test/java/com/crm/employee/EmployeeServiceIntegrationTest.java), [EmployeeReadSecurityTest](../microservices/employee-service/src/test/java/com/crm/employee/EmployeeReadSecurityTest.java) ; [employee-form.component.spec.ts](../frontend/src/app/features/employees/pages/employee-form/employee-form.component.spec.ts), [employees-page.component.spec.ts](../frontend/src/app/features/employees/pages/employees-page/employees-page.component.spec.ts), [employee-detail.component.spec.ts](../frontend/src/app/features/employees/pages/employee-detail/employee-detail.component.spec.ts).
- Contrat : CRUD /api/employees, GET /{id}/details ; ADMIN/SUPER_ADMIN, résumé personnel séparé via HR.

<a id="k"></a>

**K — Contrats**

- Backend : [EmployeeContract](../microservices/hr-service/src/main/java/com/crm/hr/entity/EmployeeContract.java), [EmployeeContractRepository](../microservices/hr-service/src/main/java/com/crm/hr/repository/EmployeeContractRepository.java), [EmployeeContractService](../microservices/hr-service/src/main/java/com/crm/hr/service/EmployeeContractService.java), [EmployeeContractController](../microservices/hr-service/src/main/java/com/crm/hr/controller/EmployeeContractController.java).
- Angular : [hr-admin.models.ts](../frontend/src/app/features/hr-admin/hr-admin.models.ts), [hr-admin-page.component.ts](../frontend/src/app/features/hr-admin/hr-admin-page.component.ts), [hr-admin.service.ts](../frontend/src/app/features/hr-admin/hr-admin.service.ts).
- Tests identifiés : [EmployeeContractServiceTest](../microservices/hr-service/src/test/java/com/crm/hr/EmployeeContractServiceTest.java) ; [hr-admin-page.component.spec.ts](../frontend/src/app/features/hr-admin/hr-admin-page.component.spec.ts), [hr-admin.service.spec.ts](../frontend/src/app/features/hr-admin/hr-admin.service.spec.ts).
- Contrat : CRUD /api/contracts ; parentContractId pour avenant, admin.

<a id="d"></a>

**D — Dossier et documents**

- Backend : [CareerEvent](../microservices/hr-service/src/main/java/com/crm/hr/entity/CareerEvent.java), [CareerEventRepository](../microservices/hr-service/src/main/java/com/crm/hr/repository/CareerEventRepository.java), [CareerEventService](../microservices/hr-service/src/main/java/com/crm/hr/service/CareerEventService.java), [EmployeeDocument](../microservices/hr-service/src/main/java/com/crm/hr/entity/EmployeeDocument.java), [EmployeeDocumentRepository](../microservices/hr-service/src/main/java/com/crm/hr/repository/EmployeeDocumentRepository.java), [EmployeeDocumentService](../microservices/hr-service/src/main/java/com/crm/hr/service/EmployeeDocumentService.java), [PersonalDocumentService](../microservices/hr-service/src/main/java/com/crm/hr/service/PersonalDocumentService.java), [EmployeeDossierController](../microservices/hr-service/src/main/java/com/crm/hr/controller/EmployeeDossierController.java), [PersonalDocumentController](../microservices/hr-service/src/main/java/com/crm/hr/controller/PersonalDocumentController.java).
- Angular : [employee-dossier.component.ts](../frontend/src/app/features/employees/pages/employee-detail/employee-dossier.component.ts) ; [my-documents.service.ts](../frontend/src/app/features/documents/my-documents.service.ts), [my-documents.component.ts](../frontend/src/app/features/documents/my-documents.component.ts).
- Tests identifiés : [EmployeeDossierServiceTest](../microservices/hr-service/src/test/java/com/crm/hr/EmployeeDossierServiceTest.java), [PersonalDocumentPolicyTest](../microservices/hr-service/src/test/java/com/crm/hr/service/PersonalDocumentPolicyTest.java), [PersonalDocumentSecurityTest](../microservices/hr-service/src/test/java/com/crm/hr/PersonalDocumentSecurityTest.java), [DocumentStoragePolicyTest](../microservices/hr-service/src/test/java/com/crm/hr/service/DocumentStoragePolicyTest.java) ; [employee-dossier.component.spec.ts](../frontend/src/app/features/employees/pages/employee-detail/employee-dossier.component.spec.ts) ; [my-documents.component.spec.ts](../frontend/src/app/features/documents/my-documents.component.spec.ts).
- Contrat : GET/POST /api/employees/{employeeId}/documents et career-events, POST /api/career-events, GET /api/documents/me, téléchargement /me/{id}/content, PATCH /api/documents/{id}/visibility ; admin et propriétaire partagé.

<a id="l"></a>

**L — Congés**

- Backend : [LeaveRequest](../microservices/hr-service/src/main/java/com/crm/hr/entity/LeaveRequest.java), [LeaveRepository](../microservices/hr-service/src/main/java/com/crm/hr/repository/LeaveRepository.java), [LeaveEmployeeAssociation](../microservices/hr-service/src/main/java/com/crm/hr/entity/LeaveEmployeeAssociation.java), [LeaveEmployeeAssociationRepository](../microservices/hr-service/src/main/java/com/crm/hr/repository/LeaveEmployeeAssociationRepository.java), [LeaveServiceImpl](../microservices/hr-service/src/main/java/com/crm/hr/service/impl/LeaveServiceImpl.java), [LeaveAccess](../microservices/hr-service/src/main/java/com/crm/hr/service/LeaveAccess.java), [LeaveController](../microservices/hr-service/src/main/java/com/crm/hr/controller/LeaveController.java), [LeaveQuota](../microservices/hr-service/src/main/java/com/crm/hr/entity/LeaveQuota.java), [LeaveQuotaRepository](../microservices/hr-service/src/main/java/com/crm/hr/repository/LeaveQuotaRepository.java), [LeaveQuotaService](../microservices/hr-service/src/main/java/com/crm/hr/service/LeaveQuotaService.java), [EmployeeAssociationService](../microservices/hr-service/src/main/java/com/crm/hr/service/EmployeeAssociationService.java).
- Angular : [leave.service.ts](../frontend/src/app/features/leaves/leave.service.ts), [leaves-page.component.ts](../frontend/src/app/features/leaves/leaves-page.component.ts), [leave-form.component.ts](../frontend/src/app/features/leaves/leave-form.component.ts), [leave-calendar.component.ts](../frontend/src/app/features/leaves/leave-calendar.component.ts), [leave-calendar.service.ts](../frontend/src/app/features/leaves/leave-calendar.service.ts).
- Tests identifiés : [LeaveServiceIntegrationTest](../microservices/hr-service/src/test/java/com/crm/hr/LeaveServiceIntegrationTest.java), [LeaveSecurityIntegrationTest](../microservices/hr-service/src/test/java/com/crm/hr/LeaveSecurityIntegrationTest.java), [LeavePdfTest](../microservices/hr-service/src/test/java/com/crm/hr/LeavePdfTest.java), [EmployeeAssociationIntegrationTest](../microservices/hr-service/src/test/java/com/crm/hr/EmployeeAssociationIntegrationTest.java) ; [leave-form.component.spec.ts](../frontend/src/app/features/leaves/leave-form.component.spec.ts), [leaves-page.component.spec.ts](../frontend/src/app/features/leaves/leaves-page.component.spec.ts), [leave-calendar.service.spec.ts](../frontend/src/app/features/leaves/leave-calendar.service.spec.ts).
- Contrat : CRUD /api/leaves, /mine, /{id}/approve|reject, /calendar ; quotas /api/leave-quotas ; contrôle LeaveAccess côté service.

<a id="t"></a>

**T — Formations et présence**

- Backend : [Training](../microservices/hr-service/src/main/java/com/crm/hr/entity/Training.java), [TrainingRepository](../microservices/hr-service/src/main/java/com/crm/hr/repository/TrainingRepository.java), [TrainingServiceImpl](../microservices/hr-service/src/main/java/com/crm/hr/service/impl/TrainingServiceImpl.java), [TrainingController](../microservices/hr-service/src/main/java/com/crm/hr/controller/TrainingController.java), [Attendance](../microservices/hr-service/src/main/java/com/crm/hr/entity/Attendance.java), [AttendanceRepository](../microservices/hr-service/src/main/java/com/crm/hr/repository/AttendanceRepository.java), [AttendanceController](../microservices/hr-service/src/main/java/com/crm/hr/controller/AttendanceController.java).
- Angular : [hr-admin.models.ts](../frontend/src/app/features/hr-admin/hr-admin.models.ts), [hr-admin-page.component.html](../frontend/src/app/features/hr-admin/hr-admin-page.component.html), [hr-admin.service.ts](../frontend/src/app/features/hr-admin/hr-admin.service.ts).
- Tests identifiés : [TrainingServiceIntegrationTest](../microservices/hr-service/src/test/java/com/crm/hr/TrainingServiceIntegrationTest.java), [AttendanceServiceIntegrationTest](../microservices/hr-service/src/test/java/com/crm/hr/AttendanceServiceIntegrationTest.java), [HrAdministrativeReadSecurityTest](../microservices/hr-service/src/test/java/com/crm/hr/HrAdministrativeReadSecurityTest.java) ; [hr-admin-page.component.spec.ts](../frontend/src/app/features/hr-admin/hr-admin-page.component.spec.ts).
- Contrat : CRUD /api/trainings, /api/attendances ; un employeeId par enregistrement, API admin.

<a id="h"></a>

**H — Workflows et pilotage RH**

- Backend : [EmployeeEvaluation](../microservices/hr-service/src/main/java/com/crm/hr/entity/EmployeeEvaluation.java), [EmployeeEvaluationRepository](../microservices/hr-service/src/main/java/com/crm/hr/repository/EmployeeEvaluationRepository.java), [JobOffer](../microservices/hr-service/src/main/java/com/crm/hr/entity/JobOffer.java), [JobOfferRepository](../microservices/hr-service/src/main/java/com/crm/hr/repository/JobOfferRepository.java), [JobApplication](../microservices/hr-service/src/main/java/com/crm/hr/entity/JobApplication.java), [JobApplicationRepository](../microservices/hr-service/src/main/java/com/crm/hr/repository/JobApplicationRepository.java), [HrWorkflowService](../microservices/hr-service/src/main/java/com/crm/hr/service/HrWorkflowService.java), [HrWorkflowController](../microservices/hr-service/src/main/java/com/crm/hr/controller/HrWorkflowController.java).
- Angular : [hr-admin.models.ts](../frontend/src/app/features/hr-admin/hr-admin.models.ts), [hr-admin-page.component.ts](../frontend/src/app/features/hr-admin/hr-admin-page.component.ts), [my-evaluations.component.ts](../frontend/src/app/features/hr-admin/my-evaluations.component.ts) ; [dashboard-data.service.ts](../frontend/src/app/features/dashboard/dashboard-data.service.ts), [dashboard.component.html](../frontend/src/app/features/dashboard/dashboard.component.html).
- Tests identifiés : [PersonalEvaluationServiceTest](../microservices/hr-service/src/test/java/com/crm/hr/PersonalEvaluationServiceTest.java), [HrNewEndpointSecurityTest](../microservices/hr-service/src/test/java/com/crm/hr/HrNewEndpointSecurityTest.java) ; [hr-admin-access.spec.ts](../frontend/src/app/features/hr-admin/hr-admin-access.spec.ts), [my-evaluations.component.spec.ts](../frontend/src/app/features/hr-admin/my-evaluations.component.spec.ts) ; [dashboard-data.service.spec.ts](../frontend/src/app/features/dashboard/dashboard-data.service.spec.ts).
- Contrat : CRUD /api/evaluations, /api/job-offers, /api/applications ; /api/evaluations/mine filtré FINALIZED ; agrégats UI en comptes de ressources.

<a id="p"></a>

**P — Paie**

- Backend : [Payroll](../microservices/payroll-service/src/main/java/com/crm/payroll/entity/Payroll.java), [PayrollRepository](../microservices/payroll-service/src/main/java/com/crm/payroll/repository/PayrollRepository.java), [PayrollServiceImpl](../microservices/payroll-service/src/main/java/com/crm/payroll/service/impl/PayrollServiceImpl.java), [FinancialValidation](../microservices/payroll-service/src/main/java/com/crm/payroll/service/FinancialValidation.java), [PayrollRequest](../microservices/payroll-service/src/main/java/com/crm/payroll/dto/PayrollRequest.java), [PayrollController](../microservices/payroll-service/src/main/java/com/crm/payroll/controller/PayrollController.java), [PayrollPdfController](../microservices/payroll-service/src/main/java/com/crm/payroll/controller/PayrollPdfController.java), [PersonalPayrollService](../microservices/payroll-service/src/main/java/com/crm/payroll/service/PersonalPayrollService.java), [PersonalPayrollController](../microservices/payroll-service/src/main/java/com/crm/payroll/controller/PersonalPayrollController.java).
- Angular : [payroll-form.component.ts](../frontend/src/app/features/payroll/payroll-form.component.ts), [payroll.service.ts](../frontend/src/app/features/payroll/payroll.service.ts), [payrolls-page.component.ts](../frontend/src/app/features/payroll/payrolls-page.component.ts), [my-payroll.service.ts](../frontend/src/app/features/payroll/my-payroll.service.ts), [my-payroll-page.component.ts](../frontend/src/app/features/payroll/my-payroll-page.component.ts), [payroll.routes.ts](../frontend/src/app/features/payroll/payroll.routes.ts).
- Tests identifiés : [PayrollFinancialPolicyTest](../microservices/payroll-service/src/test/java/com/crm/payroll/PayrollFinancialPolicyTest.java), [PayrollServiceIntegrationTest](../microservices/payroll-service/src/test/java/com/crm/payroll/PayrollServiceIntegrationTest.java), [PayrollPdfTest](../microservices/payroll-service/src/test/java/com/crm/payroll/PayrollPdfTest.java), [PersonalPayrollIntegrationTest](../microservices/payroll-service/src/test/java/com/crm/payroll/PersonalPayrollIntegrationTest.java) ; [payroll-form.component.spec.ts](../frontend/src/app/features/payroll/payroll-form.component.spec.ts), [my-payroll.service.spec.ts](../frontend/src/app/features/payroll/my-payroll.service.spec.ts), [my-payroll-page.component.spec.ts](../frontend/src/app/features/payroll/my-payroll-page.component.spec.ts).
- Contrat : CRUD /api/payrolls, /{id}/publication, /{id}/pdf ; GET /api/payrolls/me et /me/{id}/pdf ; propriété + published.

<a id="f"></a>

**F — Honoraires**

- Backend : [Fee](../microservices/fees-service/src/main/java/com/crm/fees/entity/Fee.java), [FeeRepository](../microservices/fees-service/src/main/java/com/crm/fees/repository/FeeRepository.java), [FeeRequest](../microservices/fees-service/src/main/java/com/crm/fees/dto/FeeRequest.java), [FeeServiceImpl](../microservices/fees-service/src/main/java/com/crm/fees/service/impl/FeeServiceImpl.java), [FeeController](../microservices/fees-service/src/main/java/com/crm/fees/controller/FeeController.java), [FeeStatisticsController](../microservices/fees-service/src/main/java/com/crm/fees/controller/FeeStatisticsController.java).
- Angular : [fee-form.component.ts](../frontend/src/app/features/fees/fee-form.component.ts), [fee.service.ts](../frontend/src/app/features/fees/fee.service.ts), [fees-page.component.ts](../frontend/src/app/features/fees/fees-page.component.ts), [fee-detail.component.ts](../frontend/src/app/features/fees/fee-detail.component.ts).
- Tests identifiés : [FeesServiceIntegrationTest](../microservices/fees-service/src/test/java/com/crm/fees/FeesServiceIntegrationTest.java), [FeeReadSecurityTest](../microservices/fees-service/src/test/java/com/crm/fees/FeeReadSecurityTest.java) ; [fee-form.component.spec.ts](../frontend/src/app/features/fees/fee-form.component.spec.ts), [fee.service.spec.ts](../frontend/src/app/features/fees/fee.service.spec.ts), [fees-page.component.spec.ts](../frontend/src/app/features/fees/fees-page.component.spec.ts).
- Contrat : CRUD /api/fees, /statistics ; taux × heures, admin ; pas de modèle de grille/timesheet trouvé.

<a id="i"></a>

**I — Factures/paiements**

- Backend : [Invoice](../microservices/invoice-service/src/main/java/com/crm/invoice/entity/Invoice.java), [InvoiceRepository](../microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoiceRepository.java), [InvoicePayment](../microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoicePayment.java), [InvoicePaymentRepository](../microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoicePaymentRepository.java), [InvoiceReminder](../microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoiceReminder.java), [InvoiceReminderRepository](../microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoiceReminderRepository.java), [InvoiceNumberSequence](../microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoiceNumberSequence.java), [InvoiceNumberSequenceRepository](../microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoiceNumberSequenceRepository.java), [InvoiceServiceImpl](../microservices/invoice-service/src/main/java/com/crm/invoice/service/impl/InvoiceServiceImpl.java), [FinancialValidation](../microservices/invoice-service/src/main/java/com/crm/invoice/service/FinancialValidation.java), [InvoiceController](../microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoiceController.java), [InvoicePdfController](../microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoicePdfController.java), [InvoiceStatisticsController](../microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoiceStatisticsController.java), [PersonalInvoiceService](../microservices/invoice-service/src/main/java/com/crm/invoice/service/PersonalInvoiceService.java), [PersonalInvoiceController](../microservices/invoice-service/src/main/java/com/crm/invoice/controller/PersonalInvoiceController.java).
- Angular : [invoice-form.component.ts](../frontend/src/app/features/invoices/invoice-form.component.ts), [invoice-detail.component.ts](../frontend/src/app/features/invoices/invoice-detail.component.ts), [invoice.service.ts](../frontend/src/app/features/invoices/invoice.service.ts), [invoices-page.component.ts](../frontend/src/app/features/invoices/invoices-page.component.ts) ; [client.service.ts](../frontend/src/app/features/client/client.service.ts), [client-workspace.component.ts](../frontend/src/app/features/client/client-workspace.component.ts).
- Tests identifiés :  [InvoiceFinancialPolicyTest](../microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceFinancialPolicyTest.java), [ClientStatisticsContractTest](../microservices/invoice-service/src/test/java/com/crm/invoice/ClientStatisticsContractTest.java), [PersonalPaymentPolicyTest](../microservices/invoice-service/src/test/java/com/crm/invoice/PersonalPaymentPolicyTest.java), [InvoicePdfTest](../microservices/invoice-service/src/test/java/com/crm/invoice/InvoicePdfTest.java), [PersonalInvoiceIntegrationTest](../microservices/invoice-service/src/test/java/com/crm/invoice/PersonalInvoiceIntegrationTest.java) ; [invoice-detail.component.spec.ts](../frontend/src/app/features/invoices/invoice-detail.component.spec.ts), [invoice-form.component.spec.ts](../frontend/src/app/features/invoices/invoice-form.component.spec.ts) ; [client-workspace.component.spec.ts](../frontend/src/app/features/client/client-workspace.component.spec.ts).
- Contrat : CRUD /api/invoices, /{id}/payments, /{id}/reminders, /{id}/pdf, /statistics ; GET /api/invoices/me et /me/{id}/payments, /me/statistics ; CLIENT propre.

<a id="q"></a>

**Q — Devis**

- Backend : [Quote](../microservices/invoice-service/src/main/java/com/crm/invoice/entity/Quote.java), [QuoteLine](../microservices/invoice-service/src/main/java/com/crm/invoice/entity/QuoteLine.java), [QuoteRepository](../microservices/invoice-service/src/main/java/com/crm/invoice/repository/QuoteRepository.java), [QuoteController](../microservices/invoice-service/src/main/java/com/crm/invoice/controller/QuoteController.java).
- Angular : [quote.service.ts](../frontend/src/app/features/invoices/quote.service.ts), [quotes-page.component.ts](../frontend/src/app/features/invoices/quotes-page.component.ts), [quotes-page.component.html](../frontend/src/app/features/invoices/quotes-page.component.html).
- Tests identifiés : [QuoteServiceIntegrationTest](../microservices/invoice-service/src/test/java/com/crm/invoice/QuoteServiceIntegrationTest.java), [QuoteMigrationTest](../microservices/invoice-service/src/test/java/com/crm/invoice/QuoteMigrationTest.java) ; [quote.service.spec.ts](../frontend/src/app/features/invoices/quote.service.spec.ts), [quotes-page.component.spec.ts](../frontend/src/app/features/invoices/quotes-page.component.spec.ts).
- Contrat : logique métier dans QuoteController : CRUD /api/quotes, /{id}/status, /{id}/convert et /{id}/pdf ; ADMIN/SUPER_ADMIN.

<a id="c"></a>

**C — Contacts et opportunités**

- Backend : [Customer](../microservices/customer-service/src/main/java/com/crm/customer/entity/Customer.java), [CustomerRepository](../microservices/customer-service/src/main/java/com/crm/customer/repository/CustomerRepository.java), [CustomerServiceImpl](../microservices/customer-service/src/main/java/com/crm/customer/service/impl/CustomerServiceImpl.java), [CustomerSpecification](../microservices/customer-service/src/main/java/com/crm/customer/util/CustomerSpecification.java), [CustomerController](../microservices/customer-service/src/main/java/com/crm/customer/controller/CustomerController.java), [CustomerInteraction](../microservices/customer-service/src/main/java/com/crm/customer/entity/CustomerInteraction.java), [CustomerInteractionRepository](../microservices/customer-service/src/main/java/com/crm/customer/repository/CustomerInteractionRepository.java), [SalesOpportunity](../microservices/customer-service/src/main/java/com/crm/customer/entity/SalesOpportunity.java), [SalesOpportunityRepository](../microservices/customer-service/src/main/java/com/crm/customer/repository/SalesOpportunityRepository.java), [OpportunityStageHistoryRepository](../microservices/customer-service/src/main/java/com/crm/customer/repository/OpportunityStageHistoryRepository.java), [SalesController](../microservices/customer-service/src/main/java/com/crm/customer/controller/SalesController.java), [CustomerPersonalService](../microservices/customer-service/src/main/java/com/crm/customer/service/CustomerPersonalService.java), [CustomerAssociationRepository](../microservices/customer-service/src/main/java/com/crm/customer/repository/CustomerAssociationRepository.java), [CustomerPersonalController](../microservices/customer-service/src/main/java/com/crm/customer/controller/CustomerPersonalController.java).
- Angular : [customer.service.ts](../frontend/src/app/features/customers/customer.service.ts), [customer-form.component.ts](../frontend/src/app/features/customers/customer-form.component.ts), [customer-detail.component.ts](../frontend/src/app/features/customers/customer-detail.component.ts), [customers-page.component.ts](../frontend/src/app/features/customers/customers-page.component.ts), [opportunity.service.ts](../frontend/src/app/features/customers/opportunity.service.ts), [opportunities-page.component.ts](../frontend/src/app/features/customers/opportunities-page.component.ts) ; [customer-associations.component.ts](../frontend/src/app/features/client/customer-associations.component.ts).
- Tests identifiés : [CustomerServiceIntegrationTest](../microservices/customer-service/src/test/java/com/crm/customer/CustomerServiceIntegrationTest.java), [CustomerAssociationCreationPolicyTest](../microservices/customer-service/src/test/java/com/crm/customer/CustomerAssociationCreationPolicyTest.java), [CustomerPersonalIntegrationTest](../microservices/customer-service/src/test/java/com/crm/customer/CustomerPersonalIntegrationTest.java), [SalesMigrationTest](../microservices/customer-service/src/test/java/com/crm/customer/SalesMigrationTest.java) ; [customer.service.spec.ts](../frontend/src/app/features/customers/customer.service.spec.ts), [customer-detail.component.spec.ts](../frontend/src/app/features/customers/customer-detail.component.spec.ts), [opportunity.service.spec.ts](../frontend/src/app/features/customers/opportunity.service.spec.ts).
- Contrat : CRUD /api/customers, /{id}/convert, /{id}/interactions, /api/opportunities et /{id}/history|stage ; /api/customers/me ; migrations [V20261027_03__sales_activity_and_opportunities.sql](../microservices/customer-service/src/main/resources/db/migration/V20261027_03__sales_activity_and_opportunities.sql) et [V20261028_01__opportunity_forecast_and_stage_history.sql](../microservices/customer-service/src/main/resources/db/migration/V20261028_01__opportunity_forecast_and_stage_history.sql).

<a id="x"></a>

**X — Échanges contacts**

- Backend : [CustomerCsvService](../microservices/customer-service/src/main/java/com/crm/customer/service/CustomerCsvService.java), [CustomerController](../microservices/customer-service/src/main/java/com/crm/customer/controller/CustomerController.java).
- Angular : [customer.service.ts](../frontend/src/app/features/customers/customer.service.ts), [customers-page.component.ts](../frontend/src/app/features/customers/customers-page.component.ts), [customers-page.component.html](../frontend/src/app/features/customers/customers-page.component.html).
- Tests identifiés : [CustomerCsvServiceTest](../microservices/customer-service/src/test/java/com/crm/customer/CustomerCsvServiceTest.java) ; [customer.service.spec.ts](../frontend/src/app/features/customers/customer.service.spec.ts), [customers-page.component.spec.ts](../frontend/src/app/features/customers/customers-page.component.spec.ts).
- Contrat : POST /api/customers/import (CSV), GET /export et /export.xlsx ; aucun endpoint/parser import XLSX identifié.

<a id="a"></a>

**A — Identité et propriétaire**

- Backend : [DevAdminBootstrap](../microservices/identity-service/src/main/java/com/crm/identity/config/DevAdminBootstrap.java), [DataInitializer](../microservices/identity-service/src/main/java/com/crm/identity/config/DataInitializer.java), [User](../microservices/identity-service/src/main/java/com/crm/identity/entity/User.java), [IdentityServiceImpl](../microservices/identity-service/src/main/java/com/crm/identity/service/impl/IdentityServiceImpl.java), [AuthenticationServiceImpl](../microservices/identity-service/src/main/java/com/crm/identity/service/impl/AuthenticationServiceImpl.java), [AuthenticationController](../microservices/identity-service/src/main/java/com/crm/identity/controller/AuthenticationController.java), [UserController](../microservices/identity-service/src/main/java/com/crm/identity/controller/UserController.java), [AccessTokenValidationService](../microservices/identity-service/src/main/java/com/crm/identity/security/AccessTokenValidationService.java) ; [EmployeeAssociationService](../microservices/hr-service/src/main/java/com/crm/hr/service/EmployeeAssociationService.java), [EmployeeProfileService](../microservices/hr-service/src/main/java/com/crm/hr/service/EmployeeProfileService.java), [EmployeeProfileClient](../microservices/hr-service/src/main/java/com/crm/hr/client/EmployeeProfileClient.java) ; [microservices/common-security/src/main/java/com/crm/security/JwtAuthenticationFilter.java](../microservices/common-security/src/main/java/com/crm/security/JwtAuthenticationFilter.java).
- Angular : [auth.service.ts](../frontend/src/app/core/services/auth.service.ts), [auth.guard.ts](../frontend/src/app/core/guards/auth.guard.ts), [role.guard.ts](../frontend/src/app/core/guards/role.guard.ts).
- Tests identifiés : [SessionLifecyclePolicyTest](../microservices/identity-service/src/test/java/com/crm/identity/SessionLifecyclePolicyTest.java), [UsableSuperAdminPolicyTest](../microservices/identity-service/src/test/java/com/crm/identity/UsableSuperAdminPolicyTest.java), [DevAdminBootstrapTest](../microservices/identity-service/src/test/java/com/crm/identity/config/DevAdminBootstrapTest.java) ; [auth.service.spec.ts](../frontend/src/app/core/services/auth.service.spec.ts).
- Contrat : POST /api/auth/login|refresh|logout ; /api/users/{id}/roles|status ; sid/UID et révocations ; bootstrap local/dev uniquement.

<a id="n"></a>

**N — Routes, menus et RBAC**

- Backend : [SecurityConfig](../microservices/employee-service/src/main/java/com/crm/employee/config/SecurityConfig.java); [SecurityConfig](../microservices/hr-service/src/main/java/com/crm/hr/config/SecurityConfig.java); [SecurityConfig](../microservices/payroll-service/src/main/java/com/crm/payroll/config/SecurityConfig.java); [SecurityConfig](../microservices/fees-service/src/main/java/com/crm/fees/config/SecurityConfig.java); [SecurityConfig](../microservices/invoice-service/src/main/java/com/crm/invoice/config/SecurityConfig.java); [SecurityConfig](../microservices/customer-service/src/main/java/com/crm/customer/config/SecurityConfig.java); [SecurityConfig](../microservices/identity-service/src/main/java/com/crm/identity/config/SecurityConfig.java).
- Angular : [frontend/src/app/app.routes.ts](../frontend/src/app/app.routes.ts) ; [crm-navigation.ts](../frontend/src/app/core/navigation/crm-navigation.ts) ; [dashboard-data.service.ts](../frontend/src/app/features/dashboard/dashboard-data.service.ts).
- Tests identifiés : [frontend/src/app/app.routes.spec.ts](../frontend/src/app/app.routes.spec.ts) ; [crm-navigation.spec.ts](../frontend/src/app/core/navigation/crm-navigation.spec.ts) ; [client-access.spec.ts](../frontend/src/app/features/client/client-access.spec.ts).
- Contrat : CRUD admin, lectures personnelles rôle + propriété ; pas de route EMPLOYEE contacts assignés ou CLIENT projets/communication.

<a id="u"></a>

**U — Socle et livraison**

- Backend : [Jenkinsfile](../Jenkinsfile) ; [microservices/identity-service/pom.xml](../microservices/identity-service/pom.xml) ; [docker-compose.yml](../docker-compose.yml) ; [scripts/validation/prepare-progressive.mjs](../scripts/validation/prepare-progressive.mjs).
- Angular : [frontend/package.json](../frontend/package.json) ; [frontend/angular.json](../frontend/angular.json).
- Tests identifiés : [scripts/validation/progressive.test.mjs](../scripts/validation/progressive.test.mjs) ; rapports 1A/1B/1C et blocage 2B.
- Contrat : Boot 3.3.6 vs 4.x CDC, migrations préparées, HTTPS interne optionnel ; aucune preuve de déploiement ou sauvegarde quotidienne effective.


## 3. Matrice de conformité complète

### Six modules — 63 exigences

Le détail des six modules provient des listes de la mission et des pages 6–7 du CDC. Les sous-items formations/évaluations/communications affinent les exigences génériques ; ils ne sont pas des fonctions supplémentaires imposées arbitrairement.

| ID | Module | Exigence CDC | Backend | Angular | Tests | Statut | Travail restant | Priorité |
|---|---|---|---|---|---|---|---|---|
| EMP-01 | Employés | CRUD complet (CDC p.6) | CRUD Employee [E](#e) | Liste/formulaire/détail/routes [E](#e) | Java intégration/sécurité, specs CRUD ; I:NV | COMPLETE | Intégration et suppressions avec références | Haute |
| EMP-02 | Employés | Profil détaillé personnel/coordonnées/poste/département | Employee + Department ; résumé HR propre [E](#e)[A](#a) | Détail admin et /profile [E](#e)[N](#n) | EmployeeReadSecurity, profil Java/Angular ; I:NV | COMPLETE | Prouver le canal HTTPS et parcours connecté | Haute |
| EMP-03 | Employés | Contrats et avenants | Entité parent, dates, cycles, CRUD [K](#k) | Écran contracts générique utilisable [K](#k) | EmployeeContractServiceTest + specs génériques ; I:NV | COMPLETE | Valider règles métier et intégration | Moyenne |
| EMP-04 | Employés | Historique postes/promotions | Journal CareerEvent + dépôt/service [D](#d) | Ajout/lecture/suppression dans dossier [D](#d) | Java carrière présent ; pas de test Angular ciblé carrière identifié ; I:NV | NON VALIDÉE | Tester saisie et consultation carrière de bout en bout | Moyenne |
| EMP-05 | Employés | Documents CV/diplômes/attestations | Stockage privé, partage/propriété [D](#d) | Dossier admin + /my-documents [D](#d) | Dossier, stockage, policy, security, specs partage ; I:NV | COMPLETE | MySQL, fichiers réels, retrait et TLS selon parcours | Haute |
| EMP-06 | Employés | Recherche et filtres avancés | Keyword code/nom/e-mail ; tri/page [E](#e) | Recherche simple ; pas de filtre département/poste [E](#e) | Specs liste, tests service basiques ; I:NV | PARTIELLE | Filtres combinés définis et couverts API/UI | Moyenne |
| RH-01 | RH | Demandes de congés/absences | LeaveRequest, propriété, mine [L](#l) | Formulaire et liste personnels [L](#l) | LeaveServiceIntegration, LeaveSecurity, specs ; I:NV | COMPLETE | Intégration et états sans association | Haute |
| RH-02 | RH | Workflow de validation | Approbation/refus et conflits/chevauchements [L](#l) | Actions admin et statuts [L](#l) | Tests transitions/service et composants ; I:NV | COMPLETE | Concurrence MySQL et rollback | Haute |
| RH-03 | RH | Calendrier des absences | Requête de recouvrement et filtres [L](#l) | Calendrier admin mensuel présent [L](#l) | Service calendrier testé ; spec composant calendrier absente ; I:NV | NON VALIDÉE | Tester mois/bornes/filtres/rendu connecté | Moyenne |
| RH-04 | RH | Soldes en temps réel | Attribution LeaveQuota seulement [L](#l) | Affiche quotas, annonce pas de solde [L](#l) | Test quota vérifie absence de calcul de solde ; I:NV | PARTIELLE | Acquisition/consommation/report selon règles validées | Haute |
| RH-05 | RH | Gestion des formations | Training CRUD, dates et statuts [T](#t) | Module trainings avec formulaires [T](#t) | TrainingServiceIntegration + specs HR ; I:NV | COMPLETE | Intégration et référence employé | Moyenne |
| RH-06 | RH | Affectation aux formations | employeeId persistant ; pas de vérification distante d'existence [T](#t) | Référence saisie libre [T](#t) | Tests CRUD ; référence invalide à compléter ; I:NV | PARTIELLE | Valider entité affectée et UX de sélection | Haute |
| RH-07 | RH | Suivi des formations | Statuts et certificat réservé à COMPLETED [T](#t) | Statut/certificat visibles et éditables selon règles [T](#t) | Tests métier et formulaire générique ; I:NV | COMPLETE | Rejouer parcours de suivi réel | Moyenne |
| RH-08 | RH | Évaluations de performance | Période, salarié, finalisation et lecture propre [H](#h) | Admin + my-evaluations [H](#h) | PersonalEvaluationService et specs propres ; I:NV | COMPLETE | Intégration finalisé/privé/cross-account | Haute |
| RH-09 | RH | Critères/résultats d'évaluation | Critères et appréciations qualitatifs [H](#h) | Champs texte réels [H](#h) | Tests lecture/finalisation + specs HR ; I:NV | COMPLETE | Faire accepter le format qualitatif ; pas de score inventé | Moyenne |
| RH-10 | RH | Gestion du recrutement | Offres OPEN/CLOSED et CRUD [H](#h) | job-offers admin [H](#h) | Sécurité HR + specs génériques ; I:NV | COMPLETE | Parcours réel offre/candidature | Moyenne |
| RH-11 | RH | Suivi des candidatures | Lien offre, étapes et protection offre liée [H](#h) | applications admin [H](#h) | Tests endpoints/sécurité et specs HR ; I:NV | COMPLETE | Intégration et références cohérentes | Moyenne |
| RH-12 | RH | Indicateurs RH | Comptes de ressources, pas KPI RH spécialisés [H](#h) | Cartes réelles dashboard [H](#h) | DashboardDataService specs ; I:NV | PARTIELLE | Définir KPI clés et exposer agrégats | Moyenne |
| RH-13 | RH | Statistiques RH | Absence/formation/recrutement sans analyse temporelle dédiée [H](#h) | Compteurs, pas statistiques RH complètes [H](#h) | Contrats de compteurs seulement ; I:NV | PARTIELLE | Statistiques définies et réconciliables | Moyenne |
| PAY-01 | Paie | Calcul automatisé des salaires | Net BigDecimal depuis montants saisis [P](#p) | Calcul/validation formulaire cohérents [P](#p) | PayrollFinancialPolicy, service, specs ; I:NV | PARTIELLE | Automatisation règles variables/taxes contractualisées | Haute |
| PAY-02 | Paie | Gestion des primes | bonuses validées et incluses dans net [P](#p) | Champ/formulaire/détail [P](#p) | Financier et formulaire ; I:NV | COMPLETE | Intégration et règles de prime à préciser si besoin | Haute |
| PAY-03 | Paie | Heures supplémentaires | Aucun modèle/règle identifié [P](#p) | Aucun champ/workflow [P](#p) | Aucun ciblé identifié ; I:— | ABSENTE | Temps, taux et majorations selon règles métier | Haute |
| PAY-04 | Paie | Déductions | deductions/taxAmount validés, net borné [P](#p) | Saisie et affichage [P](#p) | Financier et formulaire ; I:NV | COMPLETE | Intégration ; pas de calcul légal automatique revendiqué | Haute |
| PAY-05 | Paie | Bulletins PDF | PDF admin et personnel publié [P](#p) | Actions de téléchargement [P](#p) | PayrollPdf, PersonalPayroll, specs ; I:NV | COMPLETE | Contenu et propriété via vrai Gateway | Haute |
| PAY-06 | Paie | Historique des paiements | PAID/date et liste de paies, pas ledger de règlements [P](#p) | Historique bulletins, pas règlements détaillés [P](#p) | Tests liste/statuts ; pas ledger ; I:NV | PARTIELLE | Enregistrer/consulter règlements et corrections | Haute |
| PAY-07 | Paie | Export comptable standard | Aucun export identifié [P](#p) | Aucune action [P](#p) | Aucun ciblé ; I:— | ABSENTE | Format cible, mapping et réconciliation | Haute |
| PAY-08 | Paie | Déclarations sociales | Aucun moteur/document identifié [P](#p) | Aucun écran [P](#p) | Aucun ciblé ; I:— | ABSENTE | Juridiction, règles, format et validation métier | Haute |
| PAY-09 | Paie | Déclarations fiscales | Taxe saisie ne vaut pas déclaration [P](#p) | Aucun workflow déclaratif [P](#p) | Aucun ciblé ; I:— | ABSENTE | Règles légales explicites et recette dédiée | Haute |
| FEE-01 | Honoraires | Enregistrement des prestations | Fee CRUD daté et références [F](#f) | CRUD complet accessible admin [F](#f) | FeesServiceIntegration + specs ; I:NV | COMPLETE | Intégration et bornes monétaires FEE-05 | Haute |
| FEE-02 | Honoraires | Enregistrement des services | serviceName/description persistés [F](#f) | Saisie et consultation [F](#f) | CRUD et formulaire ; I:NV | COMPLETE | Ne pas inventer un catalogue obligatoire distinct | Moyenne |
| FEE-03 | Honoraires | Grilles tarifaires personnalisables | Aucune entité/règle de grille [F](#f) | Aucun écran [F](#f) | Aucun ciblé ; I:— | ABSENTE | Tarifs, validité, personnalisation et traçabilité | Haute |
| FEE-04 | Honoraires | Timesheet | Heures agrégées + date seulement [F](#f) | Champ d'heures, pas feuille de temps [F](#f) | CRUD uniquement ; I:NV | PARTIELLE | Entrées détaillées et suivi de période | Haute |
| FEE-05 | Honoraires | Calcul automatique des honoraires | Multiplication, validation DECIMAL incomplète AN-01 [F](#f) | Taux/heures sans bornes équivalentes [F](#f) | Calcul nominal, limites insuffisantes ; I:NV | PARTIELLE | Contrat monétaire et calcul borné HTTP/service | Haute |
| FEE-06 | Honoraires | Rapports de facturation | Statistiques/listes, pas rapport dédié [F](#f) | Liste et dashboard, pas édition de rapport [F](#f) | Statistiques/CRUD ; rapport non testé ; I:NV | PARTIELLE | Définir produire et tester rapport facturable | Moyenne |
| INV-01 | Factures | Création | Montants validés, total calculé [I](#i) | Formulaire réel [I](#i) | Financier et specs ; I:NV | COMPLETE | MySQL et scénario création | Haute |
| INV-02 | Factures | Modification | Verrou, refus paiement/lignes/historique [I](#i) | Statuts autorisés et erreurs de conflit [I](#i) | InvoiceFinancialPolicy + specs ; I:NV | COMPLETE | Concurrence réelle, rollback, conservation | Haute |
| INV-03 | Factures | Modèles personnalisables | PDF fixe, aucun modèle configurable [I](#i) | Aucun éditeur/sélecteur [I](#i) | PDF fixe seulement ; I:— | ABSENTE | Modèles sûrs et sélection/rendu | Haute |
| INV-04 | Factures | Numérotation automatique | Séquence verrouillée et numéro généré [I](#i) | Numéro retourné affiché [I](#i) | Tests service existants ; MySQL non exécuté ; I:NV | COMPLETE | Concurrence séquence et migration INT réelle | Haute |
| INV-05 | Factures | Gestion des devis | Lignes, statuts, conversion et PDF [Q](#q) | Page devis et conversion [Q](#q) | QuoteServiceIntegration + specs ; I:NV | COMPLETE | Intégration conversion sans double facture | Haute |
| INV-06 | Factures | Suivi des paiements | Enregistrements partiels/complets et solde [I](#i) | Admin et lecture CLIENT [I](#i) | Financier, PersonalPayment, specs ; I:NV | COMPLETE | Ledger réel, propriété et précision SQL | Haute |
| INV-07 | Factures | Gestion des relances | Journal canal/date/notes/auteur [I](#i) | Ajout/lecture du journal [I](#i) | Tests règles facture ; relance complète à qualifier ; I:NV | PARTIELLE | Faire accepter le suivi manuel ou compléter l'action de relance | Moyenne |
| INV-08 | Factures | PDF | Génération fixe admin/CLIENT [I](#i) | Téléchargement protégé [I](#i) | InvoicePdf/PersonalInvoice + specs ; I:NV | COMPLETE | Vrai Gateway et contenu exporté | Haute |
| INV-09 | Factures | Historique | Factures et paiements/relances conservés [I](#i) | Listes/détails persistants [I](#i) | Tests conservation et interfaces ; I:NV | COMPLETE | Pas de versionnement intégral revendiqué ; intégration | Moyenne |
| INV-10 | Factures | Archivage | Aucun état/action d'archivage identifié [I](#i) | Aucun parcours archive [I](#i) | Aucun ciblé ; I:— | ABSENTE | Archivage et consultation sans effacement | Haute |
| INV-11 | Factures | Statistiques | Agrégats admin et contrat CLIENT par devise [I](#i) | Dashboard réel, contrat validé [N](#n) | ClientStatisticsContract et Dashboard specs ; I:NV | COMPLETE | Agrégats SQL réels et données historiques | Haute |
| CON-01 | Contacts | CRUD contacts | CRUD Customer mais suppression non protégée AN-02 [C](#c) | CRUD complet visible [C](#c) | CustomerServiceIntegration + specs ; I:NV | PARTIELLE | Préserver interactions/opportunités à la suppression | Haute |
| CON-02 | Contacts | Clients | Type CUSTOMER et association CLIENT explicite [C](#c) | Fiche/association et espace propre [C](#c)[N](#n) | CustomerPersonal, policy et specs ; I:NV | COMPLETE | Intégration association/propriété | Haute |
| CON-03 | Contacts | Prospects | Type LEAD, conversion contrôlée [C](#c) | Filtre/création/conversion [C](#c) | CRUD/conversion et specs ; I:NV | COMPLETE | Parcours connecté et historique de conversion | Moyenne |
| CON-04 | Contacts | Fournisseurs | Type SUPPLIER distinct [C](#c) | Sélection/fiche/filtre [C](#c) | Tests types et formulaire ; I:NV | COMPLETE | Pas de rôle JWT fournisseur inventé | Moyenne |
| CON-05 | Contacts | Fiches détaillées | Coordonnées/type/affectation [C](#c) | Détail et interactions [C](#c) | Détail et service specs ; I:NV | COMPLETE | Intégration et confidentialité | Moyenne |
| CON-06 | Contacts | Historique des interactions | Table/repository EMAIL/CALL/MEETING/NOTE [C](#c) | Journal manuel réel [C](#c) | CustomerServiceIntegration et détail specs ; I:NV | COMPLETE | Intégrité suppression AN-02 et intégration | Haute |
| CON-07 | Contacts | Segmentation | Valeur segment libre, filtre [C](#c) | Champ et filtre [C](#c) | Tests contrat/filtres ; I:NV | COMPLETE | Pas de segmentation calculée exigée sans spécification | Moyenne |
| CON-08 | Contacts | Catégorisation | category libre persistée, filtre [C](#c) | Champ/filtre [C](#c) | Tests CRUD/filtres ; I:NV | COMPLETE | Référentiel administrable uniquement si contractualisé | Moyenne |
| CON-09 | Contacts | Opportunités commerciales | Étapes, montant/prévision, historique [C](#c) | Pipeline et édition [C](#c) | SalesMigration, intégration et Opportunity specs ; I:NV | COMPLETE | Intégration et référence contact conservée | Haute |
| CON-10 | Contacts | Suivi emails | Interaction EMAIL manuelle [C](#c) | Journal et saisie [C](#c) | Interactions/détail specs ; I:NV | COMPLETE | Suivi manuel ; pas de synchronisation mail revendiquée | Moyenne |
| CON-11 | Contacts | Suivi appels | Interaction CALL manuelle [C](#c) | Journal et saisie [C](#c) | Interactions/détail specs ; I:NV | COMPLETE | Recette du suivi, pas de téléphonie imposée | Moyenne |
| CON-12 | Contacts | Suivi réunions | Interaction MEETING datée [C](#c) | Journal et saisie [C](#c) | Interactions/détail specs ; I:NV | COMPLETE | Pas de calendrier externe imposé | Moyenne |
| CON-13 | Contacts | Import CSV | UTF-8/colonnes/rapport par ligne [X](#x) | Fichier CSV et rapport [X](#x) | CustomerCsvServiceTest + specs ; I:NV | COMPLETE | Intégration transactionnelle et doublons | Haute |
| CON-14 | Contacts | Export CSV | Filtres et cellules protégées [X](#x) | Téléchargement CSV [X](#x) | Tests export et service ; I:NV | COMPLETE | Vérifier fichier dans logiciel cible | Moyenne |
| CON-15 | Contacts | Import Excel | Aucun parseur/endpoint XLSX identifié [X](#x) | Sélection limitée CSV [X](#x) | Aucun import XLSX ciblé ; I:— | ABSENTE | Import XLSX borné, validé, rapport de rejet | Haute |
| CON-16 | Contacts | Export Excel | Archive XLSX générée [X](#x) | Bouton téléchargement XLSX [X](#x) | CustomerCsvServiceTest + specs ; I:NV | COMPLETE | Compatibilité réelle Excel/LibreOffice | Moyenne |
| CON-17 | Contacts | Recherche avancée | Keyword multi-champs et filtres [C](#c) | Recherche présente ; affectation non exposée [C](#c) | Tests keyword/page/filtres ; I:NV | PARTIELLE | Recherche combinée complète selon contrat et AN-03 | Moyenne |
| CON-18 | Contacts | Filtres | Type/category/segment/assignedEmployeeId [C](#c) | Trois filtres, pas assignedEmployeeId liste [C](#c) | Specs trois filtres, pas du chaînage manquant ; I:NV | PARTIELLE | Exposer le filtre existant et tester backend/UI | Moyenne |

### Exigences transversales et espaces — hors totaux des six modules

Ces exigences proviennent notamment des pages 4, 5, 8 et 9 du CDC. Elles sont comptées une seule fois ici, sans recompter les factures/congés/bulletins dans le récapitulatif métier.

| ID | Module | Exigence CDC | Backend | Angular | Tests | Statut | Travail restant | Priorité |
|---|---|---|---|---|---|---|---|---|
| TR-01 | Transverse | Bootstrap SUPER_ADMIN au premier lancement (p.9) | Bootstrap local/dev opt-in [A](#a) | Pas de provisionnement production guidé [A](#a) | DevAdminBootstrapTest ; I:NV | PARTIELLE | Provisionnement conforme et sécurisé sans secret par défaut | Haute |
| TR-02 | Transverse | Changement initial obligatoire du mot de passe | Aucun contrat/flag/endpoint de première connexion identifié [A](#a) | Pas de parcours imposé [A](#a) | Aucun ciblé ; I:— | ABSENTE | Onboarding sécurisé et invalidation des sessions | Haute |
| TR-03 | Transverse | Paramètres système globaux SUPER_ADMIN | Aucune API/config métier administrable [N](#n) | Aucun écran [N](#n) | Aucun ciblé ; I:— | ABSENTE | Définir paramètres et droits, tracer modifications | Moyenne |
| TR-04 | Transverse | Logs et monitoring SUPER_ADMIN | Actuator santé/info et logs techniques [U](#u) | Pas d'espace dédié [N](#n) | Pas de parcours monitoring identifié ; I:NV | PARTIELLE | Accès administré et preuves de supervision | Moyenne |
| TR-05 | Transverse | EMPLOYEE gère ses contacts assignés (p.5) | Champ assignedEmployeeId mais Customer admin seulement [C](#c)[N](#n) | Aucun menu/parcours propre [N](#n) | Pas de scénario employé assigné ; I:— | ABSENTE | API propriétaire et parcours dédié | Haute |
| TR-06 | Transverse | CLIENT suit demandes/projets (p.5) | Aucun modèle/workflow identifié [C](#c) | Espace limité profil/factures [N](#n) | Aucun ciblé ; I:— | ABSENTE | Définir demandes/projets puis les réaliser | Haute |
| TR-07 | Transverse | CLIENT communique avec équipe (p.5) | Journal admin ne vaut pas canal client [C](#c) | Aucune conversation/demande [N](#n) | Aucun ciblé ; I:— | ABSENTE | Parcours client-équipe sûr et traçable | Haute |
| TR-08 | Transverse | Internationalisation (p.8) | Pas de contrat de locale métier complet [U](#u) | Target extract-i18n, pas de catalogues/parcours multilingue identifié [U](#u) | Aucun test de locale ciblé ; I:— | ABSENTE | Langues, catalogues et recette UI | Moyenne |
| TR-09 | Transverse | Audit trail des actions critiques (p.8) | Historiques commerciaux partiels, pas audit identité/finance global [A](#a)[C](#c) | Pas de consultation d'audit critique [N](#n) | Historiques locaux seulement ; I:NV | PARTIELLE | Acteur/date/action/résultat protégés, sans secrets | Haute |
| TR-10 | Transverse | Sauvegarde automatique quotidienne (p.8) | Aucune planification/restauration prouvée dans dépôt [U](#u) | Sans objet | Aucune restauration exécutée ; I:— | ABSENTE | Sauvegarde et restauration contrôlées | Haute |
| TR-11 | Transverse | Stack Java/Boot/Angular/MySQL du CDC (p.4) | Java21/Boot3.3.6, pas Boot4.x [U](#u) | Angular20 [U](#u) | Builds historiques, aucune migration Boot4 validée ; I:NV | PARTIELLE | Arbitrer explicitement l'écart Boot4.x et compatibilités | Moyenne |
| TR-12 | Transverse | HTTPS obligatoire en production (p.8) | Canal interne dédié préparé ; pas preuve TLS externe production [A](#a)[U](#u) | URLs locales HTTP, pas déploiement certifié [U](#u) | Tests TLS présents, pas de handshake intégré ; I:NV | PARTIELLE | Terminaison externe et déploiement vérifiés | Haute |
| TR-13 | Transverse | Validation intégrée des parcours | Préparation progressive, dernier moteur inaccessible [U](#u) | Navigateur connecté non exécuté [U](#u) | Ciblés historiques, intégration non réalisée ; I:NV | BLOQUÉE | Précontrôles positifs, autorisation, campagne progressive puis globale | Haute |
| TR-14 | Transverse | Chiffrement données sensibles (p.8) | Mots de passe hachés, clés externes, pas preuve chiffrement stockage métier [A](#a)[U](#u) | Pas de secret sensible à exposer [A](#a) | Sécurité ciblée ; stockage non certifié ; I:NV | PARTIELLE | Préciser périmètre chiffrement, ACL et preuves | Haute |
| TR-15 | Transverse | JWT/refresh/RBAC et propriété (p.8) | UID/sid, révocation, validation Identity et contrôles propriétaires [A](#a)[N](#n) | Auth/role guards et nettoyage session [A](#a) | Tests 1B/1C pertinents, mocks et unités ; I:NV | COMPLETE | Confirmer concurrence SQL et tous services réels | Haute |
| TR-16 | Transverse | Responsive, lazy loading et UI modulaire (p.8) | API séparées [U](#u) | Material, routes lazy, styles responsive [N](#n) | Specs/routes ; audit mobile/clavier insuffisant ; I:NV | NON VALIDÉE | Tester navigation aux tailles cibles et accessibilité | Moyenne |
| TR-17 | Transverse | CI, documentation et livrables (p.9) | Jenkins et docs techniques présents [U](#u) | Tests Jasmine/Karma et build configurés [U](#u) | Pipeline non exécuté sur état local courant ; I:NV | PARTIELLE | Guide utilisateur, candidat complet, pipeline et installation démontrés | Moyenne |

Décompte calculé à partir des lignes : **63 métier + 17 transversales = 80 lignes distinctes**.

| Statut | Métier | Transverse | Ensemble |
|---|---:|---:|---:|
| COMPLETE | 39 | 1 | 40 |
| PARTIELLE | 14 | 7 | 21 |
| ABSENTE | 8 | 7 | 15 |
| NON VALIDÉE | 2 | 1 | 3 |
| BLOQUÉE | 0 | 1 | 1 |

## 4. Module Employés

Le CRUD est présent dans Employee ; les contrats, carrière et documents sont portés par HR. Ce découpage n'est pas une absence fonctionnelle. Le détail Angular combine la fiche, le département et le dossier administratif ; `/profile` utilise une association UID et un résumé HTTPS limité, sans ouvrir le CRUD global à EMPLOYEE.

Les contrats ont un parent d'avenant, contrôles d'appartenance au même employé, dates et cycles ; la suppression d'un contrat parent est protégée. Le journal de carrière enregistre poste/département/promotion mais ne synchronise pas automatiquement le poste courant : le CDC n'impose pas cette automatisation, à préciser comme décision métier plutôt que comme absence du journal. Les documents CV/DIPLOMA/CERTIFICATE/OTHER sont stockés hors web, bornés et partagés explicitement. Aucune signature électronique ou gestion de versions documentaires n'est déclarée obligatoire sans spécification.

Écart net : la recherche Employee est essentiellement un mot-clé sur code, prénom, nom et e-mail avec tri/pagination ; pas de filtres combinés de département/poste/période dans l'écran. Les tests spécifiques du nouveau partage ne prouvent pas tout le journal carrière en navigateur. Les références interservices et suppressions avec historique demandent une campagne intégrée.

## 5. Module RH

Congés : création personnelle, contrôles de propriété, transitions d'approbation/refus, chevauchement et PDF présents. Le calendrier admin avec filtres existe ; il ne faut plus le classer « Angular absent ». En revanche, un test de service calendrier n'est pas une preuve de rendu/comportement du composant mensuel.

Le registre des quotas stocke des jours attribués ; le template précise lui-même qu'aucun solde consommé/restant n'est calculé. Aucun algorithme de jours ouvrés, acquisition, report, jours fériés ou régularisation n'a été identifié : le solde temps réel est partiel, pas terminé par la présence de `grantedDays`.

Les formulaires génériques HR sont réellement utilisables pour formations, présences, contrats, évaluations, offres et candidatures : liste paginée, détail, création/édition/suppression, erreurs, chargement et contrôle admin. Une formation porte un employé et des statuts, avec certificat sur achèvement ; ce n'est pas une gestion de sessions multi-participants, que le CDC ne détaille pas. L'affectation repose cependant sur une référence libre et n'a pas le contrôle d'existence employé des nouveaux contrats/documents.

Les évaluations ont des critères/appréciations textuels, périodes et finalisation ; le salarié voit ses seules évaluations finalisées. Aucun barème numérique n'est imposé par le CDC : le mode qualitatif n'est pas automatiquement absent. Offres/candidatures et états existent ; aucun portail candidat externe ou ATS complexe n'est revendiqué. Les indicateurs RH sont surtout des comptes de ressources, insuffisants pour qualifier un pilotage RH complet sans définition des KPI attendus.

## 6. Module Paie

Calcul actuel : `baseSalary + bonuses - deductions - taxAmount`, avec BigDecimal, validation HTTP **et service**, DECIMAL(12,2), net non négatif et période contrôlée. Les taxes et retenues restent des montants saisis, pas un moteur de règles légales. Les primes et déductions sont opérationnelles dans ce périmètre. Aucun calcul distinct d'heures supplémentaires, export comptable ou déclaration sociale/fiscale identifié.

Les PDF admin et personnels existent ; `PersonalPayrollService` filtre par employé authentifié et `published=true`. Le statut PAID est une déclaration manuelle, sans écritures de paiement, référence bancaire ou rapprochement. Liste de paies/périodes/date de paiement n'est donc pas un historique de règlements complet.

Un bulletin publié reste éditable/supprimable par le CRUD administratif : comportement démontré, mais l'immutabilité après publication est une décision de finalisation, pas une exigence explicite à attribuer rétroactivement au Lot 1A. Il faut définir correction/versionnement avant de considérer la paie publiable en production.

## 7. Module Honoraires

`Fee` est une prestation datée avec salarié/client, nom/description, taux horaire, heures et montant. Le CRUD et les écrans existent. Le calcul est une multiplication ; aucun catalogue/grille tarifaire versionné ou personnalisé n'est identifié. Des heures agrégées dans un enregistrement ne constituent pas une timesheet avec entrées détaillées et suivi de période.

Défaut établi : `FeeRequest` vérifie la positivité mais pas `@Digits` ni la borne du résultat ; le service substitue zéro aux nulls lors d'un appel direct et multiplie sans validation financière adaptée, tandis que taux/montant sont DECIMAL(12,2). Des valeurs positives acceptées par le DTO peuvent dépasser ce contrat. L'issue précise MySQL (rejet/arrondi) n'a pas été exécutée ; le manque de contrôle préalable est prouvé dans le code. Le Lot 1A couvrait Invoice/Payroll, pas Fees.

Les statistiques et la liste ne constituent pas un rapport de facturation dédié prêt à exporter ; aucun format arbitraire n'est imposé, mais le livrable de rapport reste à définir et réaliser.

## 8. Module Factures

Création, édition autorisée avant règlement, numérotation par séquence verrouillée, devis avec lignes/statuts/conversion, PDF et statistiques sont présents. Les numéros sont générés côté service ; le schéma de séquence a sa migration corrective additive déjà préparée. Les tests Java/Angular existants sont des preuves de code et de règles, pas de concurrence MySQL effective.

Les protections Lot 1A sont conservées : CRUD ne force pas PAID/PARTIALLY_PAID ; tout paiement bloque édition/suppression, y compris statut historique incohérent ; paiement, édition/suppression et relance partagent le verrou ; conservation des lignes et de l'historique. Les paiements sont un suivi manuel, ce qui peut satisfaire le CDC « suivi » sans processeur bancaire : aucun nouvel objectif de paiement en ligne n'est inventé.

Relances : journal canal/date/notes/auteur et formulaire présents ; pas d'action de communication vers un destinataire ni échéancier. La couverture est limitée à la consignation d'une relance effectuée ailleurs ; faire accepter explicitement ce périmètre ou compléter le parcours de relance. L'envoi automatique n'est pas présenté comme une obligation littérale du CDC.

Les PDF sont des documents fixes, pas des modèles personnalisables. Historique = factures consultables et paiements/relances conservés, pas un audit versionné de toutes les écritures. Aucune fonction d'archivage métier trouvée. Les statistiques CLIENT utilisent bien le contrat Lot 1A ; les réponses invalides ne sont pas remplacées silencieusement par zéro.

## 9. Module Contacts

Customer représente le contact métier avec `contactType` CUSTOMER/LEAD/SUPPLIER ; ces types ne doivent pas être assimilés au rôle JWT CLIENT. Un rôle CLIENT est associé explicitement à une fiche. Le nom de classe Customer ne suffit donc pas à conclure que prospects et fournisseurs sont absents.

Fiches, journal d'interactions EMAIL/CALL/MEETING/NOTE, catégories/segments libres, conversion LEAD→CUSTOMER, opportunités avec étapes/prévisions/historique et exports CSV/XLSX existent. Le suivi des communications est manuel, pas une synchronisation de boîtes mail. Cette limitation doit être visible, sans inventer une obligation SMTP/IMAP dans le CDC.

L'import est uniquement CSV : sélection/validation `.csv`, décodage UTF-8, colonnes contrôlées et rapport par ligne ; un export XLSX n'est pas un import Excel. Les exports protègent les valeurs de cellules contre les formules ; aucune intégration comptable n'en découle. Les catégories/segments ne sont pas des règles dynamiques d'audience.

Deux écarts : le filtrage backend `assignedEmployeeId` n'est pas transmis par la liste Angular Customer ; la suppression d'un contact sans association protégée ne contrôle pas ses interactions/opportunités. Ces tables n'ont pas de FK customer_id dans les migrations inspectées et la fiche n'a pas de cascade JPA correspondante : le chemin autorise structurellement des références orphelines. Aucun effacement réel n'a été tenté pendant l'audit.

## 10. Quatre espaces utilisateurs

| Rôle | Routes/menu et appels constatés | Autorisations serveur et lacunes |
|---|---|---|
| SUPER_ADMIN | Home/dashboard, `/users`, employés/départements, associations, contacts/opportunités, RH, devis/factures, paie/frais | CRUD admin et gestion des rôles/statuts ; dernier compte utilisable protégé. Les paramètres globaux, interface logs/monitoring et première connexion imposant changement de mot de passe ne sont pas couverts. Bootstrap uniquement local/dev avec configuration explicite, pas le provisionnement standard du CDC. |
| ADMIN | Menus métier d'administration, `/users` selon restrictions, congés et registres, contacts/finance | SecurityConfig autorise ADMIN/SUPER_ADMIN ; Identity empêche ADMIN de promouvoir/modifier les comptes privilégiés. Pas de droit implicite sur les endpoints personnels CLIENT/EMPLOYEE : accès admin via leurs parcours dédiés. |
| EMPLOYEE | `/profile`, `/leaves`, `/my-payroll`, `/my-evaluations`, `/my-documents`, dashboard propre | Principal typé UID, association HR, propriété et publication/partage/finalisation filtrées côté serveur ; pas d'accès global Employee/Customer. **Gestion des contacts assignés absente du parcours** : Customer reste réservé admin, malgré le champ d'affectation. Profil dépend du canal HTTPS configuré, non validé en réel. |
| CLIENT | `/client`, profil, factures/PDF/paiements propres et dashboard financier | Client résolu côté serveur via UID, vérification `findByIdAndCustomerId` avant paiements/PDF. Projets, demandes et communication avec l'équipe non implémentés. Aucun choix d'ID client Angular utilisé comme identité. |

Les routes lazy et `authGuard`/`roleGuard` assurent la navigation ; l'API conserve ses propres contrôles. Les services partagés vérifient Identity, sid et état du compte ; une indisponibilité se ferme en 503. Les états vide/chargement/erreur sont codés dans les espaces personnels et les registres HR, mais leur rendu connecté/réactivité mobile n'est pas démontré ici. Aucune page vide n'est déclarée observée sans navigateur : les absences ci-dessus correspondent à des routes/API manquantes.

Backend non entièrement exposé dans Angular : filtres HR `employeeId`/`offerId` disponibles dans certains endpoints mais service générique listant uniquement page/mot-clé ; filtre Customer `assignedEmployeeId` absent du service de liste. Ce sont des écarts ciblés, **pas** la preuve que tout HR ou Contacts manque d'interface. Le résumé interne Employee n'est pas une API oubliée du navigateur : son accès uniquement via HR est intentionnel.

## 11. Fonctionnalités complètes dans leur périmètre de code

- **EMP-01** — CRUD complet (CDC p.6) : Intégration et suppressions avec références.
- **EMP-02** — Profil détaillé personnel/coordonnées/poste/département : Prouver le canal HTTPS et parcours connecté.
- **EMP-03** — Contrats et avenants : Valider règles métier et intégration.
- **EMP-05** — Documents CV/diplômes/attestations : MySQL, fichiers réels, retrait et TLS selon parcours.
- **RH-01** — Demandes de congés/absences : Intégration et états sans association.
- **RH-02** — Workflow de validation : Concurrence MySQL et rollback.
- **RH-05** — Gestion des formations : Intégration et référence employé.
- **RH-07** — Suivi des formations : Rejouer parcours de suivi réel.
- **RH-08** — Évaluations de performance : Intégration finalisé/privé/cross-account.
- **RH-09** — Critères/résultats d'évaluation : Faire accepter le format qualitatif ; pas de score inventé.
- **RH-10** — Gestion du recrutement : Parcours réel offre/candidature.
- **RH-11** — Suivi des candidatures : Intégration et références cohérentes.
- **PAY-02** — Gestion des primes : Intégration et règles de prime à préciser si besoin.
- **PAY-04** — Déductions : Intégration ; pas de calcul légal automatique revendiqué.
- **PAY-05** — Bulletins PDF : Contenu et propriété via vrai Gateway.
- **FEE-01** — Enregistrement des prestations : Intégration et bornes monétaires FEE-05.
- **FEE-02** — Enregistrement des services : Ne pas inventer un catalogue obligatoire distinct.
- **INV-01** — Création : MySQL et scénario création.
- **INV-02** — Modification : Concurrence réelle, rollback, conservation.
- **INV-04** — Numérotation automatique : Concurrence séquence et migration INT réelle.
- **INV-05** — Gestion des devis : Intégration conversion sans double facture.
- **INV-06** — Suivi des paiements : Ledger réel, propriété et précision SQL.
- **INV-08** — PDF : Vrai Gateway et contenu exporté.
- **INV-09** — Historique : Pas de versionnement intégral revendiqué ; intégration.
- **INV-11** — Statistiques : Agrégats SQL réels et données historiques.
- **CON-02** — Clients : Intégration association/propriété.
- **CON-03** — Prospects : Parcours connecté et historique de conversion.
- **CON-04** — Fournisseurs : Pas de rôle JWT fournisseur inventé.
- **CON-05** — Fiches détaillées : Intégration et confidentialité.
- **CON-06** — Historique des interactions : Intégrité suppression AN-02 et intégration.
- **CON-07** — Segmentation : Pas de segmentation calculée exigée sans spécification.
- **CON-08** — Catégorisation : Référentiel administrable uniquement si contractualisé.
- **CON-09** — Opportunités commerciales : Intégration et référence contact conservée.
- **CON-10** — Suivi emails : Suivi manuel ; pas de synchronisation mail revendiquée.
- **CON-11** — Suivi appels : Recette du suivi, pas de téléphonie imposée.
- **CON-12** — Suivi réunions : Pas de calendrier externe imposé.
- **CON-13** — Import CSV : Intégration transactionnelle et doublons.
- **CON-14** — Export CSV : Vérifier fichier dans logiciel cible.
- **CON-16** — Export Excel : Compatibilité réelle Excel/LibreOffice.
- **TR-15** — JWT/refresh/RBAC et propriété (p.8) : Confirmer concurrence SQL et tous services réels.

Ces fonctions n'ont pas à être réécrites. Les tests et validations intégrées restent à compléter selon le niveau de preuve indiqué ; COMPLETE n'autorise aucun déploiement.

## 12. Fonctionnalités partielles

- **EMP-06** — Recherche et filtres avancés : Filtres combinés définis et couverts API/UI.
- **RH-04** — Soldes en temps réel : Acquisition/consommation/report selon règles validées.
- **RH-06** — Affectation aux formations : Valider entité affectée et UX de sélection.
- **RH-12** — Indicateurs RH : Définir KPI clés et exposer agrégats.
- **RH-13** — Statistiques RH : Statistiques définies et réconciliables.
- **PAY-01** — Calcul automatisé des salaires : Automatisation règles variables/taxes contractualisées.
- **PAY-06** — Historique des paiements : Enregistrer/consulter règlements et corrections.
- **FEE-04** — Timesheet : Entrées détaillées et suivi de période.
- **FEE-05** — Calcul automatique des honoraires : Contrat monétaire et calcul borné HTTP/service.
- **FEE-06** — Rapports de facturation : Définir produire et tester rapport facturable.
- **INV-07** — Gestion des relances : Faire accepter le suivi manuel ou compléter l'action de relance.
- **CON-01** — CRUD contacts : Préserver interactions/opportunités à la suppression.
- **CON-17** — Recherche avancée : Recherche combinée complète selon contrat et AN-03.
- **CON-18** — Filtres : Exposer le filtre existant et tester backend/UI.
- **TR-01** — Bootstrap SUPER_ADMIN au premier lancement (p.9) : Provisionnement conforme et sécurisé sans secret par défaut.
- **TR-04** — Logs et monitoring SUPER_ADMIN : Accès administré et preuves de supervision.
- **TR-09** — Audit trail des actions critiques (p.8) : Acteur/date/action/résultat protégés, sans secrets.
- **TR-11** — Stack Java/Boot/Angular/MySQL du CDC (p.4) : Arbitrer explicitement l'écart Boot4.x et compatibilités.
- **TR-12** — HTTPS obligatoire en production (p.8) : Terminaison externe et déploiement vérifiés.
- **TR-14** — Chiffrement données sensibles (p.8) : Préciser périmètre chiffrement, ACL et preuves.
- **TR-17** — CI, documentation et livrables (p.9) : Guide utilisateur, candidat complet, pipeline et installation démontrés.

Priorité aux comportements manquants démontrés, pas à l'ajout d'une bibliothèque ou à une refonte globale. Certaines limites demandent d'abord un contrat métier (calcul légal, KPI, relance manuelle acceptable, règles du solde).

## 13. Fonctionnalités absentes

- **PAY-03** — Heures supplémentaires : Temps, taux et majorations selon règles métier.
- **PAY-07** — Export comptable standard : Format cible, mapping et réconciliation.
- **PAY-08** — Déclarations sociales : Juridiction, règles, format et validation métier.
- **PAY-09** — Déclarations fiscales : Règles légales explicites et recette dédiée.
- **FEE-03** — Grilles tarifaires personnalisables : Tarifs, validité, personnalisation et traçabilité.
- **INV-03** — Modèles personnalisables : Modèles sûrs et sélection/rendu.
- **INV-10** — Archivage : Archivage et consultation sans effacement.
- **CON-15** — Import Excel : Import XLSX borné, validé, rapport de rejet.
- **TR-02** — Changement initial obligatoire du mot de passe : Onboarding sécurisé et invalidation des sessions.
- **TR-03** — Paramètres système globaux SUPER_ADMIN : Définir paramètres et droits, tracer modifications.
- **TR-05** — EMPLOYEE gère ses contacts assignés (p.5) : API propriétaire et parcours dédié.
- **TR-06** — CLIENT suit demandes/projets (p.5) : Définir demandes/projets puis les réaliser.
- **TR-07** — CLIENT communique avec équipe (p.5) : Parcours client-équipe sûr et traçable.
- **TR-08** — Internationalisation (p.8) : Langues, catalogues et recette UI.
- **TR-10** — Sauvegarde automatique quotidienne (p.8) : Sauvegarde et restauration contrôlées.

L'absence est constatée dans les modules actifs et leurs UI, pas dans un système externe non inspecté. Aucun ancien monolithe `backend/` n'est utilisé pour déclarer une fonctionnalité livrée via les microservices/Gateway.

## 14. Fonctionnalités non validées et blocages

- **EMP-04** — Historique postes/promotions : Tester saisie et consultation carrière de bout en bout.
- **RH-03** — Calendrier des absences : Tester mois/bornes/filtres/rendu connecté.
- **TR-16** — Responsive, lazy loading et UI modulaire (p.8) : Tester navigation aux tailles cibles et accessibilité.

- **TR-13** — Validation intégrée des parcours : Précontrôles positifs, autorisation, campagne progressive puis globale.

Toutes les lignes implémentées ont une intégration I:NV. Les preuves historiques sont : Lot 1A 33 Java/158 Angular, Lot 1B 178 Java/390 Angular, Lot 1C 130 Java/571 Angular ; **elles se recouvrent et ne s'additionnent pas**. Les références de tests hors de ces sélections n'impliquent pas une exécution récente. Aucun nouveau nombre de tests réussis ou taux de couverture n'est produit par 3A.

Le Lot 2B-R a seulement préparé une topologie progressive. Le moteur Docker était inaccessible et la RAM libre insuffisante au dernier contrôle ; aucune nouvelle mesure n'a été faite. Il faut prouver migration neuve/upgrade, types/contraintes MySQL, concurrence et rollbacks, vrai TLS, routage, révocation multiservice et navigateur. Une série de groupes séparés ne prouve pas la pile complète simultanée.

## 15. Anomalies démontrées et risques distincts

| ID | Constat établi par code | Conséquence / priorité |
|---|---|---|
| AN-01 | FeeRequest sans contrainte de précision/bornes du résultat et FeeServiceImpl sans validation monétaire de service, malgré DECIMAL(12,2) [F](#f) | Contrat financier incomplet, arrondis/rejet SQL possibles ; ajouter des validations cohérentes et tests limites. Haute. Aucun résultat MySQL inventé. |
| AN-02 | CustomerServiceImpl.deleteCustomer sans contrôle d'interactions/opportunités ; absence de FK vers customers dans les migrations commerciales et de cascade JPA [C](#c) | Possibilité structurelle d'orphelins ; définir conservation et blocage/archivage, tester suppression et concurrence. Haute. Aucune donnée réelle prétendue corrompue. |
| AN-03 | Angular CustomerService.list n'envoie pas assignedEmployeeId, malgré le paramètre backend ; HrAdminService.list n'expose pas certains filtres HR [C](#c), [H](#h) | Capacités backend partiellement inaccessibles depuis les listes Angular. Moyenne. |
| AN-04 | Le plan ancien qualifie i18n/audit trail de hors CDC, tandis que le PDF p.8 les exige explicitement | Écart documentaire de périmètre corrigé dans cet audit, sans modifier les rapports historiques. À intégrer à la feuille de route. |

Risques ou décisions, **pas vulnérabilités exécutées démontrées** : bulletin publié mutable ; références Employee libres dans certains registres ; absence de catalogue de devises ; documents/SQL non transactionnels ensemble ; migration BOOLEAN/TIMESTAMP et verrouillages MySQL non éprouvés ; stockage et HTTPS de production non certifiés. Ne pas déclarer une fuite horizontale sur la seule présence d'un identifiant : les endpoints personnels contrôlent effectivement le propriétaire. CSRF désactivé avec bearer stateless n'est pas automatiquement une faille ; il faut examiner le mode de transport/authentification en intégration.

## 16. Priorités de développement

1. **Haute : écarts CDC absents et décisions préalables** — paie/heures supplémentaires/export/déclarations avec règles légales explicites ; grilles et timesheets ; soldes de congés ; contacts assignés et espaces CLIENT projets/communication ; import Excel, modèles/archivage ; bootstrap et première connexion.
2. **Haute : intégrité transversale** — AN-01/AN-02, publication et corrections de paie à contractualiser, tests de non-régression des protections 1A/1B/1C. Ces correctifs doivent accompagner les lots métier, sans attendre un chantier technique abstrait.
3. **Moyenne : parcours et Angular** — filtres backend non exposés, recherche avancée employés, KPI RH définis, tests composants calendrier/carrière et UX erreurs. Réutiliser les interfaces existantes.
4. **Obligatoire avant livraison : preuves** — reprendre les précontrôles et les groupes 2B-R uniquement avec ressources/autorisation, puis intégration globale sur environnement adapté. Intégrer i18n, audit, sauvegarde/restauration, HTTPS externe et décision de conformité Spring Boot 4.x ; ne pas supposer que Jenkins a testé les fichiers non committés/non suivis.

L'ordre exprime les écarts de conformité, pas une instruction de développer maintenant. Les risques d'intégrité doivent être traités au premier lot autorisé qui touche le domaine concerné.

## 17. Proposition des prochains lots

| Lot proposé / complexité indicative | Objectif et fonctionnalités visibles | Services / Angular | Dépendances, risques, tests et acceptation |
|---|---|---|---|
| 3B — Paie exploitable, forte | Heures supplémentaires, variables tracées, règles de salaire et retenues contractualisées, historique de règlements, publication/correction, export comptable et déclarations requises | Payroll, HR ; formulaires, bulletins, exports Angular | Juridiction, période, taux, formats comptables/social/fiscal à fournir : ne rien inventer. Risques rétroactivité/arrondis/confidentialité. Tests de référence validés métier, limites DECIMAL, propriété et versions. Acceptation : paie explicable et reproductible, export accepté par système cible, déclarations contrôlées, bulletin personnel correct. |
| 3C — Cycle RH et pilotage, forte | Solde congés explicable, consommation/quotas, références de formation contrôlées, filtres utilisables, KPI RH ; compléter les workflows existants selon règles validées | HR, Employee, Department ; leaves/calendar/hr-admin/dashboard | Acquisition, jours ouvrés, report et KPI décidés. Risques double consommation/concurrence. Tests chevauchement/annulation/périodes/solde et composants ; acceptation : solde recalculable depuis événements, résultats identiques API/UI. Ne pas refaire contrats/recrutement déjà présents. |
| 3D — Prestations à facturation, forte | Grilles tarifaires, timesheet détaillée, calcul Fee borné, rapport de facturation et lien explicite vers factures/devis existants | Fees, Invoice, Customer ; fees, grilles, temps et rapports | Règles prix/période/devise et relation prestation-facture à fixer. Risques double facturation et références. Tests AN-01, prix historique, agrégations et droits. Acceptation : prestation→temps→montant→rapport traçable sans réécriture rétroactive. |
| 3E — CRM et espaces relationnels, forte | Import Excel, contacts assignés EMPLOYEE, conservation des interactions/opportunités, filtres avancés ; demandes/projets et communication CLIENT | Customer, Identity ; customers, associations, client et nouvel espace de demandes/projets | Définir projet/demande et communication selon CDC sans imposer fournisseur mail ; résoudre AN-02/AN-03. Tests fichiers hostiles, doublons, droits horizontaux, suppression protégée. Acceptation : EMPLOYEE ne gère que ses contacts, CLIENT ne voit que son dossier, imports/export fiables. Conserver les types et exports déjà implémentés. |
| 3F — Facturation documentaire, moyenne à forte | Modèles personnalisables, archivage, parcours relance accepté métier et historique conservé | Invoice, Customer ; templates, factures et client | Décider champs/archivage/communication, respecter les protections 1A et le suivi manuel existant. Risques mutation après paiement et divulgation PDF. Tests rendu, propriété, paiements, verrouillages et archive. Acceptation : modèle réellement sélectionnable, archives retrouvables, relance traçable utilisable. |
| 3G — Conformité transverse et livraison, forte | Bootstrap sécurisé/initial password, paramètres/logs-monitoring autorisés, audit critique, i18n, sauvegardes restaurables, HTTPS de production et alignement stack CDC | Identity/common-security/Gateway, tous domaines, Angular et CI | Arbitrage version Spring Boot/Cloud et langues, journalisation minimisée et politique de rétention. Tests migration sécurité, premier accès, audit sans secrets, restauration et locale. Acceptation : installation documentée, comptes sûrs, traces vérifiables, exigences p.8–9 prouvées. Aucun secret par défaut en dur. |

En parallèle des lots autorisés : **campagne 2B-R puis validation globale**. Complexités relatives (pas des estimations de jours) ; elles dépendent des arbitrages métier et de l'environnement. Jenkins doit tester un candidat contenant réellement les nouvelles sources et migrations, dans un espace isolé ; l'audit ne demande ni commit ni lancement CI.

## 18. Limites de l'audit

Audit statique du dépôt courant : pas d'exécution de tests applicatifs, migration, serveur, Docker, réseau métier, SMTP, export dans logiciel tiers, navigateur ou restauration. Pas de couverture mesurée ni certification légale de la paie/facturation. La syntaxe et présence des tests ne prouvent pas leur succès ; les traces historiques sont décrites comme telles. Les fonctions absentes ne peuvent être garanties absentes de services externes non fournis.

Le CDC est générique sur plusieurs règles ; les détails supplémentaires de la mission sont examinés sans transformer arbitrairement une possibilité technique (barème chiffré, banque, envoi automatique, sessions multi-salariés) en obligation contractuelle. Les lignes transversales du PDF ont un comptage séparé. Le monolithe legacy est conservé mais n'est pas crédité comme parcours courant des microservices.

Une photographie SHA-256 des **974 fichiers préexistants suivis/non suivis** (hors fichiers de secrets exclus de la lecture) a été prise avant rédaction. Comparaison finale réussie : **aucun changement ni disparition**. Les 80 IDs, les totaux, les 19 sections et les liens locaux ont été vérifiés ; aucun lien manquant. Contrôle git diff --check réussi. Seul ce rapport est ajouté au périmètre versionnable ; auxiliaires et extraction sous target/lot3a ignorés par Git. Aucun commit, push, changement de branche, staging ou suppression.

## 19. Conclusion générale

**CONFORMITÉ FONCTIONNELLE PARTIELLE.** Le socle et plusieurs modules/parcours sont cohérents et doivent être conservés. Les absences identifiées, les limites métier et les écarts d'intégrité empêchent une conformité complète ; l'intégration réelle reste à démontrer. Le verdict ne découle ni d'un nombre de fichiers ni d'une somme de tests : il repose sur la matrice reliée au code et au CDC. Aucun développement ou déploiement n'a été réalisé pendant ce Lot 3A.
