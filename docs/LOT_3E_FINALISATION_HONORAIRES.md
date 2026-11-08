# Lot 3E — Finalisation fonctionnelle du module Honoraires et Frais

Date : 5 novembre 2026. Branche conservée : `publication-clean`.

## 1. Résumé exécutif

**LOT 3E PARTIEL — blocs A à E développés dans le périmètre défini, 97 cas Java et 1 187 tests Angular réussis ; build Angular de production réussi avec avertissements. Intégration réelle NON VALIDÉE.**

Le module comporte désormais des prestations avec références vérifiées, des tarifs horaires datés, des temps détaillés annulables avant enregistrement et un calcul exact avec conservation du taux appliqué. Les rapports et le CSV technique regroupent les montants par période, prestation, client, devise et origine, sans double comptabiliser les temps transférés en honoraires.

AN-01 est corrigée aux frontières DTO et service. Les anciens montants et heures historiques ne sont pas réécrits. Aucun contrat de conversion Fees→Invoice suffisamment défini n’a été trouvé : enregistrer un honoraire ne crée aucune facture et ne prouve aucun paiement. Le suivi des montants effectivement facturés reste à contractualiser.

Aucun démarrage Docker, MySQL ou microservice CRM, aucune exécution Flyway ni recette connectée. Les deux petits serveurs HTTP synthétiques utilisés par les tests ne sont pas des services CRM. Aucun commit, push, staging, changement de branche, nettoyage ou modification de secrets.

## 2. Fonctionnalités réalisées

Lecture des [rapports 3A](LOT_3A_AUDIT_CONFORMITE_FONCTIONNELLE.md), [3B](LOT_3B_FINALISATION_PAIE.md), [3C](LOT_3C_FINALISATION_RH.md), [3D](LOT_3D_STABILISATION_INTEGREE.md) et du [CDC CRM](CDC%20CRM%201.pdf), via l’extraction locale conservée. Inspection de Fees, Invoice, Customer, des contrats Employee, de common-security et des écrans, routes, tests et statistiques Angular.

Le plan a conservé le CRUD existant, traité les limites financières, ajouté tarifs et temps, puis raccordé les rapports et vérifié la non-régression. Les formulaires, listes et l’aide de sélection Employee/Customer existants sont réutilisés.

- **A — Finance :** précision DTO/service, calcul BigDecimal arrondi et borné avant écriture, refus des produits hors limites, conservation des valeurs historiques.
- **B — Tarifs :** création, consultation paginée, modification d’un tarif non utilisé, désactivation/réactivation et périodes inclusives non ambiguës pour une prestation.
- **C — Temps :** date, durée, description, références héritées de la prestation, consultation paginée par période, modification autorisée et annulation motivée sans effacement.
- **D — Calcul :** sélection du tarif actif à la date du travail, capture du taux et du montant, enregistrement idempotent d’un honoraire puis gel des temps.
- **E — Rapports :** groupes par prestation/client, synthèses client et devise, filtres de période/prestation/client, données historiques indéterminées explicites et CSV protégé.

Une prestation porte un salarié, un client facultatif et une devise explicite fixés à sa création. Les références nouvellement affectées sont résolues auprès de leurs services propriétaires avec le bearer de l’appelant. Aucune base Invoice, Customer ou Employee n’est jointe directement.

## 3. Fichiers modifiés

Inventaire relatif à la photographie de début du Lot 3E, et non au HEAD Git déjà modifié : **28 fichiers préexistants modifiés et 30 nouveaux fichiers, rapport compris**.

### Fichiers préexistants modifiés

