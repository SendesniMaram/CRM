# Préparation de rotation des secrets CRM

> Archive documentaire : les constats, chiffres, anciens commits et autorisations ci-dessous décrivent la campagne datée du rapport. Ils ne constituent pas un état courant ni un plan à réappliquer sur `publication-clean`. Toute opération réelle nécessite une nouvelle vérification et l’autorisation adaptée.

Date : 20 octobre 2026, Africa/Tunis. Branche `microservices`. Analyse des sources et de l'historique local, modifications non commitées comprises.

**VERDICT : NEEDS CLARIFICATION pour préparer l'exécution. La publication conserve la réserve BLOCKED de l'audit pré-commit. Aucune rotation effectuée.** Les deux valeurs locales doivent être considérées comme compromises ; leur utilisation effective dans les environnements actifs n'est pas établie. Les procédures existantes constituent une base, mais ne doivent pas être exécutées telles quelles.

## 1. Périmètre, preuves et incertitudes

Documents consultés : `docs/CRM_PRE_COMMIT_AUDIT.md`, `docs/ROTATION_SECRETS.md`, `docs/ROTATION_EXECUTION.md`. Sources examinées : Compose principal et surcharge de rotation, exemple d'environnement, workflow GitHub Actions, configurations JWT et datasource, émission des refresh tokens Identity, helpers de rotation, scripts de démonstration et préparation E2E.

Contrôles réalisés uniquement en lecture : état Git, plage des commits non poussés, comparaison exacte en mémoire des deux valeurs locales avec des contenus historiques, recherche des références et lecture des procédures. Aucun secret, empreinte de secret, mot de passe ou token n'est reproduit ici.

| Constat | Preuve et portée |
|---|---|
| APP_JWT_SECRET local correspond à une valeur historique | Correspondance exacte avec `.env.example` au commit `a79d51c`, ancêtre de `origin/microservices` |
| MYSQL_ROOT_PASSWORD local correspond à une valeur historique | Correspondance exacte avec `docker-compose.yml` au commit `bc9fa2f`, ancêtre de `origin/microservices` |
| Exposition publique antérieure | Caractère public établi dans l'audit pré-commit et signalé par la mission ; aucune nouvelle interrogation GitHub pendant cette analyse |
| Fichier `.env` actuellement ignoré | Confirmé par `git check-ignore` ; cela ne protège pas une valeur déjà publiée ailleurs |
| Exemple de configuration actuel | Les deux champs sensibles de `.env.example` sont vides |
| Environnement réellement chargé | Inconnu : aucune lecture de l'environnement des conteneurs actifs, ni connexion réelle |
| Compromission effective | Non démontrée ; aucune recherche d'intrusion ou tentative d'exploitation effectuée |

L'égalité avec une valeur publiquement accessible suffit à exiger son remplacement dès lors qu'elle sert encore d'identifiant actif. Elle ne prouve ni une intrusion, ni que le mot de passe réellement enregistré dans MySQL correspond encore au `.env`.

L'inventaire des autres machines, IDE, tâches planifiées, pipelines, sauvegardes et environnements doit être confirmé par leur responsable. Il est interdit de conclure que seul le poste local utilise ces valeurs.

## 2. Dépendances et chargement

### APP_JWT_SECRET

| Consommateur | Dépendance constatée |
|---|---|
| Identity | Configuration `app.jwt.secret`, émission et validation des access tokens |
| Employee, Department, Customer, HR, Payroll, Fees, Invoice | Même variable fournie par Compose ; vérification locale via `common-security`, puis vérification centrale Identity |
| Bibliothèque common-security | Consomme la propriété Spring dans chaque application ; aucun processus autonome à redémarrer |
| Monolithe `backend/` | Propriété `jwt.secret=${APP_JWT_SECRET}` ; hors pile Compose examinée, usage effectif à confirmer |
| Gateway et Discovery | Pas de dépendance à cette clé identifiée ; Gateway relaie les requêtes |
| Angular | Ne doit jamais recevoir la clé ; reçoit seulement les jetons de session |

