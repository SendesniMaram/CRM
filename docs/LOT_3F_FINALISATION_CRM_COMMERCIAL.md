# Lot 3F — Finalisation fonctionnelle du CRM commercial et des contacts

Date : 7 novembre 2026. Branche conservée : `publication-clean`.

## 1. Résumé exécutif

**LOT 3F PARTIEL — archivage, recherche combinée, import XLSX, contacts affectés EMPLOYEE et demandes/échanges CLIENT développés. 168 tests Java locaux et 1 230 tests Angular réussis ; build de production réussi avec avertissements. Intégration réelle NON VALIDÉE.**

AN-02 est traitée par un archivage réversible : le contact, ses interactions, ses opportunités et ses associations sont conservés. AN-03 est corrigée pour Contacts : Angular transmet maintenant assignedEmployeeId avec les autres filtres. Un lecteur XLSX borné réutilise la validation CSV et refuse les formules et macros.

EMPLOYEE consulte ses seuls contacts actifs affectés, modifie uniquement leurs coordonnées autorisées et journalise ses interactions. CLIENT crée des demandes, suit leurs statuts et échange avec les administrateurs dans un espace partagé. Aucun moteur de gestion de projet n’est inventé : cette partie du CDC demeure à définir.

Aucun Docker, MySQL, Flyway, service CRM ou Gateway réel lancé. Un serveur HTTP Identity synthétique est utilisé par les tranches MVC existantes et nouvelles ; il ne constitue pas une intégration CRM. Aucun secret modifié, staging, commit, push, changement de branche ou suppression de données préexistantes.

## 2. Fonctionnalités développées

Les [rapports 3A](LOT_3A_AUDIT_CONFORMITE_FONCTIONNELLE.md), [3E](LOT_3E_FINALISATION_HONORAIRES.md), [3D](LOT_3D_STABILISATION_INTEGREE.md), [1C](LOT_1C_ASSOCIATIONS_ESPACES_PERSONNELS.md) et le [CDC CRM](CDC%20CRM%201.pdf), via son extraction locale conservée, ont été consultés. Inspection de Customer/Identity/Employee/common-security, de l’association HR personnelle existante et des routes, composants, services et tests Angular associés.

Plan suivi : préserver les sources ; corriger l’intégrité et les filtres ; ajouter l’import ; ouvrir des API personnelles distinctes et leurs écrans ; tester localement puis documenter les limites. Les règles UID/sid et les bases isolées des microservices sont conservées.

- **A — Intégrité :** DELETE sur un contact archive sans effacer ; restauration explicite ; fiche archivée en lecture seule ; historique commercial consultable.
- **B — Recherche :** mot-clé sur nom/e-mail/société/téléphone, type, catégorie, segment, état actif/archivé et employé affecté, combinés en base avec pagination et tri autorisé.
- **C — XLSX :** sélection puis confirmation Angular, import des valeurs d’une feuille, validation commune CSV, références Employee vérifiées et rapport des lignes acceptées/rejetées.
- **D — EMPLOYEE :** association UID→HR→employé résolue côté serveur ; liste/recherche/détail propres, coordonnées limitées, journal personnel visible explicitement et paginé.
- **E — CLIENT :** association UID→Customer existante, création idempotente de demande, liste par statut, échange client/équipe, réponses administratives, résolution/réouverture et historique.

Le CDC p.5 prévoit gestion des contacts affectés, suivi des demandes/projets et communication avec l’équipe ; il ne définit ni planification de projets, ni SLA, ni canal SMTP. Le lot réalise un suivi simple de demandes et un échange dans l’application, sans les présenter comme un système complet de projets.

## 3. Fichiers modifiés et créés

Inventaire relatif à la photographie de début de 3F, et non au HEAD déjà modifié : **37 fichiers préexistants modifiés et 34 nouveaux fichiers, rapport compris**.

### Fichiers préexistants modifiés