- [frontend/src/app/core/services/auth-navigation.ts](../frontend/src/app/core/services/auth-navigation.ts)
- [frontend/src/app/features/dashboard/dashboard-data.service.spec.ts](../frontend/src/app/features/dashboard/dashboard-data.service.spec.ts)
- [frontend/src/app/features/dashboard/dashboard-data.service.ts](../frontend/src/app/features/dashboard/dashboard-data.service.ts)
- [frontend/src/app/features/dashboard/dashboard.component.html](../frontend/src/app/features/dashboard/dashboard.component.html)
- [frontend/src/app/features/fees/fee-detail.component.html](../frontend/src/app/features/fees/fee-detail.component.html)
- [frontend/src/app/features/fees/fee-detail.component.ts](../frontend/src/app/features/fees/fee-detail.component.ts)
- [frontend/src/app/features/fees/fee-form.component.html](../frontend/src/app/features/fees/fee-form.component.html)
- [frontend/src/app/features/fees/fee-form.component.spec.ts](../frontend/src/app/features/fees/fee-form.component.spec.ts)
- [frontend/src/app/features/fees/fee-form.component.ts](../frontend/src/app/features/fees/fee-form.component.ts)
- [frontend/src/app/features/fees/fee-reference-picker.component.ts](../frontend/src/app/features/fees/fee-reference-picker.component.ts)
- [frontend/src/app/features/fees/fee.models.ts](../frontend/src/app/features/fees/fee.models.ts)
- [frontend/src/app/features/fees/fees-page.component.html](../frontend/src/app/features/fees/fees-page.component.html)
- [frontend/src/app/features/fees/fees-page.component.ts](../frontend/src/app/features/fees/fees-page.component.ts)
- [frontend/src/app/features/fees/fees.routes.spec.ts](../frontend/src/app/features/fees/fees.routes.spec.ts)
- [frontend/src/app/features/fees/fees.routes.ts](../frontend/src/app/features/fees/fees.routes.ts)
- [microservices/fees-service/src/main/java/com/crm/fees/config/SecurityConfig.java](../microservices/fees-service/src/main/java/com/crm/fees/config/SecurityConfig.java)
- [microservices/fees-service/src/main/java/com/crm/fees/controller/FeeController.java](../microservices/fees-service/src/main/java/com/crm/fees/controller/FeeController.java)
- [microservices/fees-service/src/main/java/com/crm/fees/controller/FeeStatisticsController.java](../microservices/fees-service/src/main/java/com/crm/fees/controller/FeeStatisticsController.java)
- [microservices/fees-service/src/main/java/com/crm/fees/dto/FeeRequest.java](../microservices/fees-service/src/main/java/com/crm/fees/dto/FeeRequest.java)
- [microservices/fees-service/src/main/java/com/crm/fees/dto/FeeResponse.java](../microservices/fees-service/src/main/java/com/crm/fees/dto/FeeResponse.java)
- [microservices/fees-service/src/main/java/com/crm/fees/entity/Fee.java](../microservices/fees-service/src/main/java/com/crm/fees/entity/Fee.java)
- [microservices/fees-service/src/main/java/com/crm/fees/exception/GlobalExceptionHandler.java](../microservices/fees-service/src/main/java/com/crm/fees/exception/GlobalExceptionHandler.java)
- [microservices/fees-service/src/main/java/com/crm/fees/mapper/FeeMapper.java](../microservices/fees-service/src/main/java/com/crm/fees/mapper/FeeMapper.java)
- [microservices/fees-service/src/main/java/com/crm/fees/repository/FeeRepository.java](../microservices/fees-service/src/main/java/com/crm/fees/repository/FeeRepository.java)
- [microservices/fees-service/src/main/java/com/crm/fees/service/IFeeService.java](../microservices/fees-service/src/main/java/com/crm/fees/service/IFeeService.java)
- [microservices/fees-service/src/main/java/com/crm/fees/service/impl/FeeServiceImpl.java](../microservices/fees-service/src/main/java/com/crm/fees/service/impl/FeeServiceImpl.java)
- [microservices/fees-service/src/test/java/com/crm/fees/FeeReadSecurityTest.java](../microservices/fees-service/src/test/java/com/crm/fees/FeeReadSecurityTest.java)
- [microservices/fees-service/src/test/java/com/crm/fees/FeesServiceIntegrationTest.java](../microservices/fees-service/src/test/java/com/crm/fees/FeesServiceIntegrationTest.java)

### Nouveaux fichiers

