# Lot 3G — Finalisation fonctionnelle de la facturation et rapprochement des honoraires

Date : 7 novembre 2026. Branche conservée : `publication-clean`.

## 1. Résumé exécutif

**LOT 3G PARTIEL — modèles structurés, émission avec PDF figé, suivi technique des échéances/relances, archivage et rapprochement d’honoraires développés. Validation intégrée réelle NON RÉALISÉE.**

La sélection finale comporte **70 cas Java Invoice et 103 cas Fees, soit 173 cas distincts**, ainsi que **1 254 tests Angular réussis**. Le build Angular de production réussit avec les avertissements existants. Ces résultats sont locaux ; ils ne certifient aucun service CRM connecté, schéma MySQL ou déploiement.

Le rapprochement réalise un sous-ensemble explicite : seuls les honoraires générés depuis des temps figés, avec client et devise fiables, peuvent alimenter un nouveau brouillon. Invoice conserve le registre des réservations et des liens. Fees est consulté en lecture seule. Aucune transaction distribuée, synchronisation des anciens statuts libres ou facture automatique à l’enregistrement d’un honoraire n’est revendiquée.

Aucun Docker, MySQL, Flyway, Gateway ou microservice CRM lancé. Aucun secret modifié, staging, commit, push, changement de branche, nettoyage ou effacement de données préexistantes.

## 2. État initial

Lecture des rapports [3A](LOT_3A_AUDIT_CONFORMITE_FONCTIONNELLE.md), [3E](LOT_3E_FINALISATION_HONORAIRES.md), [3F](LOT_3F_FINALISATION_CRM_COMMERCIAL.md), [3D](LOT_3D_STABILISATION_INTEGREE.md), des protections des lots antérieurs et du [CDC CRM](CDC%20CRM%201.pdf), via son extraction locale conservée. Inspection d’Invoice, Quote, paiements, PDF, services personnels, Fees et ses temps figés, routes Gateway, sécurité commune et interfaces Angular.

Le CRUD, la séquence de numérotation, les devis et leur conversion, les paiements partiels et l’association User–Customer existaient. Les PDF étaient générés à la demande, sans modèle configurable ni capture persistante d’émission. Les relances étaient un journal manuel. Il n’existait ni archivage ni contrat de rapprochement fiable avec Fees.

Le plan a conservé ces mécanismes, ajouté les lectures et mutations nécessaires, puis vérifié les invariants financiers et personnels. Les blocages RAM/Docker de 3D restent des observations historiques : aucune nouvelle mesure ni tentative de démarrage d’infrastructure n’est réalisée dans ce lot, conformément à la demande.

## 3. Fonctionnalités développées

- **Modèles :** liste paginée, création, modification avant utilisation, activation/désactivation, logo PNG/JPEG normalisé, textes et deux présentations de lignes, prévisualisation et sélection explicites.
- **Émission :** capture du PDF, empreinte SHA-256 et auteur ; gel du modèle utilisé et de la facture. Une nouvelle demande d’émission avec le même modèle retourne les mêmes octets. Le suivi des paiements reste séparé du document.
- **Échéances :** consultation par période d’échéance, client, statut et archive ; vues à venir/en retard, total, payé, restant et prochain créneau technique, sans addition entre devises.
- **Relances :** politique inactive par défaut, décalage/intervalle/canal/texte configurables ; préparation manuelle par lots bornés ; file PREPARED/HANDLED/CANCELLED et compte rendu d’action réellement effectuée. Aucun moteur d’envoi ni état SENT fictif.
- **Archivage :** conservation de la facture, de ses lignes, paiements, documents et liens ; accès personnel maintenu pour son CLIENT propriétaire.
- **Rapprochement :** sélection administrative d’honoraires éligibles, réservation durable, création d’un brouillon avec lignes capturées, UUID idempotent, suivi d’échec, reprise et libération motivée avant confirmation.

L’archivage sûr retenu exige une facture PAID dont le registre règle exactement le total positif. Les impayés et les anciens PAID sans rapprochement du registre ne sont pas masqués par une archive. Aucun état d’annulation, avoir, suppression physique ou procédure légale n’est inventé.

## 4. Fichiers modifiés et nouveaux

Inventaire relatif à la photographie de début de lot, et non au HEAD déjà modifié : **27 fichiers préexistants modifiés et 37 nouveaux fichiers, rapport compris**. La photographie SHA-256 de **1 086 fichiers préexistants**, hors secrets et clés exclus de la lecture, ne présente aucune disparition ; **1 059 fichiers sont inchangés**.

Les modifications se concentrent sur Invoice, les écrans Facturation et l’affichage de l’archive dans l’espace CLIENT. Fees reçoit seulement un nouveau service de lecture et son contrôleur/test. Le seul changement partagé de navigation concerne l’URL de retour autorisée `/invoices/workbench`. Les sources Customer, HR, Payroll, Identity, Employee, common-security, les rapports antérieurs et les scripts de validation restent inchangés. Les 27 migrations historiques sont préservées.

