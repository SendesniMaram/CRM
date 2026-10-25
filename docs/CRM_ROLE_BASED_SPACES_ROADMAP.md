# Espaces utilisateurs CRM : audit par rôle et feuille de route

> Archive documentaire : les constats, chiffres, anciens commits et autorisations ci-dessous décrivent la campagne datée du rapport. Ils ne constituent pas un état courant ni un plan à réappliquer sur `publication-clean`. Toute opération réelle nécessite une nouvelle vérification et l’autorisation adaptée.

Date : 19 octobre 2026, Africa/Tunis. Branche `microservices`, état local non commité inclus. **Analyse uniquement ; aucune implémentation ni évolution RBAC.**

## État de reprise — 22 octobre 2026

Cette feuille de route conserve l'analyse du 19 octobre ci-dessous. Depuis, le parcours Angular administratif `/users` permet la consultation de l'annuaire multi-rôles et la création de comptes EMPLOYEE : payload limité aux champs autorisés, succès uniquement sur 201, mot de passe effacé après soumission, sans choix de rôle ou de flags. Création du compte et association sont deux opérations distinctes ; un échec d'association ne recrée ni ne supprime automatiquement le compte. Les écrans restent réservés à ADMIN/SUPER_ADMIN. Les réponses de mutation sont vérifiées et un 503 ne provoque pas une déconnexion injustifiée. Les détails du contrat d'annuaire sont conservés dans [SPRINT2A_LOT_1_REVIEW.md](SPRINT2A_LOT_1_REVIEW.md) et les tests Angular sous `frontend/src/app/features/users/`.

Décision 2B.1 validée : association User ↔ Employee facultative 1:1, propriété unique de HR dans `leave_employee_associations`, sans migration MySQL immédiate. Le principal partagé conserve le `userId` issu du claim `uid` validé ; le username reste un nom d'affichage/authentification compatible, jamais une clé de propriété. Aucune correspondance automatique par nom, email ou égalité d'identifiants. Les associations concurrentes et incohérentes sont refusées ; aucune réaffectation ne doit transférer implicitement un historique.

Les sous-lots B à D sont présents dans les sources locales : principal typé, service centralisé `EmployeeAssociationService`, puis résolution des congés personnels par `AuthenticatedUserPrincipal.require(authentication).userId()`. Liste, pagination, détail, création, modification, suppression autorisée et PDF conservent leurs contrôles de propriété ; compte non associé ou principal incompatible refusé. Les permissions administratives restent inchangées. Les parcours legacy d'association restent soumis aux contraintes serveur ; leur présence dans Angular n'autorise aucun transfert d'historique. Le profil personnel et les lots suivants restent à réaliser après validation.

Les dernières validations rapportées sont isolées : lot B, 712 tests ; lot C, 373 tests finaux ; lot D, 280 tests HR et 112 common-security, sans échec final. Ces campagnes ne s'additionnent pas et ne prouvent ni déploiement ni E2E réel. Les sources et tests non commitées doivent rester ensemble pour reprendre le travail ; les réserves de publication et de capacité E2E restent applicables.

## 1. Conclusion et preuves

Le constat CLIENT est confirmé dans le code : ses menus métier se limitent à Accueil et Tableau de bord ; DashboardDataService retourne un état vide sans requête métier pour le scope client. Ce n'est pas une panne d'une API personnelle existante : les contrats personnels et l'association User–Customer nécessaires ne sont pas implémentés.

ADMIN n'est pas globalement limité au seul Accueil : les menus, routes et règles serveur lui donnent les principaux CRUD également accessibles à SUPER_ADMIN. L'espace SUPER_ADMIN reste lui aussi incomplet au regard du CDC : pas de console d'administrateurs, paramètres globaux ou journal de sécurité. EMPLOYEE possède un vrai parcours de congés, mais pas un espace personnel complet.

