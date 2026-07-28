# TODO - État actuel du projet CRM (après dernier commit)

## ✅ Dernier commit : `66eafc8` (microservices)
> Implémenter l'infrastructure technique du microservice Employee et migrer l'API JWT

---

## ✅ Services commités et stables

- [x] **Backend monolithique** (`backend/`) — port 8081, MySQL `crm_db`
- [x] **Discovery Service** (`microservices/discovery-service/`) — port 8761
- [x] **Gateway Service** (`microservices/gateway-service/`) — port 8080
- [x] **Identity Service** (`microservices/identity-service/`) — port 8082, MySQL `identity_db`
- [x] **CI/CD GitHub Actions** — Matrix 3 services + SonarQube Cloud

---

## 🔄 Modifications en cours (non commitées)

### Fichiers modifiés
- [x] `employee-service/.../EmployeeRepository.java` — Ajout `existsByEmployeeCode()`, `existsByEmail()`, `JpaSpecificationExecutor`
- [x] `employee-service/.../IEmployeeService.java` — Ajout méthodes paginées
- [x] `employee-service/.../EmployeeServiceImpl.java` — CRUD complet avec validation unicité + pagination/search + Feign Client vers Department
- [x] `gateway-service/.../application.yml` — Routes employee-service ajoutées

### Nouveaux fichiers (non suivis)
- [x] `employee-service/.../EmployeeController.java` — Controller REST complet
- [x] `employee-service/.../EmployeeSpecification.java` — Recherche JPA par mot-clé
- [x] `microservices/department-service/` — Squelette complet du microservice Department

---

## ✅ Ce qui a été fait dans cette session

- [x] **Création de `OpenApiConfig.java`** pour employee-service et department-service
- [x] **Création de la config complète** `application.yml` pour department-service
- [x] **Correction de `application.yml`** pour employee-service (ajout jpa, springdoc, resilience4j)
- [x] **Vérification compilation employee-service** — ✅ BUILD SUCCESS (37 fichiers, 9.4s)
- [x] **Vérification compilation department-service** — ✅ BUILD SUCCESS (14 fichiers, 7.8s)

---

## 🚧 Travaux restants

### 1. 🔧 Ajouter employee-service et department-service au CI/CD
- [ ] Mettre à jour `.github/workflows/build.yml` pour inclure les 2 nouveaux services dans la matrix

### 2. 🧪 Créer les bases MySQL
- [ ] `CREATE DATABASE IF NOT EXISTS employee_db;`
- [ ] `CREATE DATABASE IF NOT EXISTS department_db;`

### 3. 📦 Commiter les modifications en cours
- [ ] `git add microservices/employee-service/` (fichiers modifiés + nouveaux)
- [ ] `git add microservices/department-service/` (nouveau service)
- [ ] `git add microservices/gateway-service/src/main/resources/application.yml`
- [ ] `git commit -m "Finaliser employee-service CRUD, ajouter department-service et config Gateway"`

### 4. 📝 Mettre à jour la documentation
- [ ] Ajouter employee-service (port 8083) et department-service (port 8084) dans le README.md
- [ ] Ajouter les 2 nouveaux services dans le tableau d'architecture

### 5. 🚀 Lancer les services en local (optionnel)
- [ ] Démarrer discovery-service → gateway-service → identity-service → employee-service → department-service

---

## 📊 Architecture actuelle

```
Gateway (8080)
  ├── /identity/** → Identity Service (8082)
  ├── /employee/** → Employee Service (8083)
  └── /department/** → Department Service (8084)
          ↓
    Eureka Discovery (8761)
