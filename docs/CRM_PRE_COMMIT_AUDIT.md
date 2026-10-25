# Audit technique pré-commit CRM et plan Git sûr

> Archive documentaire : les constats, chiffres, anciens commits et autorisations ci-dessous décrivent la campagne datée du rapport. Ils ne constituent pas un état courant ni un plan à réappliquer sur `publication-clean`. Toute opération réelle nécessite une nouvelle vérification et l’autorisation adaptée.

Date : 20 octobre 2026, Africa/Tunis. Branche examinée : `microservices`.

**VERDICT : BLOCKED pour la validation globale de publication.** Les tests et le build autorisés réussissent et un plan de commits locaux est proposé, mais des valeurs configurées dans le .env local correspondent à des secrets historiques déjà publiés. Leur réutilisation doit être traitée explicitement avant de considérer la publication/livraison sûre. Aucune opération de rotation ou de réécriture d'historique n'est autorisée par ce rapport.

> Note documentaire du 22 octobre 2026 : les constats, décomptes et résultats ci-dessous restent ceux de l'audit du 20 octobre. La liste documentaire du groupe 5 a été réduite aux chemins encore présents après consolidation des rapports intermédiaires. Les sources locales ont évolué depuis ; consulter [CRM_ROLE_BASED_SPACES_ROADMAP.md](CRM_ROLE_BASED_SPACES_ROADMAP.md) et revalider le plan Git avant toute reprise. La réserve de publication n'est pas levée par ce nettoyage.

## 1. Périmètre et garanties

Audit en lecture seule des sources et de l'historique ; seules les sorties générées de tests/build, un fichier vide de validation Compose sous target et ce rapport sont écrits. Aucun code, fichier de configuration existant, secret, .env ou donnée MySQL modifié. Aucun conteneur démarré, arrêté, redémarré ou interrogé pour ses données. Les commandes Docker utilisées sont uniquement `compose config`, sans opération sur le daemon.

Aucun git add, commit, push, reset, clean, stash, checkout, fetch ou rebase exécuté. Aucun changement de remote/branche ou effacement de fichier local. Les fichiers ignorés restent en place ; les profils navigateur ne sont pas ouverts. Sprint 2B non commencé.

Le scan de sécurité ne restitue ni secret, ni token, ni mot de passe. Ses résultats sont des métadonnées de localisation et de correspondance ; ce n'est pas une certification exhaustive de confidentialité ou une preuve que les valeurs locales sont encore celles des conteneurs actifs.

## 2. État Git complet

| Élément | Constat |
|---|---|
| HEAD | `00c2870956826adbb32b9cc22b9506879f5cdfdd` |
| Dernier commit | feat(frontend): finaliser l'interface CRM et l'expérience utilisateur ; 17 octobre 2026 |
| Branche active | microservices |
| Tracking | origin/microservices |
| Référence distante enregistrée et vérifiée en lecture réseau | `39c43b8781020814002c793c8374166dc7a51dfa` |
| Avance / retard | **13 / 0**, confirmé par la référence distante lue |
| Index par rapport à HEAD | Vide : aucun changement stagé |
| Working tree suivi | **92 fichiers modifiés**, aucune suppression ni fichier suivi ajouté à l'index |
| Non suivis, expansion complète | **110 fichiers** |
| Inventaire initial à examiner | **202 fichiers distincts** avant création de ce rapport |
| Conflits d'index | Aucun ; git ls-files --unmerged vide |
| Fichiers suivis à caractère généré détectés à HEAD | Aucun target/node_modules/dist/log/class/JAR/DB/dump/clé privée dans l'inventaire de noms |
| git diff --check | Code 0 ; avertissements LF/CRLF seulement |

Puisque l'index est vide, les différences suivies HEAD→working tree et index→working tree coïncident. `git diff --stat` annonce 92 fichiers, 878 insertions et 201 suppressions de lignes ; les 110 fichiers non suivis ne sont pas inclus dans cette statistique. Aucun fichier supprimé ne doit être confondu avec les lignes supprimées de fichiers modifiés.

Branches locales : `main` à 45a0890, `microservices` à 00c2870 et `agents/verifier-si-cette-prompte-re-alise-ou-non` à 45a0890. Cette dernière est attachée à un autre worktree sous CRM.worktrees ; il n'a pas été modifié. Références distantes locales : origin/main à 45a0890 et origin/microservices à 39c43b8 ; origin/HEAD pointe sur main.

### Treize commits existants non poussés

Ces commits précèdent les changements non committés et seront aussi publiés par un futur push de cette branche. Ils ne doivent pas être oubliés dans la revue.