Références consultées : les trois audits demandés et `docs/CDC CRM 1.pdf`, ainsi que navigation/guards/routes Angular, dashboard, dialogue de compte, congés et SecurityConfig des huit services métier/Identity. Les services de propriété HR ont été relus. Les chemins ci-dessous sont relatifs au dépôt.

**CDC directement consulté cette fois :** extraction des dix pages du PDF avec JShell, PDFBox 3.0.8 et commons-logging déjà présents dans le cache Maven. Aucune installation ni export du texte dans un fichier. Les sections du document sont utilisées plutôt que des numéros de page potentiellement ambigus. Cette lecture complète la limite d'extraction signalée dans l'audit précédent ; cet ancien fichier n'est pas modifié.

« Disponible » ci-dessous signifie présent et relié dans les sources, avec preuves isolées antérieures lorsqu'elles existent, jamais validé sur les images actives. Aucun test, Login ou appel métier réel n'est exécuté pendant cette analyse. Les 868 tests Java et 732 tests Angular sont les résultats historiques du lot F.

### Références techniques principales

| Référence | Fichiers | Apport |
|---|---|---|
| UI1 | frontend/src/app/core/navigation/crm-navigation.ts ; app.routes.ts ; core/guards/role.guard.ts | Menus et routes par rôle |
| UI2 | frontend/src/app/layout/components/navbar/account-dialog.component.ts ; layout/components/sidebar/sidebar.component.ts | Mon compte et déconnexion |
| UI3 | frontend/src/app/features/dashboard/dashboard-data.service.ts ; dashboard.component.html | Appels et messages du dashboard |
| UI4 | frontend/src/app/features/leaves/leave.service.ts ; leaves.routes.ts ; leaves-page.component.ts | Demande personnelle, décisions, association sans réaffectation UI |
| API1 | microservices/identity-service/src/main/java/com/crm/identity/controller/{AuthenticationController,UserController}.java ; dto/UserResponse.java ; service/impl/IdentityServiceImpl.java | Auth, annuaire, création EMPLOYEE et projection de rôles |
| API2 | microservices/<service>/src/main/java/com/crm/<domaine>/config/SecurityConfig.java | Matrice serveur des méthodes HTTP |
| API3 | microservices/hr-service/src/main/java/com/crm/hr/service/{LeaveAccess,impl/LeaveServiceImpl}.java ; controller/LeaveAssociationController.java ; entity/LeaveEmployeeAssociation.java | Filtrage propriétaire et cycle des associations |
| API4 | Entités/services/contrôleurs Employee, Customer, Payroll, Invoice et Fees examinés dans l'audit fonctionnel | Références métier, calculs et absence d'API personnelle |

## 2. Matrice des espaces actuels

Hypothèse de lecture : un compte portant uniquement le rôle de sa colonne. Les comptes multi-rôles cumulent des autorités ; le libellé visuel et le scope dashboard donnent priorité SUPER_ADMIN, ADMIN, EMPLOYEE, puis CLIENT. Ce libellé n'est pas un contrôle serveur et ne doit pas servir à retirer ou donner des droits.

| Fonction actuelle | SUPER_ADMIN | ADMIN | EMPLOYEE | CLIENT |
|---|---|---|---|---|
| Accueil /home | Oui | Oui | Oui | Oui |
| Tableau de bord /dashboard | Compteurs et synthèses admin | Même scope admin | Congés propres | Message, aucune donnée métier chargée |
| Employés /employees | CRUD/détail | CRUD/détail | Pas d'UI ; API administrative refusée | Refus |
| Départements /departments | CRUD | CRUD | Menu absent ; GET API autorisé actuellement | Refus |
| Congés /leaves | Toutes demandes, décisions, PDF | Idem | Propres demandes/PDF selon association | Refus |
| /leaves/new et /:id/edit | Pas pour rôle seul | Pas pour rôle seul | Demander/modifier ses demandes autorisées | Refus |
| Associations /leaves#associations | Liste/options/création | Idem | Refus | Refus |
| Réaffectation association | API seulement | API seulement | Refus | Refus |
| Paie /payroll | CRUD et PDF | CRUD et PDF | Refus ; pas de bulletin personnel | Refus |
| Honoraires /fees | CRUD/calcul | CRUD/calcul | Refus | Refus |
| Clients /customers | CRUD | CRUD | Refus | Refus, y compris sa propre fiche |
| Factures /invoices | CRUD/PDF | CRUD/PDF | Refus | Refus, y compris ses factures |
| Utilisateurs | GET/POST EMPLOYEE par API, sans page | Idem | Pas d'annuaire autorisé | Pas d'annuaire autorisé |
| Dossiers RH / présences / formations | API admin, sans UI | Idem | Pas de lecture personnelle | Refus |
| Mon compte | Dialogue de session | Dialogue de session | Dialogue de session | Dialogue de session |
| Console rôles / paramètres / audit | Absente | Absente | Sans objet | Sans objet |

