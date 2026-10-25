# Documents PDF CRM — audit et validation du 17 octobre 2026

> Archive documentaire : les constats, chiffres, anciens commits et autorisations ci-dessous décrivent la campagne datée du rapport. Ils ne constituent pas un état courant ni un plan à réappliquer sur `publication-clean`. Toute opération réelle nécessite une nouvelle vérification et l’autorisation adaptée.

Campagne initiale ci-dessous ; les observations ultérieures sont résumées dans la note datée suivante.

## Historique utile et limites de validation — consolidation du 22 octobre 2026

Les corrections Angular ont supprimé les débordements avec `min-width: 0` et retour à la ligne des contenus longs. La campagne du 17 octobre comptait 714 tests Angular réussis et des contrôles visuels à 768 px/zoom 200 %. Ces résultats historiques ne remplacent pas la checklist [PDF_ANGULAR_MANUAL_VALIDATION.md](PDF_ANGULAR_MANUAL_VALIDATION.md).

Le 17 octobre à 15:45:19 UTC, les images Invoice, HR et Payroll comportant les contrôleurs PDF ont effectivement été mises en service. Le contrôle suivant à 16:01:48 UTC constatait leur disponibilité et des refus 401 sans JWT, en direct et via Gateway. Les JAR actifs avaient été comparés aux JAR testés. La campagne Java correspondante comptait 242 tests réussis (Invoice 49, HR 161, Payroll 32) ; les 714 tests Angular provenaient de la campagne antérieure. Aucun téléchargement PDF authentifié ni validation complète du rendu métier réel n'a été établi. Ces observations et identifiants d'images historiques ne décrivent pas les conteneurs actuels et n'autorisent pas une reprise sur une ancienne image.

L'accès personnel aux congés utilise désormais le userId du principal validé et l'association centralisée HR ; le username ne détermine plus la propriété. Les routes administratives, identifiants de demandes et relations Employee sont conservés. La paie personnelle reste hors périmètre. Toute nouvelle validation réelle exige son autorisation et doit préserver les conditions de sécurité courantes.

Verdict : **READY FOR REVIEW** pour les trois documents prioritaires, avec accès administratif uniquement à la paie. Aucune mise en service, rotation, migration, opération SQL sur MySQL, modification de `.env`, opération de commit ou de push n'a été effectuée.

## Audit et périmètre

| Document | Données disponibles et utilisées | Autorisations conservées |
| --- | --- | --- |
| Facture | Numéro, référence client, émission, échéance, sous-total, taxe, total enregistré, devise, statut, notes | ADMIN, SUPER_ADMIN, comme la lecture existante |
| Congé | Référence employé, dates, motif accessible au propriétaire, statut réel, validateur enregistré pour APPROVED/REJECTED | ADMIN, SUPER_ADMIN ou EMPLOYEE propriétaire, contrôlé par `LeaveAccess` dans le service existant |
| Paie | Référence employé, période, date de paiement, salaire de base, primes, retenues, impôt, net enregistré, devise, statut | ADMIN, SUPER_ADMIN ; EMPLOYEE et CLIENT refusés |

La facture ne contient pas de lignes détaillées ni d'identité/adresse client complète dans son DTO. Le congé ne possède pas de champ type. Ces informations ne sont pas inventées. Aucun nouveau calcul, taux, signature, date d'approbation ou mention légale n'est ajouté. Les montants sont formatés depuis les BigDecimal existants ; le total et le net ne sont jamais recalculés.

Le lien fiable utilisateur–employé n'existe pas dans payroll-service. L'ouverture aux salariés est donc bloquée jusqu'à une décision et une association côté serveur testée contre les accès croisés. L'association RH n'est pas supposée disponible dans la paie et aucun identifiant fourni par Angular ne sert de preuve de propriété.

## Intégration

Pas de nouveau microservice, de modification des routes Gateway, des guards, des rôles ou des schémas. Les contrôleurs PDF distincts réutilisent les interfaces métier et les chaînes Spring Security existantes.

| Service | Endpoint interne | URL Gateway utilisée par Angular |
| --- | --- | --- |
| invoice-service | GET /api/invoices/{id}/pdf | /invoice/api/invoices/{id}/pdf |
| hr-service | GET /api/leaves/{id}/pdf | /hr/api/leaves/{id}/pdf |
| payroll-service | GET /api/payrolls/{id}/pdf | /payroll/api/payrolls/{id}/pdf |

Succès : `application/pdf`, `Content-Disposition: attachment`, nom stable fondé uniquement sur l'identifiant, `Cache-Control: no-store`, `Pragma: no-cache`, `Vary: Authorization`, `X-Content-Type-Options: nosniff`. Authentification et autorisation restent obligatoires ; 401/403/404 suivent les conventions existantes. Angular accepte les réponses PDF et les erreurs JSON.