### Fichiers préexistants modifiés

- [frontend/src/app/core/services/auth-navigation.ts](../frontend/src/app/core/services/auth-navigation.ts)
- [frontend/src/app/features/client/client-workspace.component.html](../frontend/src/app/features/client/client-workspace.component.html)
- [frontend/src/app/features/client/client.service.ts](../frontend/src/app/features/client/client.service.ts)
- [frontend/src/app/features/invoices/invoice-detail.component.html](../frontend/src/app/features/invoices/invoice-detail.component.html)
- [frontend/src/app/features/invoices/invoice-detail.component.ts](../frontend/src/app/features/invoices/invoice-detail.component.ts)
- [frontend/src/app/features/invoices/invoice-form.component.ts](../frontend/src/app/features/invoices/invoice-form.component.ts)
- [frontend/src/app/features/invoices/invoice.models.ts](../frontend/src/app/features/invoices/invoice.models.ts)
- [frontend/src/app/features/invoices/invoices-page.component.html](../frontend/src/app/features/invoices/invoices-page.component.html)
- [frontend/src/app/features/invoices/invoices-page.component.spec.ts](../frontend/src/app/features/invoices/invoices-page.component.spec.ts)
- [frontend/src/app/features/invoices/invoices-page.component.ts](../frontend/src/app/features/invoices/invoices-page.component.ts)
- [frontend/src/app/features/invoices/invoices.routes.spec.ts](../frontend/src/app/features/invoices/invoices.routes.spec.ts)
- [frontend/src/app/features/invoices/invoices.routes.ts](../frontend/src/app/features/invoices/invoices.routes.ts)
- [microservices/invoice-service/src/main/java/com/crm/invoice/config/SecurityConfig.java](../microservices/invoice-service/src/main/java/com/crm/invoice/config/SecurityConfig.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoiceController.java](../microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoiceController.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoicePdfController.java](../microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoicePdfController.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/controller/PersonalInvoiceController.java](../microservices/invoice-service/src/main/java/com/crm/invoice/controller/PersonalInvoiceController.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/controller/QuoteController.java](../microservices/invoice-service/src/main/java/com/crm/invoice/controller/QuoteController.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/dto/InvoiceResponse.java](../microservices/invoice-service/src/main/java/com/crm/invoice/dto/InvoiceResponse.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/entity/Invoice.java](../microservices/invoice-service/src/main/java/com/crm/invoice/entity/Invoice.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoiceLine.java](../microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoiceLine.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/exception/GlobalExceptionHandler.java](../microservices/invoice-service/src/main/java/com/crm/invoice/exception/GlobalExceptionHandler.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/mapper/InvoiceMapper.java](../microservices/invoice-service/src/main/java/com/crm/invoice/mapper/InvoiceMapper.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/pdf/PdfReport.java](../microservices/invoice-service/src/main/java/com/crm/invoice/pdf/PdfReport.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/service/PersonalInvoiceService.java](../microservices/invoice-service/src/main/java/com/crm/invoice/service/PersonalInvoiceService.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/service/impl/InvoiceServiceImpl.java](../microservices/invoice-service/src/main/java/com/crm/invoice/service/impl/InvoiceServiceImpl.java)
- [microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceFinancialPolicyTest.java](../microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceFinancialPolicyTest.java)
- [microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceServiceIntegrationTest.java](../microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceServiceIntegrationTest.java)

### Nouveaux fichiers

