# Jeu de démonstration TechNova Consulting

Les noms, sociétés et adresses de `technova.json` sont explicitement synthétiques. Les 11 téléphones utilisent uniquement `+1 202 555 0100–0199`, plage fictive non opérationnelle réservée par [NANPA](https://nanpa.com/numbering/555-line-numbers). Les e-mails restent en `example.invalid`. Les références techniques et les montants sont conservés. Cette version ne doit pas servir à écraser des enregistrements existants : les anciens contenus restent bloquants pour l'import idempotent et subsistent dans l'historique Git tant qu'un nettoyage distinct n'a pas été autorisé.

Les données de `technova.json` reprennent les 40 enregistrements validés : 3 départements, 6 employés, 5 clients, 6 congés, 8 factures, 6 honoraires, 6 paies. Les huit factures totalisent **30 583 TND**. Les taxes et paies sont fictives.

## Préparation et vérification sans écriture

Depuis la racine du dépôt, avec Windows PowerShell 5.1 et les services déjà démarrés :

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\demo\Import-TechNova.ps1 -DryRun
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\demo\Test-TechNova.Local.ps1
```

`Bypass` s'applique uniquement au processus lancé ; aucune stratégie Windows permanente n'est changée. Aucune dépendance à installer. Le DryRun lit et valide seulement le JSON : **aucune requête réseau, aucun login, aucune écriture**. Il ne certifie donc ni la santé des services ni l'absence de doublons réels. Les tests locaux remplacent entièrement les API par des objets en mémoire ; leurs créations et PATCH affichés sont simulés.

Les réponses JSON sont lues depuis les octets HTTP puis décodées explicitement en UTF-8, même si Spring n'indique pas de `charset`. Cela évite le décodage Latin-1 de Windows PowerShell 5.1 qui transforme par exemple `Développement` en `DÃ©veloppement` et crée un faux conflit. Aucun nom n'est normalisé, ignoré ou réparé dans les données : les véritables différences restent bloquantes. Le test local reproduit ce conflit, puis reprend avec DEV déjà présent (39 créations simulées), et vérifie une seconde exécution sans doublons.

## Insertion réelle — uniquement après autorisation

Commande documentée mais **non exécutée lors de la préparation** :

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\demo\Import-TechNova.ps1
```

1. Vérifier que personne ne charge le même jeu simultanément. Ne pas utiliser de transcription, débogage HTTP, proxy de capture ou journalisation externe des corps de requêtes.
2. Le script demande la phrase exacte `INSERER DEMO-TNC`, puis ouvre `Get-Credential` pour un compte SUPER_ADMIN existant. Ne jamais passer un mot de passe ou JWT en argument, variable persistante ou fichier. Le mot de passe est masqué et conservé uniquement en mémoire ; sa représentation en clair est nécessaire brièvement pour le login HTTP local. PowerShell/.NET ne garantissent pas l'effacement immédiat de cette mémoire.
3. Une seule tentative POST `/identity/api/auth/login` est faite. Elle peut créer/remplacer les jetons Identity selon le comportement normal du backend. Le champ de saisie accepte le nom d'utilisateur ou l'email : comme Angular, le script envoie `email` si l'identifiant contient `@`, sinon `username`, avec l'autre champ à `null`. Le mot de passe est transmis sans modification.
4. Le script contrôle le rôle SUPER_ADMIN dans la réponse et les claims du JWT, ainsi que son expiration. La signature est vérifiée par les API Spring Security ; aucune clé de signature n'est lue ou générée par le script.
5. Il lit **toutes les pages** des sept listes et effectue un contrôle préalable complet avant toute écriture métier. Une erreur d'authentification, de pagination ou un conflit arrête le traitement.
6. Chaque création est précédée d'une nouvelle recherche de doublon, utilise les IDs réels, puis est vérifiée par GET. Les calculs dérivés ne sont jamais envoyés : factures `subtotal + tax`, honoraires `hourlyRate × workedHours`, paie `baseSalary + bonuses − deductions − taxAmount`.
7. Seuls les congés créés dans cette exécution peuvent recevoir les deux PATCH approve et deux PATCH reject. Le statut initial PENDING vient du backend.
8. Le contrôle final relit les données, vérifie les relations, les 40 fiches et les montants. Il affiche les IDs/références, les nombres créés/réutilisés, la répartition 2/2/2 des congés, les sommes globales de factures par devise et les comptes attendus du Dashboard. Aucun salaire nominatif ni jeton n'est affiché.

## API Gateway utilisées

Base fixe : `http://localhost:8080`, aucune redirection de base configurable.

| Module | GET liste et POST création | GET vérification |
|---|---|---|
| Départements | `/department/api/departments` | `/{id}` |
| Employés | `/employee/api/employees` | `/{id}` |
| Clients | `/customer/api/customers` | `/{id}` |
| Congés | `/hr/api/leaves` | `/{id}` |
| Factures | `/invoice/api/invoices` | `/{id}` |
| Honoraires | `/fees/api/fees` | `/{id}` |
| Paie | `/payroll/api/payrolls` | `/{id}` |

Listes : `?page=N&size=100&sortBy=id&direction=asc`, pour demander explicitement le format paginé plutôt que les anciens endpoints retournant un tableau. Transitions : PATCH `/hr/api/leaves/{id}/approve` ou `/reject`.

## Reprise et limites

- Une deuxième exécution après succès vérifie et réutilise les 40 fiches identiques, sans nouveau POST métier ni PATCH. Le login peut toujours écrire ses jetons Identity.
- Les collisions sont recherchées par code/nom de département, code/email employé, nom/email client, numéro de facture, nom de prestation, employé/période de paie, et motif ou chevauchement de congé.
- Un enregistrement correspondant mais différent provoque un arrêt : aucun écrasement, suppression, PUT ou SQL. Les données préexistantes ne sont pas corrigées.
- En cas d'interruption après un POST, ne pas supposer son échec et ne pas relancer aveuglément. La reprise relit les données. Un ancien congé dont le statut diffère du plan **bloque avant toute écriture métier** ; sa correction nécessite un examen et une autorisation distincts. Les transactions ne sont pas distribuées, aucune annulation automatique n'est tentée.
- Éviter les modifications concurrentes : les contrôles GET puis POST ne sont pas une transaction atomique. Les contraintes serveur restent l'autorité finale.
- Les dates de congé sont fixes (novembre/décembre 2026). Le script refuse de les créer si elles deviennent passées ; il ne les décale pas automatiquement.
- Les IDs employés et clients sont convertis en chaînes pour Congés/Factures/Honoraires/Paie ; `departmentId` reste numérique. Aucun compte Identity ni association supplémentaire n'est créé. Les six nouveaux employés ne disposent donc pas d'une connexion personnelle.
- Le salaire employé et les honoraires ne portent aucune devise dans leurs DTO ; seule la paie et les factures enregistrent `TND`. Le Dashboard doit garder le total monétaire des honoraires « Indisponible ».
- Statuts Factures (`PAID`, `SENT`, `DRAFT`) et Paie (`DEMO`) : champs libres existants, sans transition serveur dédiée. Aucune nouvelle énumération métier n'est introduite.
- Descriptions de département limitées à 255 caractères, malgré le DTO autorisant 500, conformément à la colonne actuelle.
- Les données sont marquées DEMO dans les champs disponibles. Le schéma actuel n'a aucun indicateur global `isDemo` ; le Dashboard global les compte avec les autres données.
- Si les données initiales sont inchangées : comptes attendus après insertion = 3 départements, 8 employés, 5 clients, 7 congés dont 3 PENDING, 8 factures, 6 honoraires et 6 paies. Les valeurs réelles sont recalculées par GET, jamais supposées.
- La lecture des factures est bornée à 100 000 lignes pour le script ; le Dashboard a sa propre limite de 1 000 factures pour les agrégats. Aucune validation visuelle navigateur n'est automatisée ici.

Les scénarios fonctionnels à compléter sont dans [`TESTS_MVP_CRM.md`](../../TESTS_MVP_CRM.md). Aucun test réel ne doit être marqué PASS avant exécution.

## Vérifier le MVP après l'import, sans écriture métier

```powershell
# Sans réseau ni authentification
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\demo\Test-CRM-MVP.ps1 -LocalCheck
# Réponses simulées uniquement
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\demo\Test-CRM-MVP.Local.ps1
# API réelles : à lancer manuellement, avec confirmation TESTER CRM et Get-Credential
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\demo\Test-CRM-MVP.ps1
```

Ce vérificateur réutilise le décodage UTF-8 et le contrat login de l'importeur, sans appeler l'importeur principal. Il bloque toutes les écritures métier. Le seul POST possible est une tentative de login explicitement autorisée ; ses écritures normales de session Identity sont possibles. La référence actuelle comprend 7 congés dont **2 PENDING** (l'ancienne projection avant import était 3). Aucun compte supplémentaire n'est utilisé. Les résultats des 48 contrôles simulés ne prouvent ni les réponses authentifiées réelles ni le rendu visuel Angular.