- [frontend/src/app/app.routes.ts](../frontend/src/app/app.routes.ts)
- [frontend/src/app/core/navigation/crm-navigation.ts](../frontend/src/app/core/navigation/crm-navigation.ts)
- [frontend/src/app/core/services/auth-navigation.ts](../frontend/src/app/core/services/auth-navigation.ts)
- [frontend/src/app/features/client/client-workspace.component.html](../frontend/src/app/features/client/client-workspace.component.html)
- [frontend/src/app/features/customers/customer-detail.component.html](../frontend/src/app/features/customers/customer-detail.component.html)
- [frontend/src/app/features/customers/customer-detail.component.ts](../frontend/src/app/features/customers/customer-detail.component.ts)
- [frontend/src/app/features/customers/customer-form.component.html](../frontend/src/app/features/customers/customer-form.component.html)
- [frontend/src/app/features/customers/customer-form.component.spec.ts](../frontend/src/app/features/customers/customer-form.component.spec.ts)
- [frontend/src/app/features/customers/customer-form.component.ts](../frontend/src/app/features/customers/customer-form.component.ts)
- [frontend/src/app/features/customers/customer.models.ts](../frontend/src/app/features/customers/customer.models.ts)
- [frontend/src/app/features/customers/customer.service.spec.ts](../frontend/src/app/features/customers/customer.service.spec.ts)
- [frontend/src/app/features/customers/customer.service.ts](../frontend/src/app/features/customers/customer.service.ts)
- [frontend/src/app/features/customers/customers-page.component.html](../frontend/src/app/features/customers/customers-page.component.html)
- [frontend/src/app/features/customers/customers-page.component.spec.ts](../frontend/src/app/features/customers/customers-page.component.spec.ts)
- [frontend/src/app/features/customers/customers-page.component.ts](../frontend/src/app/features/customers/customers-page.component.ts)
- [frontend/src/app/features/customers/customers.routes.ts](../frontend/src/app/features/customers/customers.routes.ts)
- [frontend/src/app/features/home/home.component.spec.ts](../frontend/src/app/features/home/home.component.spec.ts)
- [microservices/customer-service/src/main/java/com/crm/customer/client/CustomerEmployeeDirectory.java](../microservices/customer-service/src/main/java/com/crm/customer/client/CustomerEmployeeDirectory.java)
- [microservices/customer-service/src/main/java/com/crm/customer/config/SecurityConfig.java](../microservices/customer-service/src/main/java/com/crm/customer/config/SecurityConfig.java)
- [microservices/customer-service/src/main/java/com/crm/customer/controller/CustomerController.java](../microservices/customer-service/src/main/java/com/crm/customer/controller/CustomerController.java)
- [microservices/customer-service/src/main/java/com/crm/customer/controller/SalesController.java](../microservices/customer-service/src/main/java/com/crm/customer/controller/SalesController.java)
- [microservices/customer-service/src/main/java/com/crm/customer/dto/CustomerRequest.java](../microservices/customer-service/src/main/java/com/crm/customer/dto/CustomerRequest.java)
- [microservices/customer-service/src/main/java/com/crm/customer/dto/CustomerResponse.java](../microservices/customer-service/src/main/java/com/crm/customer/dto/CustomerResponse.java)
- [microservices/customer-service/src/main/java/com/crm/customer/entity/Customer.java](../microservices/customer-service/src/main/java/com/crm/customer/entity/Customer.java)
- [microservices/customer-service/src/main/java/com/crm/customer/entity/CustomerInteraction.java](../microservices/customer-service/src/main/java/com/crm/customer/entity/CustomerInteraction.java)
- [microservices/customer-service/src/main/java/com/crm/customer/exception/GlobalExceptionHandler.java](../microservices/customer-service/src/main/java/com/crm/customer/exception/GlobalExceptionHandler.java)
- [microservices/customer-service/src/main/java/com/crm/customer/mapper/CustomerMapper.java](../microservices/customer-service/src/main/java/com/crm/customer/mapper/CustomerMapper.java)
- [microservices/customer-service/src/main/java/com/crm/customer/repository/CustomerInteractionRepository.java](../microservices/customer-service/src/main/java/com/crm/customer/repository/CustomerInteractionRepository.java)
- [microservices/customer-service/src/main/java/com/crm/customer/repository/CustomerRepository.java](../microservices/customer-service/src/main/java/com/crm/customer/repository/CustomerRepository.java)
- [microservices/customer-service/src/main/java/com/crm/customer/service/CustomerCsvService.java](../microservices/customer-service/src/main/java/com/crm/customer/service/CustomerCsvService.java)
- [microservices/customer-service/src/main/java/com/crm/customer/service/ICustomerService.java](../microservices/customer-service/src/main/java/com/crm/customer/service/ICustomerService.java)
- [microservices/customer-service/src/main/java/com/crm/customer/service/impl/CustomerServiceImpl.java](../microservices/customer-service/src/main/java/com/crm/customer/service/impl/CustomerServiceImpl.java)
- [microservices/customer-service/src/main/java/com/crm/customer/util/CustomerSpecification.java](../microservices/customer-service/src/main/java/com/crm/customer/util/CustomerSpecification.java)
- [microservices/customer-service/src/main/resources/application.yml](../microservices/customer-service/src/main/resources/application.yml)
- [microservices/customer-service/src/test/java/com/crm/customer/CustomerReadSecurityTest.java](../microservices/customer-service/src/test/java/com/crm/customer/CustomerReadSecurityTest.java)
- [microservices/customer-service/src/test/java/com/crm/customer/CustomerServiceIntegrationTest.java](../microservices/customer-service/src/test/java/com/crm/customer/CustomerServiceIntegrationTest.java)
- [microservices/gateway-service/src/main/resources/application.yml](../microservices/gateway-service/src/main/resources/application.yml)

### Nouveaux fichiers