Les implémentations JWT Identity et common-security construisent la clé depuis les octets UTF-8 de la chaîne fournie ; elles ne décodent pas automatiquement une chaîne Base64. Elles imposent au moins 32 octets. Pour la génération future, conserver la proposition d'une source cryptographiquement sûre de 64 octets aléatoires, encodés pour le transport, avec la même représentation exacte partout. Ne pas changer implicitement le format de lecture du secret pendant cette opération.

### MYSQL_ROOT_PASSWORD

Le Compose principal fournit cette variable à MySQL et l'utilise comme `SPRING_DATASOURCE_PASSWORD` des huit services Identity/métier, avec le compte `root`. C'est une dépendance fortement privilégiée et commune à toute la pile.

La surcharge `docker-compose.rotation.yml` prépare des identifiants applicatifs distincts : `IDENTITY`, `EMPLOYEE`, `DEPARTMENT`, `CUSTOMER`, `HR`, `PAYROLL`, `FEES`, `INVOICE`, chacun avec ses variables `<PREFIX>_DB_USERNAME` et `<PREFIX>_DB_PASSWORD`. Elle impose aussi des images explicitement choisies, `ddl-auto=validate` et l'absence d'initialisation SQL automatique. Elle n'est active que si elle est explicitement sélectionnée avec le Compose principal.

Cette surcharge ne crée aucun compte, ne change aucun mot de passe stocké dans MySQL et ne révoque aucune session. Le document général décrivant ces variables comme non câblées doit être compris pour le Compose principal seul ; la surcharge préparée les câble déjà.

### Sources de configuration

- Docker Compose interpole les variables depuis l'environnement de lancement et les fichiers d'environnement sélectionnés. Une variable déjà exportée peut prendre priorité sur le `.env` attendu ; les options `--env-file`, le répertoire de projet et la combinaison des fichiers Compose doivent être explicités lors de la future intervention.
- Les variables sont transmises aux nouveaux conteneurs. Modifier `.env` ne met pas à jour les processus existants ; un simple redémarrage du même conteneur conserve sa configuration d'environnement. Une recréation contrôlée sera nécessaire si ce mécanisme est retenu.
- Spring Boot lancé directement n'importe pas automatiquement le `.env` de Compose. Les configurations IDE, variables système/processus, arguments Spring et profils doivent être inventoriés séparément, sans enregistrer leurs valeurs dans un rapport.
- Le workflow `.github/workflows/build.yml` actuel utilise des tests isolés ; aucune injection de ces deux secrets d'exploitation n'y est identifiée. `SONAR_TOKEN` est un secret distinct, hors périmètre de rotation.
- La préparation E2E utilise `E2E_JWT_SECRET`, `E2E_MYSQL_PASSWORD` et `E2E_ADMIN_PASSWORD` distincts. Leur indépendance doit être conservée ; aucune valeur d'exploitation ne doit être recopiée dans les fixtures.

Ne jamais publier une sortie complète de `docker compose config`, de `docker inspect`, d'un environnement de processus ou d'un diagnostic Spring susceptible de résoudre ces variables.

## 3. Conséquences d'une rotation JWT

Une clé HMAC connue permet de fabriquer des signatures valides. Les contrôles Sprint 1B de compte courant, uid et rôles limitent certaines incohérences, mais ne rendent pas sûre une clé connue : un attaquant pouvant cibler une identité et des rôles existants peut tenter une usurpation. Aucune exploitation réelle n'a été testée.

Avec la nouvelle clé, les anciens access tokens ne passent plus la validation de signature des applications mises à jour. La durée restante du JWT ne permet pas de les conserver. Une mise à jour partielle crée des refus incohérents entre services ; conserver une ancienne instance accessible maintient aussi un risque d'acceptation de jetons signés avec la clé compromise.