- [docs/LOT_3G_FINALISATION_FACTURATION.md](LOT_3G_FINALISATION_FACTURATION.md)
- [frontend/src/app/features/invoices/invoice-work.service.spec.ts](../frontend/src/app/features/invoices/invoice-work.service.spec.ts)
- [frontend/src/app/features/invoices/invoice-work.service.ts](../frontend/src/app/features/invoices/invoice-work.service.ts)
- [frontend/src/app/features/invoices/invoice-workbench.component.html](../frontend/src/app/features/invoices/invoice-workbench.component.html)
- [frontend/src/app/features/invoices/invoice-workbench.component.scss](../frontend/src/app/features/invoices/invoice-workbench.component.scss)
- [frontend/src/app/features/invoices/invoice-workbench.component.spec.ts](../frontend/src/app/features/invoices/invoice-workbench.component.spec.ts)
- [frontend/src/app/features/invoices/invoice-workbench.component.ts](../frontend/src/app/features/invoices/invoice-workbench.component.ts)
- [microservices/fees-service/src/main/java/com/crm/fees/controller/FeeBillingController.java](../microservices/fees-service/src/main/java/com/crm/fees/controller/FeeBillingController.java)
- [microservices/fees-service/src/main/java/com/crm/fees/service/FeeBillingCandidates.java](../microservices/fees-service/src/main/java/com/crm/fees/service/FeeBillingCandidates.java)
- [microservices/fees-service/src/test/java/com/crm/fees/FeeBillingCandidatesTest.java](../microservices/fees-service/src/test/java/com/crm/fees/FeeBillingCandidatesTest.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/client/InvoiceSourceClient.java](../microservices/invoice-service/src/main/java/com/crm/invoice/client/InvoiceSourceClient.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/config/InvoiceClockConfiguration.java](../microservices/invoice-service/src/main/java/com/crm/invoice/config/InvoiceClockConfiguration.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoiceWorkController.java](../microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoiceWorkController.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoiceDocument.java](../microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoiceDocument.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoiceFeeClaim.java](../microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoiceFeeClaim.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoicePresentation.java](../microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoicePresentation.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoiceReconciliation.java](../microservices/invoice-service/src/main/java/com/crm/invoice/entity/InvoiceReconciliation.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/entity/ReminderPolicy.java](../microservices/invoice-service/src/main/java/com/crm/invoice/entity/ReminderPolicy.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/entity/ReminderTask.java](../microservices/invoice-service/src/main/java/com/crm/invoice/entity/ReminderTask.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoiceDocumentRepository.java](../microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoiceDocumentRepository.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoiceFeeClaimRepository.java](../microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoiceFeeClaimRepository.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoicePresentationRepository.java](../microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoicePresentationRepository.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoiceReconciliationRepository.java](../microservices/invoice-service/src/main/java/com/crm/invoice/repository/InvoiceReconciliationRepository.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/repository/ReminderPolicyRepository.java](../microservices/invoice-service/src/main/java/com/crm/invoice/repository/ReminderPolicyRepository.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/repository/ReminderTaskRepository.java](../microservices/invoice-service/src/main/java/com/crm/invoice/repository/ReminderTaskRepository.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/service/InvoiceFollowupService.java](../microservices/invoice-service/src/main/java/com/crm/invoice/service/InvoiceFollowupService.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/service/InvoiceLifecycle.java](../microservices/invoice-service/src/main/java/com/crm/invoice/service/InvoiceLifecycle.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/service/InvoicePresentationService.java](../microservices/invoice-service/src/main/java/com/crm/invoice/service/InvoicePresentationService.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/service/InvoiceReconciliationService.java](../microservices/invoice-service/src/main/java/com/crm/invoice/service/InvoiceReconciliationService.java)
- [microservices/invoice-service/src/main/java/com/crm/invoice/service/InvoiceWorkAccess.java](../microservices/invoice-service/src/main/java/com/crm/invoice/service/InvoiceWorkAccess.java)
- [microservices/invoice-service/src/main/resources/db/migration/V20261107_01__invoice_documents_reminders_and_fee_claims.sql](../microservices/invoice-service/src/main/resources/db/migration/V20261107_01__invoice_documents_reminders_and_fee_claims.sql)
- [microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceFollowupPolicyTest.java](../microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceFollowupPolicyTest.java)
- [microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceFrozenOwnershipTest.java](../microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceFrozenOwnershipTest.java)
- [microservices/invoice-service/src/test/java/com/crm/invoice/InvoicePresentationPolicyTest.java](../microservices/invoice-service/src/test/java/com/crm/invoice/InvoicePresentationPolicyTest.java)
- [microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceReconciliationFixtureTest.java](../microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceReconciliationFixtureTest.java)
- [microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceSourceContractTest.java](../microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceSourceContractTest.java)
- [microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceWorkSecurityTest.java](../microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceWorkSecurityTest.java)

Les inventaires et preuves sont sous `target/lot3g`, ignoré par Git. Aucun fichier préexistant supprimé et aucun changement Git mis en index.

## 5. Endpoints REST

Préfixe Gateway existant : `/invoice`. Le namespace local `/api/invoices/work` est entièrement réservé ADMIN/SUPER_ADMIN, pour toutes les méthodes. Ses réponses réussies et ses PDF imposent `Cache-Control: no-store`.

| Endpoint local | Contrat |
|---|---|
| GET/POST /api/invoices/work/templates | Liste paginée / création structurée |
| PUT /api/invoices/work/templates/{id} | Modification avant utilisation |
| PATCH /api/invoices/work/templates/{id}/active | Activation/désactivation, sans altérer les documents |
| POST /api/invoices/work/templates/{id}/logo | Multipart PNG/JPEG, contenu contrôlé côté serveur |
| GET /api/invoices/work/templates/{id}/preview | PDF d’exemple, sans émission |
| GET /api/invoices/work/{id}/preview?templateId=… | Prévisualisation d’une facture ; document conservé si déjà émise |
| POST /api/invoices/work/{id}/issue | `{templateId:"…"}` ; émission explicite et capture du PDF |
| POST /api/invoices/work/{id}/archive | Archivage non destructif d’une facture effectivement soldée |
| GET /api/invoices/work/invoices | from/to obligatoires, customerId/status/archived/mode/page facultatifs |
| GET/PUT /api/invoices/work/policy | Lecture / configuration explicite de la politique technique |
| POST /api/invoices/work/reminders/prepare?page=… | Un lot de 20 factures candidates, ordre des identifiants |
| GET /api/invoices/work/reminders | File/historique paginés, état facultatif |
| POST /api/invoices/work/reminders/{id}/record | Compte rendu obligatoire ; aucun envoi réseau |
| GET /api/invoices/work/fee-candidates | customerId, currency, page ; éligibilité Fees enrichie par le registre Invoice |
| GET/POST /api/invoices/work/reconciliations | Suivi paginé / sélection explicite avec UUID |
| POST /api/invoices/work/reconciliations/{uuid}/retry | Reprise du même contenu conservé côté serveur |
| POST /api/invoices/work/reconciliations/{uuid}/abandon | Motif requis ; libération uniquement avant création confirmée |

