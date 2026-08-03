FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY target/gateway-service-0.0.1-SNAPSHOT.jar /app/gateway-service-0.0.1-SNAPSHOT.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/gateway-service-0.0.1-SNAPSHOT.jar"]