```text
00c2870 feat(frontend): finaliser l'interface CRM et l'expérience utilisateur
2293cca feat(frontend): moderniser l'accueil et la navigation CRM
814a0ef docs(mvp): documenter les tests et les limites connues
65cf538 test(demo): ajouter les donnees et scripts de validation du MVP
61f3961 fix(validation): aligner les limites des formulaires et DTO
40c0fb9 fix(config): securiser les configurations des microservices
e84ed88 feat(frontend): finaliser le dashboard dynamique
40b6d0f feat(frontend): intégrer la gestion de la paie
f469e7c feat(frontend): intégrer la gestion des honoraires
a3dd488 fix(honoraires): aligner la description sur le contrat API
986f444 feat(frontend): intégrer la gestion des clients et des factures
34d7788 fix(clients): aligner la longueur des adresses sur le contrat API
a376731 fix(securite): réserver les données administratives aux administrateurs
```

### Changements antérieurs à conserver distincts

Les éditions non committées ne sont pas toutes du Sprint 1B/2A : le monolithe backend, la migration des tests CI vers H2, la configuration/rotation des secrets, les PDF Java/Angular, rapports d'audit antérieurs, scripts et documents sources appartiennent à des travaux préexistants identifiables par leur contenu et documentation. La date d'un fichier n'établit pas son auteur ou son sprint ; aucune attribution exhaustive n'est déduite de Git pour les fichiers non committés.

## 3. Audit de sécurité et risques de publication

### Critique — réutilisation de valeurs déjà présentes dans l'historique public

Deux valeurs non triviales sont configurées dans le .env local. Une comparaison exacte effectuée uniquement en mémoire les retrouve dans l'historique Git :

- La valeur associée à **APP_JWT_SECRET**, notamment dans `.env.example` au commit `a79d51c`.
- La valeur associée à **MYSQL_ROOT_PASSWORD**, notamment dans `.github/workflows/build.yml` et `docker-compose.yml` au commit `bc9fa2f`, ainsi que dans d'anciennes configurations et README.

Ces commits sont ancêtres de HEAD **et** de origin/microservices ; l'API GitHub confirme le dépôt **public**. D'autres occurrences de ces valeurs existent sur des références qui ne sont pas ancêtres de la branche actuelle : elles ne sont pas assimilées aux treize commits à pousser.

**Aucune valeur n'est reproduite ici.** Leur validité dans les services actifs n'a pas été testée ; aucune inspection des environnements des conteneurs ni tentative de connexion n'a été faite. En revanche, la correspondance entre configuration locale et historique public suffit à imposer une revue de réutilisation/rotation. Ne pas réutiliser ces valeurs pour un environnement réel ou un nouvel E2E.

Le scan ne trouve aucune correspondance exacte de ces deux valeurs dans les fichiers suivis/non suivis actuellement candidats. Les champs sensibles de .env.example sont vides. Le .env réel est ignoré et non suivi. Un nouveau commit de nettoyage ne supprime pas l'exposition passée ; une éventuelle purge d'historique est une opération séparée, coordonnée et autorisée, et ne remplace pas la rotation.

### Autres résultats de scan

- Aucun marqueur de clé privée, token fournisseur ou JWT littéral conforme aux motifs utilisés détecté dans les textes candidats ou ajouts d'historique examinés.
- Les alertes de type password/secret dans les tests et fixtures sont principalement des valeurs synthétiques, noms de propriété ou expressions de code ; elles ne sont pas publiées comme des secrets réels confirmés.
- Le commit non poussé `65cf538` ajoute un script de démonstration local contenant des identifiants de fixture. Le script mentionne localhost et un contexte de démonstration ; **leur caractère exclusivement synthétique et non réutilisé reste à confirmer avant publication**. Aucun mot de passe de cette fixture n'est reproduit.
- `b.png` est un binaire non suivi de provenance/visuel non revu : exclu par défaut. `docs/CDC CRM 1.pdf` est une pièce source métier, également exclue du lot de code par défaut en attendant revue de publication.
- Les rapports et scripts peuvent contenir chemins locaux, noms de machine, comptes synthétiques et hypothèses obsolètes ; une relecture de confidentialité reste nécessaire avant publication des documents.
- Les profils Chrome sous frontend/tmp peuvent contenir cookies, historique et bases locales. Ils sont ignorés ; ne pas les publier ou forcer leur ajout.

Le scan couvre les fichiers texte suivis/non suivis éligibles et l'historique des références locales, avec des motifs ciblés et comparaison exacte des deux valeurs locales. Les binaires et gros fichiers ne sont pas analysés comme du texte ; les logs ignorés/profils navigateur ne font pas l'objet d'une certification de contenu. Absence de détection ne signifie pas absence de tout secret.

## 4. Audit du code et cohérence d'intégration

