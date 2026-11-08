# Lot 2B — Scénarios intégrés préparés, non exécutés

Référence de préparation : Lot 2A du 1er novembre 2026. Utiliser uniquement le modèle autonome produit par `scripts/validation/prepare-lot2b.mjs`, jamais les volumes de développement. Aucun test ci-dessous n'a été exécuté pendant le Lot 2A. Cette liste est un protocole manuel/API/navigateur, pas un runner automatisé terminé.

## Conditions d'entrée et preuves

Appliquer les contrôles et commandes de [LOT_2A_PREPARATION_VALIDATION.md](LOT_2A_PREPARATION_VALIDATION.md). Chaque phase dépend du succès de la précédente. Arrêter la campagne à la première erreur d'isolation, de migration, de TLS ou d'authentification ; conserver le projet pour diagnostic sans nettoyage automatique. Ne jamais contourner une erreur en passant Hibernate à `update`, en réparant l'historique Flyway ou en désactivant la validation Identity/TLS.

Pour chaque cas, enregistrer : identifiant du cas, heure, rôle, route, statut attendu/observé, invariant de persistance et verdict. Les preuves partagées ne doivent contenir ni bearer/refresh token, secret, corps personnel, chaîne SQL détaillée ou clé privée. Les credentials restent en mémoire de la session de test, pas dans un fichier de résultats ou une ligne de commande. Désactiver les traces HTTP détaillées et expurger les captures navigateur/HAR avant partage.

## Phase 1 — Installation neuve, démarrage et routage

| Cas | Action future | Attendu / preuve |
|---|---|---|
| INF-01 | Contrôler contexte Docker, projet, ressources absentes avant première création, chemins des binds, ports et mémoire | Aucun volume/réseau de développement ou externe ; seul projet `crm-lot2b-*` ; aucune publication autre que Gateway 18080 et Eureka 18761 en loopback |
| INF-02 | Démarrer MySQL seul sur son volume neuf ; attendre sa healthcheck authentifiée | SELECT 1 réussi ; huit schémas vides avant les services ; pas de schéma importé ni données métier |
| INF-03 | Démarrer Discovery, Identity, Gateway puis les sept services métier, séquentiellement | Aucun redémarrage en boucle/OOM ; chaque contexte s'initialise ; neuf clients visibles dans Eureka (Gateway, Identity et sept métiers) |
| INF-04 | Lire les huit `flyway_schema_history` et contrôler JPA | 24 migrations versionnées réussies au total, aucune baseline automatique ; Identity comprend session_id, HR employee_visible ; aucune écriture Hibernate de schéma |
| INF-05 | Sonder les routes Gateway publiques et protégées sans bearer | `/identity/api/test` 200 ; routes métier protégées 401 ; aucune réponse 404/503 inattendue ; distinguer une route accessible d'un parcours métier réussi |
| INF-06 | Tenter `/hr/api/internal/...`, `/employee/api/internal/...`, variantes encodées et routes discovery par nom | 400/404 sans accès interne ; aucun contournement par le Gateway ; routes normales toujours fonctionnelles |
| INF-07 | Tester CORS depuis `http://localhost:14200`, puis origine non autorisée | Authorization/Content-Type et méthodes nécessaires permis uniquement à l'origine prévue ; pas de désactivation de CORS pour faire passer le test |

Vérifier l'historique depuis MySQL dans le conteneur candidat avec ses credentials de test, jamais via le port 3307 du développement. Contrôler le champ `success`, les versions et les checksums Flyway ; le SHA-256 du manifeste est une empreinte de fichier de préparation, **pas** le checksum Flyway.

## Phase 2 — Fixtures synthétiques

Le bootstrap local existant crée uniquement `lot2b-admin` SUPER_ADMIN sur base vierge. Il est activé explicitement dans le modèle de test et n'est pas une procédure de production. Créer via les API administratives : un second SUPER_ADMIN S2, un ADMIN A, EMPLOYEE E1/E2, CLIENT C1/C2, un utilisateur sans association et des comptes inactifs/verrouillés pour les tests négatifs. Créer département, employés, clients et associations correspondantes. Conserver les identifiants retournés sous forme de chaînes, y compris les IDs dépassant la précision entière JavaScript dans un scénario dédié si la base synthétique le permet.

Ne pas importer de données utilisateur réelles. Un verrouillage de compte ou une donnée historique impossible à créer via l'API peut utiliser une fixture SQL synthétique **sur ce projet seulement**, après vérification de la cible et autorisation spécifique ; ces écritures directes ne valident pas les hooks administratifs. Ne pas enregistrer les mots de passe dans les scripts/versionnés.

