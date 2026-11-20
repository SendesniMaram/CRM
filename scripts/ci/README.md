# Jenkins CI CRM

Le Jenkinsfile compile et teste `publication-clean` sans analyse SonarQube, sans
credential Sonar, sans Docker et sans deploiement. Les configurations Sonar
existantes, notamment GitHub Actions, restent independantes et inchangees.

## Agent et plugins

- Agent Windows ou Linux avec Git, JDK 21, Maven 3 et Node.js sur son PATH.
- Node 22.12+ ou 24.x recommande ; Node 20.19+ est egalement compatible avec
  Angular 20. Le JDK utilise par Maven doit aussi etre Java 21.
- Chrome/Chromium accessible au compte de service de l'agent ; au besoin,
  configurer `CHROME_BIN` avec son chemin absolu dans les proprietes de l'agent.
  Aucun flag de desactivation du sandbox navigateur n'est ajoute.
- Plugins Jenkins : Pipeline/Declarative Pipeline, Git, JUnit et Timestamper.
- Acces HTTPS aux depots Git, Maven et npm ; aucune base reelle necessaire.
- Execution sequentielle pour contenir la memoire ; verifier la capacite de
  l'agent avant un lancement complet. Un outil absent fait echouer le precontrole.

## Reutiliser le job existant

1. Ouvrir l'ancien job CRM puis Configurer. Conserver une copie privee de sa
   configuration avant adaptation. Utiliser Pipeline script from SCM, et non
   un ancien script inline. Ne pas creer de nouveau job. Ne pas pointer son
   workspace sur le dossier de developpement CRM.
2. SCM Git : `https://github.com/SendesniMaram/CRM.git`.
3. Branch Specifier : `*/publication-clean` ; Script Path : `Jenkinsfile`.
4. Reutiliser le credential Git Jenkins existant uniquement si necessaire pour
   lire le depot. Aucun mot de passe ou token n'est inscrit dans le Jenkinsfile.
5. Examiner les declencheurs, bibliotheques partagees et actions additionnelles :
   aucun ancien appel Docker, deploiement ou Sonar ne doit etre execute en dehors
   du Jenkinsfile. Verifier les plugins et le PATH du compte de l'agent.
6. Apres publication du commit CI, verifier sa presence sur GitHub avant de
   sauvegarder ces reglages. Le lancement **Build Now** reste une action manuelle
   distincte ; la publication Git ne lance pas elle-meme ce job.

Pour un job multibranche, limiter la decouverte a `publication-clean`.
Le pipeline refuse une autre `BRANCH_NAME` et utilise un refspec explicite.
Chaque build extrait les sources dans `ci-workspace/build-<BUILD_NUMBER>` pour
eviter de republier des rapports d'un ancien build. Le cache Maven est propre
au workspace du job (`.m2-ci`), avec installation du module commun du meme commit.
Les sauvegardes et dossiers de developpement ne sont pas nettoyes. La retention
des workspaces doit etre geree par l'administrateur Jenkins, hors du depot local.

## Etapes et resultats attendus

1. Checkout de `publication-clean` avec le credential SCM existant si besoin.
2. Controle des outils et tests du precontrole CI.
3. `common-security` : compilation, tests, JAR et installation Maven locale.
4. Les dix services : compilation, tests, verification et JAR, un apres l'autre.
5. Backend historique : compilation, tests, verification et JAR.
6. Angular : `npm ci`, `npm run test:ci`, build de production.
7. Toujours : publication JUnit et archivage cible des rapports/JAR/build Angular,
   meme si une etape a echoue ; aucun environnement n'est deploye.

Les commandes Maven utilisent `clean`, JaCoCo 0.8.15 et `failIfNoTests=true`.
Les tests Java utilisent les configurations H2 existantes du depot, sans MySQL.
Un code de sortie Maven/npm non nul fait echouer la step `bat`/`sh` et le build.
Les rapports JUnit comportant des echecs forcent egalement le resultat FAILURE.
Un build reussi doit produire des rapports ; leur absence n'est pas ignoree.

Rapports publies : `microservices/*/target/surefire-reports/TEST-*.xml`,
`backend/target/surefire-reports/TEST-*.xml`, `frontend/test-results/angular.xml`
et `ci-tools.xml` pour les tests Node du precontrole CI.
Couverture archivee : JaCoCo XML/HTML et `frontend/coverage/frontend/lcov.info`.
Les archives sont limitees a ces sorties et aux JAR/build Angular : aucun `.env`,
cache Maven, profil navigateur, dump, sauvegarde ou workspace complet.

## Validation et execution manuelles

Depuis la racine, pour le precontrole local :

```powershell
node scripts/ci/check-tools.cjs
node --test scripts/ci/check-tools.test.cjs
```

Depuis `frontend`, les commandes Jenkins reutilisables sont :

```powershell
npm.cmd ci
npm.cmd run test:ci
npm.cmd run build -- --configuration production
```

La compilation Groovy seule ne valide pas le modele Declarative Jenkins.
Pour une validation officielle, utiliser le linter Declarative de l'instance
avec un acces Jenkins existant, puis executer l'ancien job adapte. Ne pas mettre de
token dans le chat, dans le depot ou dans des arguments de commande visibles.
Voir https://www.jenkins.io/doc/book/pipeline/development/#linter.

La publication du Jenkinsfile ne constitue pas une execution Jenkins reussie.
Verifier dans le build reel : chaque stage vert, rapports JUnit complets et
archives disponibles. Tout blocage d'authentification ou d'outil reste a resoudre
sur Jenkins ; il n'est pas masque par le pipeline.