- [frontend/src/app/features/fees/fee-cycle.service.spec.ts](../frontend/src/app/features/fees/fee-cycle.service.spec.ts)
- [frontend/src/app/features/fees/fee-cycle.service.ts](../frontend/src/app/features/fees/fee-cycle.service.ts)
- [frontend/src/app/features/fees/fee-finance.spec.ts](../frontend/src/app/features/fees/fee-finance.spec.ts)
- [frontend/src/app/features/fees/fee-finance.ts](../frontend/src/app/features/fees/fee-finance.ts)
- [frontend/src/app/features/fees/fee-workbench.component.html](../frontend/src/app/features/fees/fee-workbench.component.html)
- [frontend/src/app/features/fees/fee-workbench.component.scss](../frontend/src/app/features/fees/fee-workbench.component.scss)
- [frontend/src/app/features/fees/fee-workbench.component.spec.ts](../frontend/src/app/features/fees/fee-workbench.component.spec.ts)
- [frontend/src/app/features/fees/fee-workbench.component.ts](../frontend/src/app/features/fees/fee-workbench.component.ts)
- [microservices/fees-service/src/main/java/com/crm/fees/controller/FeeCycleController.java](../microservices/fees-service/src/main/java/com/crm/fees/controller/FeeCycleController.java)
- [microservices/fees-service/src/main/java/com/crm/fees/dto/FeeCycleContracts.java](../microservices/fees-service/src/main/java/com/crm/fees/dto/FeeCycleContracts.java)
- [microservices/fees-service/src/main/java/com/crm/fees/entity/FeeOffering.java](../microservices/fees-service/src/main/java/com/crm/fees/entity/FeeOffering.java)
- [microservices/fees-service/src/main/java/com/crm/fees/entity/FeeTariff.java](../microservices/fees-service/src/main/java/com/crm/fees/entity/FeeTariff.java)
- [microservices/fees-service/src/main/java/com/crm/fees/entity/FeeTimeEntry.java](../microservices/fees-service/src/main/java/com/crm/fees/entity/FeeTimeEntry.java)
- [microservices/fees-service/src/main/java/com/crm/fees/repository/FeeOfferingRepository.java](../microservices/fees-service/src/main/java/com/crm/fees/repository/FeeOfferingRepository.java)
- [microservices/fees-service/src/main/java/com/crm/fees/repository/FeeReportRepository.java](../microservices/fees-service/src/main/java/com/crm/fees/repository/FeeReportRepository.java)
- [microservices/fees-service/src/main/java/com/crm/fees/repository/FeeTariffRepository.java](../microservices/fees-service/src/main/java/com/crm/fees/repository/FeeTariffRepository.java)
- [microservices/fees-service/src/main/java/com/crm/fees/repository/FeeTimeRepository.java](../microservices/fees-service/src/main/java/com/crm/fees/repository/FeeTimeRepository.java)
- [microservices/fees-service/src/main/java/com/crm/fees/service/FeeAuthorization.java](../microservices/fees-service/src/main/java/com/crm/fees/service/FeeAuthorization.java)
- [microservices/fees-service/src/main/java/com/crm/fees/service/FeeCycleService.java](../microservices/fees-service/src/main/java/com/crm/fees/service/FeeCycleService.java)
- [microservices/fees-service/src/main/java/com/crm/fees/service/FeeFinance.java](../microservices/fees-service/src/main/java/com/crm/fees/service/FeeFinance.java)
- [microservices/fees-service/src/main/java/com/crm/fees/service/FeeReferenceDirectory.java](../microservices/fees-service/src/main/java/com/crm/fees/service/FeeReferenceDirectory.java)
- [microservices/fees-service/src/main/java/com/crm/fees/service/FeeReportService.java](../microservices/fees-service/src/main/java/com/crm/fees/service/FeeReportService.java)
- [microservices/fees-service/src/main/resources/db/migration/V20261105_01__fee_tariffs_time_and_exact_hours.sql](../microservices/fees-service/src/main/resources/db/migration/V20261105_01__fee_tariffs_time_and_exact_hours.sql)
- [microservices/fees-service/src/test/java/com/crm/fees/FeeCyclePolicyTest.java](../microservices/fees-service/src/test/java/com/crm/fees/FeeCyclePolicyTest.java)
- [microservices/fees-service/src/test/java/com/crm/fees/FeeCycleSecurityTest.java](../microservices/fees-service/src/test/java/com/crm/fees/FeeCycleSecurityTest.java)
- [microservices/fees-service/src/test/java/com/crm/fees/FeeFinancialPolicyTest.java](../microservices/fees-service/src/test/java/com/crm/fees/FeeFinancialPolicyTest.java)
- [microservices/fees-service/src/test/java/com/crm/fees/FeeReferenceDirectoryTest.java](../microservices/fees-service/src/test/java/com/crm/fees/FeeReferenceDirectoryTest.java)
- [microservices/fees-service/src/test/java/com/crm/fees/FeeReportFixtureTest.java](../microservices/fees-service/src/test/java/com/crm/fees/FeeReportFixtureTest.java)
- [microservices/fees-service/src/test/java/com/crm/fees/FeeReportPolicyTest.java](../microservices/fees-service/src/test/java/com/crm/fees/FeeReportPolicyTest.java)
- [docs/LOT_3E_FINALISATION_HONORAIRES.md](LOT_3E_FINALISATION_HONORAIRES.md) — ce rapport.

Photographie SHA-256 de **1 022 fichiers préexistants suivis/non suivis**, hors fichiers de secrets et clés exclus de la lecture : aucune disparition ; **994 fichiers photographiés inchangés**. Les 25 migrations historiques, toutes les sources Paie/RH des Lots 3B/3C, le correctif Home et les rapports 3A–3D, ainsi que les scripts/configurations de validation progressive sont préservés. Les changements partagés concernent seulement le montant Fees désormais nullable dans le dashboard et l’URL de retour administrative `/fees/workbench`.

Les preuves et inventaires sont sous `target/lot3e`, ignoré par Git. Aucun secret réel utilisé ou affiché. Les fichiers nouveaux et modifiés ont été contrôlés sans suppression de fichier préexistant.

## 4. Endpoints ajoutés

Préfixe Gateway existant : `/fees`. Chemins ci-dessous locaux à Fees. Tous les endpoints du cycle sont réservés ADMIN/SUPER_ADMIN et leurs réponses réussies imposent `Cache-Control: no-store`.