Dans Fees, préfixe Gateway `/fees` : **GET `/api/fees/billing-candidates`** et **GET `/api/fees/billing-candidates/{id}`**, administratifs et en lecture seule, avec no-store. Les routes Gateway existantes couvrent ces chemins ; aucune configuration Gateway modifiée.

Les anciens endpoints sont conservés. **Changement volontaire de DELETE `/api/invoices/{id}` :** il archive désormais, sans suppression, et refuse par 409 une facture non soldée selon le registre. Le DTO administratif ajoute archived, issued et reconciliationId ; le résumé CLIENT ajoute archived. Les anciens consommateurs de DELETE doivent accepter cette nouvelle sémantique.

Les nouveaux identifiants métier sont textuels. Les erreurs 400/401/403/404/409/502/503 sont expurgées. Un échec distant survenu après réservation peut retourner un suivi HTTP 200 **NEEDS_ATTENTION**, avec errorStatus ; cette réponse ne signifie pas qu’une facture a été créée.

## 6. Modèles et migrations

Nouvelle migration additive : [V20261107_01__invoice_documents_reminders_and_fee_claims.sql](../microservices/invoice-service/src/main/resources/db/migration/V20261107_01__invoice_documents_reminders_and_fee_claims.sql). **Préparée, NON EXÉCUTÉE.** Le dépôt comporte **28 migrations**, dont 27 historiques inchangées par empreinte.

| Modèle | Conservation et contraintes préparées |
|---|---|
| Invoice | archived/issued faux par défaut, reconciliation_id nullable ; index échéance/archive et client/archive |
| InvoicePresentation | Textes bornés, layout COMPACT/DETAIL, actif/utilisé, logo binaire normalisé |
| InvoiceDocument | PK invoice_id, modèle, contenu PDF, SHA-256, auteur/date ; contenu non modifiable par JPA |
| ReminderPolicy | Singleton désactivé, révision et paramètres techniques explicites |
| ReminderTask | Facture, révision, créneau, canal/texte capturés, état et compte rendu ; unicité facture/révision/créneau |
| InvoiceReconciliation | UUID, sélection, client/devise/dates/taxe, état, facture unique éventuelle, erreur, auteur/dates |
| InvoiceFeeClaim | fee_id primaire, opération, facture et ligne ; une seule réservation/lien par honoraire |

Les FK locales protègent documents, tâches, opérations et liens. Aucun lien SQL entre bases Fees/Invoice/Customer. Les numéros utilisent la séquence existante. L’identifiant parent de chaque nouvelle InvoiceLine est renseigné dès l’insertion ; le mapping auparavant non insérable était incohérent avec la colonne historique NOT NULL. La conversion Quote renseigne aussi ce parent.

Aucun DROP, réécriture de données historiques, baseline, repair ou clean. Le seul INSERT de la migration initialise la nouvelle politique désactivée ; l’intervalle de sept jours est une valeur technique inactive, pas une cadence légale appliquée.

Les fixtures H2 construisent leur schéma depuis les entités, sans lire ni exécuter cette migration. Elles ne valident ni DDL MySQL, ni Hibernate validate sur un schéma migré, ni collations/FK/index MySQL, ni upgrade. Les verrous pessimistes sont partagés avec les mutations concernées ; leur comportement concurrent réel reste à éprouver.

## 7. Interfaces Angular

La route lazy `/invoices/workbench` hérite des guards administratifs existants et est reliée depuis la liste et le détail de facture. Aucun droit EMPLOYEE/CLIENT n’est ajouté à l’administration. Les tests de navigation exercent les quatre rôles.

La vue rassemble modèles et formulaire, logo, sélection/prévisualisation PDF, émission explicite, filtres d’échéance/archive, politique/file de relances, honoraires éligibles et opérations de rapprochement. Les mutations sensibles demandent une confirmation ; aucune émission n’est déclenchée par la création d’un modèle ou d’un brouillon. L’indice du lot de préparation des relances est indépendant de la page des factures filtrées.

