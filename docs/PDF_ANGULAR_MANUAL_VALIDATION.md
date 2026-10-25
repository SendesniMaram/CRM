# PDF — contrôles techniques et parcours Angular avec comptes existants

> Archive documentaire : les constats, chiffres, anciens commits et autorisations ci-dessous décrivent la campagne datée du rapport. Ils ne constituent pas un état courant ni un plan à réappliquer sur `publication-clean`. Toute opération réelle nécessite une nouvelle vérification et l’autorisation adaptée.

## État technique vérifié le 17 octobre 2026 à 16:01:48 UTC

| Service | Actuator | Instance actuelle Eureka | PDF sans JWT, accès direct | PDF sans JWT via Gateway |
| --- | --- | --- | --- | --- |
| invoice-service | 200 / UP | UP | 401 | 401 |
| hr-service | 200 / UP | UP | 401 | 401 |
| payroll-service | 200 / UP | UP | 401 | 401 |

Les trois conteneurs et leurs images correspondaient, lors du contrôle historique du 17 octobre, à la mise en service résumée dans [PDF_DOCUMENTS.md](PDF_DOCUMENTS.md), sans nouveau redémarrage pendant ce contrôle. Cet état n'est pas un inventaire actuel. Les JAR exécutés avaient été comparés aux JAR testés et contiennent leurs contrôleurs PDF ; aucun montage n'injecte un autre JAR. Les refus JSON portent le chemin interne `/api/invoices/1/pdf`, `/api/leaves/1/pdf` ou `/api/payrolls/1/pdf`, y compris via Gateway : le retrait du préfixe correspond aux routes prévues. Un 401 ne prouve pas à lui seul une génération réussie ; aucun PDF authentifié reçu dans cette campagne.

Les prévols OPTIONS réels des trois routes renvoient 200 avec origine `http://localhost:4200`, méthode GET autorisée et en-tête Authorization autorisé. Accept est un en-tête CORS safelisté. Aucune modification CORS, JWT, permission ou sécurité nécessaire à ce contrôle.

MySQL reste healthy, même conteneur et même volume `crm_mysql-data`. Les huit services non ciblés conservent leurs identités et dates de démarrage. Les empreintes des fichiers protégés restent identiques. Aucun Login, Refresh, SQL d'écriture ou téléchargement de données personnelles effectué. Trace locale ignorée : `frontend/tmp/visual-review/pdf-technical-recheck.json`.

## Autorisation indispensable avant toute nouvelle connexion

L'utilisateur a confirmé ne pas disposer de sessions de démonstration dont la validité soit certaine. Les essais authentifiés restent **BLOCKED**.

Dans le code Identity existant :

- POST `/identity/api/auth/login` supprime les refresh tokens précédents du compte et en persiste un nouveau.
- POST `/identity/api/auth/refresh` révoque/met à jour un jeton et en persiste un nouveau ; Angular peut appeler automatiquement cet endpoint lors d'un renouvellement.
- Se connecter dans un autre profil ne contourne pas cette règle : les refresh tokens du même compte dans d'autres sessions peuvent cesser de fonctionner. Utiliser des comptes de revue déjà existants et disponibles pour cet usage.

**Ne pas lancer ces opérations avant autorisation explicite d'une exception limitée aux écritures de jetons de session Identity.** Cette autorisation n'inclut ni création de compte, changement de mot de passe, attribution de rôle, association RH, modification d'un dossier ni changement de secret. Sans elle, arrêter aux contrôles techniques ci-dessus.

## Parcours de test après autorisation

### 1. Préparer les comptes et les dossiers

Utiliser un compte existant ADMIN/SUPER_ADMIN et deux comptes EMPLOYEE existants. Les employés doivent déjà être associés côté serveur à deux employés distincts ; au moins l'un doit posséder un congé de démonstration. Si les comptes ou associations nécessaires manquent, arrêter et signaler le cas bloqué. Ne pas utiliser Register, modifier les rôles ou créer une association pour débloquer le test.

Choisir uniquement des dossiers synthétiques existants et identifiés : facture DEMO-TNC, congé dont le motif contient DEMO-TNC et paie liée à une référence employé DEMO-TNC. Vérifier leur correspondance avec le jeu de démonstration connu. Ne pas supposer qu'un dossier est fictif à partir du seul nom du compte. Ne pas exporter les données personnelles réelles de vos comptes existants. Si aucun dossier de démonstration n'est disponible, arrêter sans insertion.

Ouvrir le frontend déjà disponible sur `http://localhost:4200`. Utiliser des profils Chrome séparés pour l'administrateur et les deux employés afin de ne pas mélanger les sessions. Ne pas démarrer ni redémarrer Angular ou les API si elles ne sont plus disponibles : signaler le besoin d'autorisation.

### 2. Administrateur : facture et paie