| Méthode et chemin | Contrat |
|---|---|
| GET /api/fees/cycle/offerings | Recherche par intitulé, page de 20 prestations |
| POST /api/fees/cycle/offerings | Intitulé, employeeId, customerId facultatif, devise ; références distantes vérifiées |
| PATCH /api/fees/cycle/offerings/{id}/active | Désactivation ou réactivation sans effacement |
| GET /api/fees/cycle/offerings/{id}/tariffs | Liste paginée, filtre active facultatif, indicateur used |
| POST /api/fees/cycle/offerings/{id}/tariffs | Taux, validFrom, validTo facultatif, active |
| PUT /api/fees/cycle/tariffs/{id} | Modification uniquement avant utilisation |
| PATCH /api/fees/cycle/tariffs/{id}/active | État modifiable ; réactivation contrôlée contre les chevauchements |
| GET /api/fees/cycle/offerings/{id}/times?from=…&to=… | Temps paginés, annulés compris pour l’historique |
| POST /api/fees/cycle/offerings/{id}/times | UUID de requête, date, heures, description ; tarif déterminé côté serveur |
| PUT /api/fees/cycle/times/{id} | Modification avant annulation ou enregistrement en honoraire |
| POST /api/fees/cycle/times/{id}/cancel | Motif obligatoire ; conserve la ligne et ses valeurs |
| POST /api/fees/cycle/times/{id}/record-fee | Enregistrement unique d’un Fee, sans action Invoice |
| GET /api/fees/cycle/report?from=…&to=… | Lignes et synthèses ; offeringId/customerId facultatifs |
| GET /api/fees/cycle/report.csv?from=…&to=… | Même sélection, CSV technique UTF-8 avec BOM |

Le CRUD `/api/fees` est conservé. POST/PUT exigent désormais explicitement le header Authorization pour vérifier les nouvelles références. Le DTO accepte une devise facultative, utilisée telle quelle ; aucun défaut monétaire inventé. Les réponses ajoutent devise et liens d’origine.

**Évolution de contrat volontaire :** `GET /api/fees/statistics` conserve comptes, heures et distributions, mais renvoie `amount=null`. Le précédent total sans devise est supprimé de la requête et de l’affichage ; les montants consultables sont dans le rapport par devise. Le frontend courant accepte ce null explicitement. Les clients externes éventuels doivent être vérifiés.

Les identifiants du nouveau cycle sont textuels dans les réponses et conservés comme tels par Angular. Erreurs 400/401/403/404/409/502/503 selon validation, droits, état et disponibilité. Les conflits SQL restent expurgés ; les erreurs de paramètres, dates JSON et corps illisibles deviennent 400.

## 5. Modèles de données

| Modèle | Données et conservation |
|---|---|
| Fee existant | Colonnes additives exact_hours DECIMAL(8,2), currency nullable, offering_id et source_time_id ; ancien worked_hours DOUBLE et amount conservés |
| FeeOffering | Intitulé, référence Employee, référence Customer facultative, devise, actif ; identité métier et devise non réaffectables |
| FeeTariff | Prestation, taux DECIMAL(12,2), début/fin de validité, actif ; contenu figé dès toute utilisation, même ensuite annulée |
| FeeTimeEntry | UUID de requête, prestation, date, heures DECIMAL(8,2), description, tarif et taux appliqués, montant, lien Fee, annulation/motif et auteur/dates de création et mise à jour |

Le service du cycle porte les transactions. Les mutations sérialisent leurs décisions sous verrou pessimiste de la prestation, puis rafraîchissent les objets concernés sous verrou. Les écritures CRUD d’un Fee utilisent leur propre verrou commun d’édition/suppression. L’efficacité réelle de ces verrous et le rollback SQL ne sont pas prouvés par Mockito.

Le lien temps→Fee et le lien Fee→temps sont uniques, avec clés étrangères préparées. Le premier transfert crée le Fee et lie le temps dans la même transaction ; sa reprise retourne le Fee déjà créé. Aucune suppression de prestation, tarif ou temps n’est exposée. Aucun suivi de frais remboursables ou règle fiscale distincte n’est inventé au-delà du modèle Fees existant.

## 6. Interfaces Angular

La liste des honoraires mène à `/fees/workbench`, route lazy héritant des guards administratifs existants. Aucun nouveau droit global ou menu personnel n’est créé.

La vue propose prestations avec recherche/pagination et sélection d’annuaire existante, tarifs avec filtre actif/inactif et formulaire de validité, temps détaillés filtrés par période, annulation motivée et enregistrement en honoraire. Le taux réellement appliqué, la durée et le montant sont visibles. Les totaux par prestation et période sont calculés par le rapport complet, sans sommer seulement une page de temps.