L’édition des factures émises, archivées ou rapprochées est bloquée dans le formulaire ; les liens et actions de la liste/détail reflètent ces états. L’espace CLIENT conserve ses factures archivées et leurs téléchargements, avec une indication visible. Les anciens écrans conservent leurs contrats numériques ; les nouveaux contrats utilisent des IDs textuels.

Chargement, vide, erreurs, verrouillage pendant requête et reprises sont présents. Les brouillons, sélection, politique et URL PDF sont effacés lors d’un changement de session/droits ; les réponses tardives sont ignorées. Les URL de prévisualisation sont créées depuis des Blob PDF contrôlés et révoquées au remplacement/destruction. Un UUID de rapprochement est conservé après erreur tant que son contenu reste identique.

Le build compile réellement les nouvelles vues. Aucun navigateur connecté, audit ergonomique mobile/clavier ou recette avec quatre comptes réels n’est exécuté.

## 8. Contrat Fees–Invoice

**Éligibilité sûre :** honoraire généré par le cycle 3E, source_time_id et exact_hours présents, client canonique et devise explicite, aucun ancien statut libre de facturation/paiement. Fees vérifie le lien réciproque temps–Fee, l’absence d’annulation et l’égalité avec le taux, les heures, le montant et le client/devise de la prestation. Les honoraires manuels/historiques sont exclus, sans conversion forcée.

Invoice vérifie le contrat de chaque réponse : objet, IDs textuels exacts, immutabilité explicite, décimaux bornés et produit cohérent. Le client doit être une fiche Customer active de type CUSTOMER. Tous les honoraires sélectionnés appartiennent à ce client et à la même devise. Les appels portent le bearer de l’administrateur, avec Discovery, délais bornés et refus des redirections.

Le protocole sépare trois étapes :

1. Transaction locale : création/reprise de l’opération et réservation des fee_id dans un ordre déterministe.
2. Hors transaction d’écriture : vérification Customer et lecture des sources Fees immuables.
3. Transaction locale : recontrôle des réservations, numéro et brouillon Invoice, lignes et liens, puis RECONCILED. Un échec final annule cette transaction et conserve une opération reprenable.

Un même UUID avec le même contenu retourne le même résultat confirmé. Un UUID réutilisé avec un autre contenu est refusé. Une autre opération ne peut reprendre un fee_id réservé ou confirmé. Un échec après réservation devient NEEDS_ATTENTION ; la reprise conserve la sélection. Avant confirmation, une libération motivée annule l’opération et rend les réservations disponibles ; l’historique de sélection est conservé. Après confirmation, aucune libération, suppression ou modification des montants sources n’est exposée.

Les états proposés sont NON_RECONCILED pour une source libre, IN_PROGRESS, NEEDS_ATTENTION et RECONCILED ; CANCELLED désigne une opération libérée, **pas une facture annulée**. Le registre Invoice fait foi. Aucun champ « facturé » n’est écrit dans Fees et aucun total des rapports 3E n’est requalifié silencieusement. Les consommateurs qui veulent connaître le rapprochement doivent consulter Invoice.

Une panne réseau ne peut donc pas produire une écriture Fees à compenser. La compensation proposée est la libération locale avant confirmation. Cela ne constitue pas une preuve d’atomicité distribuée ni un rapprochement bancaire. La création d’une facture, son émission, son règlement et sa conformité légale sont des événements distincts.

## 9. Sécurité et autorisations

ADMIN/SUPER_ADMIN gèrent modèles, politiques, archives et rapprochements. Les nouveaux services vérifient aussi un principal UID typé et le rôle. EMPLOYEE et CLIENT sont refusés dans le namespace administratif avant les appels métier.

CLIENT reste résolu par l’association User–Customer existante. La propriété est vérifiée avant toute lecture du stockage de PDF figés ; une autre facture retourne 404. L’archive ne contourne pas ce contrôle et n’efface pas l’accès légitime. Les notes internes Invoice ne sont pas incluses dans le nouveau PDF partagé. Les textes du modèle et les lignes sont destinés au document ; aucune identité technique d’auteur, référence de paiement privée ou trace de rapprochement interne n’y est ajoutée.

JWT, sid/révocation, association personnelle, filtre commun et protections des autres domaines restent inchangés. Les erreurs distantes sont classées sans corps distant. Aucun accès anonyme ou repli sur username/e-mail. Les tests de filtres utilisent un Identity simulé, pas un JWT/Identity réel en réseau.

Les logos sont limités à 256 Kio, 2 000 pixels par dimension et un million de pixels, contrôlés avant décodage complet puis réencodés PNG. Ni SVG, HTML arbitraire, URL distante ni script n’est rendu. Les textes sont bornés et imprimés comme texte. Le PDF conservé est borné à 2 Mio et son empreinte est vérifiée à la lecture ; une corruption échoue sans régénération implicite.

## 10. Règles financières

Les calculs restent en BigDecimal. Chaque ligne de rapprochement conserve durée, taux et montant source ; Invoice additionne les montants capturés, puis une taxe facultative explicitement saisie selon le contrat existant. Aucun taux fiscal, remise, pénalité, frais de relance, change ou ventilation comptable n’est inventé.