Tous disposent de Se déconnecter dans la navigation. L'action efface la session Angular ; aucun endpoint de révocation logout correspondant n'est fourni. Mon compte affiche username, email et libellé du rôle : ce n'est pas une page de profil professionnel, ni un profil client éditable.

## 3. Analyse détaillée par rôle

### SUPER_ADMIN

Menus visibles : Accueil, Tableau de bord, Employés, Départements, Congés, Associations employés, Paie, Clients, Factures et Honoraires. Associations employés est une ancre de la page congés, pas une application autonome. Actions : création/édition/suppression des fiches administratives existantes, lecture des détails, recherche/pagination, téléchargement des PDF paie/facture/congé, approbation/refus de congés, liaison de comptes à des employés.

Les API utilisées sont les CRUD du tableau §4. GET/POST /api/users sont disponibles par API mais sans écran ; POST crée uniquement un EMPLOYEE. Il n'existe pas de gestion complète des ADMIN/SUPER_ADMIN, des rôles, de statut ou de verrouillage dans ce contrôleur.

CDC §3.1 : accès complet, gestion des administrateurs/utilisateurs, configuration globale, logs et monitoring. CDC §6.1 : création initiale et changement obligatoire du mot de passe. Écarts : bootstrap limité local/dev explicitement activé, pas de première connexion imposée ni console système. « Accès complet » ne doit pas être interprété comme contournement de validation JWT, modification libre des secrets ou droit implicite à une opération métier qui n'existe pas.

### ADMIN

Les dix entrées de navigation précédentes sont visibles ; mêmes principaux CRUD et décisions que SUPER_ADMIN. Les routes Angular et SecurityConfig citent explicitement ADMIN. Aucun défaut statique réservant l'ensemble de ces domaines au seul SUPER_ADMIN n'est identifié. Cela ne constitue pas une vérification de la session ADMIN observée sur le déploiement.

CDC §3.2 : employés/clients, validation congés, paie/honoraires, factures et rapports. Écarts : absence d'UI utilisateurs, réaffectation, dossier RH, présences et formations ; reporting borné ; pas de facturation/paiement complet. Certains CRUD backend ne constituent donc pas un espace administrateur fini.

À maintenir : ADMIN ne s'attribue aucun rôle supérieur via une création EMPLOYEE. La future gestion des administrateurs doit rester une capacité distincte réservée à SUPER_ADMIN selon décision de permissions, pas une extension automatique du POST existant.

### EMPLOYEE

Menus visibles : Accueil, Tableau de bord et Congés ; Mon compte et déconnexion communs. Actions : demander un congé (/leaves/new), éditer une demande propre autorisée, supprimer une demande PENDING dans les règles existantes, consulter ses demandes/statuts et télécharger son PDF. Pas d'approbation, pas d'association par l'employé. Les chemins new/edit exigent EMPLOYEE ; être ADMIN seul n'implique pas ce rôle personnel.

Backend : POST /api/leaves/mine, PUT /api/leaves/mine/{id}, GET /api/leaves et /{id}, DELETE /{id}, GET /{id}/pdf. LeaveAccess résout l'association par le nom authentifié ; LeaveServiceImpl applique le périmètre employeeId à la liste et à la recherche paginée, et vérifie le propriétaire du détail/modification. Une association absente bloque le parcours ; l'interface propose de contacter un administrateur.