Les refresh tokens Identity sont des valeurs opaques persistées, générées indépendamment de la clé JWT. **Changer APP_JWT_SECRET ne les invalide pas.** Sans révocation explicite, un ancien refresh token encore valide peut obtenir un nouvel access token signé avec la nouvelle clé. Pour une rotation motivée par compromission, la procédure proposée révoque toutes les sessions antérieures pendant la maintenance et impose une reconnexion. Cette écriture Identity exige une autorisation distincte et un traitement des courses avec Login/Refresh.

L'intercepteur Angular peut tenter son refresh sur un 401. Après révocation, ce refresh doit être refusé selon le contrat existant, sans boucle. Un 503 reste une indisponibilité et ne doit pas provoquer de déconnexion injustifiée.

Ordre futur proposé, après accord :

1. Bloquer les entrées et suspendre les producteurs/écritures, y compris les accès directs et les Login/Refresh ; ne pas limiter le gel à Gateway.
2. Arrêter de manière coordonnée les huit applications utilisant la clé, ainsi que toute instance legacy confirmée concernée. L'ordre détaillé dépend des processus effectivement présents.
3. Effectuer les opérations de comptes DB et de révocation prévues, sous maintenance, puis charger la nouvelle clé sur toutes les instances concernées.
4. Rendre MySQL disponible avec une vérification authentifiée ; conserver Discovery si inchangé, sinon vérifier sa disponibilité avant les applications.
5. Démarrer Identity seul, vérifier sa readiness et le contrat de validation ; démarrer ensuite les sept consommateurs progressivement pour limiter la pression mémoire. Vérifier leurs dépendances avant leurs parcours métier.
6. Valider les appels directs et via Gateway avant réouverture. Gateway/Discovery n'ont pas besoin d'une rotation de clé ; leur éventuel redémarrage doit avoir une justification séparée.

Ce séquencement n'autorise aucune commande aujourd'hui et ne permet pas de tester une nouvelle pile complète sur le poste déjà contraint en mémoire.

## 4. Conséquences et précautions MySQL

**Variable Compose et mot de passe stocké sont deux états distincts.** `MYSQL_ROOT_PASSWORD` sert à l'initialisation d'un répertoire de données vierge. Sur un volume déjà initialisé, changer sa valeur dans `.env` puis recréer le conteneur ne réalise pas un `ALTER USER` sur les comptes existants.

Il faut donc confirmer ultérieurement, avec autorisation : instance et volume exacts, comptes `root` et leurs hôtes, plugin d'authentification, droits, utilisateurs applicatifs, consommateurs externes et chemin d'administration de secours. Les noms de conteneur, sous-réseau et comptes observés dans les anciens documents sont des données historiques, pas un inventaire actuel.

La rotation du mot de passe réel demande une modification explicite du compte MySQL concerné. Les comptes `root` de différents hôtes peuvent nécessiter des modifications séparées ; leur bascule n'est pas atomique. Les connexions déjà ouvertes des pools ne prouvent pas qu'une nouvelle connexion fonctionne avec la nouvelle configuration.

Cible recommandée : huit comptes applicatifs distincts, droits minimaux sur leur propre schéma, sans privilèges globaux ni délégation. Les scripts de migration et d'administration restent séparés. Vérifier que `validate` et `init=never` conviennent aux schémas existants avant toute bascule ; ne pas accorder du DDL pour contourner une erreur de démarrage.

Les services doivent être recréés avec ces comptes dédiés avant réouverture. Tant que des services restent arrêtés pendant la maintenance, la modification root peut précéder leur redémarrage ; en revanche aucune application ne doit être rouverte avec l'ancien root. Après vérification des comptes dédiés et d'un accès administratif maîtrisé, remplacer tous les accès root concernés et synchroniser leur configuration opérationnelle. Ne pas conserver l'ancien mot de passe compromis comme second mot de passe de secours.

La healthcheck principale utilise `mysqladmin ping` : sa réussite ne prouve pas toujours une authentification valide. La surcharge de rotation prépare une vérification authentifiée `SELECT 1`, qui doit être éprouvée lors de la campagne autorisée.

## 5. Procédure proposée et prérequis bloquants