Les rapports affichent groupes et synthèses par client/devise/origine, vide, chargement, erreur et valeurs indéterminées. Les honoraires sans date sont explicitement hors rapport. Le CSV conserve les mêmes filtres. Un tableau n’affiche pas un zéro de substitution lorsqu’un service échoue.

Le contexte de session est suivi : données, formulaires et références de recherche sont effacés lors d’un changement de session/droits ; les réponses tardives et téléchargements après déconnexion sont ignorés. Les actions concurrentes de l’interface sont bloquées pendant une requête. L’UUID d’une création de temps est conservé lors d’une reprise du même contenu après erreur.

Le formulaire Fee existant refuse précision, dépassement du produit et montant arrondi nul avant appel HTTP ; un calcul entier BigInt vérifie les centimes. La sauvegarde dépend du calcul serveur. Les détails et listes masquent ou bloquent édition/suppression d’un Fee généré ou engagé. Le dashboard renvoie vers le rapport monétaire par devise.

## 7. Règles tarifaires

Un taux horaire est rattaché à une prestation et hérite de sa devise immuable. Les bornes de validité sont inclusives ; fin absente signifie sans fin. Les chevauchements de tarifs **actifs** pour la même prestation sont refusés, y compris au jour de jonction commun. Deux périodes adjacentes doivent donc se terminer/commencer sur deux dates distinctes.

À la date de réalisation du temps, exactement un tarif actif doit être trouvé. Absence ou ambiguïté provoque 409 ; aucun zéro ni tarif arbitraire n’est choisi. Le tarif, le taux et le montant sont capturés dans le temps. Désactivation ou nouveau tarif ne modifient pas les temps ni honoraires existants.

Un tarif utilisé ne peut plus changer de taux ou de période ; son état actif peut changer. Pour une nouvelle politique de prix, désactiver l’ancien puis créer un tarif non ambigu. Une modification de date ou durée d’un temps encore libre sélectionne à nouveau le tarif applicable et vérifie les références ; une correction de description seule conserve le taux et le montant déjà capturés.

Ce sont des règles techniques explicites de cohérence. Aucun contrat de majoration, forfait, remise, taux légal, change ou facturation automatique n’est supposé. Leur acceptation métier reste nécessaire.

## 8. Calculs financiers

`Montant = heures × taux applicable`, BigDecimal côté Java, arrondi au centime `HALF_UP`. Exemple testé : 2,50 h × 12,34 = 30,85. Un produit 0,015 donne 0,02. Une valeur positive arrondie à zéro est refusée.

Les taux et montants doivent tenir dans DECIMAL(12,2), soit au plus 9 999 999 999,99 ; les heures dans DECIMAL(8,2), soit au plus 999 999,99. Taux/heures strictement positifs, deux décimales maximum. DTO et appels directs de service sont contrôlés. Le produit arrondi est validé avant mutation ou sauvegarde ; ni arrondi implicite SQL ni saturation.

Les anciens montants sont restitués sans recalcul. L’ancien DOUBLE est conservé ; les nouvelles saisies stockent aussi exact_hours, qui prévaut en lecture. Aucune conversion/backfill des historiques n’a été exécutée. Un agrégat contenant des durées historiques sans exact_hours laisse les heures indéterminées ; il ne convertit pas silencieusement une somme flottante.

Les rapports additionnent les montants enregistrés, et non un produit recalculé de sommes. Les temps annulés et déjà transférés sont exclus de la catégorie UNRECORDED_TIME ; le Fee transféré figure une fois dans RECORDED_FEE. Les deux catégories et chaque devise ont des totaux distincts. Une devise historique absente interdit tout montant agrégé correspondant ; aucun défaut TND/EUR n’est inventé.

Période obligatoire et inclusive, 366 jours maximum ; rapport limité à 1 000 groupes cumulés, avec requêtes bornées à 1 001 pour détecter tout dépassement et refuser sans troncature. Tarifs/temps/prestations sont paginés. Les dates écrites sont bornées à la plage MySQL DATE 1000–9999.

Les doublons manifestes de temps actifs sont détectés par prestation/date/durée/description normalisée, avec signature unique préparée en SQL. Une annulation conserve l’UUID et les valeurs, libère cette signature et ne réactive jamais la ligne. Un UUID réutilisé avec un contenu différent est refusé. Ces protections ne prouvent pas un rapprochement de factures externe.

## 9. Sécurité

ADMIN et SUPER_ADMIN administrent le cycle et ses données internes. EMPLOYEE et CLIENT sont refusés, en lecture comme en écriture ; aucun endpoint personnel ou paramètre permettant de choisir une identité propriétaire n’est ajouté. Les contrôles HTTP sont complétés dans les nouveaux services par un principal UID typé et le rôle administratif.

