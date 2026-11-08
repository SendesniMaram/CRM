# Plan de finalisation du CRM

**Référence :** audit fonctionnel du 27 octobre 2026, branche `publication-clean`.
Ce plan découpe les lacunes du CDC en lots métier démontrables. Les améliorations proposées hors CDC (par exemple localisation multilingue ou audit trail global) sont signalées comme telles et ne sont pas des critères d’acceptation obligatoires du CDC. Maintenir les autorisations côté API, contrôle de propriétaire, validation et gestion des erreurs dans chaque lot.

## Priorité 1 — Fermer les parcours principaux pour la démonstration

### Lot 1 — Portails personnels et parcours RH/paie de bout en bout

**Objectif :** permettre une démonstration cohérente des principaux parcours des quatre rôles et fermer les ruptures qui touchent directement les bénéficiaires.

- **Fonctions :** finaliser le compte CLIENT → association client → profil/factures/PDF via Gateway; finaliser le compte EMPLOYEE → association employé → profil et bulletin propre; rendre les pages de présence et formation utilisables ou limiter explicitement la démonstration aux congés; afficher les états vides, erreurs et chargements pour ces chemins.
- **Services :** `identity-service`, `employee-service`, `customer-service`, `hr-service`, `invoice-service`, `payroll-service`, `gateway-service`, `common-security`.
- **Angular :** `features/client/`, `features/profile/`, `features/users/`, `features/payroll/`, `features/hr-admin/`, `features/leaves/`, `app.routes.ts`, `core/navigation/crm-navigation.ts`, `shared/pdf-download/`.
- **Migrations :** aucune migration de domaine client supplémentaire prévue si la migration existante `customer-service/.../V20261023_01__customer_user_associations.sql` est appliquée. Ajouter migrations versionnées pour toute nouvelle association/colonne de paie ou HR. Vérifier dans le lot le mécanisme reproductible d’application des migrations aux bases de service.
- **Tests à ajouter :** tests d’autorisation propriétaire CLIENT/EMPLOYEE et refus cross-account; association et désassociation; tests composants et services Angular; tests contractuels Gateway; E2E ciblés login → association → données personnelles → logout; PDF rattaché au bon propriétaire.
- **Critères d’acceptation :** un CLIENT ne voit que son profil/factures et télécharge un PDF de sa facture; un EMPLOYEE ne voit que sa fiche et ses bulletins; un autre identifiant fourni en paramètre ne permet aucun accès transversal; un congé peut être soumis puis approuvé/refusé; présence/formation affichent des données API et actions réellement persistées si présentées à la démonstration; erreurs 401/403/404 sont compréhensibles; les parcours sont rejoués via Gateway contre une base dédiée de test.
- **Dépendances :** association stable compte-employé et compte-client; identité du propriétaire disponible dans le JWT; format de PDF; schéma de test isolé. Décider avant le développement si les pages Attendance/Training sont montrées en V1 ou retirées de la navigation jusqu’à leur finition.
- **Risques de régression :** erreur de correspondance compte ↔ employé/client, pagination et PDF, filtrage d’autorisation commun, routes autorisées. Ne jamais élargir les API de paie à tous les EMPLOYEE sans filtrage par propriétaire.

### Lot 2 — Fiabiliser les opérations admin qui alimentent les parcours

- **Fonctions :** parcours compte/role/activation, création employé et affectation département, client et association, création facture, préparation de paie; expliciter les états d’un compte désactivé et les erreurs d’association; confirmer cohérence des identifiants lors d’une suppression ou réaffectation.
- **Services :** `identity-service`, `employee-service`, `department-service`, `customer-service`, `invoice-service`, `payroll-service`, `fees-service`.
- **Angular :** `features/users/`, `features/employees/`, `features/departments/`, `features/customers/`, `features/invoices/`, `features/payroll/`, `features/fees/`.
- **Migrations :** migration dédiée uniquement si le cycle de vie de compte ou les statuts d’association demandent de nouveaux champs; assurer une migration compatible avec les enregistrements existants. Documenter les bases/schema attendus pour installation neuve.
- **Tests à ajouter :** scénarios de workflow intégration avec dépendances interservices simulées et un E2E complet Admin; limites Bean Validation; droits ADMIN/SUPER_ADMIN; échec d’un service dépendant sans persistance incohérente.
- **Critères d’acceptation :** admin crée un compte, l’active, attribue un rôle permis, l’associe; crée un employé relié à un département; crée un client associé, puis facture; chaque ressource est visible après rechargement; les rôles interdits et identifiants invalides sont refusés avec messages utiles.
- **Dépendances :** lot 1 définit les propriétés et attentes des comptes personnels; préparation de données de démonstration reproductible.
- **Risques :** changements de modèle d’identité affectant JWT/refresh, réaffectation avec congés ou factures existants, doublons lors de reprises réseau.

