FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY target/fees-service-0.0.1-SNAPSHOT.jar /app/fees-service-0.0.1-SNAPSHOT.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "/app/fees-service-0.0.1-SNAPSHOT.jar"]