| Surface | État vérifié / risque restant |
|---|---|
| Identity | Signature/claims, uid Login/Refresh, compte courant, rotation, endpoint interne et rôles d'annuaire cohérents ; le DTO ajoute roles/accountNonLocked ; roleType stabilisé change potentiellement l'ancien choix arbitraire multi-rôle |
| common-security | Validation locale puis Identity ; 204 seul accepté ; 401/503 distincts ; pas de cache positif, redirection ou fallback ; quatre validations et délais bornés |
| Sept consommateurs | Propriété Identity obligatoire et destination fixe ; tests de démarrage invalide et filtres ; droits métier conservés ; ne pas séparer le changement de bibliothèque des configurations/tests consommateurs |
| HR/Identity | HR exige roles et accountNonLocked : ancien Identity → 502 sûr, jamais fallback legacy ; livraison future Identity avant HR obligatoire |
| Interservices | Employee→Department et HR→Identity/Employee propagent le Bearer et les refus ; pas de retour null métier de secours ; annuaires et réseaux réels non validés ici |
| Gateway | Filtres réactifs avant sélection et après réécriture, namespace interne/encodages, pas de filtre servlet ; conservation Login/Refresh/Bearer/statuts couverte par tests |
| Compose | Sept origines internes cohérentes, secrets obligatoires ; ports métier publiés et Identity sans healthcheck/readiness contractuelle ; service_started ne prouve pas prêt |
| PDF | POM PDFBox, contrôleurs, sécurité GET et Angular liés ; à garder ensemble avec tests ; génération synthétique n'est pas preuve de déploiement |
| Angular | 2A.2 admin uniquement ; collection roles ; sélection explicite des IDs, erreurs et reprise ; contrats personnels Paie/Facture restent fermés |
| Monolithe backend | Éditions distinctes : secrets externalisés, H2 de test ajouté ; pas inclus dans Compose ; sa présence n'est pas un service supplémentaire de l'architecture microservices |
| CI | Matrice dix applications + job commun + backend ; common installé avant sept consommateurs ; pas de job Angular identifié dans ce workflow |
| Scripts E2E | Préparation autonome tmpfs/réseau/ports, pas d'exécution automatique ; le modèle actuel ne fixe pas les budgets mémoire du plan optimisé |

Le JAR commun target et celui du cache Maven utilisé ont le même SHA-256 :
`2F65BFB7659FB4E6D16F8A3E6746F3A2F67E386F714CDE6390177D410C74D322`.
Cela vérifie les deux artefacts locaux, pas un build sur runner vierge. Une construction propre des dépendances en CI reste nécessaire.

Aucun élargissement RBAC serveur supplémentaire identifié par les diffs relus : les règles admin présentes dans HEAD, les contrôles de propriété HR et les tests restent en place. Cette conclusion n'est pas une preuve d'absence de toute faille logique. Les suites CRUD `addFilters=false` ne sont pas présentées comme preuve de sécurité ; les suites dédiées avec filtres et propriété apportent une preuve isolée complémentaire.

Limites connues non résolues : association congés par username ; absence d'association personnelle globale ; disponibilité Identity/DB ; DNS natif pouvant retenir les workers ; clés HMAC partagées ; HTTP interne de confiance ; requêtes déjà admises ; ancien access token pouvant redevenir utilisable après réactivation. Les bases principales restent utilisées avec des privilèges larges dans le modèle de base ; le modèle de rotation dédié n'est pas déployé par cet audit.

## 5. Résultats exacts des validations

Toutes les suites sont exécutées **séquentiellement**. Windows annonce 2 606 680 Kio libres, environ 2,49 Gio, au relevé préalable. Aucun second environnement Docker n'est démarré.

| Module / validation | Tests | Échecs | Erreurs | Ignorés | Résultat |
|---|---:|---:|---:|---:|---|
| common-security | 98 | 0 | 0 | 0 | BUILD SUCCESS |
| identity-service | 115 | 0 | 0 | 0 | BUILD SUCCESS |
| employee-service | 77 | 0 | 0 | 0 | BUILD SUCCESS |
| department-service | 44 | 0 | 0 | 0 | BUILD SUCCESS |
| customer-service | 68 | 0 | 0 | 0 | BUILD SUCCESS |
| hr-service | 217 | 0 | 0 | 0 | BUILD SUCCESS |
| payroll-service | 55 | 0 | 0 | 0 | BUILD SUCCESS |
| fees-service | 67 | 0 | 0 | 0 | BUILD SUCCESS |
| invoice-service | 72 | 0 | 0 | 0 | BUILD SUCCESS |
| gateway-service | 93 | 0 | 0 | 0 | BUILD SUCCESS |
| discovery-service | 1 | 0 | 0 | 0 | BUILD SUCCESS |
| **Microservices + bibliothèque** | **907** | **0** | **0** | **0** | **11 succès** |
| backend monolithique | 8 | 0 | 0 | 0 | BUILD SUCCESS |
| **Total Java de cette campagne** | **915** | **0** | **0** | **0** | **12 succès** |
| Angular ChromeHeadless | 792 | 0 | — | 0 | Succès, code 0 |

Build Angular production : **code 0**, bundle initial **868,37 kB**, budget d'avertissement **500 kB**, dépassement **368,37 kB**. Aucun seuil augmenté et aucun échec masqué. Les avertissements de JVM agent dynamique, H2 et LF/CRLF sont conservés dans les sorties ; aucune correction de code pendant cet audit.

