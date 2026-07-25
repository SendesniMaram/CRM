# 🏗️ CRM - Gestion de la Relation Client

## 📋 Table des matières

1. [Présentation du projet](#-présentation-du-projet)
2. [Architecture](#-architecture)
3. [Technologies](#-technologies)
4. [Structure du projet](#-structure-du-projet)
5. [Modules détaillés](#-modules-détaillés)
6. [Configuration](#-configuration)
7. [Installation et exécution](#-installation-et-exécution)
8. [Tests](#-tests)
9. [CI/CD - GitHub Actions](#-cicd---github-actions)
10. [État du projet](#-état-du-projet)
11. [Travaux réalisés](#-travaux-réalisés)
12. [Bonnes pratiques Git](#-bonnes-pratiques-git)
13. [Dépannage](#-dépannage)

---

## 🎯 Présentation du projet

Application **CRM (Customer Relationship Management)** développée avec **Spring Boot** en architecture **hybride** : un backend monolithique et une infrastructure microservices pour l'authentification et la découverte de services.

Le projet couvre :
- La gestion des employés, départements et utilisateurs
- L'authentification JWT avec refresh tokens
- Le routage via une API Gateway Spring Cloud
- La découverte de services avec Eureka
- L'analyse de code statique avec **SonarQube Cloud**
- L'intégration continue via **GitHub Actions**

---

## 🏛 Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client / Navigateur                      │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Gateway Service (port 8080)                     │
│                    Spring Cloud Gateway                          │
└──────┬────────────────────────────────────┬──────────────────────┘
       │                                    │
       ▼                                    ▼
┌──────────────────┐           ┌──────────────────────────┐
│  Identity Service│           │    Backend Monolith      │
│   (port 8082)    │           │      (port 8081)         │
│  Auth + JWT      │           │  Employés, Départements, │
│  Refresh Tokens  │           │  Utilisateurs            │
│  H2 (test) / MySQL│           │  MySQL                   │
└──────────────────┘           └──────────────────────────┘
       │                                    │
       └──────────┬─────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────────────┐
│              Discovery Service (port 8761)                      │
│                   Eureka Server                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Flux d'authentification

```
Register → POST /api/auth/register → 201 Created + LoginResponse
Login    → POST /api/auth/login    → 200 OK + JWT + RefreshToken
Refresh  → POST /api/auth/refresh  → 200 OK + New JWT + New RefreshToken
Users    → GET  /api/users         → 200 OK + Liste utilisateurs
```

---

## 🛠 Technologies

| Technologie | Version |
|-------------|---------|
| Java | **21** (Temurin) |
| Spring Boot (backend) | **3.5.16** |
| Spring Boot (microservices) | **3.3.6** |
| Spring Cloud | **2023.0.2** |
| Spring Cloud Gateway | ✅ |
| Spring Cloud Netflix Eureka | ✅ |
| Spring Security + JWT (jjwt 0.12.5) | ✅ |
| JPA / Hibernate | ✅ |
| MySQL 8 | ✅ |
| H2 (tests) | ✅ |
| Maven | **3.9.16** |
| Lombok | ✅ |
| SonarQube Cloud | ✅ |
| GitHub Actions | ✅ |

---

## 📂 Structure du projet

```
crm/
│
├── backend/                          ← Monolithe Spring Boot
│   ├── src/main/java/com/crm/
│   │   ├── CrmApplication.java
│   │   ├── config/                   # DataInitializer, OpenApiConfig
│   │   ├── controller/               # Auth, Department, Employee, Home, User
│   │   ├── dto/                      # Request/Response DTOs
│   │   ├── entity/                   # Department, Employee, Role, User
│   │   ├── enums/                    # EmployeeStatus, Gender, RoleType
│   │   ├── exception/               # Gestion centralisée des erreurs
│   │   ├── mapper/                   # MapStruct mappers
│   │   ├── repository/               # JPA repositories
│   │   ├── security/                 # JWT + configuration sécurité
│   │   └── service/                  # Interfaces + implémentations
│   └── src/test/
│
├── microservices/
│   ├── discovery-service/            ← Eureka Server (port 8761)
│   ├── gateway-service/              ← API Gateway (port 8080)
│   └── identity-service/             ← Auth microservice (port 8082)
│       ├── src/main/java/com/crm/identity/
│       │   ├── IdentityServiceApplication.java
│       │   ├── config/               # DataInitializer, SecurityConfig
│       │   ├── controller/           # AuthenticationController, UserController
│       │   ├── dto/                  # Login, Register, RefreshToken DTOs
│       │   ├── entity/               # User, Role, RefreshToken
│       │   ├── enums/                # RoleType (SUPER_ADMIN, ADMIN, etc.)
│       │   ├── exception/            # Gestion des erreurs (400, 401, 404, 409, 500)
│       │   ├── repository/           # JPA repositories
│       │   ├── security/            # JWT, CustomUserDetails, Filter
│       │   └── service/             # Authentication + Identity services
│       └── src/test/
│
├── frontend/                         ← Frontend (non modifié)
├── postman/                          ← Collections Postman
│
├── .github/
│   └── workflows/
│       └── build.yml                 ← CI/CD SonarQube Cloud (matrix 3 services)
│
├── .gitignore                        ← Filtrage des artefacts de build
└── README.md                         ← Cette documentation
```

---

## 🔧 Modules détaillés

### 1. Backend (Monolithe) — `backend/`

| Aspect | Détail |
|--------|--------|
| **Port** | 8081 (configurable via `PORT` env) |
| **Base de données** | MySQL `crm_db` |
| **Java** | 21 |
| **Spring Boot** | 3.5.16 |
| **Sécurité** | JWT (jjwt 0.12.5) + Spring Security |
| **Fonctionnalités** | CRUD Employés, Départements, Utilisateurs, Authentification |

**Entity / Controller mapping :**
- `Department` → `DepartmentController` → `/api/departments`
- `Employee` → `EmployeeController` → `/api/employees`
- `User` → `UserController` → `/api/users`
- `Auth` → `AuthController` → `/api/auth/**`

---

### 2. Discovery Service — `microservices/discovery-service/`

| Aspect | Détail |
|--------|--------|
| **Port** | 8761 |
| **Java** | 21 |
| **Spring Boot** | 3.3.6 |
| **Spring Cloud** | 2023.0.2 |
| **Rôle** | Serveur Eureka (enregistrement/découverte des services) |
| **Dashboard** | `http://localhost:8761` |

**Configuration clé :**
```yaml
eureka:
  client:
    register-with-eureka: false    # Le serveur ne s'enregistre pas lui-même
    fetch-registry: false
```

---

### 3. Gateway Service — `microservices/gateway-service/`

| Aspect | Détail |
|--------|--------|
| **Port** | 8080 |
| **Java** | 21 |
| **Spring Boot** | 3.3.6 |
| **Spring Cloud** | 2023.0.2 |
| **Rôle** | Spring Cloud Gateway avec routage dynamique Eureka |
| **Routes** | Découverte automatique via `discovery.locator.enabled=true` |

**Route configurée :**
```yaml
- id: identity-test
  uri: lb://IDENTITY-SERVICE
  predicates:
    - Path=/identity/api/test
```

---

### 4. Identity Service — `microservices/identity-service/`

| Aspect | Détail |
|--------|--------|
| **Port** | 8082 |
| **Java** | 21 |
| **Spring Boot** | 3.3.6 |
| **Spring Cloud** | 2023.0.2 |
| **Base de données** | MySQL `identity_db` (prod) / H2 (tests) |

**API exposée :**

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/auth/register` | Inscription (retourne JWT + refresh token) |
| POST | `/api/auth/login` | Connexion (retourne JWT + refresh token) |
| POST | `/api/auth/refresh` | Rafraîchir le JWT |
| GET | `/api/users` | Liste des utilisateurs (authentifié) |
| GET | `/api/users/{id}` | Détail d'un utilisateur (authentifié) |

**Entités JPA :**
- `User` : id, firstName, lastName, email, password, phone, username, enabled, createdAt
- `Role` : id, roleType (SUPER_ADMIN, ADMIN, EMPLOYEE, CLIENT), user (ManyToOne)
- `RefreshToken` : id, token, expiryDate, user (ManyToOne)

**Sécurité :**
- JWT avec jjwt 0.12.5
- Filtre `JwtAuthenticationFilter`
- `SecurityConfig` avec endpoint protégés
- `DataInitializer` : seed des 4 rôles au démarrage

---

## ⚙ Configuration

### Base de données

```properties
# Backend (MySQL)
spring.datasource.url=jdbc:mysql://localhost:3306/crm_db
spring.datasource.username=root
spring.datasource.password=Root123@

# Identity Service (MySQL - prod)
spring.datasource.url=jdbc:mysql://localhost:3306/identity_db
spring.datasource.username=root
spring.datasource.password=Root123@

# Identity Service (H2 - test)
spring.datasource.url=jdbc:h2:mem:identity_db;MODE=MySQL
```

### Ports

| Service | Port |
|---------|------|
| Gateway | 8080 |
| Backend | 8081 |
| Identity | 8082 |
| Eureka | 8761 |

---

## 🚀 Installation et exécution

### Prérequis

- Java **21** (Temurin ou Zulu)
- Maven **3.9+**
- MySQL **8.0+**
- Git

### Étapes

```bash
# 1. Cloner le dépôt
git clone <url-du-repo>
cd crm

# 2. Créer les bases de données MySQL
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS crm_db;"
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS identity_db;"

# 3. Démarrer les services (dans l'ordre)

# 3a. Discovery Service (Eureka)
cd microservices/discovery-service
mvn clean spring-boot:run

# 3b. Gateway Service
cd ../gateway-service
mvn clean spring-boot:run

# 3c. Identity Service
cd ../identity-service
mvn clean spring-boot:run

# 3d. Backend
cd ../../backend
mvn clean spring-boot:run
```

### Compilation et tests

```bash
# Tous les services
cd microservices/discovery-service && mvn clean verify
cd ../gateway-service && mvn clean verify
cd ../identity-service && mvn clean verify
cd ../../backend && mvn clean verify
```

---

## 🧪 Tests

| Service | Tests | Statut |
|---------|-------|--------|
| `discovery-service` | 1 test (smoke test context) | ✅ Passes |
| `gateway-service` | Test commenté | ⚠️ À réactiver |
| `identity-service` | 1 test (smoke test context H2) | ✅ Passes |
| `backend` | 4 tests (Swagger, Context, Mapper, JWT) | ✅ Passes |

### Exécuter tous les tests

```bash
# Depuis la racine
cd microservices/discovery-service; mvn clean verify
cd ../identity-service; mvn clean verify
cd ../../backend; mvn clean test
```

---

## 🤖 CI/CD - GitHub Actions

### Workflow : `SonarQube Microservices`

```yaml
# .github/workflows/build.yml
```

**Déclencheurs :**
- `push` sur `main` et `microservices`
- `pull_request` (opened, synchronize, reopened)

**Matrix strategy** — 3 jobs parallèles :

| Job | Service | Port | Project Key SonarQube |
|-----|---------|------|----------------------|
| 1 | `discovery-service` | 8761 | `SendesniMaram_CRM_discovery-service` |
| 2 | `gateway-service` | 8080 | `SendesniMaram_CRM_gateway-service` |
| 3 | `identity-service` | 8082 | `SendesniMaram_CRM_identity-service` |

**Étapes :**
1. `actions/checkout@v4` avec `fetch-depth: 0`
2. `actions/setup-java@v4` — **JDK 21** (Zulu) ← Corrigé !
3. `actions/cache` — Cache SonarQube + Cache Maven
4. `mvn -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar`

**Configuration SonarQube :**
```bash
-Dsonar.organization=sendesnimaram
-Dsonar.projectKey=SendesniMaram_CRM_${service}
```

### Correctif appliqué

**Problème :** Le workflow utilisait `java-version: 17` alors que tous les POM exigent `<java.version>21</java.version>`.
**Erreur :** `release version 21 not supported`
**Correctif :** Changement de `java-version: 17` → `java-version: 21` (et `Set up JDK 17` → `Set up JDK 21`).

---

## 📊 État du projet

### Fichier .gitignore

Le `.gitignore` filtre désormais :

| Catégorie | Règles |
|-----------|--------|
| Build Maven | `target/`, `**/target/`, `*.class`, `*.jar`, `*.war`, `*.ear` |
| Rapports Maven | `**/surefire-reports/`, `**/maven-status/`, `**/maven-archiver/`, `**/generated-sources/` |
| Logs | `*.log`, `**/*.log` |
| SonarQube | `.scannerwork/`, `.sonarqube/`, `**/.scannerwork/` |
| IDE | `.vscode/`, `.idea/`, `*.iml`, `.settings/`, `.project`, `.classpath` |
| OS | `Thumbs.db`, `.DS_Store`, `Desktop.ini` |
| Environnement | `.env`, `application-local.*` |

### Suivi Git — État propre

- **58 fichiers générés retirés de l'index** (target/, .class, .log, surefire-reports, maven-status)
- `git ls-files -- '*/target/*' '*.class' '*.jar' '*.log'` → **vide** ✅
- Après `mvn clean verify`, aucun artefact n'apparaît dans `git status` ✅

---

## 📝 Travaux réalisés

### Phase 1 : Initialisation du projet
- Structure Spring Boot monolithique (backend)
- CRUD complet : Employés, Départements, Utilisateurs
- Authentification JWT + Spring Security
- Base de données MySQL avec JPA/Hibernate

### Phase 2 : Migration microservices
- Création de `discovery-service` (Eureka Server, port 8761)
- Création de `gateway-service` (Spring Cloud Gateway, port 8080)
- Création de `identity-service` (Auth microservice, port 8082)
- Configuration Spring Cloud 2023.0.2 sur Java 21

### Phase 3 : Finalisation identity-service
- **Refonte complète de l'authentification :**
  - Inscription avec validation (`@Email`, `@Size`, confirmation password)
  - Connexion avec retour JWT + refresh token
  - Rafraîchissement de token
- **Ajout de 12 fichiers Java :**
  - `config/DataInitializer.java` — seed des rôles
  - `controller/UserController.java` — consultation utilisateurs
  - `dto/ApiError.java`, `RefreshTokenRequest.java`, `RefreshTokenResponse.java`, `UserResponse.java`
  - `entity/RefreshToken.java` — stockage des refresh tokens
  - `enums/RoleType.java` — SUPER_ADMIN, ADMIN, EMPLOYEE, CLIENT
  - `exception/GlobalExceptionHandler.java`, `ResourceNotFoundException.java`
  - `repository/RefreshTokenRepository.java`
- **14 fichiers modifiés :** +488 lignes / -54 lignes dans identity-service
- **Dépendance ajoutée :** `spring-boot-starter-validation`

### Phase 4 : CI/CD — GitHub Actions + SonarQube Cloud
- Création de `.github/workflows/build.yml`
- Matrix strategy pour les 3 microservices
- Caches Maven et SonarQube
- Analyse de code statique avec SonarQube Cloud

### Phase 5 : Correction pipeline
- **Problème :** `release version 21 not supported` (workflow utilisait JDK 17)
- **Correctif :** Changement `java-version: 17` → `java-version: 21` dans `build.yml`

### Phase 6 : Nettoyage Git
- Création du `.gitignore` professionnel (Maven, IDE, OS, logs, SonarQube)
- `git rm --cached` de **58 fichiers générés** (target/, .class, .log, rapports)
- Vérification : aucun artefact build suivi par Git
- `mvn clean verify` ne pollue plus `git status`

---

## ✅ Bonnes pratiques Git

### .gitignore
```gitignore
# Maven
target/
**/target/
*.class
*.jar
*.war

# Logs
*.log
**/*.log

# IDE
.vscode/
.idea/
*.iml

# OS
.DS_Store
Thumbs.db
```

### Workflow Git recommandé

```bash
# Avant chaque commit
git status                              # Vérifier l'état
git add -p                              # Ajouter sélectivement
git commit -m "type: message explicite" # Convention de message

# Avant chaque push
mvn clean verify                        # Tests + compilation
```

---

## 🔍 Dépannage

### Erreur : `release version 21 not supported`

**Cause :** Le JDK configuré est inférieur à 21.
**Solution :**
```yaml
# .github/workflows/build.yml
- name: Set up JDK 21
  with:
    java-version: 21   # ← et non 17
```

### Erreur : `java.lang.NoClassDefFoundError`

**Cause :** Fichier `.class` manquant ou JAR corrompu.
**Solution :**
```bash
mvn clean install   # Recompiler tout le projet
```

### Test qui échoue : `contextLoads`

**Cause :** Connexion MySQL impossible pendant les tests.
**Solution :** Vérifier que les profils de test utilisent H2 :
```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
```

### Port déjà utilisé

```bash
netstat -ano | findstr :8080    # Vérifier le port
taskkill /PID <PID> /F          # Libérer le port
```

---

## 📜 Licence

Projet académique dans le cadre du développement d'une application CRM avec architecture microservices Spring Cloud.

---

*Documentation générée le $(date +%Y-%m-%d) — Dernière mise à jour : juillet 2026*
