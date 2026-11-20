# Configuration et rotation des secrets CRM

La procédure opérationnelle détaillée, les commandes PowerShell et le retour
arrière sont préparés dans [ROTATION_EXECUTION.md](ROTATION_EXECUTION.md).
Ce document reste le contexte général ; suivre les points d'arrêt du runbook
opérationnel lors d'une future exécution explicitement autorisée.

Cette procédure est préparée, **non exécutée**. Elle nécessite une autorisation
distincte pour modifier les identifiants SQL, le `.env` actif et recréer les
services. Les modifications des fichiers du dépôt ne modifient aucun conteneur
déjà lancé. Les valeurs actuellement exposées restent à remplacer.

## Configuration des fichiers

- Compose exige `MYSQL_ROOT_PASSWORD` et `APP_JWT_SECRET` non vides, sans repli.
- `.env.example` est un modèle vide. Ne pas le copier sur un `.env` existant.
- `.env` et ses variantes sont ignorés ; `.env.example` reste versionnable.
- Le backend historique exige `SPRING_DATASOURCE_URL`,
  `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` et `APP_JWT_SECRET`.
  Spring Boot ne charge pas automatiquement le `.env` utilisé par Compose.
  Fournir ces variables via le lanceur ou le gestionnaire de secrets ; ne pas
  charger un `.env` avec `Invoke-Expression`.
- Les tests du backend utilisent H2 en mémoire et une clé explicitement réservée
  aux tests. La CI backend n'a plus besoin de MySQL.
- Vérifier la configuration avec `powershell -NoProfile -File
  scripts/Test-SecureConfiguration.ps1`. Ce script utilise des valeurs factices,
  un fichier d'environnement temporaire vide et `docker compose config --quiet`.
  Il ne lit pas le `.env` actif et ne démarre aucun conteneur.

## 1. Préparer la fenêtre et la sauvegarde

1. Inventorier les consommateurs : Identity, les sept services métier, le backend
   historique s'il est utilisé, les postes locaux et les éventuels secrets CI.
2. Identifier les comptes MySQL et leurs hôtes autorisés avec un administrateur.
   Le mot de passe de l'environnement du conteneur peut différer de celui du
   volume initialisé ; ne pas le présumer valide.
3. Préparer une sauvegarde cohérente de toutes les bases CRM et vérifier sa
   restauration dans un environnement isolé. Les fichiers de sauvegarde peuvent
   contenir des données et des empreintes de mots de passe : stockage chiffré
   hors du dépôt. Cette préparation n'autorise aucune commande SQL aujourd'hui.
4. Relever en lecture seule les effectifs et invariants DEMO-TNC : 3 départements,
   6 employés, 5 clients, 6 congés, 8 factures, 6 honoraires, 6 paies. Prévoir de
   refaire les contrôles après bascule.
5. Ne jamais supprimer ou réinitialiser le volume `mysql-data`. Ne pas utiliser
   `docker compose down -v`, des scripts d'import ou une initialisation de schéma.

## 2. Générer et stocker les nouvelles valeurs

Générer indépendamment chaque secret avec un générateur cryptographique : au
moins 64 octets aléatoires pour la clé HMAC JWT, encodés en Base64 ; au moins
32 octets aléatoires pour chaque mot de passe DB. Exemple PowerShell de
génération **sans sortie ni modification de fichier**, à exécuter seulement lors
de la rotation autorisée :

```powershell
$jwtBytes = New-Object byte[] 64
$random = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $random.GetBytes($jwtBytes) } finally { $random.Dispose() }
$newJwtSecret = [Convert]::ToBase64String($jwtBytes)
# Transmettre la variable au gestionnaire de secrets approuvé, sans Write-Output.
# Ne pas la placer dans une commande, un journal, un ticket ou un commit.
```

Stocker les valeurs dans le gestionnaire de secrets ou le `.env` local protégé,
uniquement après autorisation. Le fichier `.env.example` doit rester vide.
Ne pas réutiliser les anciennes valeurs ou les fixtures de test.

## 3. Préparer les comptes DB dédiés

| Service | Compte proposé | Base autorisée |
|---|---|---|
| Identity | crm_identity | identity_db |
| Employee | crm_employee | employee_db |
| Department | crm_department | department_db |
| Customer | crm_customer | customer_db |
| HR | crm_hr | hr_db |
| Invoice | crm_invoice | invoice_db |
| Fees | crm_fees | fees_db |
| Payroll | crm_payroll | payroll_db |
| Backend historique, si utilisé | crm_legacy | crm_db |