La vérification de référence exige un bearer, un identifiant canonique positif et une réponse distante objet avec l’identifiant entier exact attendu. Null, tableau, texte, décimal et mauvais identifiant sont refusés. Les redirections sont interdites ; des délais de connexion/lecture sont bornés. Les refus 401/403/404 sont conservés, l’indisponibilité devient 503 et une réponse invalide 502, sans corps distant transmis.

Le client utilise Discovery et les endpoints administratifs Employee/Customer existants. JWT, validation Identity, restrictions des autres modules et common-security restent inchangés. Le transport réellement annoncé par Discovery et les communications réseau doivent encore être éprouvés dans l’environnement candidat.

Les anciennes références libres restent lisibles et peuvent être conservées inchangées lors d’une édition autorisée ; création ou nouvelle affectation doit être vérifiée. Cela conserve l’historique sans prétendre prouver son existence actuelle.

Un Fee issu des temps est figé dès son enregistrement. Pour les anciens statuts déclaratifs libres, seuls vide/PENDING/UNPAID/UNBILLED/NOT_INVOICED/« À FACTURER » permettent édition/suppression ; les autres états sont protégés par prudence. Cela n’en fait pas une preuve Invoice. Une procédure de correction ultérieure reste à définir.

Le CSV est technique, UTF-8 avec BOM, guillemets doublés, cellules protégées contre =, +, -, @ et les espaces de tête. Il exporte uniquement les groupes sélectionnés et ne répète pas les totaux synthétiques comme lignes d’activité. Aucun secret ou erreur SQL brute n’est exposé.

## 10. Migrations

Nouvelle migration : [V20261105_01__fee_tariffs_time_and_exact_hours.sql](../microservices/fees-service/src/main/resources/db/migration/V20261105_01__fee_tariffs_time_and_exact_hours.sql). **Préparée, NON EXÉCUTÉE.**

Contrôles statiques : version distincte après V1 ; tables et identifiants BIGINT cohérents ; montants DECIMAL(12,2), heures exactes DECIMAL(8,2) ; références uniques UUID/source/liaison, index de période/validité et clés étrangères ; temps créé avant la FK réflexive depuis fees. Aucun DROP, UPDATE de données, baseline, repair ou clean. Les nouvelles colonnes historiques sont nullable.

Le dépôt comporte désormais **26 migrations, dont 25 historiques inchangées par SHA-256**. L’inventaire complet est sous `target/lot3e/migrations.json`. La migration n’a pas été utilisée par les fixtures H2.

Restent à valider sur MySQL 8 : installation neuve/upgrade sur copie autorisée, syntaxe DDL, types Hibernate, clés étrangères, checks, collations, index/plans, projections JPA, paramètres JDBC et transactions/concurrence. Les DDL MySQL ne garantissent pas un rollback global.

## 11. Tests exécutés

Exécutions séquentielles hors ligne, cache Maven local, heap Maven 512 Mio/JVM tests 384 Mio ; Angular Node 1 536 Mio et deux workers. Aucun build Java et Angular simultané.

| Suite Java | Cas réussis | Portée |
|---|---:|---|
| FeeFinancialPolicyTest | 12 | Calcul, limites, arrondis, précision, refus avant écriture, protection des historiques |
| FeeCyclePolicyTest | 25 | Tarifs, chevauchements, snapshots, UUID/doublons, annulation, gel et transfert unique |
| FeeCycleSecurityTest | 11 | Filtres réels/MVC, quatre rôles, 401/503, no-store, erreurs expurgées et validation HTTP |
| FeeReadSecurityTest | 31 | Non-régression des lectures/écritures Fees et statistiques administratives |
| FeeReferenceDirectoryTest | 5 | Référence canonique, réponse stricte, namespaces, absence de Discovery, petit serveur HTTP synthétique |
| FeeReportFixtureTest | 5 | SQL de rapport sur tables H2 synthétiques : bornes, filtres, devises, exclusions et valeurs inconnues |
| FeeReportPolicyTest | 7 | Totaux séparés, historiques, erreurs, bornes, CSV et refus personnels |
| SqlErrorPrivacyTest | 1 | Absence de fuite SQL |
| **Total Java distinct** | **97** | Dernière sélection finale, sans addition des relances |

Les services sont testés avec dépôts/annuaires simulés. FeeCycleSecurityTest utilise les filtres réels avec résultat Identity simulé, sans réseau/base. FeeReadSecurityTest utilise une tranche WebMvcTest et le filtre JWT/parseur de production, avec un serveur Identity HTTP **synthétique local**. FeeReferenceDirectoryTest utilise un autre petit serveur synthétique pour les headers et erreurs ; aucun Employee, Customer ou Identity CRM réel.