Exemple exercé en H2 : deux honoraires de 2,50 h × 12,34, soit chacun 30,85 ; sous-total 61,70, taxe saisie 1,00, total 62,70. Les limites DECIMAL(12,2), la précision des entrées et le total sont contrôlés avant écriture. Aucune somme entre devises. Les quantités capturées conservent la convention décimale 3E.

Les factures rapprochées sont figées dès la création du brouillon pour préserver leur preuve source ; leur émission reste séparée. Les nouvelles émissions figent aussi les autres factures. Une modification de modèle ou un paiement ultérieur ne réécrit pas les octets conservés. Aucun numéro historique n’est remplacé.

Les factures historiques sans capture conservent leur ancien chemin PDF ; elles ne sont pas rétroactivement déclarées émises ou converties. Le nouvel indicateur issued signifie une émission explicite enregistrée par ce mécanisme, pas une qualification légale des historiques. Les anciens PAID sans registre concordant ne sont pas rapprochés automatiquement.

Le restant provient du total enregistré moins les paiements. Une facture soldée/archivée ou un état non éligible n’entre pas dans la préparation de relances. Le paiement complet annule les préparations ouvertes ; leur traitement recontrôle le solde et la révision de politique. Les dates de suivi utilisent la date UTC du serveur ; la période d’échéance est inclusive et bornée à 366 jours. Aucun envoi automatique n’est ajouté.

## 11. Tests Java et Angular

Exécutions séquentielles hors ligne, cache Maven local, MAVEN_OPTS 512 Mio et JVM de tests 384 Mio ; Node Angular limité à 1 536 Mio, deux workers. Aucun build Java et Angular simultané.

| Suite Invoice | Cas réussis |
|---|---:|
| InvoiceFinancialPolicyTest | 13 |
| InvoiceFollowupPolicyTest | 10 |
| InvoicePresentationPolicyTest | 10 |
| InvoiceReconciliationFixtureTest | 9 |
| InvoiceSourceContractTest | 5 |
| InvoiceWorkSecurityTest | 7 |
| InvoiceFrozenOwnershipTest | 3 |
| PersonalPaymentPolicyTest | 6 |
| ClientStatisticsContractTest | 1 |
| InvoicePdfTest | 5 |
| SqlErrorPrivacyTest | 1 |
| **Total Invoice distinct** | **70** |

Fees : les **97 cas de la sélection 3E sont réexécutés**, plus **six FeeBillingCandidatesTest**, soit **103 cas réussis**. Total Java du lot : **173**, sans addition des relances ou des résultats historiques.

Les politiques, contrats distants et services utilisent des mocks. InvoiceWorkSecurityTest exerce la chaîne de filtres et MVC réels, avec résultat Identity et services simulés : quatre rôles, toutes méthodes dont PATCH, 401/503, no-store, entrées invalides, bearer et erreurs expurgées. InvoiceFrozenOwnershipTest utilise le vrai service personnel avec association/dépôt simulés et prouve l’ordre propriété puis stockage PDF.

Les **neuf fixtures Invoice Hibernate/H2** utilisent de vrais dépôts Spring Data, paramètres JPQL, transactions locales, contraintes créées depuis les entités, lignes enfants et séquence. Elles couvrent création et capture, idempotence, refus de réutilisation, réservation durable, reprise, libération, payload différent, refus personnels, rollback de la transaction finale après une exception de persistance injectée et conservation des lignes/paiements/liens/PDF après archive. Ce rollback est une preuve H2 avec faute injectée, pas une erreur SQL MySQL reproduite ni une campagne concurrente.

Les cinq fixtures de rapports Fees et les serveurs HTTP Identity/référence synthétiques déjà utilisés par les suites 3E sont inclus dans ses 97 cas. Ils ne sont pas des services CRM. Le test PDF Invoice existant utilise une tranche WebMvcTest ; aucun SpringBootTest avec base n’est exécuté.

**Angular : 1 254/1 254 réussis**, sélection complète `src/app/**/*.spec.ts`, ChromeHeadless **157**. Les 24 nouveaux cas couvrent les contrats HTTP/PDF et IDs textuels, modèles et formulaire rendu, confirmation, préparation par lot, UUID/reprise, état incomplet, libération protégée, purge de session/réponses tardives, logo et guards de workbench pour quatre rôles. Les specs des lots précédents sont incluses, pas additionnées. HTTP simulé uniquement.

**Build production réussi : 889,30 kB initiaux**, sous le seuil bloquant existant de 1 MB. Avertissements conservés : budget indicatif initial 500 kB, styles Payroll/Invoices au-dessus du seuil indicatif et NG8011 dans CustomersPageComponent. Aucun budget ni source de ces modules extérieurs n’est modifié pour les masquer.