- [frontend/src/app/features/customers/assigned-contacts.component.html](../frontend/src/app/features/customers/assigned-contacts.component.html)
- [frontend/src/app/features/customers/assigned-contacts.component.spec.ts](../frontend/src/app/features/customers/assigned-contacts.component.spec.ts)
- [frontend/src/app/features/customers/assigned-contacts.component.ts](../frontend/src/app/features/customers/assigned-contacts.component.ts)
- [frontend/src/app/features/customers/client-requests.component.html](../frontend/src/app/features/customers/client-requests.component.html)
- [frontend/src/app/features/customers/client-requests.component.spec.ts](../frontend/src/app/features/customers/client-requests.component.spec.ts)
- [frontend/src/app/features/customers/client-requests.component.ts](../frontend/src/app/features/customers/client-requests.component.ts)
- [frontend/src/app/features/customers/commercial-routes.spec.ts](../frontend/src/app/features/customers/commercial-routes.spec.ts)
- [frontend/src/app/features/customers/commercial-work.scss](../frontend/src/app/features/customers/commercial-work.scss)
- [frontend/src/app/features/customers/contact-work.service.spec.ts](../frontend/src/app/features/customers/contact-work.service.spec.ts)
- [frontend/src/app/features/customers/contact-work.service.ts](../frontend/src/app/features/customers/contact-work.service.ts)
- [microservices/customer-service/src/main/java/com/crm/customer/client/AssignedEmployeeDirectory.java](../microservices/customer-service/src/main/java/com/crm/customer/client/AssignedEmployeeDirectory.java)
- [microservices/customer-service/src/main/java/com/crm/customer/controller/ContactWorkController.java](../microservices/customer-service/src/main/java/com/crm/customer/controller/ContactWorkController.java)
- [microservices/customer-service/src/main/java/com/crm/customer/entity/ClientCase.java](../microservices/customer-service/src/main/java/com/crm/customer/entity/ClientCase.java)
- [microservices/customer-service/src/main/java/com/crm/customer/entity/ClientCaseEvent.java](../microservices/customer-service/src/main/java/com/crm/customer/entity/ClientCaseEvent.java)
- [microservices/customer-service/src/main/java/com/crm/customer/entity/ClientCaseMessage.java](../microservices/customer-service/src/main/java/com/crm/customer/entity/ClientCaseMessage.java)
- [microservices/customer-service/src/main/java/com/crm/customer/repository/ClientCaseEventRepository.java](../microservices/customer-service/src/main/java/com/crm/customer/repository/ClientCaseEventRepository.java)
- [microservices/customer-service/src/main/java/com/crm/customer/repository/ClientCaseMessageRepository.java](../microservices/customer-service/src/main/java/com/crm/customer/repository/ClientCaseMessageRepository.java)
- [microservices/customer-service/src/main/java/com/crm/customer/repository/ClientCaseRepository.java](../microservices/customer-service/src/main/java/com/crm/customer/repository/ClientCaseRepository.java)
- [microservices/customer-service/src/main/java/com/crm/customer/service/AssignedContactService.java](../microservices/customer-service/src/main/java/com/crm/customer/service/AssignedContactService.java)
- [microservices/customer-service/src/main/java/com/crm/customer/service/ClientCaseService.java](../microservices/customer-service/src/main/java/com/crm/customer/service/ClientCaseService.java)
- [microservices/customer-service/src/main/java/com/crm/customer/service/ContactAccess.java](../microservices/customer-service/src/main/java/com/crm/customer/service/ContactAccess.java)
- [microservices/customer-service/src/main/java/com/crm/customer/service/CustomerXlsxService.java](../microservices/customer-service/src/main/java/com/crm/customer/service/CustomerXlsxService.java)
- [microservices/customer-service/src/main/resources/db/migration/V20261105_02__commercial_archive_and_client_cases.sql](../microservices/customer-service/src/main/resources/db/migration/V20261105_02__commercial_archive_and_client_cases.sql)
- [microservices/customer-service/src/test/java/com/crm/customer/AssignedContactPolicyTest.java](../microservices/customer-service/src/test/java/com/crm/customer/AssignedContactPolicyTest.java)
- [microservices/customer-service/src/test/java/com/crm/customer/AssignedEmployeeDirectoryTest.java](../microservices/customer-service/src/test/java/com/crm/customer/AssignedEmployeeDirectoryTest.java)
- [microservices/customer-service/src/test/java/com/crm/customer/ClientCasePolicyTest.java](../microservices/customer-service/src/test/java/com/crm/customer/ClientCasePolicyTest.java)
- [microservices/customer-service/src/test/java/com/crm/customer/CommercialIntegrityPolicyTest.java](../microservices/customer-service/src/test/java/com/crm/customer/CommercialIntegrityPolicyTest.java)
- [microservices/customer-service/src/test/java/com/crm/customer/CommercialJpaFixtureTest.java](../microservices/customer-service/src/test/java/com/crm/customer/CommercialJpaFixtureTest.java)
- [microservices/customer-service/src/test/java/com/crm/customer/CommercialSecurityTest.java](../microservices/customer-service/src/test/java/com/crm/customer/CommercialSecurityTest.java)
- [microservices/customer-service/src/test/java/com/crm/customer/CommercialUploadConfigurationTest.java](../microservices/customer-service/src/test/java/com/crm/customer/CommercialUploadConfigurationTest.java)
- [microservices/customer-service/src/test/java/com/crm/customer/CustomerXlsxHttpTest.java](../microservices/customer-service/src/test/java/com/crm/customer/CustomerXlsxHttpTest.java)
- [microservices/customer-service/src/test/java/com/crm/customer/CustomerXlsxImportTest.java](../microservices/customer-service/src/test/java/com/crm/customer/CustomerXlsxImportTest.java)
- [microservices/gateway-service/src/test/java/com/crm/gateway/CommercialRouteConfigurationTest.java](../microservices/gateway-service/src/test/java/com/crm/gateway/CommercialRouteConfigurationTest.java)
- [docs/LOT_3F_FINALISATION_CRM_COMMERCIAL.md](LOT_3F_FINALISATION_CRM_COMMERCIAL.md) — ce rapport.

