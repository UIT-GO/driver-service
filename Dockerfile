# syntax=docker/dockerfile:1

# Build stage
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw && ./mvnw dependency:go-offline

COPY src/ src/

RUN ./mvnw clean package

# Package stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/target/driver-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 3031

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:3031/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
