FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY target/customer-service-0.0.1-SNAPSHOT.jar /app/customer-service-0.0.1-SNAPSHOT.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/customer-service-0.0.1-SNAPSHOT.jar"]