| Étape future | Condition de passage |
|---|---|
| Inventaire validé | Responsable et environnement connus pour chaque consommateur ; images, chemins de chargement et accès directs identifiés sans diffusion des secrets |
| Runbook révisé | Corrections ci-dessous approuvées, procédure de reprise sans ancien secret et fenêtre de maintenance définies |
| Capacité disponible | Budget mémoire/disque validé pour sauvegarde et restauration de contrôle ; environnement dédié si le poste ne le permet pas |
| Sauvegarde cohérente | Écritures gelées, sauvegarde chiffrée hors dépôt, ACL contrôlées, restauration isolée réellement réussie et preuve associée au fichier exact |
| État de reprise | Nouveaux identifiants sauvegardés de façon sécurisée avant les écritures irréversibles ; accès de secours indépendant vérifié |
| Comptes applicatifs | Création et droits vérifiés ; comptes partiellement créés inventoriés en cas d'échec, sans suppression automatique |
| Bascule sous maintenance | Root changé explicitement, configuration synchronisée, refresh antérieurs révoqués, nouvelle clé déployée partout ; ordre affiné selon inventaire |
| Validation | Nouvelle connexion de chaque service, contrats Identity/HR, RBAC quatre rôles, propriété, PDF et comportements 401/403/503 vérifiés |
| Réouverture | Aucune ancienne instance accessible, contrôles approuvés et journaux expurgés ; surveillance et reprise documentées |

Les sauvegardes proposées utilisent DPAPI CurrentUser : leur récupération dépend du contexte Windows et de ses éléments de récupération. Définir le détenteur, le lieu de restauration et la procédure de récupération ; une empreinte de fichier seule ne prouve pas une sauvegarde restaurable.

Le helper collecte des schémas métier, dont le schéma legacy, et des configurations sensibles dans un bundle chiffré. Il ne sauvegarde pas automatiquement tout l'état des comptes système MySQL : prévoir un inventaire sécurisé des droits et paramètres nécessaires à la reprise. Les anciennes clés présentes dans une sauvegarde restent des éléments de diagnostic, jamais des valeurs à remettre en service.

### Ajustements requis des procédures existantes

1. `ROTATION_EXECUTION.md` est antérieur aux derniers sprints et référence un autre HEAD : réconcilier ses hypothèses avec les sources et images réellement retenues.
2. Supprimer du futur plan d'exécution toute possibilité de réactiver une clé ou un mot de passe compromis, même temporairement. L'exception d'urgence évoquée par l'ancien document n'est pas retenue ici.
3. Revoir le contrôle RBAC du runbook : sa matrice générique attend 403 pour certains GET Department EMPLOYEE alors que la SecurityConfig actuelle autorise cette lecture. Corriger le test attendu, sans modifier le RBAC.
4. Qualifier les images de reprise : une ancienne image peut manquer des contrôles Sprint 1B ou du contrat Identity/HR `roles` et `accountNonLocked`. Un simple retour à un ancien ID ne garantit pas une reprise sûre.
5. Vérifier les noms de projet, IDs, volumes et sous-réseaux avant les commandes ciblées ; les valeurs historiques codées dans les helpers ne suffisent pas à identifier une cible actuelle.
6. Le restore-test du helper démarre un MySQL temporaire en tmpfs sans limite mémoire explicite. Sa consommation et ses limites doivent être cadrées avant autorisation ; ne pas le lancer sur un hôte déjà insuffisant.
7. Le wrapper Docker attend la fin du processus sans délai global explicite. Prévoir des délais et une reprise opérateur maîtrisée pour éviter une maintenance bloquée.
8. Le contrôle de taille de dump intervient après capture en mémoire ; 64 MiB n'est donc pas une borne prouvée de consommation. Dimensionner ou préparer une méthode chiffrée en flux si nécessaire.

Les scripts n'ont pas été chargés ni exécutés. Leurs paramètres `Authorized` sont des garde-fous opérateur, pas une autorisation acquise par ce rapport.

