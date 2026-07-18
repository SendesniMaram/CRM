# CRM (Spring Boot + Angular) — Stage

## Présentation
Projet CRM réalisé durant un stage. Le backend monolithique existe déjà, et une infrastructure microservices (Eureka + API Gateway + services métiers) a été mise en place afin de préparer la migration progressive.

## Architecture actuelle
- `backend/` : monolithe existant (non migré pour le moment)
- `microservices/` : infrastructure microservices mise en place
  - `discovery-service` (Eureka)
  - `gateway-service` (Spring Cloud Gateway)
  - `identity-service` (premier service métier, squelette)

## Structure
- `backend/`
- `microservices/`
  - `discovery-service/`
  - `gateway-service/`
  - `identity-service/`

## Technologies
- Java 21
- Spring Boot 3.3.6
- Spring Cloud 2023.0.2
- Maven
- Angular
- MySQL
- Eureka
- Spring Cloud Gateway

## Description des services
### backend/
Monolithe existant (non modifié pendant cette finalisation).

### discovery-service
Serveur Eureka.
- Port : `8761`

### gateway-service
API Gateway basé sur Spring Cloud Gateway.
- Port : `8080`

### identity-service
Premier microservice métier (squelette).
- Port : `8082`

## Configuration & URLs
- Eureka : http://localhost:8761
- Gateway : http://localhost:8080
- Identity : http://localhost:8082

## Ordre de démarrage
1. `discovery-service`
2. `gateway-service`
3. `identity-service`

## État actuel
- L’infrastructure microservices est mise en place.
- Les microservices métiers seront développés progressivement.
- Le backend monolithique n’est pas encore migré.