## Phase 3 — Identité et sessions (Lot 1B)

Toutes les routes ci-dessous passent par `http://localhost:18080`.

| Cas | Séquence future | Attendu |
|---|---|---|
| SES-01 | POST `/identity/api/auth/login`, puis appel autorisé | JWT signé avec UID canonique et sid UUID ; ne pas confondre inspection locale et authentification serveur ; no-store |
| SES-02 | POST `/identity/api/auth/refresh` avec `{refreshToken}` puis réutiliser l'ancien refresh | Rotation avec même sid ; ancien refresh 401 ; pas de nouvelle session sur réutilisation |
| SES-03 | POST `/identity/api/auth/logout` avec le refresh actuel, puis appel avec ancien JWT | Logout 204 no-store ; ancien accès 401 via Identity et chacun des sept métiers ; refaire logout → 204 ; corps vide/trop long → 400 |
| SES-04 | Connexion A, connexion B, logout avec refresh A | A révoquée ; B reste valide car sid différent ; vérifier aussi ancien refresh après rotation |
| SES-05 | Désactiver E1 par PATCH `/identity/api/users/{id}/status`, puis réactiver | Sessions E1 révoquées après commit ; réactivation ne les restaure pas ; connexion neuve requise |
| SES-06 | Changer ses rôles via PUT `/identity/api/users/{id}/roles`, ajout puis retrait, et remplacement identique | Changement sensible révoque ; ancien JWT ne gagne aucun rôle ; remplacement strictement identique ne révoque pas inutilement ; interface ferme sa propre session modifiée |
| SES-07 | Simuler indisponibilité Identity uniquement sur la pile candidate, puis restauration | Les services refusent avec 503, sans accès métier ni succès depuis un cache positif ; pas de boucle refresh navigateur |
| SES-08 | Réseau perdu au logout, réponse tardive après reconnexion | Effacement local immédiat, absence de faux succès serveur ; aucun token ni donnée d'ancienne session restauré |
| SES-09 | Deux transactions login/refresh/logout puis rôle/statut concurrents | Exclusion selon verrou utilisateur, pas de session réactivée ; tester commit, rollback et timeout ; une requête autorisée avant révocation peut terminer |

Pour la révocation multi-service, utiliser une identité de test possédant les rôles permettant un accès de référence avant révocation, ou des fixtures par rôle. Un 403 préexistant ne prouve pas une révocation.

## Phase 4 — Dernier SUPER_ADMIN utilisable

| Cas | Action future | Attendu |
|---|---|---|
| ADM-01 | Auto-désactivation du SUPER_ADMIN connecté | Refus, aucune mutation |
| ADM-02 | Retirer le rôle du dernier SUPER_ADMIN actif/non verrouillé | 409 ; rôle et sessions inchangés après rollback ; message existant éventuellement générique, juger aussi la persistance |
| ADM-03 | Comptes SUPER_ADMIN désactivés/verrouillés présents en plus du dernier utilisable | Ils ne permettent pas de supprimer le dernier utilisable |
| ADM-04 | S1 et S2 tentent simultanément de réduire les deux derniers comptes utilisables | Sérialisation sur rôle commun, acteur relu ; au moins un utilisable demeure ; aucun deadlock non traité ou demi-révocation |

Recréer la fixture dans une nouvelle campagne après un scénario mutatif si nécessaire. Ne jamais tenter ces opérations sur un administrateur réel.

## Phase 5 — Associations et espaces personnels (Lot 1C)

Rejouer tous les cas du [protocole Lot 1C](LOT_1C_VALIDATION_INTEGREE.md), notamment :

- POST `/hr/api/employee-associations` et `/customer/api/customers/associations` : références valides, idempotence, conflits UID/cible, comptes inactifs/verrouillés, rôle incorrect, 401/403, annuaire 404/502/503. Deux créations concurrentes ne remplacent jamais le gagnant. Conserver les contraintes d'unicité réelles.
- E1 : `/hr/api/employee-associations/me`, `/hr/api/employee-profile/me`, congés propres et `/payroll/api/payrolls/me`. E2 ne lit ni ne modifie les ressources de E1 ; seuls bulletins publiés accessibles. Sans association : état explicite, pas de fallback username/e-mail.
- Administrateur : déposer un PDF de test dans le dossier E1. GET `/hr/api/documents/me` vide avant partage ; PATCH `/hr/api/documents/{id}/visibility` avec booléen obligatoire ; apparition pour E1 seulement ; GET `/hr/api/documents/me/{id}/content` → pièce jointe/no-store/nosniff. Retirer le partage : disparition et 404 au téléchargement. Tester pièce privée, étrangère, absente, fichier absent, trop grand et liens symboliques dans un stockage de test contrôlé.
- C1 : profil `/customer/api/customers/me`, factures `/invoice/api/invoices/me`, détail, PDF et `/{id}/payments`. C2 ne lit aucune donnée de C1. Pas de paramètres de ciblage personnels acceptés sur les nouveaux endpoints ; backend filtrant réellement. Notes et références administratives absentes des paiements personnels.
- Angular : guards et menus des quatre rôles, `/profile`, `/leaves`, `/my-payroll`, `/my-documents`, `/client`, `/dashboard` ; chargement, vide, erreurs et rechargement ; changement de compte pendant une requête ; montants exacts et statistiques existantes.