La photographie SHA-256 porte sur **1 052 fichiers préexistants suivis/non suivis**, hors secrets et clés exclus de la lecture. **Aucune disparition ; 1015 fichiers photographiés inchangés.** Les 612 fichiers protégés contrôlés — sources Paie/RH/Honoraires/Facturation, Identity/Employee/common-security, rapports antérieurs et scripts de validation — sont inchangés.

Les changements partagés sont limités aux routes, à la navigation, au lien dans l’espace CLIENT et à une route Gateway explicite. Le spec Home du Lot 3D conserve ses assertions exactes par rôle et ajoute seulement /my-contacts à EMPLOYEE ; aucune permission ancienne n’est retirée ou élargie.

Les 26 migrations historiques sont préservées par empreinte. Les preuves, logs et inventaires sont sous `target/lot3f`, ignoré par Git. Aucun nettoyage ou accès à un secret réel.

## 4. Endpoints REST

Préfixe Gateway : `/customer`. Chemins ci-dessous locaux au service Customer.

| Endpoint | Accès et contrat |
|---|---|
| GET /api/customers | ADMIN/SUPER_ADMIN ; paramètres page/size/sortBy/direction/keyword/contactType/category/segment/assignedEmployeeId/status |
| DELETE /api/customers/{id} | ADMIN/SUPER_ADMIN ; archive de façon idempotente, 204, aucune suppression physique |
| PATCH /api/customers/{id}/restore | ADMIN/SUPER_ADMIN ; restaure sans changer les références ni l’historique |
| POST /api/customers/import.xlsx | ADMIN/SUPER_ADMIN ; multipart file, bearer pour les références, rapport par ligne |
| GET /api/customers/export et /export.xlsx | Exports conservés, mêmes filtres d’affectation et d’archive, limite explicite de 10 000 contacts |
| GET /api/contact-work/assigned | EMPLOYEE ; keyword/page, employé résolu via association HR personnelle |
| GET /api/contact-work/assigned/{id} | EMPLOYEE ; contact actif toujours affecté à l’employé authentifié, DTO réduit |
| PUT /api/contact-work/assigned/{id} | EMPLOYEE ; uniquement email, phone, address |
| GET /api/contact-work/assigned/{id}/interactions | EMPLOYEE ; page de ses interactions explicitement visibles |
| POST /api/contact-work/assigned/{id}/interactions | EMPLOYEE ; type, date, compte rendu ; employeeId imposé côté serveur |
| GET /api/contact-work/client/cases | CLIENT ; ses demandes, filtre status et page |
| POST /api/contact-work/client/cases | CLIENT ; requestId UUID, subject, description, client imposé côté serveur |
| GET /api/contact-work/client/cases/{id} | CLIENT ; détail propre |
| GET/POST /api/contact-work/client/cases/{id}/messages | CLIENT ; échanges propres paginés / message avec UUID |
| GET /api/contact-work/client/cases/{id}/history | CLIENT ; historique de statuts propre et paginé |
| GET /api/contact-work/admin/cases et /cases/{id} | ADMIN/SUPER_ADMIN ; demandes et détails administratifs |
| GET/POST /api/contact-work/admin/cases/{id}/messages | ADMIN/SUPER_ADMIN ; consultation et réponse partagée |
| GET /api/contact-work/admin/cases/{id}/history | ADMIN/SUPER_ADMIN ; historique paginé |
| PATCH /api/contact-work/admin/cases/{id}/status | ADMIN/SUPER_ADMIN ; OPEN, IN_PROGRESS ou RESOLVED |

Les réponses réussies du nouvel espace imposent `Cache-Control: no-store`. Ses pages ont 20 lignes ; les nouveaux identifiants sont exposés en texte. Les paramètres de ciblage employeeId/customerId/userId sont refusés dans ce namespace personnel ; un ID de ressource reste soumis au contrôle de propriété.

Le statut des filtres Contacts signifie **ACTIVE ou ARCHIVED**, dérivé de l’archive ; aucun nouvel état commercial arbitraire n’est ajouté. Les champs de tri sont en liste blanche, size entre 1 et 100, recherche limitée à 200 caractères et caractères %/_ traités littéralement.

Compatibilité : le GET sans paramètres peut encore retourner le tableau historique, mais seulement jusqu’à 100 contacts ; au-delà, erreur demandant une pagination explicite, sans troncature. Angular utilise toujours les paramètres paginés. Les erreurs 400/401/403/404/409/413/502/503 sont expurgées selon entrée, autorisation, conflit, taille ou disponibilité.

