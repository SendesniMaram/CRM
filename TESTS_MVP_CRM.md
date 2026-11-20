# Validation MVP CRM — TechNova Consulting

Les résultats fonctionnels ci-dessous sont **NON TESTÉ** : la préparation du script ne réalise aucune insertion ni connexion réelle. Renseigner « Résultat observé » et choisir PASS ou FAIL uniquement après le test. Les tests négatifs qui envoient une écriture doivent utiliser un environnement isolé ou une autorisation spécifique ; ils ne font pas partie des 40 créations et 4 transitions autorisées.

| ID | Module | Prérequis | Étapes | Résultat attendu | Résultat observé | Statut |
|---|---|---|---|---|---|---|
| AUTH-01 | Authentification | Compte SUPER_ADMIN existant, services UP | Lancer le script autorisé ; confirmer ; saisir les identifiants dans Get-Credential | Une tentative login ; rôle SUPER_ADMIN reconnu ; aucun secret affiché | À remplir | NON TESTÉ |
| AUTH-02 | Authentification | Environnement isolé, compte non administrateur existant | Exécuter avec ce compte | Arrêt après login ; aucune écriture métier | À remplir | NON TESTÉ |
| AUTH-03 | Authentification | Environnement isolé | Fournir une fois un mot de passe incorrect | Arrêt immédiat sans retry ni fuite de réponse | À remplir | NON TESTÉ |
| DEP-01 | Départements | Insertion autorisée | Lire les trois codes DEMO-TNC, puis leurs détails | Trois départements, codes uniques, descriptions ≤255 caractères | À remplir | NON TESTÉ |
| EMP-01 | Employés | Départements DEMO créés | Lire les six codes DEMO-TNC-E01…E06 et les détails | Six employés, répartition DEV=3/RH=1/FIN=2, IDs de département corrects | À remplir | NON TESTÉ |
| EMP-02 | Employés | Deux employés initiaux présents | Comparer leurs fiches avant/après | Aucune modification des données initiales | À remplir | NON TESTÉ |
| CLI-01 | Clients | Insertion terminée | Ouvrir Clients ; rechercher DEMO ; consulter les cinq fiches | Coordonnées fictives, cinq entreprises prévues, adresses conformes | À remplir | NON TESTÉ |
| CLI-02 | Clients | Données de démo chargées | Rechercher Sfax ; trier ; changer taille/page | Résultats, ordre et totalElements cohérents, aucun doublon | À remplir | NON TESTÉ |
| CON-01 | Congés | Employés DEMO créés | Lire les six motifs DEMO-TNC-L01…L06 | Bons employés, dates exactes, aucun chevauchement par employé | À remplir | NON TESTÉ |
| CON-02 | Congés | Autorisation des quatre transitions | Vérifier les GET après les PATCH | L01/L02 APPROVED ; L03/L04 PENDING ; L05/L06 REJECTED | À remplir | NON TESTÉ |
| CON-03 | Congés | Congé préexistant connu | Comparer son GET avant/après | Demande initiale et association Identity inchangées | À remplir | NON TESTÉ |
| FAC-01 | Factures | Clients DEMO créés | Lire les huit numéros DEMO-TNC-2026-001…008 | Références client réelles converties en chaînes ; devise TND ; dates exactes | À remplir | NON TESTÉ |
| FAC-02 | Factures | Huit factures accessibles | Additionner les totalAmount des huit fiches | 30 583 TND ; chaque total = subtotal + tax | À remplir | NON TESTÉ |
| FAC-03 | Factures | Module ouvert | Rechercher un numéro ; trier les montants ; paginer ; consulter | Résultats cohérents et statuts 2 PAID / 4 SENT / 2 DRAFT | À remplir | NON TESTÉ |
| HON-01 | Honoraires | Employés et clients DEMO créés | Lire H01…H06 et leurs relations | Six prestations correctement associées, descriptions ≤500 caractères | À remplir | NON TESTÉ |
| HON-02 | Honoraires | Six prestations accessibles | Comparer amount à hourlyRate × workedHours | 1200, 900, 600, 660, 760, 300 ; somme 4420 sans devise supposée | À remplir | NON TESTÉ |
| PAI-01 | Paie | Employés DEMO créés | Lire les six fiches période 2026-09 | Bons IDs employés, TND, statut DEMO, paiement 2026-09-30 | À remplir | NON TESTÉ |
| PAI-02 | Paie | Session administrative autorisée | Comparer les nets à la formule backend sans publier les noms/salaires | Nets 3050, 2840, 2470, 3160, 2635, 2100 ; somme 16255 TND | À remplir | NON TESTÉ |
| SEC-01 | Autorisations | Sessions EMPLOYEE/CLIENT existantes | Ouvrir routes administratives et GET protégés | Aucune donnée administrative/paie d'autrui exposée ; refus backend approprié | À remplir | NON TESTÉ |
| SEC-02 | Congés personnels | Compte EMPLOYEE déjà associé | Consulter ses congés ; tenter un GET d'un autre employé | Seules les données autorisées sont accessibles, refus pour autrui | À remplir | NON TESTÉ |
| VAL-01 | Formulaires | Navigateur ouvert | Saisir un champ requis vide, email client invalide, montant négatif sans soumettre | Messages français et soumission empêchée quand la validation existe | À remplir | NON TESTÉ |
| VAL-02 | Backend | Environnement isolé et autorisation de tests négatifs | Envoyer dates congé inversées, champ obligatoire absent, facture dupliquée | Erreur contrôlée ; aucune nouvelle donnée persistée | À remplir | NON TESTÉ |
| DASH-01 | Dashboard | Session SUPER_ADMIN, import confirmé | Actualiser ; comparer les sept comptes aux GET complets | Référence actuelle annoncée : employés8/clients5/congés7/PENDING2/factures8/honoraires6/paies6 | À remplir | NON TESTÉ |
| DASH-02 | Dashboard | Factures présentes | Comparer total et répartition aux GET | Démo seule : 30583 TND ; statuts 2/4/2 ; devises séparées ; pas de total global issu d'une seule page | À remplir | NON TESTÉ |
| DASH-03 | Dashboard | Contrat Honoraires inchangé | Examiner la carte montant des honoraires | Indisponible : aucune devise inventée | À remplir | NON TESTÉ |
| DASH-04 | Dashboard | Sessions EMPLOYEE et CLIENT existantes | Ouvrir Dashboard puis changer de session | Aucun KPI administratif ni snapshot du compte précédent exposé | À remplir | NON TESTÉ |
| DASH-05 | États vides/erreurs | Environnement isolé, sans arrêt des services réels | Simuler une liste vide et un GET en erreur via outils de test | Vide distinct d'une erreur ; erreur n'affichant jamais un faux zéro | À remplir | NON TESTÉ |
| UI-01 | Navigation | Navigateur authentifié | Ouvrir Clients/Factures/Honoraires/Paie ; mobile puis bureau ; examiner console/réseau | Pages lisibles, navigation cohérente, pas d'erreur inattendue | À remplir | NON TESTÉ |
| IMP-01 | Import | Première exécution réussie | Relancer avec confirmation ; examiner les résultats et GET | 0 création métier, 0 PATCH ; réutilisation exacte ; login Identity possible | À remplir | NON TESTÉ |
| IMP-02 | Import | Environnement isolé avec conflit préparé | Relancer avec fiche DEMO différente | Arrêt avant première écriture métier ; aucun écrasement | À remplir | NON TESTÉ |
| IMP-03 | Préservation | État Git et comptes Identity relevés | Comparer après import | Dashboard/rapport inchangés, aucun compte créé, aucun SQL/schéma modifié | À remplir | NON TESTÉ |