CDC §3.3 : profil personnel, congés, fiches de paie, contacts assignés. Seul le workflow de congés est relié dans cet espace. Pas de GET personnel de fiche employé, de paie propre ou de contact assigné. Les GET Department sont autorisés plus largement par le serveur que par l'UI : décider annuaire partagé ou visibilité limitée avant de changer quoi que ce soit.

Le dashboard charge des données réelles au sens des services HTTP Angular : sa source est la liste de congés filtrée par le serveur. Il n'est pas un tableau RH complet, et son fonctionnement réseau réel n'est pas prouvé par cet audit.

### CLIENT

Menus visibles : Accueil et Tableau de bord ; Mon compte et déconnexion communs. L'accueil n'offre pas de raccourci de création métier. DashboardDataService.load() retourne immédiatement emptyDashboard('client') ; aucun chargement de facture, profil Customer, demande ou projet. Le message affiché reflète donc une lacune de produit réelle.

Les seules opérations connectées disponibles sont essentiellement les opérations de session Login/Refresh ; l'inscription publique crée un User CLIENT. Elle ne crée pas et ne lie pas une fiche Customer. Les pages /customers et /invoices sont administratives : leur absence du menu CLIENT et leur refus serveur sont cohérents avec l'absence actuelle de contrôle propriétaire.

CDC §3.4 : consultation de ses factures, suivi de ses demandes et projets, communication avec l'équipe. Ces trois capacités sont attendues, pas de simples propositions d'amélioration. En revanche le CDC ne définit pas le modèle détaillé des demandes/projets/messages : statuts, pièces jointes, participants, rétention et responsabilités doivent être décidés avant implémentation.

## 4. Cartographie des endpoints réellement reliés

Chemins locaux backend ci-dessous ; Angular passe par Gateway avec le préfixe indiqué. GET détail/PDF ne doit jamais être déduit comme personnel du seul fait de contenir un ID.

| Préfixe Gateway | API locale | Utilisateurs et opérations actuelles |
|---|---|---|
| /identity | POST /api/auth/login, /register, /refresh | Authentification/inscription/renouvellement ; inscription CLIENT imposée |
| /identity | GET /api/users, /{id} ; POST /api/users | ADMIN/SUPER_ADMIN, annuaire et création EMPLOYEE, pas d'UI dédiée |
| /employee | /api/employees, /{id}, /{id}/details | GET/POST/PUT/DELETE administratifs, détail Feign Department |
| /department | /api/departments, /{id} | CRUD admin ; GET également EMPLOYEE |
| /customer | /api/customers, /{id} | CRUD ADMIN/SUPER_ADMIN |
| /hr | /api/leaves, /mine, /mine/{id}, /{id}/approve, /{id}/reject, /{id}/pdf | Personnel EMPLOYEE et administration, selon méthode/propriété |
| /hr | /api/leaves/associations, /options, /{userId} | Annuaire/liaison/réaffectation admin ; PUT non exposé par LeaveService Angular |
| /hr | /api/hr-records, /api/attendances, /api/trainings | CRUD admin backend sans page Angular |
| /payroll | /api/payrolls, /{id}, /{id}/pdf | CRUD/PDF admin seulement |
| /fees | /api/fees, /{id} | CRUD admin seulement |
| /invoice | /api/invoices, /{id}, /{id}/pdf | CRUD/PDF admin seulement |

Le validateur `/api/internal/access-token/validate` reste une auto-vérification technique par Bearer, bloquée à la Gateway ; pas une API de profil. Les routes administratives restent fermées aux utilisateurs ordinaires dans toutes les propositions.

## 5. CDC explicite et améliorations proposées