La route Gateway ajoutée cible uniquement `/customer/api/contact-work/**` vers CUSTOMER-SERVICE avec StripPrefix=1. La configuration et les protections de chemins internes sont testées localement ; aucun routage HTTP réel n’est revendiqué.

## 5. Interfaces Angular

La liste Contacts offre les filtres combinés, l’état archivé, la restauration et l’accès aux demandes clients administratives. La confirmation de suppression est remplacée par une confirmation d’archivage précisant la conservation de l’historique. Les fiches archivées sont identifiées, leur édition est bloquée et leurs interactions restent consultables.

Le formulaire conserve l’employé déjà affecté au lieu de l’effacer lors d’une édition. Une nouvelle affectation est saisissable avec vérification serveur. Les IDs numériques non sûrs en JavaScript sont refusés dans l’ancien formulaire plutôt qu’arrondis. Le filtre assignedEmployeeId est transmis comme texte, sans coercition.

Pour XLSX, le fichier sélectionné et les contraintes sont montrés avant le bouton **Confirmer l’import XLSX**. L’import crée les lignes valides et rapporte les rejets ; ce n’est pas une transaction globale de remplacement. Un refus ou une réponse perdue n’est pas présenté comme un import nul garanti. Aucun nouveau package frontend ou moteur Excel.

`/my-contacts` est une route EMPLOYEE lazy et protégée, accessible dans le menu et l’accueil. Elle propose recherche et pagination serveur, détail, coordonnées modifiables limitées, journal personnel et ajout de compte rendu. Les notes des administrateurs et anciennes interactions ne sont pas ouvertes automatiquement.

`/client/requests` est relié depuis l’espace CLIENT. `/customers/requests` est réservé aux administrateurs et relié depuis Contacts. Les vues proposent filtre de statut, pages, création, conversation, réponses et historique. Les identités techniques d’auteurs ne sont pas exposées ; les réponses sont étiquetées Client ou Équipe.

Les formulaires sont simples et réutilisent Angular Forms et les composants existants ; tableaux défilants et styles flexibles. États de chargement, vide, erreur et accès sont testés sur composants rendus. Les réponses tardives des nouveaux espaces sont ignorées lors d’un changement de session ; données et brouillons sont effacés. Le même contenu conserve son UUID après erreur de création de demande ou de message. Aucun navigateur connecté ou audit mobile réel exécuté.

## 6. Modèles et migrations

Nouvelle migration additive : [V20261105_02__commercial_archive_and_client_cases.sql](../microservices/customer-service/src/main/resources/db/migration/V20261105_02__commercial_archive_and_client_cases.sql). **Préparée, NON EXÉCUTÉE.** Le dépôt compte 27 migrations, dont 26 historiques inchangées.

| Modèle | Ajout et conservation |
|---|---|
| Customer | archived obligatoire, faux par défaut ; index affectation/archive/id ; aucun montant ou historique réécrit |
| CustomerInteraction | employee_visible faux par défaut ; index contact/employé/visibilité/id ; les nouvelles interactions personnelles le positionnent explicitement à vrai |
| ClientCase | Client propriétaire, UUID de création unique, sujet, description, statut et date de création |
| ClientCaseMessage | Demande parente, UUID unique, contenu immuable, origine CLIENT/TEAM, auteur UID et date |
| ClientCaseEvent | Demande parente, nouveau statut, auteur UID et date ; historique sans endpoint de suppression |

Les nouvelles demandes référencent customers, les messages et événements référencent leur demande. Aucun lien SQL interservices. Les anciennes tables interactions/opportunités ne reçoivent pas de FK rétroactive susceptible d’échouer sur d’éventuels orphelins historiques : leur rapprochement reste à décider sans effacement.

Les mutations d’archive, de fiche, d’interaction et de rattachement d’opportunité utilisent le verrou du contact lorsque pertinent. Les créations de demandes sont sérialisées sur le contact ; messages et transitions prennent le verrou de la demande et un rafraîchissement sous verrou. Les contraintes UUID sont une protection supplémentaire préparée. Les mocks ne démontrent ni concurrence SQL ni rollback.

Contrôles statiques : version nouvelle et distincte, tables parentes créées avant les enfants, IDs BIGINT, colonnes/longueurs, contraintes uniques, FK et index. Aucun DROP, UPDATE de données, clean, baseline ou repair. Pas d’exécution de la migration dans les fixtures. La configuration multipart est explicitement alignée à 5 Mio par fichier et 6 Mio par requête.

## 7. Sécurité et autorisations

Les namespaces administratifs et personnels restent distincts : aucun droit EMPLOYEE sur le CRUD Customer global, aucune lecture financière ou commerciale interne ouverte à CLIENT. Les nouveaux services ajoutent des contrôles de rôle et de principal UID typé. JWT, validation Identity, sid/révocation et common-security ne sont pas modifiés.