## Priorité 2 — Couvrir les exigences métier absentes ou réduites

### Lot 3 — Dossier employé et cycle RH

- **Fonctions CDC :** contrats/avenants, historique de poste, documents administratifs; quotas et soldes de congé, calendrier, évaluations et recrutement; compléter présence et formation avec workflows et interface dédiée.
- **Services :** principalement `hr-service` et `employee-service`; `identity-service` pour accès au recrutement si candidat authentifié; `common-security`.
- **Angular :** étendre `features/hr-admin/`, `features/leaves/`, `features/employees/`; créer les features UI manquantes pour contrats, documents, recrutement, évaluations et calendrier. Ne pas présenter les écrans RH génériques comme des workflows finis.
- **Migrations :** migrations HR pour règles d’acquisition/consommation et période de congés, contrats/événements/document metadata, évaluations et entités recrutement; choisir stockage documentaire et contraintes de rétention avant migration.
- **Tests à ajouter :** transitions de workflow et règles de solde (jours ouvrés, chevauchement, annulation); propriétaire/employeur; accès aux documents; tests de calendrier autour des fuseaux; historique immuable et permissions d’approbation.
- **Critères d’acceptation :** solde explicable par événements; calendrier respecte filtres et droits; chaque contrat/document possède métadonnées, accès protégé et historique; une évaluation/recrutement peut suivre les états métier définis par le CDC; saisie et consultation passent par Angular et persistent après rechargement.
- **Dépendances :** décisions RH sur quotas, jours fériés, visibilité et conservation; le CDC ne précise pas assez les algorithmes de calcul et étapes de recrutement, à trancher en spécification métier avant développement.
- **Risques :** règles de congé erronées, fuite de données RH sensibles, stockage de documents sans politique de taille/format/rétention.

### Lot 4 — CRM commercial et cycle de facturation

- **Fonctions CDC :** catégorisation clients/prospects/fournisseurs, contacts et historique d’interactions, opportunités et affectation; import/export; grilles tarifaires, timesheets et rapports; modèles de facture, numérotation, devis, paiements/relances, historique/archivage et statistiques.
- **Services :** `customer-service`, `fees-service`, `invoice-service`; `identity-service` pour attribution des employés; éventuellement service dédié si le modèle CRM dépasse la responsabilité Customer.
- **Angular :** étendre `features/customers/`, `features/fees/`, `features/invoices/` et créer vues opportunités/interactions/devis/paiements/import-export.
- **Migrations :** schémas versionnés pour types/contacts/activités/opportunités, tarif/horaire, lignes de devis/facture, événements de paiement et relance; contraintes d’unicité pour numéros par période; stratégie d’évolution sans perte des factures actuelles.
- **Tests à ajouter :** règles de numérotation concurrente; calcul du total/arrondis/devise; transitions devis→facture et paiement; import CSV hostile/invalide et rapport d’erreurs; droits d’accès et journal des changements financiers.
- **Critères d’acceptation :** employé attribué peut renseigner une interaction; prospect transformable en client sans doublon; devis accepté convertible en facture avec lignes conservées; paiement met à jour le solde sans altérer l’historique; export reflète les filtres; PDF affiche les données persistées.
- **Dépendances :** règles comptables, devises/taxes, séquence de numérotation, format import et profils fournisseurs; définir les attentes ambiguës du CDC avant de fixer les entités.
- **Risques :** modification rétroactive de documents comptables, doublons d’import, arrondis et concurrence sur les numéros.