| Fonction cible | Source CDC directe | État et nature de la solution proposée |
|---|---|---|
| Gestion ADMIN/utilisateurs, paramètres, logs | §3.1, §6.1, §5.4 | Exigence ; console dédiée et garde-fous anti-escalade proposés comme réalisation |
| Profil personnel employé | §3.3 | Exigence ; endpoint /me et DTO minimisé proposés, pas imposés par le CDC |
| Paies propres et PDF | §3.3 et §4.3 | Exigence ; filtrage propriétaire avant nouvelles routes |
| Contacts assignés | §3.3, §4.6 | Exigence ; modèle d'affectation encore absent |
| Factures propres Client et PDF | §3.4, §4.5 | Exigence ; lien User–Customer et API personnelle à créer |
| Demandes/projets et communication Client | §3.4 | Exigence de capacité ; modèle et canaux à définir |
| Soldes, calendrier, formations, évaluations, recrutement | §4.2 | Exigences ; pas toutes affectées explicitement à un rôle dans le CDC |
| Reporting, factures, honoraires | §3.2, §4.4–4.5 | Exigences ; définitions des KPI et droits organisationnels à préciser |
| Profil Client éditable | Pas explicitement formulé comme droit dans §3.4 | Proposition utile, ne pas assimiler à une obligation de modification de la fiche comptable |
| Dashboard personnalisé, raccourcis, messages d'activation | Dashboard RH prévu §4.2 ; pas de détail Client | Amélioration UX proposée ; ne remplace aucune API personnelle |
| Invites email, choix actif de rôle, réaffectation avancée | Pas prescrits précisément | Propositions à valider ; aucune livraison implicite |

La disponibilité d'une API Training n'implique pas un catalogue ou un certificat fichier ; le champ certificat est booléen. Les statuts PAID/paymentStatus ne sont pas des transactions ou des preuves de paiement.

## 6. Exigences de sécurité avant les espaces personnels

### Associations explicites

User.id signé dans uid représente le compte ; Employee.id et Customer.id représentent des fiches métier. Ne jamais utiliser leur égalité, un email, un nom ou un customerId fourni par le navigateur pour autoriser l'accès.

Pour Employee, l'association actuelle HR userId/username→employeeId est propre aux congés ; clé primaire username et résolution depuis sub. Elle ne devient pas automatiquement le référentiel transversal de paie. Décider un propriétaire d'association, un contrat de résolution par uid, ses contraintes d'unicité et son cycle de vie. Réutiliser Identity/Employee/HR existants, sans relation JPA entre bases. Une évolution de schéma nécessite une migration préparée et autorisée ultérieurement, aucune modification dans cet audit.

Pour Client, créer ultérieurement une association explicite User.id→Customer.id dans un service existant responsable. Décider s'il existe plusieurs contacts connectés par société Customer et plusieurs sociétés par compte : ne pas imposer un 1:1 avant décision. Provisionnement administratif contrôlé ou invitation vérifiée ; inscription publique jamais liée automatiquement sur email. Compte non associé = espace en attente et aucune donnée retournée.

Changement ou retrait d'association : éviter qu'un nouveau compte récupère l'historique d'une autre personne. Règles de réaffectation, conservation et suspension explicites ; tests de concurrence, suppression/recréation et refus si le référentiel est inaccessible. Pas de fallback « dernier propriétaire connu » autorisant un accès.

### Contrôles objet côté serveur

Propositions de surfaces distinctes, **non existantes à ce jour** : Employee GET /api/employees/me ; Payroll GET /api/payrolls/mine, /mine/{id}, /mine/{id}/pdf ; Customer GET /api/customers/me ; Invoice GET /api/invoices/mine, /mine/{id}, /mine/{id}/pdf. Les noms restent à approuver ; aucune route ajoutée ici.

Chaque service destinataire valide toujours le JWT et l'état courant, puis résout l'association autorisée depuis uid. Liste et pagination doivent être filtrées dans le repository/service, avec totalElements également limité au propriétaire ; détail, PDF et recherche utilisent un prédicat propriétaire serveur. Le navigateur n'envoie pas l'ID du propriétaire comme preuve. Aucun téléchargement via URL publique devinable, aucun filtrage Angular de liste administrative.

