# TODO — Conteneurisation Docker des microservices CRM

## Objectif
Créer UNIQUEMENT les 10 Dockerfiles (un par microservice). Aucune autre modification du projet.

## Contexte technique
- Image officielle : `eclipse-temurin:21-jre-jammy`
- WORKDIR : `/app`
- Copie : uniquement le JAR généré (`target/<service>-0.0.1-SNAPSHOT.jar`)
- EXPOSE : port du service
- ENTRYPOINT : `java -jar`
- Pas de Maven dans le conteneur (JAR pré-généré)
- Pas de docker-compose.yml, pas de modification POM / application.yml / README / GitHub Actions / tests

## Étapes

- [x] 1. Créer `microservices/discovery-service/Dockerfile` (EXPOSE 8761)
- [x] 2. Créer `microservices/gateway-service/Dockerfile` (EXPOSE 8080)
- [x] 3. Créer `microservices/identity-service/Dockerfile` (EXPOSE 8082)
- [x] 4. Créer `microservices/employee-service/Dockerfile` (EXPOSE 8083)
- [x] 5. Créer `microservices/department-service/Dockerfile` (EXPOSE 8084)
- [x] 6. Créer `microservices/customer-service/Dockerfile` (EXPOSE 8085)
- [x] 7. Créer `microservices/hr-service/Dockerfile` (EXPOSE 8086)
- [x] 8. Créer `microservices/payroll-service/Dockerfile` (EXPOSE 8087)
- [x] 9. Créer `microservices/fees-service/Dockerfile` (EXPOSE 8088)
- [x] 10. Créer `microservices/invoice-service/Dockerfile` (EXPOSE 8089)

## Suivi final
- [x] 11. Fournir la liste des Dockerfiles créés et leur emplacement
- [x] 12. Fournir les commandes Maven de génération des JAR (séparées par `;`)
- [x] 13. Fournir les commandes `docker build` (une par service, séparées par `;`)