EMPLOYEE est résolu depuis `/api/employee-associations/me` du service HR, avec le bearer validé de l’appelant. userId doit être le texte canonique correspondant au principal et employeeId doit être un entier canonique positif représentable en Long. Réponse invalide, absence d’association ou indisponibilité ferme l’accès ; aucun fallback username/e-mail ou employeeId Angular.

Chaque détail, modification ou interaction vérifie à nouveau l’affectation et l’absence d’archive. Seuls e-mail, téléphone et adresse sont modifiables. Les interactions personnelles sont filtrées par contact, employé et employee_visible=true. Les historiques administratifs antérieurs restent privés par défaut, même s’ils mentionnent cet employé.

CLIENT est résolu exclusivement par CustomerPersonalService et l’association User–Customer existante. Une demande d’un autre client retourne 404 avant lecture des messages/historique ou écriture. Les DTO personnels n’exposent ni UID d’auteur ni classifications internes ; les messages sont volontairement partagés avec le client et l’équipe, jamais des notes internes.

Les créations de demandes et messages sont idempotentes par UUID canonique et contenu ; un UUID réutilisé avec un autre contenu/propriétaire est refusé. Les demandes OPEN/IN_PROGRESS acceptent les messages ; RESOLVED les bloque. Seuls les administrateurs changent le statut ou rouvrent une demande, avec événement conservé. Ce suivi technique n’implique aucun SLA ou moteur de projet.

Le lecteur XLSX accepte au plus **5 Mio compressés, 10 Mio décompressés, 100 entrées ZIP, une feuille, 2 000 lignes de données, neuf colonnes et 5 000 caractères par valeur** ; chaînes partagées bornées. Pas d’extraction sur disque, de moteur d’exécution, de DTD/entités externes ou de suivi de liens externes ; macros/objets incorporés et formules sont refusés. Le classeur est vérifié avant toute création de ligne.

Colonnes et validations sont celles du CSV : name requis, contrôles des coordonnées/types, doublons existants refusés, employés nouvellement affectés vérifiés. Les erreurs de référence deviennent des rejets de ligne expurgés. Les doublons concurrents de nom ne sont pas garantis par une contrainte nouvelle ; les contraintes historiques e-mail/téléphone et leur collation doivent être vérifiées sur MySQL.

Les redirections des clients de référence sont refusées et les délais réseau sont bornés. Les exports conservent leurs protections contre les formules. Aucune donnée réelle ni credential utilisé pendant les tests.

## 8. Tests Java

Exécutions **séquentielles, hors ligne**, cache Maven local, MAVEN_OPTS -Xmx512m et JVM de tests -Xmx384m. Aucun build Java et Angular simultané.

| Suite | Cas réussis |
|---|---:|
| AssignedContactPolicyTest | 9 |
| AssignedEmployeeDirectoryTest | 4 |
| ClientCasePolicyTest | 12 |
| CommercialIntegrityPolicyTest | 7 |
| CommercialJpaFixtureTest | 5 |
| CommercialSecurityTest | 12 |
| CommercialUploadConfigurationTest | 1 |
| CustomerAssociationCreationPolicyTest | 9 |
| CustomerCsvServiceTest | 4 |
| CustomerPersonalPolicyTest | 8 |
| CustomerReadSecurityTest | 34 |
| CustomerXlsxHttpTest | 6 |
| CustomerXlsxImportTest | 9 |
| SqlErrorPrivacyTest | 1 |
| **Total Customer distinct** | **121** |
| CommercialRouteConfigurationTest — Gateway | 1 |
| InternalPathPolicyTest — Gateway | 46 |
| **Total Java distinct** | **168** |

Les tests de politique utilisent dépôts et annuaires simulés ; ils couvrent archivage, absence d’effacement, coordonnées autorisées, contrôles croisés, pages, erreurs, refus de rôle, idempotence et transitions. Les tranches MVC utilisent les filtres réels : CommercialSecurityTest simule le résultat Identity ; CustomerReadSecurityTest et CustomerXlsxHttpTest utilisent un Identity HTTP synthétique local avec le parseur/filtre de production.

Les cinq fixtures **Hibernate/H2** créent de petites tables en mémoire depuis les entités, sans SpringBootTest ni Flyway. Elles exercent les vraies Specifications JPA combinées avec pagination, les jokers littéraux, l’archive conservant interaction/opportunité/historique et la requête JPQL de demandes avec propriétaire/statut, dont les paramètres null de la portée administrative. Ce sont des preuves H2, **pas** du SQL/MySQL ou des migrations réelles.

L’import est testé sur des ZIP/XLSX synthétiques, chaînes inline/partagées, types numériques simples, trous de cellules, colonnes invalides, doublons, références refusées, formules, macros, liens externes, DTD et limites. L’API d’import vérifie réellement le raccordement au validateur Employee simulé avant création. Les contrôles Gateway sont unitaires/statiques ; aucun serveur Gateway démarré.