Contacts : nouvelle affectation autorisée côté serveur ; listes, détails, modifications, exports et interactions filtrés selon l'affectation. Projets/demandes/messages : contrôle de participation et de visibilité de chaque objet et pièce jointe. Ne pas réutiliser l'accès administratif global pour donner un portail Client.

Minimiser les DTO : ne pas renvoyer salaire, notes internes, coordonnées d'autres personnes, mot de passe, refresh token ou rôles administratifs à un destinataire sans besoin. Un futur /me éditable ne permet pas de changer identité technique, rôle, propriétaire, statut, salaire ou référence client via mass assignment.

### Rôles, session et erreurs

Conserver les contrôles Sprint 1B : signature/claims, état courant, rôles signés, refus sûr sans cache positif, 401 invalide, 403 interdit, 503 vérification impossible. Choisir un refus 403 ou 404 cohérent pour un objet d'autrui afin de limiter l'énumération ; ne pas exposer son existence dans le corps d'erreur. Une association absente n'est pas une panne Identity ; définir une erreur fonctionnelle distincte sans relâcher l'accès.

L'annuaire UserResponse expose un roleType unique issu de findFirst alors que User possède plusieurs rôles ; HR filtre ce champ sur EMPLOYEE. Corriger ce contrat avant les parcours multi-rôles. Les règles cumulatives et un éventuel choix de contexte personnel/admin doivent être décidés ; changer le contexte UI ne modifie jamais les autorités serveur.

Le menu caché et les guards Angular sont une aide UX ; ils ne protègent pas un appel direct. Les PDF doivent appliquer les mêmes contrôles que leurs détails. Un échec 503 ne doit provoquer ni fausse confirmation, ni document valide, ni déconnexion injustifiée.

## 7. Priorités et découpage proposé

Les sprints sont des incréments fonctionnels, pas une promesse de couvrir tout le CDC en quelques semaines. Chaque sprint commence après validation de son périmètre. Réutiliser les services actuels ; aucun nouveau microservice n'est nécessaire pour démarrer.