Validations statiques :
- Compose principal avec valeurs uniquement synthétiques et fichier env vide : code 0.
- Suppression de chacune des deux variables obligatoires du seul processus de validation : rejet attendu, code 1, nom de variable identifié.
- Compose principal + rotation avec variables synthétiques : code 0.
- `node --check scripts/e2e/prepare-sprint1b.mjs` : code 0 ; générateur non exécuté.
- `git diff --check` : code 0.
- Les 93 tests Gateway incluent routage, gardes et cohérence Compose ; aucune interprétation de config en readiness réelle.

Commandes exécutées :
```powershell
git status --short
git status --porcelain=v1 --untracked-files=all
git branch -avv
git worktree list --porcelain
git log --oneline origin/microservices..HEAD
git rev-list --left-right --count HEAD...refs/remotes/origin/microservices
git diff --stat
git diff --name-status
git diff --cached --stat
git diff --cached --quiet
git ls-files --unmerged
git ls-files --modified --others --exclude-standard
git ls-files --others --ignored --exclude-standard
git diff --check
git ls-remote origin refs/heads/microservices refs/heads/main

# Pour chaque module du tableau, avec son POM :
mvn -B "-Dmaven.repo.local=C:\Users\ASUS\.m2\repository" -f microservices/<module>/pom.xml test
mvn -B "-Dmaven.repo.local=C:\Users\ASUS\.m2\repository" -f backend/pom.xml test

npm.cmd --prefix frontend test -- --watch=false --browsers=ChromeHeadless
npm.cmd --prefix frontend run build -- --configuration production

# config seulement ; env vide sous target, processus à variables synthétiques :
docker compose --env-file target/precommit-empty-environment.txt -f docker-compose.yml config --quiet
docker compose --env-file target/precommit-empty-environment.txt -f docker-compose.yml -f docker-compose.rotation.yml config --quiet

node --check scripts/e2e/prepare-sprint1b.mjs
```

Les scripts en mémoire ont aussi lu les diffs/logs historiques et comparé les valeurs sans les restituer, calculé les SHA-256 et interrogé les métadonnées publiques GitHub. Le fichier d'environnement de validation est vide, ignoré, créé sous target et conservé ; aucun .env existant lu par Compose. Les scripts PowerShell de rotation ne sont pas exécutés : seuls leurs contenus et modèles sont examinés.

Journaux générés : `microservices/<module>/target/precommit-audit.log`, `backend/target/precommit-audit.log`, `frontend/tmp/precommit-angular.log`, `frontend/tmp/precommit-build.log`, `target/precommit-diff-check.log`. Ils sont ignorés, non proposés au commit.

Les tests utilisent H2, MockMvc/Mockito et HTTP local simulé ; Angular utilise ChromeHeadless/HttpTestingController. Ils ne constituent ni un E2E réel ni la validation de MySQL, des images ou de la configuration du CRM actif.

## 6. Fichiers sensibles ou temporaires à exclure

- `.env`, variantes locales/production et tout secret/certificat privé. Ne pas forcer leur ajout ; .env.example reste admissible uniquement avec champs sensibles vides.
- Tous les `target/`, `frontend/dist/`, `frontend/.angular/`, `frontend/node_modules/`, `frontend/tmp/`, rapports Surefire, classes/JAR et logs.
- Profils navigateur, bases cache/SQLite, cookies, captures non revues ; `b.png` exclu par défaut.
- `docs/CDC CRM 1.pdf` hors lot de code par défaut : confirmer publication du document source séparément.
- Dumps SQL, bases et fichiers d'environnement générés, même synthétiques ; le SQL d'initialisation généré sous target reste un artefact, pas une migration approuvée.

Inventaire ignoré complet à un instant de la campagne : **54 562 fichiers**, majoritairement frontend/tmp et node_modules ; ce nombre peut augmenter avec les tests. L'ancien premier comptage tronqué n'est pas utilisé. Aucun de ces fichiers n'est supprimé. Les listes des 200 candidats textuels ci-dessous sont une proposition après revue, pas une autorisation d'ajout automatique.

## 7. Plan de commits proposé

Ordre recommandé : **1 → 2 → 3 → 4 → 5**. Les fichiers sont pris comme snapshots complets de l'état testé ; pas de prétention à reconstruire un commit par ancien sprint à partir de hunks entremêlés. Après autorisation future, chaque snapshot stagé devra être relu et les tests adaptés à cet état intermédiaire exécutés. La réussite du working tree complet ne prouve pas automatiquement la réussite de chaque préfixe de commits.

### 1 — Configuration, rotation et monolithe

Message proposé : `fix(config): externaliser les secrets et isoler les tests legacy`.

Regroupe les changements antérieurs au Sprint 1B : monolithe H2, suppression des valeurs de secours, exemple vide, CI et procédures de rotation. Aucun secret réel à inclure. Dépendances : ressources de test H2 et dépendance POM doivent rester ensemble. La procédure de rotation n'est pas son exécution ; aucune modification de base ou secret autorisée ici.

Chemins exacts (14 fichiers constatés) :

