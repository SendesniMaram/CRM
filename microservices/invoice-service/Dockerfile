FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY target/invoice-service-0.0.1-SNAPSHOT.jar /app/invoice-service-0.0.1-SNAPSHOT.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "/app/invoice-service-0.0.1-SNAPSHOT.jar"]