| Sprint/lot | Rôles et capacité | Backend proposé | Angular proposé | Tests et critères de sortie | Dépendances |
|---|---|---|---|---|---|
| **2A.1 Contrat annuaire** | ADMIN/SUPER_ADMIN ; onboarding fiable | Liste complète de rôles rétrocompatible ; règles pour comptes inactifs et erreurs annuaire | Modèles HTTP alignés, sans choix libre de rôle à la création | Tests mapper multi-rôle/ordre et JSON, HR inclut EMPLOYEE sans dépendre de findFirst ; RBAC identique | Décision comptes multi-rôles |
| **2A.2 Comptes et associations** | ADMIN/SUPER_ADMIN ; créer EMPLOYEE et lier une fiche | Réutiliser POST users et association existants, conserver contraintes de réaffectation | Liste/création utilisateurs, liaison et dialogue de réaffectation, erreurs séparées | H2/MockMvc et Angular : 201 confirmé, doublons et références refusés, 503 sans écriture HR, aucun transfert de congés ; pas de suppression compensatoire automatique du compte | 2A.1 ; compte et fiche Employee distincts |
| **2B Référentiel personnel** | EMPLOYEE/CLIENT ; prérequis de tous les /me | Décider source d'autorité par uid ; association Client persistée ; contrat résolution et cycle de vie ; migration seulement après accord dédié | État « espace à activer » et administration des liens selon droits approuvés | Deux Users et deux fiches avec IDs volontairement différents, conflits/recréation/panne ; aucune ouverture de lecture métier avant succès | Cardinalités et politique d'historique, stratégie de migration |
| **2C Espace Employé minimal** | EMPLOYEE ; profil, paies propres/PDF et congés existants | Endpoints personnels distincts Employee/Payroll et filtrage repository ; congés préservés | Mon profil en lecture, Mes bulletins, dashboard alimenté ; UI congés conservée | A/B : listes, pagination, détail/PDF et accès direct ; mauvais propriétaire refusé ; aucun salaire d'autrui ; ADMIN conserve CRUD | 2B ; règles de visibilité des bulletins et périodes |
| **2D Espace Client minimal** | CLIENT ; Mes factures/PDF | Contrats Invoice personnels filtrés par association ; politique factures publiées/statuts | Liste/détail/PDF personnels, dashboard à données bornées et états utiles | Deux clients/sociétés, essais croisés ID/PDF/recherche ; compte non lié sans données ; aucune route CRUD administrative ouverte | 2B ; visibilité brouillon/émis/annulé, cardinalité Customer |
| **2E Administration RH** | ADMIN/SUPER_ADMIN ; dossiers, présence, formation | Réutiliser CRUD HR ; clarifier dates historiques Training et références | Pages/services dédiés et validation ; une sous-feature par lot | MockMvc/H2 règles de dates et transitions ; loading/empty/error ; EMPLOYEE/CLIENT restent refusés à l'API globale | Arbitrage accès personnel futur et intégrité Employee |
| **3A Contacts assignés** | EMPLOYEE et ADMIN ; contacts du périmètre | Affectation persistée dans Customer ou domaine Contact cadré, contrôles objet et interactions | Mes contacts, détail et actions autorisées ; assignation administrative | Réaffectation et lecture/écriture/export croisés refusés ; totaux limités ; pas de détournement d'une fiche client comptable | 2B ; distinction Customer/Contact et règle d'affectation |
| **3B Demandes/projets Client** | CLIENT et équipe autorisée | Modèle de demandes/projets/participants et transitions dans un service existant choisi après cadrage | Mes demandes/projets, suivi, messages et pièces jointes si validés | Participation sur chaque objet, refus transversal, transitions et pièces jointes ; messagerie non simulée présentée comme livrée | Acteurs internes, statuts, canaux et rétention à définir |
| **3C Console Super Admin** | SUPER_ADMIN | Création/gestion ADMIN, politique anti-escalade et dernier superadmin, commande atomique statut/révocation, audit des opérations | Console comptes/paramètres autorisés et consultation d'audit expurgé | ADMIN refusé aux opérations réservées ; impossibilité d'auto-escalade ; transaction rollback/révocation ; pas de secrets affichés | Matrice de permissions signée, bootstrap/récupération, conservation audit |
| **Transversal reporting** | ADMIN/SUPER_ADMIN, dashboards personnels bornés | Agrégats serveur par périmètre et période ; définition explicite des KPI | Indicateurs utiles sans données fictives ; distinguer vide/incomplet/panne | Pas d'agrégation de données d'autrui ; devises séparées ; pas d'assimilation facturé/encaissé ; pagination exhaustive testée | Définitions métier et endpoints filtrés |

Ordre prioritaire : 2A, puis 2B ; 2C et 2D peuvent être priorisés selon besoin métier après 2B. Les capacités 3A–3C demandent leur propre estimation et peuvent couvrir plusieurs sprints. Les rapports avancés, soldes RH, évaluations, recrutement, grilles, devis et paiements restent un backlog CDC explicite à découper ; ils ne sont pas réputés terminés par cette feuille de route.

Définition minimale d'un espace fonctionnel : SUPER_ADMIN dispose d'une administration des comptes privilégiés protégée ; ADMIN peut effectuer les parcours existants sans API manuelle pour onboarding ; EMPLOYEE consulte son profil et ses bulletins et gère ses congés ; CLIENT consulte réellement ses factures. Cette définition minimale ne remplit pas encore toutes les exigences CDC, notamment contacts assignés et projets/communication.

## 8. Stratégie de tests et critères transversaux

Validation sans Docker possible : tests unitaires des résolveurs/mappers/règles, H2/MockMvc avec filtres actifs pour persistance et propriété, dépendances interservices HTTP simulées explicitement, tests Angular HttpTestingController avec guards/intercepteur réels. Une suite CRUD addFilters=false ne prouve aucune autorisation. Ne pas annoncer un nombre de tests avant exécution.