**Avancement local du 28 octobre 2026 — première tranche du Sprint 4 :** les types prospect/client/fournisseur, les champs catégorie/segment, la conversion prospect→client et les migrations du customer-service sont présents. L’interface permet de filtrer ces contacts par type, catégorie et segment. L’historique d’interactions (appel, e-mail, réunion, note) est persisté par contact, consultable et alimentable depuis sa fiche; les contrôles d’accès admin et les tests backend/frontend correspondants sont couverts. Ces changements restent locaux, non commités; les opportunités, devis, paiements/relances et import/export ne sont pas déclarés terminés par cette tranche.

## Priorité 3 — Expérience, pilotage et exploitation durable

### Lot 5 — Indicateurs, exports et finition UX

- **Fonctions CDC :** indicateurs RH et facturation réellement calculés, filtres combinés et exports manquants, gestion cohérente des états vides/chargements/erreurs; responsive/accessibilité à vérifier sur parcours.
- **Services :** agrégats dédiés dans HR/Invoice/Payroll/Fees ou endpoints de lecture adaptés; éviter d’agréger toutes les données par appels Angular fragiles.
- **Angular :** `features/dashboard/`, listes métier et composants partagés de filtre/export/erreur.
- **Migrations :** index adaptés aux filtres et volumes identifiés par mesures; pas de migration de modèle sans besoin mesuré.
- **Tests à ajouter :** contrat des agrégats, tests de filtres et pagination, snapshots de calcul sans données fictives, audit clavier/ARIA ciblé; tests de performance sur volumes représentatifs.
- **Critères d’acceptation :** indicateurs identiques aux agrégations API de référence; chaque donnée du dashboard est reliée à une API; les filtres affectent réellement les résultats; navigation clavier et messages d’erreur vérifiés.
- **Dépendances :** clôture des lots de données métiers; définition des KPI et périmètres par rôle.
- **Risques :** lenteur de requêtes et incohérence des chiffres lors de pagination ou filtrage.

### Lot 6 — Installation et livraison reproductibles intégrées aux lots métier

- **Fonctions :** migration/versionnement de toutes les bases; démarrage sur schéma vierge; intégration des tests Angular dans CI; packaging/démonstration frontend; vérification restaurabilité. Ce lot soutient la stabilité, sans remplacer les lots fonctionnels.
- **Services/outils :** ressources de migrations dans les microservices, `docker-compose.yml`, `Jenkinsfile`, `.github/workflows/`, configuration frontend et guide de démonstration.
- **Migrations :** établir une baseline vérifiée pour les schémas existants, puis versions ordonnées et idempotence appropriée; tester création neuve et upgrade. Ne pas lancer de migration destructive sur une base utilisateur.
- **Tests à ajouter :** pipeline isolé qui compile/teste Angular et services; Compose sur projet/volume jetable; smoke test Gateway; restauration réelle dans environnement non production.
- **Critères d’acceptation :** un opérateur suit le guide pour installer une base vide, lancer l’application, exécuter un parcours démonstrateur, puis reconstruire l’environnement; CI affiche les résultats backend + Angular; sauvegarde restaurée et contrôlée.
- **Dépendances :** schémas définitifs des lots métiers; credentials/environnements CI configurés.
- **Risques :** incompatibilité de baseline avec les volumes locaux; la CI peut déployer ou nettoyer un environnement si les cibles ne sont pas isolées. Conserver les contrôles de sécurité et éviter les secrets dans les journaux.

## Ordre conseillé

1. **Lot 1** : ferme d’abord l’accès personnel à la paie et valide les parcours Client/Employee/HR que l’interface annonce.
2. **Lot 2** : rend la création des données de démonstration fiable.
3. **Lot 3** : complète le cycle RH demandé.
4. **Lot 4** : étend CRM commercial et cycle financier.
5. **Lot 5** : améliore le pilotage avec des données réelles.
6. **Lot 6** : rend chaque version et environnement reproductible, avec migrations et CI, intégré aux livraisons métier.

## Premier lot recommandé

**Implémenter le Lot 1 — Portails personnels et parcours RH/paie de bout en bout.** Commencer par l’accès sécurisé de l’employé à ses propres bulletins (API et PDF filtrés par identité), puis rejouer les parcours Client existants via Gateway et terminer les pages Attendance/Training qui sont déjà annoncées dans la navigation. Ce lot ferme une lacune visible et bloquante du CDC tout en donnant une démonstration cohérente à chaque rôle. Il n’est pas commencé dans le cadre de cet audit.