Commandes finales :

    # MAVEN_OPTS=-Xmx512m
    mvn -o -B -ntp -Dmaven.repo.local=C:/Users/ASUS/.m2/repository -f microservices/customer-service/pom.xml -Dtest=CommercialIntegrityPolicyTest,AssignedContactPolicyTest,ClientCasePolicyTest,CustomerXlsxImportTest,CustomerXlsxHttpTest,AssignedEmployeeDirectoryTest,CommercialJpaFixtureTest,CommercialSecurityTest,CommercialUploadConfigurationTest,CustomerCsvServiceTest,CustomerReadSecurityTest,CustomerPersonalPolicyTest,CustomerAssociationCreationPolicyTest,SqlErrorPrivacyTest -DargLine=-Xmx384m test
    mvn -o -B -ntp -Dmaven.repo.local=C:/Users/ASUS/.m2/repository -f microservices/gateway-service/pom.xml -Dtest=CommercialRouteConfigurationTest,InternalPathPolicyTest -DargLine=-Xmx384m test

Les relances ne s’additionnent pas : 113, 119 et 120 ont précédé la sélection Customer finale de 121. Les résultats historiques des Lots 3B–3E ne sont pas ajoutés à 168. CustomerServiceIntegrationTest a été adapté à l’archivage et compilé, mais **pas exécuté**. Les suites SpringBootTest avec base ne sont pas lancées.

## 9. Tests Angular

**1 230/1 230 tests réussis**, ChromeHeadless 156, sélection complète `src/app/**/*.spec.ts`. La sélection ciblée finale de 153 cas Contacts/Client/navigation/accueil est incluse dans ce total, sans addition. Node est limité à 1 536 Mio, deux workers.

Les tests couvrent transmission des filtres combinés et des exports, conservation de l’affectation en édition, archives/restauration, import XLSX avec confirmation/rapport/erreurs, HTTP personnels sans ID de ciblage, recherche/pagination, coordonnées, messages et historique, reprise UUID, états vides/erreurs et réponses tardives après changement de session. Les nouvelles routes sont exercées avec les vrais guards pour les quatre rôles, sans appel métier réel.

Les specs des Lots 3B/3C/3E sont incluses et leurs sources inchangées. L’attente exacte de Home est mise à jour pour le seul lien EMPLOYEE ajouté. Les services HTTP sont simulés : aucun parcours connecté validé.

**Build production réussi**, bundle initial **889,27 kB**, sous le seuil bloquant existant de 1 MB. Avertissements conservés : budget indicatif initial de 500 kB, styles Payroll/Invoices et NG8011 de projection d’icônes dans CustomersPageComponent. Aucun budget ou source Paie/Facturation modifié pour les masquer.

    # Depuis frontend : CHROME_BIN configuré, NODE_OPTIONS=--max-old-space-size=1536, NG_BUILD_MAX_WORKERS=2
    .\node_modules\.bin\ng.cmd test --no-watch --browsers=ChromeHeadless --karma-config=karma.ci.conf.cjs --include=src/app/**/*.spec.ts
    .\node_modules\.bin\ng.cmd build --configuration production --output-path ../target/lot3f/frontend-production

Échecs intermédiaires corrigés : paramètre status manquant dans deux signatures d’export ; matcher Mockito delete ambigu ; anciennes attentes de texte « suppression » après passage à l’archive ; ordre du nouveau lien de navigation pour conserver le regroupement. Les remplacements tenant compte des fins de ligne Windows ont aussi été vérifiés pour la limite multipart et l’ancien test de suppression. Aucun échec résiduel dans les sélections finales.

## 10. Anomalies corrigées

- **AN-02 :** suppression physique Customer remplacée par archive/restauration ; préservation des références et historiques, refus des nouvelles interactions/rattachements commerciaux sur une fiche archivée.
- **AN-03 Contacts :** assignedEmployeeId transmis et combiné dans liste et exports Angular ; recherches bornées et tri validé.
- **Affectation effacée à l’édition :** l’ancien formulaire ne renvoyait pas l’employé affecté ; champ conservé et nouvelle affectation explicitement saisissable/vérifiée.
- **Import Excel absent :** lecteur de valeurs XLSX borné et raccordé aux mêmes validations que CSV, avec refus précoce des contenus actifs et confirmation Angular.
- **Espaces personnels absents :** APIs et écrans de contacts EMPLOYEE et demandes CLIENT avec contrôles serveur de propriété.
- **Entrées invalides :** paramètres/corps JSON illisibles et dépassement multipart traités en erreurs 400/413 expurgées.

Les orphelins éventuellement présents avant 3F ne sont pas supprimés ou réparés sans preuve. L’anomalie de filtres HR mentionnée également dans AN-03 reste hors périmètre ; seul le volet Contacts est corrigé.

## 11. Exigences du CDC actualisées

COMPLETE décrit le périmètre de code relié backend/Angular et testé localement, comme dans 3A ; **toute intégration réelle reste NON VALIDÉE**. Les matrices historiques ne sont pas réécrites.