## 6. Reprise sans réactivation des secrets compromis

En cas d'échec, maintenir la maintenance et conserver les nouvelles valeurs. Réutiliser uniquement une image préalablement qualifiée compatible avec les contrôles de sécurité et les nouveaux identifiants. Si la nouvelle clé ou un nouveau mot de passe doit être abandonné, générer une troisième valeur indépendante lors d'une intervention autorisée, puis terminer la bascule vers cette valeur.

Pour un ALTER root partiel, conserver l'état chiffré de reprise, déterminer quel compte administratif fonctionne et terminer explicitement la modification des comptes restants. Ne pas restaurer aveuglément l'ancien `.env`, ne pas rétablir root dans les applications, ne pas réactiver les refresh tokens révoqués.

Une restauration de données métier nécessite son propre diagnostic et son autorisation ; aucun import automatique de dump, aucune suppression de volume, aucune réinitialisation de MySQL ne fait partie de la rotation.

## 7. Historique Git et publication

État observé : HEAD `00c2870956826adbb32b9cc22b9506879f5cdfdd`, branche `microservices`, index vide. Comparaison avec la référence locale `origin/microservices` : **13 commits en avance, 0 en retard**. Cette analyse ne rafraîchit pas les références distantes ; l'état GitHub réel devra être relu avant publication.

Les diffs des treize commits ont été comparés en mémoire aux deux valeurs locales : aucune correspondance exacte détectée. Cela ne constitue pas une recherche exhaustive de tout autre secret. Les deux valeurs ont été retrouvées dans des ancêtres de la référence distante : elles étaient déjà exposées avant cette plage.

| Commits non poussés | Revue de publication |
|---|---|
| `a376731` | Restriction des données administratives ; conserver la revue RBAC et les tests associés |
| `34d7788`, `986f444` | Contrats clients/factures et frontend ; revue normale des données de démonstration et DTO |
| `a3dd488`, `f469e7c`, `40b6d0f` | Honoraires et paie ; vérifier l'absence de données financières personnelles dans les exemples |
| `e84ed88` | Dashboard ; ne pas assimiler son fonctionnement à une preuve E2E |
| `40c0fb9` | Configuration ; examiner la version historique du commit et le résultat final, pas seulement le working tree corrigé |
| `61f3961` | Validation formulaires/DTO ; conserver les changements contractuels ensemble |
| `65cf538` | Revue prioritaire : scripts demo, fixtures et identifiants présents dans `Test-TechNova.Local.ps1` |
| `814a0ef` | Documentation MVP ; rechercher données opérationnelles et instructions obsolètes |
| `2293cca`, `00c2870` | Navigation/interface ; revue normale des ressources et contrats |

Les six fichiers demo introduits par `65cf538` sont :

- `scripts/demo/Import-TechNova.ps1`
- `scripts/demo/README.md`
- `scripts/demo/Test-CRM-MVP.Local.ps1`
- `scripts/demo/Test-CRM-MVP.ps1`
- `scripts/demo/Test-TechNova.Local.ps1`
- `scripts/demo/technova.json`

Le README décrit des données fictives et les tests locaux simulent les API. Les identifiants de fixture ne correspondent pas aux deux valeurs du `.env` examinées. Cela ne prouve pas leur absence de réutilisation ailleurs, ni le caractère synthétique de toutes les coordonnées : confirmation du propriétaire nécessaire. Aucun script demo n'a été exécuté, y compris ceux effectuant un login réel.

Un push de la branche publiera toute sa plage de commits, pas uniquement les futurs ajouts. Si un contenu de `65cf538` ou d'un autre commit non poussé est déclaré impropre à publication, il n'est pas possible de l'exclure par une simple sélection de fichiers du prochain commit. Suspendre le push et demander une décision distincte sur la préparation de l'historique. Aucune réécriture automatique n'est proposée comme action autorisée.

