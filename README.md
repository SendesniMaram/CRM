# CRM (Spring Boot + Angular) — Stage

## Présentation

Ce projet est développé dans le cadre d'un stage.

Il s'agit d'une application CRM dont le backend monolithique existant est progressivement préparé à une architecture microservices basée sur Spring Cloud.

À ce stade, seule l'infrastructure microservices est mise en place. La migration des fonctionnalités métier sera réalisée progressivement.

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
    └── identity-service/        # Premier microservice (squelette)
```

---

## Technologies

- Java 21
- Spring Boot 3.3.6
- Spring Cloud 2023.0.2
- Maven
- Angular
- MySQL
- Spring Cloud Netflix Eureka
- Spring Cloud Gateway

---

## Description des services

### backend

Application monolithique existante.

Aucune migration métier n'a encore été réalisée.

---

### discovery-service

Serveur Eureka utilisé pour la découverte des services.

Port :

```
8761
```

---

### gateway-service

Point d'entrée unique des futurs microservices.

Port :

```
8080
```

---

### identity-service

Premier microservice métier.

À ce stade, il s'agit uniquement d'un squelette technique permettant de préparer la future migration du module d'authentification.

Port :

```
8082
```

---

## Ordre de démarrage

Démarrer les applications dans l'ordre suivant :

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

✔ Infrastructure microservices créée

✔ Eureka Server opérationnel

✔ API Gateway opérationnelle

✔ Identity Service créé (squelette)

✔ Backend monolithique conservé sans modification

✔ Architecture prête pour la migration progressive des modules métier

---

## Prochaines étapes

- Migration progressive de l'authentification vers `identity-service`
- Création des futurs microservices métier
- Intégration des bases de données dédiées à chaque service
- Communication entre services via Eureka et Gateway

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
Identity   Employee   Department    Payroll         ...
 Service    Service      Service     Service
    │           │            │            │
    ▼           ▼            ▼            ▼
 MySQL DB   MySQL DB    MySQL DB    MySQL DB
 
```