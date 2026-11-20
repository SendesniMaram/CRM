Rapport des tests globaux CRM — 17 octobre 2026 (Africa/Tunis)

> Archive documentaire : les constats, chiffres, anciens commits et autorisations ci-dessous décrivent la campagne datée du rapport. Ils ne constituent pas un état courant ni un plan à réappliquer sur `publication-clean`. Toute opération réelle nécessite une nouvelle vérification et l’autorisation adaptée.

Verdict : BLOCKED pour une validation globale sans réserve. Aucun échec fonctionnel dans les suites isolées finales. Les configurations sensibles préexistantes et la validation visuelle/authentifiée restante empêchent de déclarer la stabilité globale entièrement vérifiée. Aucun commit ou push autorisé ni effectué.

1. Tests backend

| Module | Réussis | Échecs / erreurs / ignorés |
|---|---:|---|
| common-security | 5 | 0 / 0 / 0 |
| discovery-service | 1 | 0 / 0 / 0 |
| gateway-service | 1 | 0 / 0 / 0 |
| identity-service | 20 | 0 / 0 / 0 |
| employee-service | 49 | 0 / 0 / 0 |
| department-service | 20 | 0 / 0 / 0 |
| hr-service | 155 | 0 / 0 / 0 |
| customer-service | 45 | 0 / 0 / 0 |
| invoice-service | 43 | 0 / 0 / 0 |
| fees-service | 44 | 0 / 0 / 0 |
| payroll-service | 27 | 0 / 0 / 0 |
| Backend historique isolé | 4 | 0 / 0 / 0 |
| Total | 414 | 0 / 0 / 0 |

Onze suites Maven exécutées séparément, datasource forcée vers H2 en mémoire, Eureka réseau désactivé. Discovery nécessite son client interne : relance réussie avec register-with-eureka=false et fetch-registry=false, après un premier échec dû à ma désactivation complète du client.

Backend historique : POM temporaire backend/target/audit-isolated-pom.xml dérivé du POM existant, ajout H2 en portée test et chemins sources/resources explicites, datasource et dialecte H2 forcés. Sources et POM versionné inchangés. Les quatre tests couvrent contexte, Swagger, JWT et mapping. Une tentative antérieure incluant JwtServiceTest a démarré Spring et tenté MySQL : connexion échouée, aucune écriture effectuée. Cette tentative ne constitue pas une validation isolée ; seuls les résultats H2 finaux sont comptés.

Les suites existantes couvrent authentification, JWT, rôles, accès HTTP, contrôleurs, services, DTO, exceptions et calculs. Leur réussite ne prouve pas une couverture exhaustive de chaque règle métier.

2. Angular et build

Commande : npm.cmd test -- --watch=false --browsers=ChromeHeadless
Résultat : 679/679 réussis, Chrome Headless 155, aucun test ignoré annoncé. La dernière référence connue de 669 est dépassée de 10 tests.

Couverture des suites : /home, navbar, sidebar et réduction, menu mobile, retour du focus/Escape, navigation par rôle, redirections après login, dashboard, login, register, formulaires des sept modules, déconnexion, états de chargement et erreurs.

Commande : npm.cmd run build -- --configuration production
Résultat : succès, aucune erreur finale. Bundle initial 852,42 kB bruts, transfert estimé 207,13 kB. Budget warning 500 kB dépassé de 352,42 kB ; seuil erreur 1 MB respecté. /home est chargé à la demande (13,42 kB bruts, transfert estimé 3,95 kB). Risque : coût du chargement initial sur réseau lent ; aucune mesure Lighthouse/Core Web Vitals réalisée. Les premières tentatives Angular ont échoué sur les restrictions de lecture du sandbox ; les relances autorisées réussissent.

3. Infrastructure

MySQL 8 : conteneur running et HEALTHY.
Eureka 8761, Gateway 8080, Identity 8082, Employee 8083, Department 8084, Customer 8085, HR 8086, Payroll 8087, Fees 8088, Invoice 8089 : /actuator/health HTTP 200, status UP.
Les neuf applications attendues sont enregistrées UP dans Eureka.
Angular : http://localhost:4200/home HTTP 200. Cette réponse HTML ne valide pas le rendu authentifié.
Les GET anonymes des sept modules via Gateway répondent HTTP 401, comme attendu. Cela ne valide pas les droits après connexion.
Aucun service indisponible constaté ; aucun redémarrage effectué.

4. Données DEMO-TNC

Lecture MySQL effectuée avec les identifiants applicatifs conservés en mémoire, uniquement SELECT dans une transaction START TRANSACTION READ ONLY, puis COMMIT de cette transaction de lecture. Aucune instruction SQL d'écriture ni modification de donnée.
Effectifs concordants avec scripts/demo/technova.json : départements 3, employés 6, clients 5, congés 6, factures 8, honoraires 6, paies 6. Les clients ont été identifiés par leurs adresses demo.tnc.c01 à c05 sur example.invalid.
Anomalies trouvées : 0 pour références département/employé, références et dates des congés, client/date/total des factures, références et produit taux × heures des honoraires, formule du net des paies liées aux employés DEMO-TNC.
Limite : il ne s'agit pas d'une comparaison de toutes les colonnes de chaque ligne au JSON ni d'un parcours de chaque écran connecté.
L'accès root avec le mot de passe de l'environnement du conteneur a été refusé ; les identifiants applicatifs ont permis la lecture. Cause possible : divergence avec le volume initialisé, non établie. Aucun identifiant affiché.