Après autorisation, créer chaque compte avec un mot de passe indépendant et des
hôtes limités au réseau utile. Prévoir uniquement SELECT, INSERT, UPDATE, DELETE
sur sa propre base, selon les besoins vérifiés. Aucun droit global, GRANT OPTION
ou DDL pour les comptes applicatifs. Les migrations utilisent un compte distinct.
Vérifier les besoins particuliers du backend historique et de son initialiseur
avant de lui retirer les droits DDL ; prévoir son schéma avant de le démarrer.

Préparer ensuite les variables `IDENTITY_DB_USERNAME`, `IDENTITY_DB_PASSWORD`,
etc. pour chaque service et modifier leurs références Compose lors de la bascule.
Ces variables ne sont **pas encore câblées** : les fichiers actuels conservent
la connexion root pour éviter une migration prématurée. Garder les URL et bases
existantes ; maintenir `ddl-auto=validate` et `spring.sql.init.mode=never` dans
les microservices. Aucun changement de permissions métier JWT ou applicatives.

## 4. Bascule autorisée et conséquences

1. Créer puis vérifier les nouveaux comptes avec l'administrateur ; faire une
   vérification des privilèges sans écrire dans les tables métier.
2. Configurer leurs nouvelles variables dans l'environnement protégé. Recréer
   les services applicatifs de façon contrôlée et vérifier leurs healthchecks,
   enregistrement Eureka et lectures de données. Ne pas supprimer les volumes.
3. Changer le mot de passe root exposé par l'administration MySQL et mettre à
   jour son stockage protégé après retrait des dépendances applicatives à root.
   Changer `MYSQL_ROOT_PASSWORD` dans `.env` ne change **pas** le mot de passe
   dans un volume MySQL existant. Prévoir aussi l'actualisation de l'environnement
   et du healthcheck MySQL dans la fenêtre autorisée.
4. Distribuer la même **nouvelle** clé JWT aux émetteurs et validateurs des
   microservices. Le backend historique peut avoir sa propre clé indépendante
   si ses jetons ne sont pas partagés. Vérifier ce périmètre avant la bascule.
5. La validation actuelle ne gère qu'une clé à la fois. Prévoir une fenêtre de
   maintenance pour éviter les erreurs entre instances mises à jour à des moments
   différents. Les anciens access tokens doivent être rejetés ; reconnexion
   attendue. Ne pas conserver une clé exposée pour faciliter une transition.
6. Décider explicitement de la révocation des refresh tokens si une compromission
   est suspectée : changer la clé JWT ne révoque pas automatiquement ces tokens.
   Cette révocation constituerait une écriture et exige l'autorisation de rotation.

## 5. Contrôles après rotation

- Tous les healthchecks UP et MySQL healthy, puis lectures via Gateway pour les
  sept modules. Effectifs et calculs DEMO-TNC inchangés.
- Login avec un compte de test existant, nouveau JWT accepté, ancien JWT rejeté,
  expiration et rôles conservés. Vérifier les accès ADMIN, EMPLOYEE et CLIENT
  sans créer de compte ni changer ses droits. Les logins peuvent écrire des
  refresh tokens : les réaliser uniquement pendant la rotation autorisée.
- Vérifier les redirections, le rafraîchissement et la déconnexion. Ne pas tester
  les écritures métier sur les données réelles.
- Rejouer les tests isolés et `git diff --check`. Vérifier que les nouvelles
  valeurs ne figurent dans aucun fichier versionné, rapport, log ou sortie CI.
- En cas d'échec, conserver les données et diagnostiquer. Ne pas restaurer une
  clé ou un mot de passe exposé comme solution durable ; préparer un autre secret.

## Historique et GitHub

Les anciennes valeurs restent dans l'historique accessible : notamment
`.env.example`, `docker-compose.yml`, `backend/src/main/resources/application.properties`,
`backend/src/main/java/com/crm/security/jwt/JwtService.java`, `README.md`, le
workflow CI et d'anciennes configurations de microservices. Un ancien journal
Identity contient aussi un mot de passe Spring généré dont la validité est inconnue.
Les commits concernés incluent `6362c16`, `5e6f174`, `40c0fb9` et `ba1d614`.

La suppression des littéraux actuels n'efface pas leur historique. La rotation
est nécessaire pour les valeurs actives exposées. Aucune réécriture d'historique
n'est réalisée ni autorisée par cette procédure. Après rotation, décider
séparément si un nettoyage coordonné de l'historique GitHub est nécessaire.