Les cinq fixtures H2 créent trois tables minimales en mémoire, lisent les deux requêtes natives depuis les annotations du dépôt et utilisent leurs paramètres JDBC nommés, y compris les filtres. Aucune adaptation de dialecte n’a été nécessaire pour ces requêtes simples. Cela prouve leurs résultats sur les fixtures, **pas** le dialecte MySQL effectif, le mapping des projections JPA ni l’exécution Flyway.

**Angular : 1 187/1 187 réussis**, ChromeHeadless 156, sélection complète `src/app/**/*.spec.ts`. Les specs Paie/RH, navigation et accueil 3D sont incluses. Les nouveaux tests couvrent calcul exact, bornes de formulaire, gel, grands IDs textuels, HTTP/erreurs, tarifs, temps, reprise UUID, rapports, réponses tardives/session et accès à workbench selon les quatre rôles. Services HTTP simulés, aucun backend CRM connecté.

La sélection ciblée Fees/dashboard antérieure a passé 117 cas ; elle est incluse dans la suite complète finale et ne s’y ajoute pas. Les cas ajoutés ensuite aux formulaires/dashboard sont inclus dans les 1 187.

**Build Angular production réussi**, bundle initial **888,70 kB**, sous le seuil bloquant existant de 1 MB. Avertissements conservés : budget indicatif initial 500 kB, styles Payroll/Invoices et projection d’icônes NG8011 dans CustomersPageComponent. Aucun budget ou source de ces modules n’a été modifié pour les masquer.

Commandes finales :

    # MAVEN_OPTS=-Xmx512m
    mvn -o -B -ntp -Dmaven.repo.local=C:/Users/ASUS/.m2/repository -f microservices/fees-service/pom.xml -Dtest=FeeFinancialPolicyTest,FeeCyclePolicyTest,FeeReadSecurityTest,SqlErrorPrivacyTest,FeeReferenceDirectoryTest,FeeReportPolicyTest,FeeReportFixtureTest,FeeCycleSecurityTest -DargLine=-Xmx384m test
    # Depuis frontend, NODE_OPTIONS=--max-old-space-size=1536, NG_BUILD_MAX_WORKERS=2 et CHROME_BIN configuré :
    .\node_modules\.bin\ng.cmd test --no-watch --browsers=ChromeHeadless --karma-config=karma.ci.conf.cjs --include=src/app/**/*.spec.ts
    .\node_modules\.bin\ng.cmd build --configuration production --output-path ../target/lot3e/frontend-production

Échecs intermédiaires corrigés : appels Mockito anciens après ajout du bearer ; fixtures HTTP sans Content-Type JSON, stubbings imbriqués et restubbings d’exceptions ; configuration zoneless manquante dans les nouveaux tests HTTP Angular ; attente null au lieu de chaîne vide pour une erreur absente du nouveau test dashboard. Aucun de ces échecs n’est compté comme succès. Les sélections finales n’ont ni échec ni test ignoré.

## 12. Tests non exécutés

**NON EXÉCUTÉS :** Docker/MySQL/Flyway, migrations neuves/upgrade, Hibernate validate réel, transactions et verrous concurrents, rollback après erreur SQL, contraintes/collations MySQL, Gateway/Discovery/Identity/Employee/Customer réels, HTTPS, navigateur connecté quatre comptes, performance/charge, conversion en facture ou rapprochement bancaire.

FeesServiceIntegrationTest a été adapté au bearer et au validateur distant simulé et compilé, mais **pas exécuté**. Aucun SpringBootTest avec base ni démarrage CRM. Les suites Java des autres domaines ne sont pas relancées ; leurs décomptes historiques 3B/3C/3D ne sont pas ajoutés aux 97 cas Fees. La suite Angular complète les couvre côté interface avec simulations.

Les blocages RAM/Docker du Lot 3D restent des observations historiques : aucune mesure n’a été renouvelée ici, puisque le démarrage d’infrastructure est interdit dans ce lot.

## 13. Anomalies corrigées

- **AN-01 :** DTO sans précision et multiplication de service non bornée ; validation cohérente et BigDecimal arrondi/borné avant mutation et sauvegarde.
- **Montant global sans devise :** ancienne somme monétaire indifférenciée supprimée ; API nullable explicite, dashboard adapté et rapports par devise.
- **Mutations destructives possibles :** blocage des honoraires générés/engagés et des temps transférés ; tarifs utilisés figés et annulation conservant l’historique.
- **Références nouvelles libres :** vérification authentifiée des références Employee/Customer lors de création ou réaffectation ; anciens IDs inchangés conservables.
- **Erreurs d’entrée :** corps/date JSON illisibles et paramètres incorrects traités en 400 ; erreurs distantes classées sans exposer leur contenu.

Ces constats et correctifs sont établis dans le code/tests locaux. Aucun rejet, arrondi, orphelin ou défaut concurrent MySQL n’est prétendu reproduit. Les anomalies Customer AN-02/AN-03 du Lot 3A restent hors périmètre et non résolues par ce lot.