Commandes finales :

    # MAVEN_OPTS=-Xmx512m ; cache local, hors ligne
    mvn -o -B -ntp -Dmaven.repo.local=C:/Users/ASUS/.m2/repository -f microservices/invoice-service/pom.xml -Dtest=InvoiceFrozenOwnershipTest,InvoiceReconciliationFixtureTest,InvoiceSourceContractTest,InvoiceWorkSecurityTest,InvoicePresentationPolicyTest,InvoiceFollowupPolicyTest,InvoiceFinancialPolicyTest,PersonalPaymentPolicyTest,ClientStatisticsContractTest,InvoicePdfTest,SqlErrorPrivacyTest -DargLine=-Xmx384m test
    mvn -o -B -ntp -Dmaven.repo.local=C:/Users/ASUS/.m2/repository -f microservices/fees-service/pom.xml -Dtest=FeeBillingCandidatesTest,FeeFinancialPolicyTest,FeeCyclePolicyTest,FeeReadSecurityTest,SqlErrorPrivacyTest,FeeReferenceDirectoryTest,FeeReportPolicyTest,FeeReportFixtureTest,FeeCycleSecurityTest -DargLine=-Xmx384m test
    # Depuis frontend : CHROME_BIN configuré, NODE_OPTIONS=--max-old-space-size=1536, NG_BUILD_MAX_WORKERS=2
    .\node_modules\.bin\ng.cmd test --no-watch --browsers=ChromeHeadless --karma-config=karma.ci.conf.cjs --include=src/app/**/*.spec.ts
    .\node_modules\.bin\ng.cmd build --configuration production --output-path ../target/lot3g/frontend-production

Échecs intermédiaires corrigés : les deux attentes Angular de « suppression » après passage à l’archive ; l’espion Mockito d’un dépôt Spring Data proxifié dans une fixture, remplacé par un mock déléguant les lectures ; une attente de texte dans le nouveau test d’écran. Aucun passage incomplet ou échoué n’est compté comme succès. Les derniers décomptes remplacent les sélections antérieures.

## 12. Tests non exécutés

**NON EXÉCUTÉS :** Docker, MySQL neuf/upgrade, Flyway, Hibernate validate sur base migrée, DDL/collations/index/FK réels, verrous concurrents et deadlocks MySQL, rollback après erreur SQL réelle, Gateway/Discovery/Identity/Fees/Customer connectés, HTTPS, vrai JWT révoqué, navigateur connecté quatre comptes, performance/charge, campagne d’envoi ou délivrabilité.

InvoiceServiceIntegrationTest est adapté à la conservation d’une facture impayée et compilé, mais pas exécuté. QuoteServiceIntegrationTest et les autres SpringBootTest avec base ne sont pas lancés. Les suites Java des domaines Paie/RH/Customer ne sont pas relancées ; leurs sources sont conservées et leurs tests Angular sont inclus dans la sélection complète.

Les ressources hôte et Docker ne sont pas remesurées. Aucun résultat 3D/3E/3F n’est présenté comme une nouvelle observation opérationnelle.

## 13. Anomalies corrigées

- Suppression physique encore possible d’une facture impayée : refus et remplacement de DELETE par un archivage contrôlé, sans effacement de facture ni historique.
- Absence de preuve stable de présentation à l’émission : modèle utilisé figé, PDF stocké et vérifié par empreinte, même après paiement/désactivation.
- Mapping InvoiceLine empêchant l’insertion du parent alors que la colonne historique est NOT NULL : parent explicitement inséré pour rapprochement et conversion Quote, chemin d’insertion vérifié sur H2. Aucun échec MySQL antérieur n’est prétendu reproduit.
- Relances manuelles possibles sur des factures soldées ou non éligibles : contrôle du registre et de l’état, annulation des préparations, déduplication par créneau/révision et recontrôle avant traitement.
- Absence de rapprochement fiable : sources immuables vérifiées, sélection explicite, réservation unique, suivi durable et reprise/libération sans double facture.
- Risques introduits pendant le développement : politique et brouillons purgés au changement de session, lots de préparation indépendants des pages filtrées, notes internes exclues du PDF partagé ; tests correspondants ajoutés.

Les défauts concurrentiels, transports ou contraintes MySQL ne sont pas déclarés corrigés sur la seule foi de mocks/H2. Aucune anomalie des autres domaines n’est requalifiée implicitement.

## 14. Exigences INV-01 à INV-10 actualisées

COMPLETE qualifie seulement le périmètre de code et de tests locaux défini. **Toute intégration réelle reste NON VALIDÉE.** La matrice historique 3A n’est pas réécrite.