- `.env.example`
- `.github/workflows/build.yml`
- `.gitignore`
- `README.md`
- `backend/pom.xml`
- `backend/src/main/java/com/crm/security/jwt/JwtService.java`
- `backend/src/main/resources/application.properties`
- `backend/src/test/java/com/crm/security/jwt/JwtConfigurationTest.java`
- `backend/src/test/resources/application.properties`
- `docker-compose.rotation.yml`
- `docs/ROTATION_EXECUTION.md`
- `docs/ROTATION_SECRETS.md`
- `scripts/rotation/Rotation-Helpers.ps1`
- `scripts/rotation/Test-RotationPlan.ps1`

Exclusions : tous les fichiers des autres groupes à ce stade, ainsi que la liste globale sensible/temporaire ci-dessous. Aucun ajout récursif global ni `git add .` proposé.

### 2 — Identity : sessions, uid, introspection et annuaire

Message proposé : `fix(identity): valider les sessions et exposer un annuaire multi-roles`.

Snapshot complet des fichiers Identity concernés, incluant Sprint 1 et lots A/B/2A.1. Éviter un découpage par sprint qui isolerait DTO, émission uid, politique et tests associés. Dépend du socle configuration ; les autres services ne doivent pas être livrés avant cet Identity compatible. Ne change pas les rôles attribués.

Chemins exacts (24 fichiers constatés) :

- `microservices/identity-service/src/main/java/com/crm/identity/config/SecurityConfig.java`
- `microservices/identity-service/src/main/java/com/crm/identity/controller/AccessTokenValidationController.java`
- `microservices/identity-service/src/main/java/com/crm/identity/controller/AuthenticationController.java`
- `microservices/identity-service/src/main/java/com/crm/identity/dto/LoginResponse.java`
- `microservices/identity-service/src/main/java/com/crm/identity/dto/UserResponse.java`
- `microservices/identity-service/src/main/java/com/crm/identity/repository/RefreshTokenRepository.java`
- `microservices/identity-service/src/main/java/com/crm/identity/security/AccessTokenValidationFilter.java`
- `microservices/identity-service/src/main/java/com/crm/identity/security/AccessTokenValidationService.java`
- `microservices/identity-service/src/main/java/com/crm/identity/security/CustomUserDetailsService.java`
- `microservices/identity-service/src/main/java/com/crm/identity/security/JwtAuthenticationFilter.java`
- `microservices/identity-service/src/main/java/com/crm/identity/security/JwtService.java`
- `microservices/identity-service/src/main/java/com/crm/identity/service/impl/AuthenticationServiceImpl.java`
- `microservices/identity-service/src/main/java/com/crm/identity/service/impl/IdentityServiceImpl.java`
- `microservices/identity-service/src/test/java/com/crm/identity/AccessTokenValidationAvailabilityTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/AccessTokenValidationIntegrationTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/AdminEmployeeCreationTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/SessionSecurityIntegrationTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/UidIssuanceIntegrationTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/security/AccessTokenClaimsTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/security/AccessTokenValidationServiceTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/security/JwtServiceTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/security/UidIssuanceTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/service/impl/DirectoryRolesTest.java`
- `microservices/identity-service/src/test/java/com/crm/identity/service/impl/RefreshTokenSecurityTest.java`

Exclusions : tous les fichiers des autres groupes à ce stade, ainsi que la liste globale sensible/temporaire ci-dessous. Aucun ajout récursif global ni `git add .` proposé.

### 3 — Sécurité distribuée, consommateurs, PDF et Gateway

Message proposé : `feat(security): integrer la validation Identity et proteger les services`.

Groupe atomique de common-security, sept consommateurs, leurs configurations/tests, protections Gateway/Compose et PDF Java. Séparer la bibliothèque stricte des propriétés de ses consommateurs ferait échouer leurs contextes. Les PDF, POM et tests de lecture partagent aussi des fichiers modifiés avec l'intégration : inclure leurs fichiers complets plutôt que reconstruire des hunks non testés. Dépend d'Identity ; inclut les adaptations HR au nouveau contrat d'annuaire. Groupe large mais cohérent ; ne vaut pas autorisation de livraison.

Chemins exacts (99 fichiers constatés) :