## 14. Exigences FEE-01 à FEE-06 actualisées

COMPLETE signifie périmètre de code relié backend/Angular avec tests correspondants, comme dans 3A. **Toutes les intégrations réelles restent NON VALIDÉES.** Les rapports historiques ne sont pas réécrits.

| Exigence | Avant 3E | Après 3E | Preuves et limites |
|---|---|---|---|
| FEE-01 Prestations | COMPLETE | COMPLETE conservée et enrichie | CRUD existant, prestations vérifiées, tests finance/cycle/HTTP et écrans ; MySQL et annuaires réels à valider |
| FEE-02 Description/services | COMPLETE | COMPLETE conservée | Description Fee et travail détaillé, consultation, modification autorisée ; interfaces rendues et règles testées |
| FEE-03 Grilles personnalisables | ABSENTE | COMPLETE pour tarifs horaires par prestation | Création/édition avant usage, validité, état, chevauchement, devise héritée et gel historique ; acceptation métier et MySQL restants |
| FEE-04 Temps détaillés | PARTIELLE | COMPLETE dans le cycle défini | Date/durée/description, références héritées vérifiées, période/pagination, annulation, doublons, gel après transfert ; concurrence réelle non testée |
| FEE-05 Calcul automatique | PARTIELLE | COMPLETE dans le modèle durée × taux | AN-01, précision/limites/arrondi, tarif daté et snapshot, tests Java/Angular ; aucune règle légale ou conversion monétaire revendiquée |
| FEE-06 Rapports de facturation | PARTIELLE | PARTIELLE ; rapports techniques complets sur les données fiables disponibles | Totaux période/prestation/client/devise/origine, filtres et CSV testés ; montants effectivement facturés/non facturés non démontrables sans contrat Invoice |

## 15. Risques et décisions restantes

Définir avec le métier le contrat Fees→Invoice si requis : client destinataire, lignes, regroupement, devise, taxes/remises éventuelles, date de facturation, idempotence, états et corrections. Les statuts historiques libres ne constituent pas une source fiable de montant déjà facturé. Aucune nouvelle liaison de bases ni copie d’état Invoice n’est introduite.

Faire accepter la durée décimale à deux chiffres et HALF_UP comme convention technique, la devise explicite, les périodes inclusives et le gel dès enregistrement d’un honoraire. Définir les corrections d’honoraires générés ou engagés, car aucun mécanisme destructif de déverrouillage n’est exposé.

Les anciens montants, références libres, devises/dates absentes et heures DOUBLE ne sont pas réconciliés automatiquement. Leurs valeurs restent lisibles ; les synthèses indéterminées sont explicites. Une politique de rapprochement historique doit être décidée avant tout backfill.

Le rapport distingue enregistrement interne et facture ; les lignes sans date sont exclues d’une période. Les limites techniques ne sont pas des règles de travail ou de facturation. Index, charge, pagination profonde, collisions réelles et timeout interservices sous verrou restent à observer sur un candidat MySQL.

Les clients externes consommant statistics.amount doivent accepter null. Les identifiants du cycle sont textuels ; les anciens écrans Fee conservent leur contrat numérique et rejettent les IDs JavaScript non sûrs plutôt que les arrondir. Les décisions légales Paie/congés, les risques Customer et les blocages intégrés restent ceux des rapports antérieurs.

## 16. Prochaines étapes

Après autorisation opérationnelle et précontrôles positifs, préparer une **nouvelle** campagne isolée à partir du [Lot 2B-R](LOT_2B_R_VALIDATION_PROGRESSIVE.md). Les anciens manifestes antérieurs à la migration 3E ne prouvent pas le candidat courant et ne doivent pas être réutilisés silencieusement.

1. Vérifier schéma neuf et upgrade sur copie sauvegardée/autorisée : conservation des historiques, DDL, types Hibernate, contraintes, collations et projections du rapport.
2. Tester deux prestations, deux clients/salariés, plusieurs devises et quatre rôles ; vérifications distantes, refus croisés, indisponibilité et vrais tokens via Gateway.
3. Exercer jonctions de validité, chevauchements, tarifs utilisés/désactivés, précision/arrondis/limites, absence de tarif et historique inchangé.
4. Tester simultanément doubles créations UUID/signature, mises à jour de tarifs/temps et doubles transferts ; vérifier un seul Fee, pas de doublon, rollback et gel après transfert.
5. Réconcilier rapports/CSV avec un jeu connu, temps annulés/transférés, filtres, valeurs historiques inconnues, volumes et navigation mobile/clavier dans un navigateur connecté.

Les preuves expurgées sont dans `target/lot3e/results-sanitized.json`, `inventory.json` et `migrations.json`. Les logs bruts et le build sont ignorés par Git. Aucun déploiement ni certification du CRM complet.

**LOT 3E PARTIEL.**