| Exigence | Avant 3F | Après 3F et limite |
|---|---|---|
| CON-01 CRUD contacts | PARTIELLE | COMPLETE dans la politique d’archive/restauration ; suppression destructive non exposée |
| CON-06 Historique des interactions | COMPLETE | COMPLETE conservée ; archive sans effacement, historique personnel explicitement visible et limité |
| CON-09 Opportunités | COMPLETE | COMPLETE conservée ; opportunités et historique préservés lors de l’archive ; pas de refonte du pipeline |
| CON-15 Import Excel | ABSENTE | COMPLETE dans le contrat XLSX une feuille/valeurs/limites documentées ; MySQL et logiciels Excel réels à valider |
| CON-17 Recherche avancée | PARTIELLE | COMPLETE pour les combinaisons nom/e-mail/type/catégorie/segment/affectation/archive avec tri/page |
| CON-18 Filtres | PARTIELLE | COMPLETE pour les filtres exposés et transmis, dont assignedEmployeeId |
| TR-05 Contacts assignés EMPLOYEE | ABSENTE | COMPLETE pour consultation, coordonnées et journal personnel autorisé ; refus croisés testés |
| TR-06 Demandes/projets CLIENT | ABSENTE | PARTIELLE : demandes suivies réalisées ; gestion de projets non définie et non implémentée |
| TR-07 Communication CLIENT-équipe | ABSENTE | COMPLETE pour échanges partagés dans l’application, réponses et historique ; aucun SMTP revendiqué |

## 12. Limites et décisions restantes

**NON EXÉCUTÉS :** MySQL neuf/upgrade, Flyway, Hibernate validate sur schéma candidat, DDL/index/FK/collations réels, concurrence/rollback SQL, vrais services HR/Employee/Identity, Gateway en réseau, HTTPS, navigateur connecté et recette des quatre comptes, performances/charge et import depuis une installation Excel/LibreOffice réelle.

Les contrôles statiques et H2 ne prouvent pas la migration MySQL, les verrous concurrents, les délais interservices réels ni la cohérence au moment d’un changement d’affectation. Une requête autorisée peut terminer pendant un retrait de droits/affectation ; les lectures suivantes doivent être refusées. Les réponses anciennes ne sont pas réaffichées par les nouveaux écrans après changement de session.

Définir les projets, leurs participants, livrables et droits avant une extension de TR-06. Faire accepter le cycle technique OPEN/IN_PROGRESS/RESOLVED, la clôture bloquant les nouveaux messages et la réouverture administrative. Les échanges sont partagés ; aucune note interne confidentielle ni pièce jointe n’est proposée.

L’archive empêche de nouvelles demandes CLIENT pour ce contact, mais conserve ses anciennes demandes et leur consultation ; les échanges des demandes encore ouvertes restent possibles. Les associations User–Customer, factures et historiques existants sont conservés. La suppression physique et une purge éventuelle demandent une politique séparée.

L’import est synchrone et partiel par ligne, sans écrasement ni dry-run serveur. Une confirmation de fichier n’est pas une prévisualisation de chaque ligne. Une réponse perdue nécessite un contrôle des contacts avant reprise ; les doublons existants sont rejetés. La résolution d’employés pour de nombreuses lignes et les durées de traitement restent à mesurer ; aucun import en tâche de fond n’est inventé.

Seules les valeurs simples d’un classeur mono-feuille sont prises en charge ; fichiers chiffrés, .xls/.xlsm, formules, objets et liens externes sont refusés. Les colonnes restent celles du CSV existant. Les limites techniques et les restrictions d’édition sont explicites ; elles ne sont pas des règles légales.

Les nouveaux IDs restent textuels ; les anciens DTO Customer utilisent encore des nombres pour certains IDs. Le formulaire refuse les affectations non sûres en JavaScript et le filtre reste textuel. Les recherches combinées et exports doivent être observés sur volumétrie réelle. Les décisions métier et blocages de validation intégrée des rapports antérieurs demeurent ; RAM et Docker n’ont pas été remesurés.

## 13. Prochaines étapes

Après précontrôles positifs et autorisation opérationnelle, préparer une **nouvelle campagne isolée** selon le [Lot 2B-R](LOT_2B_R_VALIDATION_PROGRESSIVE.md), avec les migrations et artefacts courants. Les anciens manifestes ne prouvent pas le candidat 3F et ne doivent pas être réutilisés silencieusement.

1. Vérifier installation/upgrade MySQL sur copies autorisées, schéma, valeurs par défaut, index et conservation des historiques sans effacement.
2. Exercer quatre rôles et deux associations User–Employee/User–Customer ; recherche personnelle, refus croisés, erreurs HR/Employee/Identity et révocation réelle.
3. Tester archive/restauration en concurrence avec interaction, opportunité, édition et création de demande ; prouver absence d’effacement et comportement des transactions.
4. Importer un jeu XLSX/CSV connu dans l’environnement isolé, comparer lignes/doublons/rejets/références et tester les bornes, traitements interrompus et fichiers de logiciels réels.
5. Tester demandes/messages/statuts concurrents et reprises UUID, puis navigation, formulaires, erreurs et rendu mobile/clavier sur navigateur connecté.

Preuves expurgées : `target/lot3f/results-sanitized.json`, `inventory.json` et `migrations.json`. Les logs bruts et le build sont des auxiliaires ignorés par Git. Aucun CRM complet, déploiement ou conformité intégrée déclaré validé.

**LOT 3F PARTIEL.**