Génération synchrone en mémoire avec **Apache PDFBox 3.0.8**, sous [licence Apache 2.0](https://pdfbox.apache.org/), version vérifiée sur la [page officielle de téléchargement](https://pdfbox.apache.org/download.html). Aucune bibliothèque PDF préexistante ni fichier de licence du projet n'a été trouvé dans les sources examinées. Les obligations de licence et notices de dépendances doivent être conservées lors de la distribution ; aucune conclusion juridique sur la licence globale du projet n'est supposée.

Le petit moteur A4 est identique dans les trois services pour préserver leurs builds indépendants et éviter une dépendance à un nouveau module partagé. Une évolution du moteur doit être reportée dans les trois copies. Il utilise Helvetica, les accents français, des marges régulières, un tableau avec retour à la ligne, un en-tête répété et une pagination. Les caractères non pris en charge par cette police, notamment certains alphabets non latins et emoji, deviennent `?` ; une police Unicode embarquée serait nécessaire pour élargir ce périmètre.

Aucun fichier PDF métier n'est sauvegardé côté serveur, aucune ressource distante n'est chargée et aucun contenu documentaire n'est journalisé par le nouveau code. Les téléchargements suivent l'intercepteur JWT existant. Le composant Angular ne reçoit qu'un type de document fermé et un identifiant entier positif ; il refuse les réponses vides ou d'un autre Content-Type, crée un nom local sûr et libère l'URL Blob. Le bouton natif permet le clavier, montre un spinner et empêche les doubles soumissions. Une erreur est annoncée par `role="alert"`, sans afficher le corps sensible d'une réponse API. Aucune confirmation de succès anticipée.

Les boutons Factures/Paie se trouvent dans leurs pages de détail déjà protégées par rôle. Le bouton Congés est dans la liste filtrée par le backend. La protection décisive demeure dans les microservices.

## Fichiers de cette intervention

Fichiers existants modifiés :

- `microservices/invoice-service/pom.xml`
- `microservices/hr-service/pom.xml`
- `microservices/payroll-service/pom.xml`
- `frontend/src/app/features/invoices/invoice-detail.component.ts`
- `frontend/src/app/features/invoices/invoice-detail.component.html`
- `frontend/src/app/features/payroll/payroll-detail.component.ts`
- `frontend/src/app/features/payroll/payroll-detail.component.html`
- `frontend/src/app/features/leaves/leaves-page.component.ts`
- `frontend/src/app/features/leaves/leaves-page.component.html`

Nouveaux fichiers :

- `microservices/invoice-service/src/main/java/com/crm/invoice/controller/InvoicePdfController.java`
- `microservices/invoice-service/src/main/java/com/crm/invoice/pdf/PdfReport.java`
- `microservices/hr-service/src/main/java/com/crm/hr/controller/LeavePdfController.java`
- `microservices/hr-service/src/main/java/com/crm/hr/pdf/PdfReport.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/controller/PayrollPdfController.java`
- `microservices/payroll-service/src/main/java/com/crm/payroll/pdf/PdfReport.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/InvoicePdfTest.java`
- `microservices/invoice-service/src/test/java/com/crm/invoice/PdfPaginationTest.java`
- `microservices/hr-service/src/test/java/com/crm/hr/LeavePdfTest.java`
- `microservices/payroll-service/src/test/java/com/crm/payroll/PayrollPdfTest.java`
- `frontend/src/app/shared/pdf-download/pdf-download.service.ts`
- `frontend/src/app/shared/pdf-download/pdf-download.component.ts`
- `frontend/src/app/shared/pdf-download/pdf-download.component.spec.ts`
- `docs/PDF_DOCUMENTS.md`

Les scripts de contrôle Chrome, captures et logs sont uniquement dans `frontend/tmp/visual-review/`, déjà ignoré. Les exemples PDF/PNG sont dans les répertoires Maven `target/pdf-examples/`, ignorés. Ils utilisent exclusivement des données synthétiques et ne sont pas à ajouter au commit.

## Validation réalisée

| Contrôle | Résultat |
| --- | --- |
| Suite invoice-service | 49 tests réussis, aucun échec/erreur/ignoré |
| Suite hr-service | 161 tests réussis, aucun échec/erreur/ignoré |
| Suite payroll-service | 32 tests réussis, aucun échec/erreur/ignoré |
| Total backend concerné | **242 PASS**, dont 17 nouveaux tests PDF |
| Suite Angular complète | **705 PASS**, dont 5 nouveaux tests de téléchargement |
| Build Angular production | PASS |
| Bundle initial | **867,33 kB**, transfert estimé **211,02 kB**, identique au build avant intervention à la précision affichée |
| Budget initial | Avertissement préexistant : 500 kB dépassés de 367,33 kB |
| Chrome réel automatisé | **13 contrôles PASS**, six rendus à 375/1440 px, aucune exception JavaScript ni débordement horizontal global |
| git diff --check | PASS |

Les nouveaux tests backend vérifient un PDF lisible avec PDFBox, A4, Content-Type, Content-Disposition, cache, montants enregistrés volontairement distincts d'un recalcul, accents, texte long sur plusieurs pages, documents inexistants, absence d'authentification, rôles interdits et autorisés. Les tests RH traversent le vrai service et son association de propriété : propriétaire, autre salarié malgré un paramètre employé falsifié, association absente, administration, PENDING/APPROVED/REJECTED et validateur réel du dossier de test. Les suites utilisent les configurations H2 en mémoire ou des services simulés, sans MySQL.

Chrome charge le véritable frontend local, avec toutes les requêtes API vers la Gateway interceptées et des réponses synthétiques. Contrôles : focus natif, spinner et désactivation, téléchargement d'un vrai PDF synthétique, nom `facture-42.pdf`, erreur API et nouvelle tentative, bouton congé salarié, guards Factures/Paie et redirection anonyme. Le tableau Congés conserve son défilement horizontal interne sur petit écran ; aucun élargissement de la page. Les captures ont été examinées, et le contraste du bouton sur les bandeaux sombres a été corrigé avant la dernière validation.

Les trois exemples PDF ont été rasterisés et examinés visuellement. Le contrôle Chrome ne constitue pas un test bout en bout des nouvelles API déployées : les services actifs n'ont pas été redémarrés. Les autres suites de microservices ne sont pas relancées puisque leurs sources sont inchangées. Les contrôles navigateur à 768 px, au zoom 200 % et avec lecteur d'écran ne font pas partie de cette campagne PDF.

Reproduction des suites isolées depuis la racine, sans lancer de service :

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
mvn.cmd -f microservices/invoice-service/pom.xml test
mvn.cmd -f microservices/hr-service/pom.xml test
mvn.cmd -f microservices/payroll-service/pom.xml test
Push-Location frontend
npm.cmd test -- --watch=false --browsers=ChromeHeadless
npm.cmd run build -- --configuration production
Pop-Location
git diff --check
```

## Artifacts locaux synthétiques

| Document | PDF de test | Capture desktop | Capture mobile |
| --- | --- | --- | --- |
| Facture | [PDF](../microservices/invoice-service/target/pdf-examples/invoice-synthetic.pdf) | [1440 px](../frontend/tmp/visual-review/pdf-invoices-1440.png) | [375 px](../frontend/tmp/visual-review/pdf-invoices-375.png) |
| Congé | [PDF](../microservices/hr-service/target/pdf-examples/leave-synthetic.pdf) | [1440 px](../frontend/tmp/visual-review/pdf-leaves-1440.png) | [375 px](../frontend/tmp/visual-review/pdf-leaves-375.png) |
| Paie | [PDF](../microservices/payroll-service/target/pdf-examples/payroll-synthetic.pdf) | [1440 px](../frontend/tmp/visual-review/pdf-payroll-1440.png) | [375 px](../frontend/tmp/visual-review/pdf-payroll-375.png) |

Ces liens sont destinés à la revue locale : les artefacts ignorés ne seront pas disponibles dans un clone Git.

## Extensions à soumettre à validation

- Honoraires : réutiliser les rubriques et montants réellement stockés dans fees-service après confirmation des droits de lecture et de la nature du document. Ne pas supposer de calcul de TVA ou de mentions fiscales.
- Fiche employé : confirmer les champs personnels exportables et les droits de lecture champ par champ avant toute implémentation.
- Fiche client : confirmer l'identité entre compte connecté et fiche client avant un accès propriétaire ; ne pas assimiler automatiquement un identifiant de formulaire à l'utilisateur JWT.

Aucun de ces trois documents supplémentaires n'est implémenté. Une validation métier et des autorisations suffisantes sont nécessaires avant extension.

## Préservation et limites de mise en service

Branche `microservices`, HEAD inchangé `00c2870956826adbb32b9cc22b9506879f5cdfdd`, index vide. Les empreintes des 380 fichiers hors frontend du relevé préalable ont été comparées : seules les trois dépendances Maven autorisées diffèrent. `.env`, `REVUE_FINALE_CRM.md`, configurations sensibles, scripts de rotation et autres changements préexistants sont préservés. Les données DEMO-TNC n'ont pas été consultées ni modifiées pendant cette intervention.

Avant mise en service autorisée : reconstruire les trois applications puis vérifier les nouveaux endpoints via Gateway avec des comptes de revue autorisés, confirmer les refus croisés et les téléchargements réels. Aucune action de déploiement ou redémarrage n'est déclenchée par cette procédure de revue.