- `docker-compose.yml`
- `microservices/common-security/src/main/java/com/crm/security/AccountValidationClient.java`
- `microservices/common-security/src/main/java/com/crm/security/AccountValidationProperties.java`
- `microservices/common-security/src/main/java/com/crm/security/AccountValidationResult.java`
- `microservices/common-security/src/main/java/com/crm/security/JwtAuthenticationFilter.java`
- `microservices/common-security/src/main/java/com/crm/security/JwtSecurityAutoConfiguration.java`
- `microservices/common-security/src/main/java/com/crm/security/JwtService.java`
- `microservices/common-security/src/test/java/com/crm/security/AccountValidationClientTest.java`
- `microservices/common-security/src/test/java/com/crm/security/AccountValidationConfigurationTest.java`
- `microservices/common-security/src/test/java/com/crm/security/AccountValidationHttpTest.java`
- `microservices/common-security/src/test/java/com/crm/security/AccountValidationPerformanceTest.java`
- `microservices/common-security/src/test/java/com/crm/security/JwtAuthenticationFilterTest.java`
- `microservices/common-security/src/test/java/com/crm/security/JwtServiceTest.java`
- `microservices/customer-service/src/main/resources/application.yml`
- `microservices/customer-service/src/test/java/com/crm/customer/CentralizedJwtSecurityTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/CustomerReadSecurityTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/CustomerServiceIntegrationTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/CustomerServiceSmokeTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/security/AccountValidationConfigurationTest.java`
- `microservices/customer-service/src/test/java/com/crm/customer/security/TestIdentitySupport.java`
- `microservices/department-service/src/main/resources/application.yml`
- `microservices/department-service/src/test/java/com/crm/department/CentralizedJwtSecurityTest.java`
- `microservices/department-service/src/test/java/com/crm/department/DepartmentServiceIntegrationTest.java`
- `microservices/department-service/src/test/java/com/crm/department/DepartmentServiceSmokeTest.java`
- `microservices/department-service/src/test/java/com/crm/department/security/AccountValidationConfigurationTest.java`
- `microservices/department-service/src/test/java/com/crm/department/security/TestIdentitySupport.java`
- `microservices/employee-service/src/main/java/com/crm/employee/client/DepartmentClientConfiguration.java`
- `microservices/employee-service/src/main/java/com/crm/employee/client/DepartmentClientFallback.java`
- `microservices/employee-service/src/main/java/com/crm/employee/client/DepartmentClientFallbackFactory.java`
- `microservices/employee-service/src/main/java/com/crm/employee/exception/GlobalExceptionHandler.java`
- `microservices/employee-service/src/main/java/com/crm/employee/service/impl/EmployeeServiceImpl.java`
- `microservices/employee-service/src/main/resources/application.yml`
- `microservices/employee-service/src/test/java/com/crm/employee/CentralizedJwtSecurityTest.java`
- `microservices/employee-service/src/test/java/com/crm/employee/EmployeeReadSecurityTest.java`
- `microservices/employee-service/src/test/java/com/crm/employee/EmployeeServiceIntegrationTest.java`
- `microservices/employee-service/src/test/java/com/crm/employee/EmployeeServiceSmokeTest.java`
- `microservices/employee-service/src/test/java/com/crm/employee/client/DepartmentClientAuthorizationTest.java`
- `microservices/employee-service/src/test/java/com/crm/employee/client/DepartmentClientFallbackTest.java`
- `microservices/employee-service/src/test/java/com/crm/employee/security/AccountValidationConfigurationTest.java`
- `microservices/employee-service/src/test/java/com/crm/employee/security/TestIdentitySupport.java`
- `microservices/fees-service/src/main/resources/application.yml`
- `microservices/fees-service/src/test/java/com/crm/fees/CentralizedJwtSecurityTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/FeeReadSecurityTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/FeesServiceIntegrationTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/FeesServiceSmokeTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/security/AccountValidationConfigurationTest.java`
- `microservices/fees-service/src/test/java/com/crm/fees/security/TestIdentitySupport.java`
- `microservices/gateway-service/src/main/java/com/crm/gateway/security/InternalPathPolicy.java`
- `microservices/gateway-service/src/main/java/com/crm/gateway/security/InternalPathRoutingFilter.java`
- `microservices/gateway-service/src/main/java/com/crm/gateway/security/InternalPathWebFilter.java`
- `microservices/gateway-service/src/test/java/com/crm/gateway/ComposeSecurityConfigurationTest.java`
- `microservices/gateway-service/src/test/java/com/crm/gateway/GatewaySecurityRoutingTest.java`
- `microservices/gateway-service/src/test/java/com/crm/gateway/security/InternalPathPolicyTest.java`
- `microservices/hr-service/pom.xml`
- `microservices/hr-service/src/main/java/com/crm/hr/client/DirectoryAccount.java`
- `microservices/hr-service/src/main/java/com/crm/hr/client/LeaveDirectoryClient.java`
- `microservices/hr-service/src/main/java/com/crm/hr/controller/LeaveAssociationController.java`
- `microservices/hr-service/src/main/java/com/crm/hr/controller/LeavePdfController.java`
- `microservices/hr-service/src/main/java/com/crm/hr/exception/GlobalExceptionHandler.java`
- `microservices/hr-service/src/main/java/com/crm/hr/pdf/PdfReport.java`
- `microservices/hr-service/src/main/resources/application.yml`
- `microservices/hr-service/src/test/java/com/crm/hr/AttendanceServiceIntegrationTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/CentralizedJwtSecurityTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/HrAdministrativeReadSecurityTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/HrServiceIntegrationTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/HrServiceSmokeTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/LeaveAssociationReassignmentTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/LeavePdfTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/LeaveSecurityIntegrationTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/LeaveServiceIntegrationTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/TrainingServiceIntegrationTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/client/DirectoryAccountTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/client/LeaveDirectoryClientTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/security/AccountValidationConfigurationTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/security/TestIdentitySupport.java`
- `microservices/invoice-service/pom.xml`
- `microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoicePdfController.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/pdf/PdfReport.java`
- `microservices/invoice-service/src/main/resources/application.yml`
- `microservices/invoice-service/src/test/java/com/crm/invoice/CentralizedJwtSecurityTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/InvoicePdfTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceReadSecurityTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceServiceIntegrationTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/InvoiceServiceSmokeTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/PdfPaginationTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/security/AccountValidationConfigurationTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/security/TestIdentitySupport.java`
- `microservices/payroll-service/pom.xml`
- `microservices/payroll-service/src/main/java/com/crm/payroll/controller/PayrollPdfController.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/pdf/PdfReport.java`
- `microservices/payroll-service/src/main/resources/application.yml`
- `microservices/payroll-service/src/test/java/com/crm/payroll/CentralizedJwtSecurityTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollPdfTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollReadSecurityTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollServiceIntegrationTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollServiceSmokeTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/security/AccountValidationConfigurationTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/security/TestIdentitySupport.java`
- `scripts/Test-SecureConfiguration.ps1`

