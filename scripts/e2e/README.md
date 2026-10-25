# Démonstration locale isolée

Ces fichiers préparent une campagne ; ils ne valident pas une pile déployée.
La préparation ne lance ni Docker, ni SQL, ni HTTP, ni génération de secrets.
Ne jamais fusionner ce modèle autonome avec le Compose principal ou celui de rotation.

## Première campagne minimale : API SUPER_ADMIN et Department

Préparation sans démarrage : ajouter `--minimal` au générateur, à la place de
`--demo`. Le modèle contient exclusivement MySQL, Discovery, Identity, Gateway
et Department. Il ne crée que les schémas identity_db et department_db, ne monte
aucune migration Customer et n'exige aucune clé de service ni certificat.
Le bootstrap Hibernate update est réservé à ces schémas éphémères vierges.

Budget candidat : MySQL 1 GiB et quatre JVM de 512 MiB, soit 3 GiB de limites
cumulées. Exiger au moins 4 GiB libres côté hôte avant la campagne et vérifier
la marge Docker réellement disponible ; ces budgets ne sont pas une preuve de
réussite du démarrage. Ne pas lancer en parallèle avec des builds gourmands.
Ne pas arrêter la pile crm ou les applications utilisateur sans permission.

Modèle préparé : `target/crm-sprint1b-e2e-20261024-000002/compose.json`.
Les noms de projet, réseau, images et conteneurs sont propres à cette campagne.
MySQL utilise uniquement un tmpfs de 512 MiB, sans port hôte et sans volume existant.
Les seuls ports métier publiés sont 18080, 18082 et 18084, sur 127.0.0.1.
Discovery n'est pas publié ; les services se joignent sur le réseau isolé.

Après autorisation explicite et précontrôles positifs uniquement :

1. Fournir trois secrets de test indépendants dans un fichier protégé hors dépôt,
   par exemple `C:/Users/ASUS/.crm-demo-20261024-000002/first-e2e.env`.
   Les seules lignes attendues sont E2E_MYSQL_PASSWORD, E2E_JWT_SECRET et
   E2E_ADMIN_PASSWORD, valeurs sans guillemets. Ne pas réutiliser les secrets
   existants ; le JWT doit atteindre 32 octets et le mot de passe bootstrap
   respecter 12 à 72 octets. Ni le fichier ni sa génération ne sont réalisés par
   le générateur. Restreindre les ACL au propriétaire ; ne jamais afficher le contenu.
2. Préparer les JAR actuels puis les quatre images, séquentiellement :

```powershell
$candidateModules = 'common-security','discovery-service','identity-service','gateway-service','department-service'
foreach ($candidateModule in $candidateModules) {
  $candidateGoal = if ($candidateModule -eq 'common-security') { 'install' } else { 'package' }
  mvn -B -o "-Dmaven.repo.local=C:/Users/ASUS/.m2/repository" "-f" "microservices/$candidateModule/pom.xml" "-DskipTests" $candidateGoal
  if ($LASTEXITCODE -ne 0) { throw "Candidate packaging failed" }
}
$candidateCompose = 'target/crm-sprint1b-e2e-20261024-000002/compose.json'
$candidateProject = 'crm-sprint1b-e2e-20261024-000002'
$candidateEnv = 'C:/Users/ASUS/.crm-demo-20261024-000002/first-e2e.env'
docker compose -p $candidateProject --env-file $candidateEnv -f $candidateCompose config --quiet
docker compose -p $candidateProject --env-file $candidateEnv -f $candidateCompose build discovery-service identity-service gateway-service department-service
```

3. Démarrer MySQL seul ; sa healthcheck doit authentifier SELECT 1. Démarrer
   ensuite Discovery, Identity, Gateway puis Department ; entre chaque étape,
   contrôler la disponibilité et l'enregistrement Eureka, avec un délai global
   d'attente borné, sans imprimer les environnements ou les logs non expurgés :

```powershell
docker compose -p $candidateProject --env-file $candidateEnv -f $candidateCompose up -d --no-deps mysql
docker compose -p $candidateProject --env-file $candidateEnv -f $candidateCompose up -d --no-deps discovery-service
docker compose -p $candidateProject --env-file $candidateEnv -f $candidateCompose up -d --no-deps identity-service
docker compose -p $candidateProject --env-file $candidateEnv -f $candidateCompose up -d --no-deps gateway-service
docker compose -p $candidateProject --env-file $candidateEnv -f $candidateCompose up -d --no-deps department-service
node scripts/e2e/run-minimal.mjs --authorized $candidateProject $candidateEnv
```

