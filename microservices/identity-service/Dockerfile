FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY target/identity-service-0.0.1-SNAPSHOT.jar /app/identity-service-0.0.1-SNAPSHOT.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/identity-service-0.0.1-SNAPSHOT.jar"]