Un nouveau commit supprimant un secret ne le supprime pas de ses parents. Une éventuelle purge d'historique est un chantier séparé, coordonné avec les collaborateurs et la plateforme ; elle ne remplace jamais la rotation et ne garantit pas l'effacement des clones ou copies externes.

## 8. Exclusions et contrôles avant publication

Exclure de tout ajout : `.env` et variantes locales, fichiers temporaires de rotation, dumps SQL réels, bundles DPAPI, fichiers pending/receipts opérationnels, clés privées, exports d'environnement, inspections Docker, logs non revus, cookies/profils navigateur et répertoires générés `target`, `node_modules`, `dist`, `.angular`, `frontend/tmp`.

Conserver hors publication en attendant revue explicite : `b.png` et `docs/CDC CRM 1.pdf`. Leur caractère binaire et leur provenance imposent une revue de confidentialité et de droits ; aucune suppression locale demandée.

Les procédures, scripts et exemples peuvent être versionnés après revue de leur contenu, avec des placeholders ou valeurs manifestement synthétiques. Les documents déjà présents doivent aussi être revus pour leurs noms de comptes, données personnelles, chemins et observations d'exploitation.

Conditions de reprise :

1. Inventaire des usages actifs confirmé et rotation autorisée, exécutée et validée sur chaque environnement concerné ; ou preuve explicite qu'une occurrence est inactive, avec mise au rebut contrôlée de sa configuration.
2. Identifiants demo et données publiables confirmés synthétiques et non réutilisés ; sinon décision sur les commits concernés avant tout push.
3. Aucun secret dans les futurs fichiers indexés, leurs diffs, les messages de commit et tous les commits réellement publiés. Effectuer une recherche complémentaire de secrets, sans imprimer leurs valeurs.
4. Plan de commits de l'audit pré-commit revu ; ajout sélectif uniquement après validation utilisateur, puis revue de l'index. Aucun `git add` aujourd'hui.
5. Référence distante, divergences, permissions et protections relues juste avant push. Aucun changement de remote ou force-push implicite.
6. Ne pas convertir la réussite des tests isolés en autorisation de déploiement ou en validation E2E.

## 9. Résultats, autorisations et décision

Aucune suite applicative relancée : les **915 tests Java et 792 tests Angular réussis**, ainsi que le build Angular avec avertissement de taille, sont les résultats historiques de l'audit pré-commit. Ils ne valident pas une rotation réelle, les identifiants actifs ou la restauration d'une sauvegarde.

La première tentative de comparaison automatisée via Python n'a pas pu démarrer, l'exécutable étant inaccessible ; la comparaison a ensuite été réalisée avec Node.js, sans écriture ni valeur sensible affichée. Aucun échec n'est présenté comme un contrôle réussi.

Restent soumis à une autorisation explicite :

- inspection opérationnelle des environnements actifs et vérifications authentifiées MySQL ;
- révision des procédures/helpers existants et qualification de leur exécution ;
- création de sauvegardes sensibles et restauration dans un environnement isolé ;
- génération, stockage et modification des secrets ;
- création/gestion des comptes MySQL, modification root et révocation des refresh tokens ;
- gel du trafic, arrêt/recréation/redémarrage des composants ciblés et tests réels ;
- opérations Git d'indexation, commits, push ou éventuel traitement d'historique, chacune dans le périmètre approuvé.

Le seul livrable écrit pendant cette mission est ce rapport. Les fichiers préexistants et le `.env` sont préservés ; aucune commande Docker, aucun accès MySQL, aucune authentification réelle, aucune génération de secret, aucun changement d'index ou d'historique Git.

**VERDICT FINAL : NEEDS CLARIFICATION.** Avant une exécution sûre, confirmer les environnements et accès de secours, la capacité de restauration, les images compatibles, le plan de maintenance et la politique de révocation de toutes les sessions. La reprise de publication reste bloquée par les réserves de confidentialité et de réutilisation tant qu'elles ne sont pas levées. La validation E2E réelle du Sprint 1B reste en attente ; aucune livraison ni début de Sprint 2B n'est autorisé par cette analyse.
