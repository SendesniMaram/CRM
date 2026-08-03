FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY target/hr-service-0.0.1-SNAPSHOT.jar /app/hr-service-0.0.1-SNAPSHOT.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/hr-service-0.0.1-SNAPSHOT.jar"]