## Phase 6 — Finance et confidentialité (Lot 1A)

| Cas | Action future | Attendu |
|---|---|---|
| FIN-01 | Créer facture sous-total nul/négatif, précision excessive, somme débordante, échéance avant émission, statut inconnu/PAID imposé | Refus ; aucune ligne persistée ; pas de normalisation silencieuse |
| FIN-02 | Facture UNPAID/PENDING → paiement partiel puis complet | Solde exact ; PARTIALLY_PAID puis PAID dérivés ; édition/suppression refusées après tout paiement ; historique conservé |
| FIN-03 | Paiement sur DRAFT ou supérieur au restant dû | Conflit sans mutation |
| FIN-04 | Transactions concurrentes paiement contre édition/suppression/relance, dans les deux ordres | Attente du verrou commun ; après commit total cohérent ou 409 ; rejouer avec rollback et paiement partiel/complet |
| FIN-05 | Conversion devis avec lignes, tentative de changement client/devise/sous-total | Lignes conservées et règles service appliquées ; pas d'altération du sous-total dérivé |
| FIN-06 | Paie : salaire non positif, net négatif, DECIMAL(12,2) dépassé, mois invalide, puis saisie valide | Rejet des écritures invalides ; valeur persistée exacte de la saisie valide ; anciennes données pas réécrites |
| FIN-07 | CLIENT avec zéro facture, une/multiples devises et montants zéro | Contrat byCurrency inchangé, compteurs cohérents ; pas de zéro inventé sur réponse invalide ; API et dashboard concordants |
| FIN-08 | Conflits d'unicité/intégrité contrôlés et erreur de persistance simulée sur données de test | 409 constant pour intégrité, autre erreur générique ; aucun SQL/table/contrainte/valeur pilote dans réponse ou diagnostic partagé |

## Phase 7 — HTTPS réel HR → Employee

Vérifier les cinq fichiers externes décrits dans le protocole Lot 1C : validité/chaîne/SAN `employee-service`, PKCS12 alias `employee`, correspondance clé privée/publique RSA et `kid=profile`, permissions en lecture seule, horloges synchronisées. Un bootstrap réussi n'est pas encore la preuve d'un handshake et d'une réponse profil conforme.

Tester successivement profil correct, employé absent, mauvais certificat/SAN, certificat expiré/inconnu, mauvaise clé de signature, service indisponible, timeout et redirection. Résultat correct uniquement sur chaîne et identité valides ; refus fermé sinon, sans fuite de token. Aucun port 8443 publié sur l'hôte, aucun accès au résumé interne depuis le port HTTP public ou Gateway. Réaliser les variantes de configuration sur une campagne distincte ; ne pas modifier les certificats d'un autre environnement.

## Phase 8 — Upgrade et clôture

L'installation vierge ne valide pas l'upgrade. Préparer une seconde campagne avec une **copie autorisée et sauvegardée**, anonymisée, des huit schémas et du stockage documentaire cohérent. Vérifier d'abord leurs historiques Flyway et empreintes ; aucun import ni `baseline`, `repair` ou `clean` automatique. Sur schéma historique sans historique Flyway, bloquer jusqu'à revue manuelle du point de départ.

Vérifier préservation des factures/paiements, sid NULL historique refusé sans effacement, documents existants privés après ajout de colonne, publication de paie et rollback transactionnel. MySQL peut auto-committer les DDL : ne pas promettre un rollback intégral d'une migration échouée ; restaurer une copie dans une nouvelle campagne après diagnostic.

Archiver résultats expurgés, versions/digests images, SHA-256 des JAR, manifeste migrations et mesures mémoire/OOM, avec sauvegarde protégée du projet de test si nécessaire. Arrêter uniquement le projet candidat sans suppression de volumes. Aucun déploiement n'est une conséquence automatique de ces tests.
