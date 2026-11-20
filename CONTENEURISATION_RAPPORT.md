# Rapport de vérification — Conteneurisation des microservices CRM

**Date :** 2026-08-02
**Périmètre :** Étape préparatoire à la conteneurisation (vérification uniquement).

---

## 1. Résumé

| Critère | Résultat |
|---|---|
| Dockerfiles vérifiés | 10 / 10 |
| Référence JAR correcte | ✅ 10 / 10 |
| Port Docker = port Spring Boot | ✅ 10 / 10 |
| Image Java 21 (`eclipse-temurin:21-jre-jammy`) | ✅ 10 / 10 |
| Build Maven (`mvn package`) | ✅ 10 / 10 — BUILD SUCCESS |
| Anomalies détectées | Aucune |
| Corrections réalisées | Aucune (aucune modification nécessaire) |

---

## 2. Dockerfiles vérifiés

| Service | Fichier Dockerfile | Image de base | JAR copié / exécuté | EXPOSE |
|---|---|---|---|---|
| Discovery Service | `microservices/discovery-service/Dockerfile` | `eclipse-temurin:21-jre-jammy` | `discovery-service-0.0.1-SNAPSHOT.jar` | 8761 |
| Gateway Service | `microservices/gateway-service/Dockerfile` | `eclipse-temurin:21-jre-jammy` | `gateway-service-0.0.1-SNAPSHOT.jar` | 8080 |
| Identity Service | `microservices/identity-service/Dockerfile` | `eclipse-temurin:21-jre-jammy` | `identity-service-0.0.1-SNAPSHOT.jar` | 8082 |
| Employee Service | `microservices/employee-service/Dockerfile` | `eclipse-temurin:21-jre-jammy` | `employee-service-0.0.1-SNAPSHOT.jar` | 8083 |
| Department Service | `microservices/department-service/Dockerfile` | `eclipse-temurin:21-jre-jammy` | `department-service-0.0.1-SNAPSHOT.jar` | 8084 |
| Customer Service | `microservices/customer-service/Dockerfile` | `eclipse-temurin:21-jre-jammy` | `customer-service-0.0.1-SNAPSHOT.jar` | 8085 |
| HR Service | `microservices/hr-service/Dockerfile` | `eclipse-temurin:21-jre-jammy` | `hr-service-0.0.1-SNAPSHOT.jar` | 8086 |
| Payroll Service | `microservices/payroll-service/Dockerfile` | `eclipse-temurin:21-jre-jammy` | `payroll-service-0.0.1-SNAPSHOT.jar` | 8087 |
| Fees Service | `microservices/fees-service/Dockerfile` | `eclipse-temurin:21-jre-jammy` | `fees-service-0.0.1-SNAPSHOT.jar` | 8088 |
| Invoice Service | `microservices/invoice-service/Dockerfile` | `eclipse-temurin:21-jre-jammy` | `invoice-service-0.0.1-SNAPSHOT.jar` | 8089 |

Tous les Dockerfiles utilisent la même structure mono-stage cohérente :
`FROM eclipse-temurin:21-jre-jammy` → `WORKDIR /app` → `COPY target/<service>-0.0.1-SNAPSHOT.jar /app/` → `EXPOSE <port>` → `ENTRYPOINT ["java", "-jar", "/app/<service>-0.0.1-SNAPSHOT.jar"]`.

---

## 3. Vérification des ports Docker vs Spring Boot

Le port exposé dans chaque Dockerfile (`EXPOSE`) correspond exactement au port configuré dans `server.port` de chaque `application.yml` :

| Service | Port Spring Boot (`application.yml`) | Port `EXPOSE` (Dockerfile) | Conformité |
|---|---|---|---|
| discovery-service | 8761 | 8761 | ✅ |
| gateway-service | 8080 | 8080 | ✅ |
| identity-service | 8082 | 8082 | ✅ |
| employee-service | 8083 | 8083 | ✅ |
| department-service | 8084 | 8084 | ✅ |
| customer-service | 8085 | 8085 | ✅ |
| hr-service | 8086 | 8086 | ✅ |
| payroll-service | 8087 | 8087 | ✅ |
| fees-service | 8088 | 8088 | ✅ |
| invoice-service | 8089 | 8089 | ✅ |