Le runner vérifie le projet et le port Gateway par les labels Docker avant tout
appel HTTP, puis attend au plus 90 secondes les routes test Identity et Department
(sondes en lecture seule, requêtes bornées à 2 secondes). Il contrôle 401 anonyme, Login réel, CRUD et recherche Department via
Gateway, puis 404 après suppression de sa propre fixture. Aucun JWT n'est imprimé.
La portée est **API HTTP réelle uniquement**, pas un E2E navigateur Angular.
Le runner n'a pas été exécuté en mode autorisé pendant la préparation.
En cas d'échec, il ne nettoie pas automatiquement : des données synthétiques
peuvent rester dans ce seul projet.

4. Arrêt / nettoyage limités au projet candidat, sous autorisation :

```powershell
docker compose -p $candidateProject --env-file $candidateEnv -f $candidateCompose stop
docker compose -p $candidateProject --env-file $candidateEnv -f $candidateCompose down
```

L'arrêt de MySQL détruit déjà les données tmpfs ; elles ne sont pas récupérables.
Le down retire uniquement ces conteneurs et leur réseau. Aucun down -v, prune,
suppression d'image ou arrêt de la pile crm n'est prévu. Le fichier d'identifiants
externe et les artefacts générés restent en place jusqu'à une décision séparée.
Le compte SUPER_ADMIN synthétique est créé par le bootstrap au démarrage Identity.
Les autres rôles et Angular seront abordés après ce premier passage et une
nouvelle vérification des ressources.

## Préparation sans démarrage

Depuis la racine :

```powershell
node scripts/e2e/prepare-sprint1b.mjs YYYYMMDD-HHMMSS --demo
node --test scripts/e2e/demo-model.test.mjs
```

Choisir un identifiant inédit. Les artefacts sont créés sous
`target/crm-sprint1b-e2e-<identifiant>`, sans écraser un répertoire existant.
Le frontend copié cible Gateway sur 18080 ; le lancer ultérieurement sur 14200.
Les ports publiés sont limités à 127.0.0.1. MySQL n'est pas publié et n'utilise
aucun volume persistant : ses données synthétiques disparaissent à l'arrêt.
Les limites cumulées des conteneurs approchent 8 GiB, hors navigateur, build et OS :
faire approuver la capacité disponible avant tout démarrage.

Le modèle historique sans `--demo` reste disponible mais ne couvre pas le profil
Employee TLS ni la migration Customer. Les images doivent être reconstruites depuis
les sources actuelles après les tests, avec les JAR common-security à jour.

## Matériel externe à préparer après autorisation

Ne pas lire ou réutiliser le .env d'exploitation. Fournir séparément :

- `E2E_MYSQL_PASSWORD`, `E2E_JWT_SECRET`, `E2E_ADMIN_PASSWORD` indépendants
  des secrets d'exploitation ; le dernier provisionne uniquement le Super Admin
  de test via le bootstrap local existant.
