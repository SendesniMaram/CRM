FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY target/employee-service-0.0.1-SNAPSHOT.jar /app/employee-service-0.0.1-SNAPSHOT.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/employee-service-0.0.1-SNAPSHOT.jar"]