5. Sécurité et anomalies

- docker-compose.yml:52 et les paramètres sensibles des services (125–126, 152–153, 183–184, 214–215, 245–246, 276–277, 307–308, 338–339) contiennent des valeurs littérales de repli pour mots de passe/clé JWT. Préexistant et non modifié. À traiter ou qualifier explicitement avant une approbation globale de sécurité ; aucun changement automatique effectué.
- backend/src/main/resources/application.properties:5 contient un mot de passe littéral, préexistant. Ne pas le publier s'il est réel ; sa nature doit être confirmée sans en afficher la valeur.
- Recherche heuristique dans les fichiers modifiés/non suivis : les candidats Angular détectés sont des valeurs fictives de tests. Aucun secret réel identifié dans le lot frontend. Ce scan ne certifie pas l'historique Git et n'est pas un audit complet des secrets.
- Le bundle dépasse son budget d'avertissement.
- Configuration MySQL root non concordante avec l'accès testé, à clarifier séparément.

6. UI/UX : preuve et limites

Automatisé : comportements responsive simulés, sidebar desktop réduite, fermeture mobile/Escape, navigation et éléments accessibles selon rôle, libellés ARIA, lien d'évitement, focus restauré, état actif et fragments, compte, déconnexion.
Inspection du code : breakpoints 767/650/1100 px, grilles adaptatives, focus visible, tooltips en mode réduit, reduced-motion, noms accessibles.
Non réalisé : inspection visuelle réelle desktop/tablette/mobile, zoom 200 %, débordements avec noms longs, lecteur d'écran, mesure des contrastes calculés, validation visuelle des tooltips/erreurs/chargements et parcours authentifiés en lecture seule des sept modules via Gateway.
Ces vérifications et la qualification des configurations sensibles restent nécessaires pour lever le verdict BLOCKED. Aucun changement métier proposé ou appliqué.

7. État Git et candidats

Branche : microservices. HEAD : 814a0ef — docs(mvp): documenter les tests et les limites connues.
11 fichiers suivis modifiés ; 15 fichiers non suivis présents avant ce rapport. Aucun fichier indexé. git diff --check : code 0, pas d'erreur d'espacement ; avertissements de conversion LF vers CRLF.
REVUE_FINALE_CRM.md préservé intégralement (SHA256 65330338E2ED639588BE8D69C1622DD243C5EF8091D28000CA31E858527FC5B5).
Les fichiers ci-dessous sont candidats à un futur commit après levée des réserves ; ils n'ont pas été ajoutés à l'index. REVUE_FINALE_CRM.md reste une documentation utilisateur préservée, dont l'inclusion est à décider.

- `frontend/src/app/app.routes.spec.ts`
- `frontend/src/app/app.routes.ts`
- `frontend/src/app/core/services/auth-navigation.spec.ts`
- `frontend/src/app/core/services/auth-navigation.ts`
- `frontend/src/app/features/auth/login/login.component.spec.ts`
- `frontend/src/app/features/auth/login/login.router.spec.ts`
- `frontend/src/app/layout/components/layout/layout.component.ts`
- `frontend/src/app/layout/components/navbar/navbar.component.ts`
- `frontend/src/app/layout/components/sidebar/sidebar.component.spec.ts`
- `frontend/src/app/layout/components/sidebar/sidebar.component.ts`
- `frontend/src/app/routing-audit.spec.ts`
- `REVUE_FINALE_CRM.md`
- `frontend/src/app/core/navigation/crm-navigation.spec.ts`
- `frontend/src/app/core/navigation/crm-navigation.ts`
- `frontend/src/app/core/navigation/workspace-clock.spec.ts`
- `frontend/src/app/core/navigation/workspace-clock.ts`
- `frontend/src/app/features/home/home.component.html`
- `frontend/src/app/features/home/home.component.scss`
- `frontend/src/app/features/home/home.component.spec.ts`
- `frontend/src/app/features/home/home.component.ts`
- `frontend/src/app/layout/components/layout/layout.component.spec.ts`
- `frontend/src/app/layout/components/navbar/account-dialog.component.ts`
- `frontend/src/app/layout/components/navbar/navbar.component.html`
- `frontend/src/app/layout/components/navbar/navbar.component.scss`
- `frontend/src/app/layout/components/navbar/navbar.component.spec.ts`
- `frontend/src/styles/_workspace.scss`

- RAPPORT_TESTS_GLOBAUX_CRM_2026-10-17.md (présent rapport)

Les logs audit-maven-*.log, angular-audit-tests.log, angular-audit-build.log et les sorties target/dist sont des artefacts locaux ignorés ; ne pas les ajouter de force. Aucun changement métier, déploiement, redémarrage, changement Oracle, commit ou push effectué. Seules les inspections Git demandées ont été exécutées.