| Exigence | Avant 3G | Après 3G et limites |
|---|---|---|
| INV-01 Création | COMPLETE | COMPLETE conservée ; nouveau brouillon depuis sources fiables testé en H2 ; création réseau/MySQL restante |
| INV-02 Modification | COMPLETE | COMPLETE renforcée : émission/archive/rapprochement protégés ; concurrence MySQL et procédure de correction restantes |
| INV-03 Modèles personnalisables | ABSENTE | COMPLETE dans le contrat structuré texte/logo/deux présentations, sélection/prévisualisation et capture ; recette visuelle métier restante |
| INV-04 Numérotation automatique | COMPLETE | COMPLETE conservée ; même séquence dans les transactions H2, rollback vérifié ; concurrence/migration MySQL restantes |
| INV-05 Devis | COMPLETE | COMPLETE conservée sans refonte ; parent InvoiceLine renseigné, specs Angular incluses ; conversion réelle à recetter |
| INV-06 Paiements | COMPLETE | COMPLETE conservée ; financier/propriété réexécutés, préparations invalidées au règlement ; concurrence réseau/MySQL restante |
| INV-07 Relances | PARTIELLE | PARTIELLE : échéances, politique, file dédupliquée et journal manuel développés ; aucun canal d’envoi configuré ni délivrabilité validée |
| INV-08 PDF | COMPLETE | COMPLETE enrichie : modèle, logo et document figé, propriété avant lecture ; anciens PDF conservés ; Gateway/recette visuelle restants |
| INV-09 Historique | COMPLETE | COMPLETE renforcée : documents, paiements, préparations et opérations conservés ; pas de versionnement légal général revendiqué |
| INV-10 Archivage | ABSENTE | COMPLETE dans le périmètre des factures effectivement soldées : action/filtres/consultation CLIENT et conservation H2 ; recette intégrée restante |

Le rapprochement complète le sous-ensemble sûr de FEE-06, sans transformer les rapports techniques Fees en journal comptable ni résoudre les historiques manuels. Il ne constitue pas une conformité fiscale/comptable.

## 15. Risques et décisions restantes

Faire accepter les deux présentations, les textes destinés aux clients, les seuils de logo/document, l’émission explicite et la différence entre facture historique et nouvelle capture. Définir les avoirs, corrections, annulations, changement de destinataire et traitement d’un brouillon rapproché devenu inutile ; aucun déverrouillage destructif n’est proposé.

L’archivage ne couvre pas les impayés, litiges ou annulations non contractualisées ; aucun désarchivage n’est ajouté. Les historiques PAID incohérents avec le registre demandent une réconciliation contrôlée. Les factures sans échéance restent accessibles par la liste historique mais ne figurent pas dans une période d’échéance et ne déclenchent pas de préparation.

Le canal, le calendrier et les créneaux UTC sont techniques, sans règle légale. L’envoi effectif, les destinataires/adresses, consentements éventuels, modèles de messages et preuve de livraison nécessitent un contrat et un transport configurés. Une modification de politique annule les préparations encore ouvertes ; les traitements concurrents restent à éprouver sur MySQL.

Le registre de rapprochement est autoritaire dans Invoice, sans copie de statut dans Fees. Un client ne doit pas inférer une facture depuis les anciens champs libres de Fees. Les honoraires manuels/historiques restent exclus et non rapprochés automatiquement. L’appelant doit examiner l’état NEEDS_ATTENTION même sur réponse HTTP 200. Les collisions simultanées de premières réservations peuvent renvoyer un conflit ; la reprise ne doit pas créer un nouvel UUID pour le même contenu par simple réflexe.

Vérifier les clients externes de DELETE, des DTO enrichis et du nouveau protocole de reprise. Les anciens écrans à identifiants numériques conservent leurs limites JavaScript. Mesurer plans de requête, lectures de solde par ligne, taille du stockage PDF, pagination et délais distants. Aucun mécanisme général de purge n’est créé.

## 16. Prochaines étapes

Après autorisation opérationnelle et précontrôles positifs, préparer une nouvelle campagne isolée selon le [Lot 2B-R](LOT_2B_R_VALIDATION_PROGRESSIVE.md), avec les 28 migrations et les artefacts courants. Les manifestes antérieurs à 3G ne prouvent pas ce candidat.

1. Vérifier schéma neuf puis upgrade sur copie sauvegardée/autorisée ; conservation des factures/paiements, types Hibernate, paramètres, index, contraintes et stockage binaire.
2. Tester quatre rôles et deux clients réels associés : refus croisés, archives, PDF figés, révocation et no-store via Gateway/HTTPS.
3. Exercer simultanément paiements, émission, archive et relances ; séquence, double créneau, modification de politique et traitement tardif, sans surpaiement ni mutation de document.
4. Tester deux clients/devises et plusieurs sources Fees : erreurs réelles, réponses perdues, UUID identiques/différents, réservations concurrentes, crash entre phases, reprise/libération et un seul brouillon par source.
5. Réconcilier montants et lignes avec un jeu connu, devis converti, historiques exclus, impression visuelle des PDF et parcours Angular connecté sur mobile/clavier.

Preuves expurgées : `target/lot3g/results-sanitized.json`, `inventory.json` et `migrations.json`. Logs bruts et build restent ignorés par Git. Aucun déploiement, certification du CRM complet ou conformité légale déclaré.

**LOT 3G PARTIEL.**