1. Après autorisation des écritures de session, se connecter par `/login` avec le compte existant ; ne pas communiquer le mot de passe ou JWT à l'agent dans le chat.
2. Ouvrir `/invoices`, sélectionner la facture de démonstration puis sa page `/invoices/{id}` et cliquer **Télécharger PDF**.
3. Vérifier le spinner et le bouton désactivé pendant la requête, puis le fichier `facture-{id}.pdf`.
4. Ouvrir `/payroll`, sélectionner la paie de démonstration puis `/payroll/{id}` et utiliser le même bouton. Vérifier `paie-{id}.pdf`.
5. Dans Chrome, onglet Réseau, filtrer sur `/pdf` : GET vers `http://localhost:8080/invoice/api/invoices/{id}/pdf` et `http://localhost:8080/payroll/api/payrolls/{id}/pdf`, statut **200**, Content-Type **application/pdf**, Content-Disposition **attachment**, Cache-Control **no-store**.
6. Ouvrir les fichiers reçus : A4, accents lisibles, contenu correspondant au dossier, montants identiques à l'API et à l'écran. Le total/net n'est pas recalculé dans Angular. L'absence de champs non enregistrés n'est pas remplacée par une donnée fictive.

Ne pas partager une capture des en-têtes de requête, une commande « Copy as cURL » ou un export HAR contenant l'en-tête Authorization. Relever seulement le chemin, statut et nom du fichier. Les captures et PDF à conserver pour revue doivent contenir uniquement les données synthétiques identifiées.

### 3. Salarié propriétaire : congé

1. Se connecter avec l'employé associé au congé de démonstration, après autorisation de Login/Refresh.
2. Ouvrir `/leaves` ; le serveur doit filtrer la liste pour ce propriétaire.
3. Cliquer **Télécharger PDF** sur le dossier de démonstration : GET `/hr/api/leaves/{id}/pdf`, statut **200**, fichier `conge-{id}.pdf`.
4. Comparer référence employé, dates, motif, statut et décision réellement enregistrée. Ne pas approuver, refuser, modifier ou supprimer le congé pendant le test.

### 4. Vérifier 403 et 404 sans écriture métier

La liste des congés masque normalement les dossiers d'un autre employé. Pour tester le backend lui-même, utiliser le deuxième compte EMPLOYEE existant, dans son propre profil Chrome, avec l'ID du congé synthétique du premier employé.

Le helper Console suivant, à exécuter uniquement par l'utilisateur après autorisation de session, lit le JWT de sa propre session Angular **sans l'afficher**. Il effectue seulement un GET sur les trois routes fermées autorisées et affiche uniquement statut et Content-Type. Il ne passe pas par l'intercepteur Angular et ne déclenche pas de Refresh. Il ne remplace pas les essais de téléchargement par les boutons.

```javascript
async function verifierAccesPdf(module, id) {
  const routes = {
    facture: 'invoice/api/invoices',
    conge: 'hr/api/leaves',
    paie: 'payroll/api/payrolls'
  };
  if (!Object.hasOwn(routes, module) || !Number.isSafeInteger(id) || id <= 0) {
    throw new Error('Module ou identifiant invalide.');
  }
  const token = JSON.parse(sessionStorage.getItem('crm.auth.session') || 'null')?.accessToken;
  if (!token) throw new Error('Aucune session locale. Arrêter le test.');
  const response = await fetch(`http://localhost:8080/${routes[module]}/${id}/pdf`, {
    method: 'GET',
    headers: { Authorization: `Bearer ${token}`, Accept: 'application/pdf, application/json' },
    cache: 'no-store'
  });
  await response.body?.cancel(); // Ne pas afficher ni sauvegarder le corps du dossier.
  return { statut: response.status, type: response.headers.get('Content-Type') };
}
```

Exemples d'appels à adapter aux identifiants de démonstration vérifiés :

```javascript
// Profil du deuxième employé : ID du congé DEMO du premier employé.
await verifierAccesPdf('conge', ID_CONGE_DEMO_AUTRE_EMPLOYE); // attendu : 403

// Profil EMPLOYEE : accès interdit aux documents administratifs.
await verifierAccesPdf('paie', ID_PAIE_DEMO);                // attendu : 403
await verifierAccesPdf('facture', ID_FACTURE_DEMO);          // attendu : 403

// Profil ADMIN : identifiant confirmé absent lors d'une lecture préalable.
await verifierAccesPdf('facture', ID_ABSENT_CONFIRME);        // attendu : 404
await verifierAccesPdf('conge', ID_ABSENT_CONFIRME);          // attendu : 404
await verifierAccesPdf('paie', ID_ABSENT_CONFIRME);           // attendu : 404
```

Un 401 dans ces essais indique une session absente/expirée : ce n'est pas un 403 ou 404 validé. Arrêter et ne pas renouveler la session au-delà de l'autorisation accordée. Ne pas fabriquer de JWT ni lire une clé de signature. Le contrôle sans JWT a déjà été réalisé avec succès sur les trois endpoints ; il n'est pas nécessaire de déclencher une expiration de session pour le répéter.

## Critères de fin

Conserver un tableau pour chaque document : rôle, dossier synthétique, URL, statut, nom reçu, contenu conforme oui/non. Aucun JWT, mot de passe ou contenu personnel dans le rapport.

**READY FOR COMMIT** seulement après trois téléchargements 200 réels conformes, propriété congé vérifiée, refus croisé et restrictions de rôle 403, cas inexistants 404 et parcours Angular confirmés. Jusqu'alors : **BLOCKED pour la validation fonctionnelle authentifiée**, avec disponibilité technique des trois services confirmée.
