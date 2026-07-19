# CRM (Spring Boot + Angular) — Stage

## Présentation

Ce projet est développé dans le cadre d'un stage.

Il s'agit d'une application CRM dont le backend monolithique est progressivement migré vers une architecture microservices basée sur Spring Cloud.

L'infrastructure microservices est désormais opérationnelle et le premier microservice métier (`identity-service`) implémente le module d'authentification.

---

## Architecture actuelle

```
crm/
│
├── backend/                     # Monolithe Spring Boot existant
│
└── microservices/
    ├── discovery-service/       # Eureka Server
    ├── gateway-service/         # API Gateway
    └── identity-service/        # Service d'authentification
```

---

## Technologies

- Java 21
- Spring Boot 3.3.6
- Spring Cloud 2023.0.2
- Spring Security 6
- JWT (JSON Web Token)
- Spring Data JPA
- Hibernate
- BCrypt
- Maven
- MySQL
- Angular
- Spring Cloud Netflix Eureka
- Spring Cloud Gateway

---

## Description des services

### backend

Application monolithique existante.

Les autres modules métier restent actuellement hébergés dans ce backend.

---

### discovery-service

Serveur Eureka utilisé pour la découverte des services.

Port :

```
8761
```

---

### gateway-service

Point d'entrée unique des microservices.

Port :

```
8080
```

---

### identity-service

Premier microservice métier.

Fonctionnalités actuellement implémentées :

- Authentification (Register)
- Authentification (Login)
- Gestion des utilisateurs
- Persistance MySQL dédiée (`identity_db`)
- Spring Data JPA
- BCrypt pour le chiffrement des mots de passe
- Génération de JWT
- Validation des JWT
- Spring Security
- JwtAuthenticationFilter
- CustomUserDetailsService
- Architecture REST

Port :

```
8082
```

---

## Ordre de démarrage

Démarrer les services dans l'ordre suivant :

1. discovery-service
2. gateway-service
3. identity-service

---

## URLs

| Service | URL |
|----------|-----|
| Eureka Dashboard | http://localhost:8761 |
| API Gateway | http://localhost:8080 |
| Identity Service | http://localhost:8082 |

---

## État actuel

✔ Backend monolithique fonctionnel

✔ Infrastructure microservices créée

✔ Eureka Server opérationnel

✔ API Gateway opérationnelle

✔ Identity Service opérationnel

✔ Base de données MySQL dédiée (`identity_db`)

✔ Authentification Register

✔ Authentification Login

✔ BCrypt

✔ JWT

✔ Spring Security

✔ JwtAuthenticationFilter

✔ CustomUserDetailsService

✔ Tests Maven validés (`BUILD SUCCESS`)

---

## Prochaines étapes

- Tester le flux complet d'authentification
- Intégrer complètement le Gateway avec Identity Service
- Migrer progressivement les autres modules métier
- Créer les futurs microservices (Employee, Department, Payroll, ...)

---

## Architecture cible

```
                     Angular
                        │
                        ▼
                API Gateway (8080)
                        │
                        ▼
              Eureka Discovery (8761)
                        │
 ┌────────────┬────────────┬────────────┬────────────┐
 ▼            ▼            ▼            ▼
Identity   Employee   Department    Payroll      ...
 Service    Service      Service     Service
    │           │            │            │
    ▼           ▼            ▼            ▼
 MySQL DB   MySQL DB    MySQL DB    MySQL DB
```