Aucun port ne correspond à `8081` (prévu à l'origine pour identity-service) : il s'agit d'un comportement normal, identity-service démarrant sur `8082` dans la configuration actuelle.

---

## 4. Vérification Java 21

- **Dockerfiles :** les 10 utilisent `eclipse-temurin:21-jre-jammy` (JRE 21). ✅
- **POM :** les 11 `pom.xml` (10 microservices + `common-security`) déclarent `<java.version>21</java.version>`. ✅
- **Environnement local de build :** Maven 3.9.16 + JDK 21.0.11 (Eclipse Adoptium) — cohérent. ✅

Java 21 est donc cohérent entre les Dockerfiles, les POM et l'environnement de build.

---

## 5. Vérification des builds Maven

Tous les microservices ont été construits avec `mvn -f <service>/pom.xml package` (tests inclus). Résultats :

| Service | Résultat build | Tests (run / échecs / erreurs) |
|---|---|---|
| common-security (dépendance partagée) | ✅ BUILD SUCCESS | 5 / 0 / 0 |
| discovery-service | ✅ BUILD SUCCESS | 1 / 0 / 0 |
| gateway-service | ✅ BUILD SUCCESS | 1 / 0 / 0 |
| identity-service | ✅ BUILD SUCCESS | 1 / 0 / 0 |
| employee-service | ✅ BUILD SUCCESS | 14 / 0 / 0 |
| department-service | ✅ BUILD SUCCESS | 14 / 0 / 0 |
| customer-service | ✅ BUILD SUCCESS | 14 / 0 / 0 |
| hr-service | ✅ BUILD SUCCESS | 55 / 0 / 0 |
| payroll-service | ✅ BUILD SUCCESS | 12 / 0 / 0 |
| fees-service | ✅ BUILD SUCCESS | 13 / 0 / 0 |
| invoice-service | ✅ BUILD SUCCESS | 13 / 0 / 0 |

**Total tests exécutés : 143 — 0 échec, 0 erreur.**

Chaque build a produit le JAR attendu `target/<service>-0.0.1-SNAPSHOT.jar` (JAR Spring Boot repackagé, de 50 à 86 Mo), correspondant exactement au nom référencé dans le Dockerfile.

> Note : `common-security` (module partagé sans Dockerfile) doit être installé dans le dépôt Maven local (`mvn install`) avant de construire les services qui en dépendent. Il a été installé en `com.crm:common-security:0.0.1-SNAPSHOT`.

---

## 6. Anomalies détectées

**Aucune anomalie.**

- Toutes les références de JAR dans les Dockerfiles correspondent aux artefacts générés.
- Tous les ports Docker correspondent aux ports Spring Boot.
- Toutes les images de base utilisent Java 21 (`eclipse-temurin:21-jre-jammy`).
- Tous les microservices se construisent correctement avec Maven.

*(Warnings non bloquants observés pendant les builds — LoadBalancer cache par défaut, open-in-view, PageImpl serialization, mot de passe de sécurité généré en développement — sans impact sur la conteneurisation et hors périmètre.)*

---

## 7. Corrections réalisées

**Aucune correction nécessaire.** Aucun fichier (Dockerfile, POM, configuration, code) n'a été modifié au cours de cette étape de vérification.

---

## 8. Conclusion

Le projet CRM est **prêt pour l'étape suivante de la conteneurisation** : les 10 Dockerfiles sont corrects, les ports sont cohérents, Java 21 est uniformisé et les 10 microservices produisent un JAR exécutable via Maven.

Conformément au périmètre défini, aucun `docker-compose.yml`, `Jenkinsfile` ou modification de GitHub Actions n'a été créé ou modifié.