- `E2E_KEY_MATERIAL_DIR` : chemin absolu extérieur au dépôt, protégé par ACL.
  Il contient `hr-signing-private.pem` (PKCS#8 RSA >= 2048 bits),
  `hr-signing-public.pem` (SPKI), `employee-tls.p12`,
  `employee-tls-password` (mot de passe du PKCS12), et `employee-tls.pem`
  (certificat X.509 public approuvé).
- Le PKCS12 utilise l'alias `employee`. Le certificat TLS doit être valide,
  porter le SAN DNS `employee-service` et être destiné à l'authentification serveur.
  La clé TLS et la clé de signature RS256 HR sont distinctes.

Les montages sont en lecture seule : HR reçoit sa clé privée et le certificat TLS
public ; Employee reçoit son magasin TLS, son fichier de mot de passe et la clé
publique HR. Aucun mot de passe n'est placé dans JAVA_TOOL_OPTIONS.
HR vérifie signature TLS et nom du serveur ; aucun trust-all n'est fourni.
Employee conserve HTTP sur 8083 pour les appels existants et ajoute HTTPS sur 8443,
non publié. Le filtre interne exige toujours TLS réel et le JWT de service lié à
la ressource. Le certificat ne confère aucun rôle utilisateur.

La rotation s'effectue à froid sous maintenance : distribuer la nouvelle clé
publique HR avant la bascule de kid/clé privée, retirer l'ancienne après expiration
des jetons et des requêtes en cours. Pour TLS, distribuer la confiance approuvée
et remplacer le certificat de façon coordonnée ; ne pas désactiver la vérification.
Ne jamais publier les clés, configurations résolues, JWT ou sorties d'environnement.

## Migration Customer

Le modèle de démonstration crée un Customer vierge à partir de
`customer-baseline.sql`, puis applique une copie exacte de la migration versionnée,
avant le premier démarrage Customer en `ddl-auto=validate`. Cela vérifie les
contraintes réelles plutôt que de laisser Hibernate créer une table sans FK.
Les autres domaines gardent le bootstrap Hibernate historique, exclusivement sur
ces bases de démonstration éphémères ; cela ne remplace pas leurs migrations réelles.

Pour une base existante, ne pas employer customer-baseline.sql. Sous autorisation :

1. Identifier l'instance, le schéma, sa version et le responsable ; exécuter
   `customer-preflight.sql`, uniquement en lecture.
2. Exiger MySQL >= 8.0.16, InnoDB, customers.id BIGINT signé avec clé primaire.
   Vérifier aussi le moteur par défaut, foreign_key_checks=1 et les droits CREATE/REFERENCES.
3. Si customer_user_associations existe, comparer toutes les colonnes, unicités,
   FK et CHECK avec la migration. CREATE IF NOT EXISTS ne corrige aucune dérive.
4. Geler les écritures, effectuer une sauvegarde chiffrée du schéma et des données,
   hors dépôt, et réussir une restauration de contrôle sur une instance isolée.
5. Appliquer explicitement la migration approuvée ; vérifier la PK user_id,
   l'unicité customer_id, la FK locale et le CHECK. Aucun mapping automatique.
6. Reprendre Customer puis Invoice après validation. Ne pas convertir les références
   historiques non canoniques des factures ; elles nécessitent une revue métier.

Le DDL MySQL provoque des commits implicites : un ROLLBACK SQL ne l'annule pas.
En cas d'échec, garder la maintenance et diagnostiquer l'état exact. Préférer une
correction en avant ou une reprise applicative compatible en conservant la table.
Ne supprimer aucune association déjà utilisée ; une restauration de données est
une opération séparée et doit être autorisée. La migration ne modifie ni les
customers existants ni les factures. La FK empêche la suppression d'un Customer lié.
Le test H2 ne prouve pas son exécution sur MySQL ; cette validation reste à faire.

## Scénario E2E à autoriser séparément

1. Vérifier mémoire/disque/ports, séparation des secrets et fichiers externes.
   Approuver les builds, le projet exact, le réseau et les seules données synthétiques.
2. Construire les JAR/images candidats ; démarrer MySQL isolé et vérifier une
   authentification SELECT 1. Contrôler l'initialisation Customer et ses contraintes.
3. Démarrer Discovery, Identity, puis les services métier séquentiellement, Gateway
   et le frontend copié. Vérifier readiness et enregistrement Eureka, sans imprimer
   l'environnement résolu. Une dépendance service_started n'est pas une readiness.
4. Se connecter comme Super Admin de test. L'API crée des comptes EMPLOYEE,
   l'inscription publique crée des CLIENT. Il n'existe pas de commande HTTP de
   création ADMIN : prévoir un provisionnement de fixture ADMIN explicitement
   approuvé sur la base Identity isolée, sans forger de JWT ni élargir le RBAC.
5. Créer deux fiches Employee, deux Customer et leurs comptes distincts. Vérifier
   que les uid et identifiants métier ne sont pas supposés égaux. Créer les liens
   via les API administratives, puis des congés et factures de démonstration.
6. ADMIN/SUPER_ADMIN : navigation, CRUD autorisés, formations, présences, dossiers RH,
   associations, facturation, contacts Customer existants et PDF.
7. EMPLOYEE A/B : profils propres via HTTPS interne et congés ; accès croisés refusés.
   Refuser un appel interne via Gateway et un JWT utilisateur sur ce contrat.
8. CLIENT A/B : profil, pagination, détail/PDF et dashboard personnels ; tester les
   IDs étrangers, les headers falsifiés, l'absence d'association et les conflits.
9. Passer d'un compte à l'autre : aucune donnée précédente ; tester 401/403/404,
   interruption Employee/Identity et 503 sans déconnexion injustifiée.
10. Enregistrer les résultats expurgés et distinguer succès réels, refus attendus
    et cas bloqués. N'annoncer aucun E2E réussi avant cette campagne.

Les réserves de rotation des secrets déjà exposés et de publication restent
indépendantes. Ce scénario ne donne aucune autorisation d'utiliser la pile active.