Exclusions : tous les fichiers des autres groupes à ce stade, ainsi que la liste globale sensible/temporaire ci-dessous. Aucun ajout récursif global ni `git add .` proposé.

### 4 — Frontend : session, PDF et onboarding administratif

Message proposé : `feat(frontend): fiabiliser les sessions et administrer les comptes employes`.

Regroupe PDF, guards/intercepteur/AuthService et Sprint 2A.2, qui partagent plusieurs fichiers. Dépend des endpoints et du contrat d'annuaire des groupes précédents. Aucun élargissement serveur Paie/Facture. Cette granularité est volontairement plus large que les lots historiques pour éviter de proposer des snapshots intermédiaires non compilables.

Chemins exacts (39 fichiers constatés) :

- `frontend/src/app/app.routes.ts`
- `frontend/src/app/core/guards/auth.guard.spec.ts`
- `frontend/src/app/core/guards/auth.guard.ts`
- `frontend/src/app/core/guards/role.guard.spec.ts`
- `frontend/src/app/core/guards/role.guard.ts`
- `frontend/src/app/core/http/pdf-download-context.ts`
- `frontend/src/app/core/interceptors/auth.interceptor.spec.ts`
- `frontend/src/app/core/interceptors/auth.interceptor.ts`
- `frontend/src/app/core/navigation/crm-navigation.ts`
- `frontend/src/app/core/services/auth-navigation.ts`
- `frontend/src/app/core/services/auth.service.spec.ts`
- `frontend/src/app/core/services/auth.service.ts`
- `frontend/src/app/core/services/session-unavailable.ts`
- `frontend/src/app/features/home/home.component.spec.ts`
- `frontend/src/app/features/invoices/invoice-detail.component.html`
- `frontend/src/app/features/invoices/invoice-detail.component.ts`
- `frontend/src/app/features/invoices/invoices.scss`
- `frontend/src/app/features/leaves/leave.service.spec.ts`
- `frontend/src/app/features/leaves/leave.service.ts`
- `frontend/src/app/features/leaves/leaves-page.component.html`
- `frontend/src/app/features/leaves/leaves-page.component.spec.ts`
- `frontend/src/app/features/leaves/leaves-page.component.ts`
- `frontend/src/app/features/payroll/payroll-detail.component.html`
- `frontend/src/app/features/payroll/payroll-detail.component.ts`
- `frontend/src/app/features/payroll/payroll.scss`
- `frontend/src/app/features/users/admin-account-error.ts`
- `frontend/src/app/features/users/onboarding.integration.spec.ts`
- `frontend/src/app/features/users/user-directory.service.spec.ts`
- `frontend/src/app/features/users/user-directory.service.ts`
- `frontend/src/app/features/users/users-access.spec.ts`
- `frontend/src/app/features/users/users-page.component.html`
- `frontend/src/app/features/users/users-page.component.spec.ts`
- `frontend/src/app/features/users/users-page.component.ts`
- `frontend/src/app/navigation-availability.spec.ts`
- `frontend/src/app/shared/pdf-download/_pdf-detail.scss`
- `frontend/src/app/shared/pdf-download/pdf-download.component.spec.ts`
- `frontend/src/app/shared/pdf-download/pdf-download.component.ts`
- `frontend/src/app/shared/pdf-download/pdf-download.service.ts`
- `frontend/src/app/shared/pdf-download/pdf-security.integration.spec.ts`

Exclusions : tous les fichiers des autres groupes à ce stade, ainsi que la liste globale sensible/temporaire ci-dessous. Aucun ajout récursif global ni `git add .` proposé.

### 5 — Audits, feuilles de route et préparation E2E

Message proposé : `docs(crm): documenter les validations et preparer les campagnes E2E`.

Rapports et scripts de préparation uniquement ; les affirmations historiques doivent rester datées et les anciennes réserves ne doivent pas être lues comme un état de déploiement actuel. Ajouter également docs/CRM_PRE_COMMIT_AUDIT.md. Relecture de confidentialité des rapports avant publication. Le script E2E ne démarre rien par lui-même.