Les vérifications purement locales du script sont séparées des scénarios ci-dessus : `scripts/demo/Test-TechNova.Local.ps1` utilise uniquement des réponses en mémoire. Leur succès ne valide pas une intégration réelle, une interface navigateur ou des règles fiscales.

## Campagne automatisée préparée le 16 octobre 2026

La réussite de l'import est déclarée par l'utilisatrice. Les tests API authentifiés ci-dessous attendent son lancement manuel : aucun login réel n'a été effectué pendant cette préparation. La référence globale PENDING est maintenant **2**, et non l'ancienne projection de 3. Une différence de données provoque un FAIL, jamais une correction automatique.

| ID | Scénario / méthode de vérification | Résultat attendu | Résultat observé | Statut |
|---|---|---|---|---|
| RUN-HEALTH | API réelles publiques : actuator health de Discovery, Gateway et huit services métier/Identity | HTTP 200, UP | Tous UP ; MySQL HEALTHY via Docker ; Angular /dashboard HTTP 200 | PASS |
| RUN-LOCAL | `Test-CRM-MVP.Local.ps1`, toutes les API remplacées par des réponses en mémoire | Contrôles positifs, erreurs injectées détectées, aucune écriture réseau | 48 contrôles positifs réussis ; nets, relation, devise, PENDING, pagination et méthodes interdites testés | PASS |
| RUN-LOCALCHECK | `Test-CRM-MVP.ps1 -LocalCheck` | Vérification des chemins/JSON sans réseau ni login | Vérification réussie | PASS |
| RUN-IMPORT-REGRESSION | `Test-TechNova.Local.ps1`, simulation uniquement | UTF-8, reprise, idempotence, contrat login et rôle conservés | Tous les contrôles locaux réussis ; aucune API réelle appelée | PASS |
| RUN-DASH-UNIT | Tests Angular Dashboard, services HTTP simulés | KPI, pagination, rôles, états vides et erreurs conformes | 26 tests réussis avec ChromeHeadless | PASS |
| RUN-SEC-UNIT | Tests Angular auth.guard, role.guard et auth.interceptor | Accès par rôle et gestion JWT conformes aux tests isolés | 46 tests réussis ; aucun compte réel utilisé | PASS |
| RUN-SEC-CODE | Lecture des SecurityConfig et LeaveAccess backend | Données administratives réservées ADMIN/SUPER_ADMIN ; exceptions personnelles contrôlées | Employee/Customer/Invoice/Fees/Payroll administratifs ; Department GET autorise aussi EMPLOYEE ; HR scope personnel vérifié dans LeaveAccess | PASS |
| RUN-AUTH | Login réel lancé manuellement avec Get-Credential, une tentative | SUPER_ADMIN reconnu, JWT uniquement en mémoire | Non exécuté ici | NON TESTÉ |
| RUN-COUNTS | GET réels de toutes les pages des sept modules | Départements3 / employés8 dont6 DEMO / clients5 / congés7 / factures8 / honoraires6 / paies6 | Non exécuté ici | NON TESTÉ |
| RUN-DETAILS | GET réels des 40 IDs DEMO | Tous les champs du plan, relations et IDs confirmés | Non exécuté ici | NON TESTÉ |
| RUN-SEARCH | GET réels avec keyword et pagination size=2 | DEV trouvé, six employés DEMO, cinq clients ; IDs uniques et tri ascendant | Non exécuté ici | NON TESTÉ |
| RUN-LEAVES | GET réels, dates et comparaisons par employé | 2 PENDING globalement ; DEMO 2 APPROVED/2 PENDING/2 REJECTED ; aucun chevauchement | Non exécuté ici | NON TESTÉ |
| RUN-INVOICES | GET réels des factures, calcul décimal | TND, total 30583, sous-total + taxe ; 2 PAID/4 SENT/2 DRAFT | Non exécuté ici | NON TESTÉ |
| RUN-FEES | GET réels des honoraires | amount = taux × heures ; relations valides ; champ currency absent | Non exécuté ici | NON TESTÉ |
| RUN-PAYROLL | GET réels des fiches de paie, calcul décimal sans sortie salariale nominative | Net = base + primes − déductions − impôt ; TND ; période 2026-09 | Non exécuté ici | NON TESTÉ |
| RUN-DASH-API | GET réels identiques aux requêtes du Dashboard | Comptes 8/5/7/2/8/6/6 et facturation 30583 TND ; répartitions cohérentes | Non exécuté ici ; ne prouve pas le rendu Angular | NON TESTÉ |
| RUN-VISUAL | Navigateur réel, session SUPER_ADMIN ; Dashboard et modules | Cartes, listes et console/réseau corrects | Aucun parcours visuel réalisé dans cette campagne | NON TESTÉ |
| RUN-SEC-REAL | GET réels avec rôles EMPLOYEE/CLIENT | Refus des données administratives selon les règles serveur | Non exécuté ; aucun compte supplémentaire autorisé | NON TESTÉ |

