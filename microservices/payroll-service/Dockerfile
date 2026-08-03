FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY target/payroll-service-0.0.1-SNAPSHOT.jar /app/payroll-service-0.0.1-SNAPSHOT.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "/app/payroll-service-0.0.1-SNAPSHOT.jar"]