Chemins encore présents parmi les 24 fichiers du manifeste historique ; le présent rapport reste également à examiner :

- `RAPPORT_TESTS_GLOBAUX_CRM_2026-10-17.md`
- `REVUE_FINALE_CRM.md`
- `docs/ACCESS_TOKEN_SECURITY_PLAN.md`
- `docs/AUDIT_CONFORMITE_CDC.md`
- `docs/CRM_ROLE_BASED_SPACES_ROADMAP.md`
- `docs/PDF_ANGULAR_MANUAL_VALIDATION.md`
- `docs/PDF_DOCUMENTS.md`
- `docs/SPRINT1B_E2E_OPTIMIZATION_PLAN.md`
- `docs/SPRINT1B_E2E_REVIEW.md`
- `docs/SPRINT1B_LOT_F_REVIEW.md`
- `docs/SPRINT2A_LOT_1_REVIEW.md`
- `scripts/e2e/prepare-sprint1b.mjs`

Exclusions : tous les fichiers des autres groupes à ce stade, ainsi que la liste globale sensible/temporaire ci-dessous. Aucun ajout récursif global ni `git add .` proposé.


## 8. Synchronisation GitHub et protections

Remote origin : `https://github.com/SendesniMaram/CRM.git`, aucune URL avec credential affichée. Destination envisagée : **origin/microservices**, pas main. GitHub indique main comme branche par défaut.

Lecture réseau sans fetch : microservices distant à 39c43b8, main à 45a0890 ; ces valeurs correspondent aux références locales. HEAD est descendant de origin/microservices avec 13 commits d'avance, zéro retard. Aucun conflit d'index ni divergence de graphe constaté ; les futurs conflits après nouveaux changements distants ne peuvent pas être prédits.

API publique GitHub :
- Dépôt : HTTP 200, visibilité public.
- Branche microservices : HTTP 200, `protected=false`.
- Liste de rulesets visible : HTTP 200, liste vide.
- Détail /branches/microservices/protection : HTTP 401 sans authentification ; CLI gh indisponible.

Les métadonnées publiques ne montrent donc pas de protection active pour cette branche. Les permissions du compte, politiques non visibles, restrictions organisationnelles et capacité réelle d'écriture n'ont pas été vérifiées ; aucun push de test effectué. Pas de force-push proposé.

## 9. Vérifications et décisions avant staging/push

1. Examiner le risque de réutilisation des deux valeurs locales déjà historiques/publics, confirmer les environnements concernés et autoriser une rotation séparée avec sauvegarde/retour arrière adaptés. Ne pas essayer ces valeurs contre les bases actives pendant l'audit.
2. Confirmer que les identifiants du script démo non poussé sont exclusivement synthétiques et jamais réutilisés. Revoir également les treize commits existants : un nouveau commit n'efface pas un blob antérieur.
3. Approuver explicitement le découpage, les exclusions binaires et la publication des documents. Revoir tous les futurs diffs stagés, notamment l'absence de .env/log/profil navigateur ; éviter les ajouts globaux.
4. Après autorisation de commit, tester chaque snapshot intermédiaire ou ajuster les groupes atomiques avant de le déclarer vert. Valider aussi sur un runner vierge avec common-security construit depuis les sources.
5. Préparer une CI Angular et des checks obligatoires ; décider les protections de branche et revue PR. Aucune modification GitHub réalisée ici.
6. Relire la référence distante juste avant le push, les commits effectivement inclus, les contrôles de secrets et les permissions ; ne pas modifier/rebaser/forcer automatiquement si la branche a évolué.
7. Conserver le Sprint 1B E2E en attente de capacité dédiée ; aucune livraison n'est autorisée par les succès isolés. Readiness, DB vierge/migrations et secrets de production restent des chantiers distincts.

## 10. Conclusion

Le working tree testé réussit **915 tests Java, 792 tests Angular et le build Angular production**. Les validations Compose et diff/check réussissent. Le plan contient les chemins exacts de **200 fichiers constatés**, plus ce rapport, et deux exclusions binaires explicites.

Vérification finale : les **202 fichiers déjà modifiés/non suivis au début conservent leur SHA-256** ; seul ce rapport apparaît en plus dans cet inventaire. HEAD reste 00c2870956826adbb32b9cc22b9506879f5cdfdd et l'index ne présente toujours aucune différence de contenu avec HEAD. Les sorties générées ignorées sont les seules autres écritures de campagne et ne sont pas proposées au commit.

**VERDICT FINAL : BLOCKED pour donner un feu vert global de publication, en raison de la réutilisation locale de valeurs historiquement publiques et des confirmations de confidentialité encore nécessaires.** Le code validé peut faire l'objet du plan de commits proposé après votre examen ; aucune opération Git modifiant l'index, l'historique ou le distant n'a été effectuée. Aucun déploiement ou Sprint 2B commencé.
