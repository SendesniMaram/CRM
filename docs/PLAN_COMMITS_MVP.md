# Préparation des commits locaux MVP — 16 octobre 2026

**READY TO COMMIT — aucun git add ni commit effectué. Attendre une confirmation explicite de l'utilisatrice.** Branche microservices, HEAD de référence 40b6d0f55dfeb2c0ac6d8e6adbb10284370917f2. L'index est vide. Ce manifeste contient la liste exacte proposée : **39 fichiers distincts**, en cinq commits sans fichier partagé entre groupes.

## Vérifications finales exécutées

- Backend isolé : **410 PASS**, 0 échec, 0 ignoré. Détail : common-security5, Identity20, Employee49, Department20, HR155, Customer45, Invoice43, Fees44, Payroll27, Discovery1, Gateway1. Les 403 tests des huit services JPA sont inclus dans ces 410.
- Angular : **643/643 PASS**, aucun ignoré ; deux tests supplémentaires depuis la campagne à 641.
- Build production : succès ; initial839,25 kB, transfert estimé199,27 kB, warning500 kB dépassé de339,25 kB ; seuil erreur1 MB non atteint.
- Scripts : quatre fichiers PowerShell syntaxiquement valides ; tests locaux d'import et vérificateur48 contrôles réussis, aucune API réelle appelée.
- Compose : configuration valide, huit services validate/never, URLs sans création automatique de base. Profils H2 de test préservés.
- Revue des fichiers modifiés/nouveaux : aucun changement étranger au MVP ; scan des valeurs sensibles locales disponibles, formats JWT et clés privées sans correspondance. Ce contrôle est une vérification ciblée, pas une garantie universelle d'absence de secret.
- git diff --check réussi ; avertissements de conversion LF/CRLF uniquement. Fichiers nouveaux contrôlés également avec diff --no-index --check.
- Les données DEMO40/40 sont la validation SQL de la campagne précédente, pas un test API authentifié relancé ici. Aucun accès en écriture à MySQL, redémarrage, déploiement ou intervention Oracle.

## Groupes proposés

### 1. feat(frontend): finaliser le dashboard dynamique — 11 fichiers

- `frontend/src/app/features/dashboard/dashboard.component.html`
- `frontend/src/app/features/dashboard/dashboard.component.scss`
- `frontend/src/app/features/dashboard/dashboard.component.spec.ts`
- `frontend/src/app/features/dashboard/dashboard.component.ts`
- `frontend/src/app/features/dashboard/dashboard-data.service.ts`
- `frontend/src/app/features/dashboard/dashboard-data.service.spec.ts`
- `frontend/src/app/core/testing/dashboard-fixture.ts`
- `frontend/src/app/features/auth/login/login.router.spec.ts`
- `frontend/src/app/hr-navigation.spec.ts`
- `frontend/src/app/navigation-availability.spec.ts`
- `frontend/src/app/routing-audit.spec.ts`

### 2. fix(config): securiser les configurations des microservices — 9 fichiers

- `docker-compose.yml`
- `microservices/identity-service/src/main/resources/application.yml`
- `microservices/employee-service/src/main/resources/application.yml`
- `microservices/department-service/src/main/resources/application.yml`
- `microservices/customer-service/src/main/resources/application.yml`
- `microservices/hr-service/src/main/resources/application.yml`
- `microservices/invoice-service/src/main/resources/application.yml`
- `microservices/fees-service/src/main/resources/application.yml`
- `microservices/payroll-service/src/main/resources/application.yml`

### 3. fix(validation): aligner les limites des formulaires et DTO — 10 fichiers

- `microservices/department-service/src/main/java/com/crm/department/dto/DepartmentRequest.java`
- `microservices/employee-service/src/main/java/com/crm/employee/dto/EmployeeRequest.java`
- `microservices/department-service/src/test/java/com/crm/department/DepartmentServiceIntegrationTest.java`
- `microservices/employee-service/src/test/java/com/crm/employee/EmployeeServiceIntegrationTest.java`
- `frontend/src/app/features/departments/pages/department-form/department-form.component.ts`
- `frontend/src/app/features/departments/pages/department-form/department-form.component.html`
- `frontend/src/app/features/departments/pages/department-form/department-form.component.spec.ts`
- `frontend/src/app/features/employees/pages/employee-form/employee-form.component.ts`
- `frontend/src/app/features/employees/pages/employee-form/employee-form.component.html`
- `frontend/src/app/features/employees/pages/employee-form/employee-form.component.spec.ts`

### 4. test(demo): ajouter les donnees et scripts de validation du MVP — 6 fichiers

- `scripts/demo/Import-TechNova.ps1`
- `scripts/demo/technova.json`
- `scripts/demo/Test-TechNova.Local.ps1`
- `scripts/demo/Test-CRM-MVP.ps1`
- `scripts/demo/Test-CRM-MVP.Local.ps1`
- `scripts/demo/README.md`

### 5. docs(mvp): documenter les tests et les limites connues — 3 fichiers

- `TESTS_MVP_CRM.md`
- `docs/DEPLOIEMENT_SECUR_MVP.md`
- `docs/PLAN_COMMITS_MVP.md`

## Exclusions explicites

- `REVUE_FINALE_CRM.md` : rapport historique examiné, daté du 15 octobre et référant HEAD39c43b8 et un périmètre déjà commité. Le conserver non suivi, conformément à l'exclusion antérieure ; ne pas l'inclure sans autorisation spécifique. SHA256 inchangé : 65330338E2ED639588BE8D69C1622DD243C5EF8091D28000CA31E858527FC5B5.
- `.env` : secrets locaux, ignoré. Ne pas ajouter les variables d'environnement exportées, JWT, identifiants ou mots de passe.
- `*.log`, notamment `commit-check-*.log`, `campaign-*.log` et `fix-major-*.log` : sorties techniques ignorées, pas de commit.
- `frontend/dist/`, `frontend/node_modules/`, les `target/` Maven et rapports Surefire générés : artefacts exclus par les règles existantes.
- Aucune modification attendue de `.env.example`, du monolithe historique ou des bases ; aucun autre fichier à ajouter.

## Limites à conserver dans le rapport

- Configurations et DTO corrigés **non déployés** : les images actives restent anciennes. Ce commit ne serait pas un déploiement.
- Avertissement bundle Angular839,25 kB ; avertissement de fermeture ChromeHeadless après les643 succès. Aucune correction de ces anomalies mineures dans cette mission.
- API authentifiées réelles, sessions ADMIN/EMPLOYEE/CLIENT et rendu visuel/responsive restent à valider par une méthode autorisée ; les tests isolés ne les remplacent pas.
- Un futur démarrage en validate doit échouer si un schéma manque ou diffère ; aucune création/migration automatique n'est prévue.

## Exécution Git après confirmation uniquement

Pour chaque groupe, utiliser `git add --` suivi exclusivement des chemins explicites ci-dessus, puis examiner `git diff --cached --check`, `git diff --cached --stat` et **le diff complet** avant `git commit -m` avec le message proposé. Ne jamais inclure tous les fichiers par un raccourci global ; ne pas utiliser git add ., git add -A ou git commit -a. Vérifier la branche, HEAD et tout nouvel écart avant d'agir. Aucun push. Ne pas recréer les commits précédents. Aucun commit ne nécessite une sélection de hunks entre ces cinq groupes, chaque fichier appartenant à un seul groupe.