### Exécution manuelle des API réelles

Depuis la racine du dépôt :

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\demo\Test-CRM-MVP.ps1
```

Confirmer `TESTER CRM`, puis saisir l'identifiant (nom ou email) et le mot de passe dans Get-Credential. Le seul POST permis est le login explicitement confirmé ; toutes les autres requêtes sont GET. Le login peut écrire ses jetons Identity normaux. Le vérificateur refuse PUT/PATCH/DELETE et tout POST métier, s'arrête après le premier échec et efface sa référence au JWT en sortie. Aucun fichier résultat ni secret n'est écrit automatiquement.

Chaque PASS affiché est précédé de `API REELLE` ou `SIMULATION LOCALE` pour distinguer les méthodes. Le script affiche seulement des intitulés de tests et nombres agrégés, jamais de fiche salariale nominative. Après l'exécution réelle, renseigner les résultats observés ci-dessus à partir de la sortie ; ne pas joindre de jeton, identifiant, mot de passe ou corps de réponse sensible.

Le mode `-LocalCheck` ne contacte aucune API et ne certifie pas les données. La vérification des 40 objets compare le plan UTF-8 sans exiger que les dates de congés déjà enregistrés soient encore futures. Les contrôles sont stricts : si les données ont légitimement évolué depuis le jeu de référence, examiner le FAIL et adapter les attentes après validation ; ne jamais modifier les données pour faire passer un test. Éviter les modifications concurrentes pendant les lectures, qui ne constituent pas un snapshot transactionnel entre services.

## Campagne globale du 16 octobre 2026 — résultats de cette exécution

**Verdict : MVP READY WITH RESERVATIONS.** Aucun échec dans les suites applicatives exécutées et aucune incohérence du jeu DEMO observée. Les API métier authentifiées et le rendu visuel ne sont pas validés par cette campagne. Aucune correction effectuée.

### 1. Infrastructure et état initial

Branche `microservices`, HEAD `40b6d0f`. Les huit fichiers frontend modifiés, les trois nouveaux fichiers frontend, les scripts et le rapport de revue sont préservés. Les nouveaux fichiers restent non suivis. Aucun git add, commit ou push.

Docker Engine 29.7.2 disponible. MySQL `crm-mysql-1` HEALTHY. Discovery 8761, Gateway 8080, Identity 8082, Employee 8083, Department 8084, Customer 8085, HR 8086, Payroll 8087, Fees 8088 et Invoice 8089 répondent UP via Actuator. Angular `/dashboard` répond HTTP 200 ; cela confirme le serveur Angular, pas son rendu authentifié. Les neuf applications clientes Eureka (huit services Identity/métier et Gateway) sont enregistrées UP.

Les huit conteneurs JPA utilisent effectivement `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` et des URLs sans `createDatabaseIfNotExist=true`. Aucun ERROR/FATAL trouvé dans les journaux des dix services Java sur la fenêtre des 30 minutes inspectée. Ce contrôle borné ne prouve pas l'absence d'erreur historique. Aucun conteneur redémarré, aucune intervention Oracle.

### 2. Backend — suites Maven exécutées

Commande par module : `mvn.cmd -o -Dmaven.repo.local=C:/Users/ASUS/.m2/repository -f microservices/<module>/pom.xml test`. Dépendances déjà disponibles ; aucune installation. Les tests JPA utilisent H2 en mémoire, avec configurations test ; Eureka désactivé. Les autres tests utilisent leurs contextes/mocks existants.

| Module | PASS | FAIL + erreurs | Ignorés | Résultat Maven |
|---|---:|---:|---:|---|
| common-security | 5 | 0 | 0 | BUILD SUCCESS |
| Identity | 20 | 0 | 0 | BUILD SUCCESS |
| Employee | 43 | 0 | 0 | BUILD SUCCESS |
| Department | 14 | 0 | 0 | BUILD SUCCESS |
| HR | 155 | 0 | 0 | BUILD SUCCESS |
| Customer | 45 | 0 | 0 | BUILD SUCCESS |
| Invoice | 43 | 0 | 0 | BUILD SUCCESS |
| Fees | 44 | 0 | 0 | BUILD SUCCESS |
| Payroll | 27 | 0 | 0 | BUILD SUCCESS |
| Discovery | 1 | 0 | 0 | BUILD SUCCESS |
| Gateway | 1 | 0 | 0 | BUILD SUCCESS |
| **Total backend** | **398** | **0** | **0** | **Succès** |

Totaux recoupés dans les rapports Surefire XML. Les suites couvrent les contrôleurs, services, repositories, validations, erreurs, autorisations et calculs selon les tests existants ; aucune affirmation de couverture exhaustive de tous les chemins. Discovery/Gateway n'ont chacun qu'un test de contexte, ce qui limite leur couverture fonctionnelle.

### 3. Frontend et scripts locaux

`npm.cmd test -- --watch=false --browsers=ChromeHeadless --progress=false` : **641 exécutés sur 641, tous PASS**, aucun ignoré. Cela inclut les tests Dashboard, routes/navigation, guards, intercepteur, composants, formulaires et HTTP simulé. ChromeHeadless 155 est utilisé comme lanceur de tests unitaires ; il n'y a pas eu de navigation E2E authentifiée.

`npm.cmd run build -- --configuration production` : succès. Bundle initial **839,25 kB**, transfert estimé **199,26 kB** ; budget warning 500 kB dépassé de 339,25 kB, seuil erreur 1 MB non atteint. Un avertissement de fermeture du processus Chrome apparaît après les 641 succès ; voir GLOB-A04.

`Test-CRM-MVP.Local.ps1` : **48 contrôles positifs simulés PASS**, erreurs injectées détectées (calcul net, relation, devise, nombre PENDING, pagination, garde interdisant les écritures). `Test-TechNova.Local.ps1` : succès, y compris UTF-8, reprise partielle, idempotence, rôle et contrat login. `Test-CRM-MVP.ps1 -LocalCheck` : succès sans réseau ni authentification. Ces tests ne valident pas les API métier réelles.

### 4. API réelles

Aucun identifiant administrateur accessible dans `.env` ou les variables de processus ; le conteneur Identity ne fournit que la clé de désactivation du bootstrap, aucun identifiant administrateur. Aucune session navigateur connectée disponible. **Aucun login réel effectué et aucun test métier authentifié lancé.** Le script `Test-CRM-MVP.ps1` attend l'exécution manuelle autorisée.

Les sept GET Gateway sans JWT répondent **401**, résultat attendu : Employee, Department, HR/congés, Customer, Invoice, Fees et Payroll. Les temps observés sur un seul échantillon sont respectivement **246, 97, 240, 54, 53, 55 et 41 ms**. Il s'agit des refus de sécurité, pas d'une mesure de performance des listes métier authentifiées.

### 5. Données DEMO-TNC — lectures SQL réelles uniquement

Les vérifications ci-dessous proviennent de SELECT réels, distincts des tests API. Aucun SQL d'écriture, aucun POST métier, PUT, PATCH ou DELETE effectué.

| Module | Total observé | DEMO observé | Attendu | Statut |
|---|---:|---:|---|---|
| Départements | 3 | 3 | 3 / 3 | PASS |
| Employés | 8 | 6 | 8 / 6 | PASS |
| Clients | 5 | 5 | 5 / 5 | PASS |
| Congés | 7 | 6 | 7 / 6 | PASS |
| Factures | 8 | 8 | 8 / 8 | PASS |
| Honoraires | 6 | 6 | 6 / 6 | PASS |
| Paie | 6 | 6 | 6 / 6 | PASS |

Les **40 enregistrements DEMO** ont aussi été relus en mémoire et comparés exactement à `technova.json`, champs du plan, dates, références et calculs compris, avec résolution des IDs réels. Aucun salaire nominatif ni corps de réponse n'est reproduit dans le rapport. Répartition des employés DEMO : DEV=3, RH=1, FIN=2.

Contrôles SQL : **zéro lien manquant** pour employé/département DEMO, facture/client, congé/employé, honoraire/employé, honoraire/client et paie/employé ; **zéro doublon** de code employé, numéro de facture, email client ou couple employé/période de paie. La comparaison complète au plan confirme aussi les trois références de départements et six noms de prestations attendus.

Zéro écart sur les trois formules : facture = sous-total + taxe ; honoraire = taux × heures ; net = base + primes − déductions − impôt. Totaux : **30583,00 TND facturés**, honoraires **4420,00 sans devise définie par le contrat**, nets de paie **16255,00 TND**. Ces dernières sommes sont des agrégats, sans données salariales nominatives.

Zéro congé aux dates inversées, zéro chevauchement de demandes pour le même employé, zéro échéance facture antérieure à l'émission, zéro fiche de paie hors période 2026-09 ou devise TND. Les quatre statuts terminaux DEMO attendus sont présents ; la campagne confirme leur état final, pas l'historique d'exécution des quatre PATCH.

### 6. Dashboard

| Indicateur / répartition | Valeur réelle SQL | Attendu | Méthode |
|---|---:|---:|---|
| Employés | 8 | 8 | SELECT + tests unitaires de totalElements |
| Clients | 5 | 5 | SELECT + tests unitaires |
| Congés | 7 | 7 | SELECT + tests unitaires |
| PENDING global | 2 | 2 | SELECT |
| Factures | 8 | 8 | SELECT |
| Honoraires | 6 | 6 | SELECT |
| Paie | 6 | 6 | SELECT |
| Facturation TND | 30583,00 | 30583,00 | SELECT + calculs isolés |

Congés globaux : **APPROVED=3, PENDING=2, REJECTED=2** ; DEMO uniquement : **2/2/2**. Factures : **PAID=2 / 7735 TND**, **SENT=4 / 18445 TND**, **DRAFT=2 / 4403 TND**.

Le code Dashboard utilise les API existantes, sans import de données mockées ; les anciennes données mock non référencées ne sont pas affichées. Les tests valident pagination complète, pages incomplètes/instables, erreurs distinctes de zéro, états vides, devises séparées et changement de rôle/compte. Le total monétaire des honoraires reste indisponible faute de devise. Les agrégats factures sont volontairement indisponibles au-delà de 1000 factures. Les lectures interservices ne constituent pas un snapshot transactionnel.

**Le chargement réel des KPI via une session Angular et leur rendu visuel/responsive restent NON TESTÉS.** Les comptes SQL conformes ne prouvent pas le rendu navigateur ni les réponses métier HTTP authentifiées.

### 7. Sécurité

Lecture des SecurityConfig et tests isolés : Employee, Customer, Invoice, Fees et Payroll réservent leurs données administratives à ADMIN/SUPER_ADMIN. Department autorise aussi EMPLOYEE en GET ; ses écritures restent administratives. HR autorise les lectures de congés EMPLOYEE avec isolation par `LeaveAccess` et association existante ; les autres données administratives HR restent réservées aux administrateurs.

Les guards Angular réutilisent la session, les rôles et les redirections d'accès refusé. Le Dashboard masque les snapshots d'un autre rôle/compte. Les sept refus 401 réels sont vérifiés. Les rôles ADMIN/EMPLOYEE/CLIENT n'ont pas été exercés avec des comptes réels dans cette campagne ; aucun compte créé, aucun jeton forgé et aucun secret affiché. Les tests de rôles isolés ne sont pas une preuve de session réelle par rôle.

### 8. Performance et qualité

Actuator : un échantillon par endpoint, **346 à 1439 ms**, pendant l'exécution parallèle des suites locales. Pas de test de charge, pas de percentile p95/p99, pas de mesure métier authentifiée. Les ports demandés répondent via HTTP, avec état Eureka cohérent. Le coût des lectures multiples de listes est borné dans les scripts/Dashboard mais augmentera avec la volumétrie. Les conventions répétées dans les formulaires et scripts ne justifient aucune refonte pendant cette campagne.

Les configurations effectives sont sûres pour cette exécution, mais les fichiers de démarrage ne reproduisent pas ces garanties : voir GLOB-A02. Deux limites DTO/JPA/MySQL sont également désalignées : voir GLOB-A03. Aucun de ces écarts n'a déclenché de modification automatique.

### 9. Anomalies classées

| ID | Gravité | Module | Description et reproduction sans écriture | Cause établie / correction recommandée |
|---|---|---|---|---|
| GLOB-A02 | MAJEURE | Déploiement / huit services JPA | Lire Compose : huit URLs contiennent createDatabaseIfNotExist=true ; lire les application.yml : ddl-auto=update. Une recréation future sans les surcharges sûres peut réactiver des modifications de schéma. Ne pas reproduire par redémarrage pendant cette mission. | Décalage confirmé entre configuration versionnée et configuration effective. Préparer ultérieurement une configuration persistante validate et des URLs sans création automatique, après autorisation. Conteneurs actuels tous en validate. |
| GLOB-A03 | MAJEURE | Department / Employee | DepartmentRequest.description et EmployeeRequest.address acceptent 500 caractères ; entités sans length explicite et colonnes réelles VARCHAR(255). Lire DTO, entités et information_schema. Un contenu de 256–500 caractères dépasse le stockage malgré la validation DTO. | Écart de contrat confirmé ; erreur d'écriture longue non provoquée ici. Aligner les limites DTO/JPA/schéma et ajouter un test de frontière isolé après décision fonctionnelle. Toute éventuelle migration SQL exige une autorisation distincte. Customer.address et Fee.description sont bien à 500. |
| GLOB-A01 | MINEURE | Angular build | Lancer le build production : initial 839,25 kB, warning 500 kB dépassé de 339,25 kB ; build exit 0. | Budget d'avertissement dépassé. Examiner le contenu du bundle et les possibilités de chargement différé ; ne pas relever le budget sans justification. Aucun changement effectué. |
| GLOB-A04 | MINEURE | Lanceur Karma/ChromeHeadless | Après Executed 641 of 641 SUCCESS, Karma indique que Chrome n'a pas terminé dans les délais, puis continue après SIGKILL. Commande npm test de la campagne. | Avertissement de nettoyage confirmé ; cause système/runner non établie. Surveiller les processus résiduels et diagnostiquer le nettoyage en environnement de test avant CI. Aucun processus applicatif arrêté ; aucun test fonctionnel en échec. |

Aucune anomalie BLOQUANTE observée dans les contrôles exécutés. L'absence de session autorisée et de navigateur pilotable sont des limites de validation, pas une preuve de panne.

### 10. Comptage des résultats et recommandations

Ne pas additionner les lignes suivantes comme si elles représentaient toutes des tests unitaires : les unités de comptage sont explicites.

| Corpus | PASS | FAIL | NON TESTÉ | Ignorés |
|---|---:|---:|---:|---:|
| Tests JUnit backend + Angular | **1039** (398+641) | **0** | — | **0** |
| Contrôles positifs du vérificateur simulé | **48** | **0** | — | — |
| Enregistrements DEMO comparés exactement en SQL | **40** | **0** | — | — |
| Scénarios de campagne ci-dessous | **18** | **4** | **10** | — |

Les quatre FAIL de campagne sont les critères de qualité associés aux anomalies, et non des échecs JUnit/Angular. Aucun scénario n'est classé BLOQUÉ : les vérifications impossibles sont NON TESTÉES avec la raison indiquée.

| ID | Scénario / méthode | Attendu | Observé | Statut |
|---|---|---|---|---|
| G01 | Git et préservation | microservices / 40b6d0f ; changements préservés | Conforme ; revue inchangée | PASS |
| G02 | Docker / santé HTTP | MySQL HEALTHY ; dix services Java UP ; Angular 200 | Conforme | PASS |
| G03 | Eureka | Neuf clients enregistrés UP | Conforme | PASS |
| G04 | Configurations effectives | Huit JPA validate, sans création automatique | Conforme | PASS |
| G05 | Logs récents | Aucun ERROR/FATAL dans la fenêtre inspectée | Zéro sur les dix services Java, 30 minutes | PASS |
| G06 | Maven isolé | Aucun échec | 398 tests réussis | PASS |
| G07 | Angular unitaire | Aucun échec | 641/641 réussis | PASS |
| G08 | Build production | Build terminé | Exit 0 ; warning séparé G19 | PASS |
| G09 | Scripts simulés / LocalCheck | Aucune API réelle ; scénarios conformes | Succès | PASS |
| G10 | Comparaison SQL au plan | 40 objets exactement conformes | 40/40 | PASS |
| G11 | Relations SQL | Zéro lien manquant | Zéro pour les six relations contrôlées | PASS |
| G12 | Unicité SQL | Références sans doublon | Conforme aux clés contrôlées | PASS |
| G13 | Calculs SQL | Trois formules et sommes conformes | 30583 factures / 4420 honoraires / 16255 nets | PASS |
| G14 | Dates SQL | Dates et périodes cohérentes, aucun chevauchement | Conforme | PASS |
| G15 | Statistiques SQL | Comptes et statuts attendus | 8/5/7/2/8/6/6, factures 30583 TND | PASS |
| G16 | GET métier sans JWT | Refus HTTP | Sept HTTP 401 | PASS |
| G17 | Revue des protections | Rôles et scope personnel définis | Conformes au code et tests existants | PASS |
| G18 | Dashboard isolé | Pagination, erreurs, vides, rôles sans mocks affichés | Tests inclus dans les 641 PASS | PASS |
| G19 | Budget warning initial | ≤500 kB | 839,25 kB ; GLOB-A01 | FAIL |
| G20 | Reproductibilité du démarrage sûr | Fichiers cohérents avec runtime validate | update/createDatabaseIfNotExist ; GLOB-A02 | FAIL |
| G21 | API Employee authentifiée | GET 200, contrat, pagination/recherche | Session non disponible ici | NON TESTÉ |
| G22 | API Department authentifiée | GET 200, contrat, pagination/recherche | Session non disponible ici | NON TESTÉ |
| G23 | API HR authentifiée | GET 200, contrats/statuts | Session non disponible ici | NON TESTÉ |
| G24 | API Customer authentifiée | GET 200, contrat, pagination/recherche | Session non disponible ici | NON TESTÉ |
| G25 | API Invoice authentifiée | GET 200, calculs/statuts/pagination | Session non disponible ici | NON TESTÉ |
| G26 | API Fees authentifiée | GET 200, contrat/calculs | Session non disponible ici | NON TESTÉ |
| G27 | API Payroll authentifiée | GET 200, contrat/calculs | Session non disponible ici | NON TESTÉ |
| G28 | Parcours visuel Angular / responsive | Dashboard et modules rendus correctement | Aucun navigateur connecté pilotable | NON TESTÉ |
| G29 | Performance GET authentifiés | Mesures métier représentatives | Aucun appel métier authentifié | NON TESTÉ |
| G30 | Sessions réelles ADMIN/EMPLOYEE/CLIENT | Accès/isolation conformes | Aucun compte/session supplémentaire utilisé | NON TESTÉ |
| G31 | Limites DTO/JPA/SQL | Limites de champs alignées | Deux écarts 500/255 ; GLOB-A03 | FAIL |
| G32 | Nettoyage Chrome après suite | Arrêt propre | Timeout de nettoyage ; GLOB-A04 | FAIL |

Priorités proposées, sans correction commencée : faire exécuter manuellement le script de tests API existant avec une session SUPER_ADMIN autorisée ; réaliser le parcours visuel ; traiter GLOB-A02 et GLOB-A03 dans une mission distincte ; examiner ensuite le budget et le nettoyage Chrome. Les commandes de tests réels sont celles de la section précédente ; ne pas lancer l'import pour vérifier le MVP.

Un seul document de travail est actualisé par cette campagne : `TESTS_MVP_CRM.md`. Les journaux techniques et artefacts de tests/build sont générés dans des emplacements ignorés ; ne pas les inclure dans un commit. `REVUE_FINALE_CRM.md` conserve le SHA256 `65330338E2ED639588BE8D69C1622DD243C5EF8091D28000CA31E858527FC5B5`. Aucun code applicatif, script, backend, configuration ou donnée métier modifié.

## Corrections ciblées des deux anomalies majeures — 16 octobre 2026

Cette section décrit une mission distincte, autorisée après la campagne globale. Les résultats et FAIL historiques ci-dessus restent la trace de l'état avant correction. **Verdict de cette correction : READY, sources vérifiées, non déployées.**

### GLOB-A02 — configuration de démarrage corrigée dans les fichiers

Pour Identity, Employee, Department, Customer, HR, Payroll, Fees et Invoice : les huit `src/main/resources/application.yml` utilisent maintenant `ddl-auto: validate` et `spring.sql.init.mode: never`. Compose retire `createDatabaseIfNotExist=true` des huit URLs et impose explicitement les mêmes deux paramètres. Aucun script SQL d'initialisation trouvé dans les ressources principales de ces services. Les profils H2 isolés conservent leur `create-drop` et n'ont pas été modifiés ; le monolithe historique, MySQL, les volumes, ports et bootstrap Identity ne sont pas modifiés.

`docker compose config --quiet` réussit ; la configuration résolue des huit services est contrôlée sans afficher les secrets : validate, never, aucune option de création automatique dans les URLs. Aucun compose up, build d'image, déploiement ou redémarrage effectué.

### GLOB-A03 — limites de stockage et validations alignées

Information_schema confirme à nouveau `departments.description VARCHAR(255) NULL` et `employees.address VARCHAR(255) NULL`. Les entités JPA conservent leur longueur implicite 255. Aucun besoin métier de 500 caractères n'étant établi pour ces deux champs, la capacité existante de 255 est retenue, sans migration.

Les deux DTO passent de @Size(max=500) à @Size(max=255). Les deux formulaires Angular et leurs messages suivent cette même limite ; les autres champs et modules restent inchangés. Les valeurs nulles restent permises côté API ; les formulaires conservent leur représentation vide des valeurs facultatives.

Tests H2 ajoutés pour POST et PUT, par champ : 255 caractères accentués acceptés et persistés, 256 rejetés HTTP 400 avec message de validation, null accepté. Un rejet ne crée aucun objet ou ne modifie pas la valeur existante. **12 nouveaux cas backend**, plus deux tests Angular de frontière avec absence d'appel HTTP si 256 caractères.

| Vérification exécutée | PASS | FAIL | Ignorés | Méthode |
|---|---:|---:|---:|---|
| Identity | 20 | 0 | 0 | Maven hors ligne, environnement test isolé |
| Employee | 49 | 0 | 0 | Maven, H2 ; six nouveaux cas |
| Department | 20 | 0 | 0 | Maven, H2 ; six nouveaux cas |
| Customer | 45 | 0 | 0 | Maven, H2 |
| HR | 155 | 0 | 0 | Maven, H2 |
| Invoice | 43 | 0 | 0 | Maven, H2 |
| Fees | 44 | 0 | 0 | Maven, H2 |
| Payroll | 27 | 0 | 0 | Maven, H2 |
| **Backend concerné** | **403** | **0** | **0** | Huit suites |
| Deux formulaires Angular | **50** | **0** | **0** | ChromeHeadless, HTTP simulé |

Les suites common-security, Discovery et Gateway n'ont pas été relancées pendant cette correction, leurs sources/configurations étant inchangées. La suite Angular complète et le build ne sont pas relancés : seuls les deux formulaires concernés sont vérifiés ici. Les anciennes valeurs 398/641 restent les résultats de la campagne précédente.

Fichiers concernés :

- `docker-compose.yml`.
- `microservices/identity-service/src/main/resources/application.yml`.
- `microservices/employee-service/src/main/resources/application.yml`.
- `microservices/department-service/src/main/resources/application.yml`.
- `microservices/customer-service/src/main/resources/application.yml`.
- `microservices/hr-service/src/main/resources/application.yml`.
- `microservices/payroll-service/src/main/resources/application.yml`.
- `microservices/fees-service/src/main/resources/application.yml`.
- `microservices/invoice-service/src/main/resources/application.yml`.
- `microservices/department-service/src/main/java/com/crm/department/dto/DepartmentRequest.java`.
- `microservices/employee-service/src/main/java/com/crm/employee/dto/EmployeeRequest.java`.
- `microservices/department-service/src/test/java/com/crm/department/DepartmentServiceIntegrationTest.java`.
- `microservices/employee-service/src/test/java/com/crm/employee/EmployeeServiceIntegrationTest.java`.
- `frontend/src/app/features/departments/pages/department-form/department-form.component.ts`, `.html`, `.spec.ts`.
- `frontend/src/app/features/employees/pages/employee-form/employee-form.component.ts`, `.html`, `.spec.ts`.
- Ce document `TESTS_MVP_CRM.md`.

Limites : les conteneurs actifs conservent leurs images précédentes et leurs DTO précédents tant qu'un déploiement distinct n'est pas autorisé. Ils sont restés UP, MySQL HEALTHY. Le passage à validate suppose un schéma déjà provisionné et compatible ; une base absente doit maintenant faire échouer le démarrage au lieu d'être créée. Aucun test CRUD réel ni API authentifiée exécuté. GLOB-A01 (bundle), GLOB-A04 (nettoyage Chrome) et les validations réelles/visuelles en attente ne sont pas corrigés pendant cette mission. Dashboard, scripts DEMO et revue préservés ; aucune écriture SQL, aucun changement Oracle, commit ou push.

## Vérification finale avant commits — 16 octobre 2026

**READY TO COMMIT, sous confirmation explicite ; aucun staging ni commit effectué.** Les résultats suivants sont ceux de la nouvelle exécution finale, et ne remplacent pas les résultats historiques des sections précédentes.

| Corpus réellement exécuté | PASS | FAIL | Ignorés |
|---|---:|---:|---:|
| Backend, onze modules isolés | 410 | 0 | 0 |
| Angular, suite complète | 643 | 0 | 0 |
| Contrôles positifs du vérificateur local simulé | 48 | 0 | — |

Détail backend : common-security5, Identity20, Employee49, Department20, HR155, Customer45, Invoice43, Fees44, Payroll27, Discovery1, Gateway1. Les403 tests des huit services JPA sont inclus ; les sept autres appartiennent aux trois modules d'infrastructure/sécurité. Angular ajoute deux tests de frontière aux641 de la campagne globale précédente. Les tests locaux d'import et de vérification passent aussi ; aucun login réel ni appel métier réel n'a été effectué.

Build Angular production réussi, bundle initial839,25 kB et transfert estimé199,27 kB ; avertissement de dépassement du budget500 kB maintenu. La suite Karma finit643/643 avec un avertissement de fermeture ChromeHeadless. Les configurations Compose résolues sont valides et sûres pour les huit JPA ; aucun service redémarré. Aucun fichier sensible détecté dans le scan ciblé des fichiers candidats. git diff --check et syntaxe des quatre scripts PowerShell réussis.

Les API authentifiées, le rendu visuel, les nouveaux binaires en conteneur et les données réelles ne sont pas revalidés par ces tests isolés. La conformité DEMO40/40 reste la lecture réelle documentée plus haut. Le plan exact des fichiers proposés, exclusions et cinq messages est dans [`docs/PLAN_COMMITS_MVP.md`](docs/PLAN_COMMITS_MVP.md). Le rapport historique REVUE_FINALE_CRM.md reste exclu et préservé. Aucun changement métier, SQL d'écriture, déploiement, commit ou push pendant cette vérification.