Chaque lot personnel doit inclure : deux utilisateurs du même rôle, un utilisateur d'un autre rôle, comptes sans association/inactifs/verrouillés, JWT expiré/falsifié/sans uid, rôle retiré, association modifiée ou introuvable, panne de résolution et accès direct. Tester collection, compteur, pagination, recherche, détail, PDF, mutations et exports s'ils existent, y compris l'absence d'exécution métier après refus.

Angular : menus desktop/mobile cohérents, navigation directe guardée, formulaires en double clic, reprise après erreur, données effacées au changement de session, accès clavier et messages compréhensibles. Vérifier 401→refresh borné, 403 sans contournement, 503 sans refresh/logout injustifié, absence de téléchargement sur PDF refusé. Aucun identifiant, token ou mot de passe dans les logs de test publiés.

Critères de sortie : tests ciblés et régression réussis avec résultats exacts, contrats rétrocompatibles sauf évolution approuvée, pas de nouvelle permission administrative accordée aux rôles ordinaires, migrations seulement préparées si autorisées, limites documentées. Les suites isolées ne valent pas preuve navigateur→Gateway→MySQL.

## 9. Décisions fonctionnelles requises

1. **Client et société :** un compte par Customer ou plusieurs contacts ? Un compte peut-il représenter plusieurs sociétés, avec quel choix explicite et quel contrôle serveur ?
2. **Sources d'association :** quel service possède User–Employee et User–Customer ; comment les révoquer et conserver l'historique sans réattribuer les anciens documents ?
3. **Profil :** lecture seulement ou champs éditables ; quelles données restent maîtrisées par RH/comptabilité ?
4. **Bulletins et factures :** quand deviennent-ils visibles ; brouillons exclus ; que voit-on après départ, annulation ou changement d'association ?
5. **Department :** annuaire commun EMPLOYEE ou seul département ; alignement UI/API à décider sans changement immédiat.
6. **Rôles multiples :** cumul explicite, contexte personnel/admin, interdiction éventuelle de mélanges ; jamais sélection d'un rôle plus fort depuis le client.
7. **Contacts :** Customer reste-t-il une société facturée, un contact ou les deux ; quelle règle d'affectation et de transfert ?
8. **Demandes/projets/communication :** modèle de service attendu, acteurs, statuts, visibilité des notes internes, pièces jointes, canal d'échange et conservation.
9. **Super Admin :** opérations exclusives, protection du dernier compte privilégié, bootstrap de production et récupération ; pas de route générale de lecture de secrets.
10. **Livraison :** priorité Employé ou Client après onboarding ; capacité E2E dédiée et validation métier par rôle avant déploiement.

## 10. Sprint 1B et préservation

La validation E2E réelle Sprint 1B reste en attente : les verdicts de préparation BLOCKED / REQUIRES DEDICATED ENVIRONMENT ne sont pas levés. Restent les parcours réels quatre rôles, changements de statut/rôle après commit, accès directs, panne/reprise, propagation interservices, PDF, saturation et latences sur MySQL temporaire. Aucune image active n'est validée ou remplacée par ce rapport.

Limites conservées : ancien JWT pouvant redevenir utilisable à la réactivation, opérations déjà admises, dépendance Identity/base, secret HMAC partagé et transport interne HTTP de confiance. Les nouveaux espaces doivent intégrer ces contraintes et ne pas contourner l'introspection.

Le seul fichier créé est ce document. Aucun code, RBAC, MySQL, Docker, .env, secret ou document antérieur modifié ; aucun test, commit, push ou déploiement. La lecture du PDF utilise uniquement les bibliothèques locales existantes. Les modifications locales sont conservées.

**Verdict : feuille de route prête à validation fonctionnelle. Aucune implémentation commencée ; conformité CDC complète et E2E réel non acquis.**
