# Multi-stage build for gateway-service
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copy parent pom and common-lib
COPY pom.xml .
COPY common-lib ./common-lib
COPY gateway-service ./gateway-service

# Build the project
RUN mvn clean package -DskipTests -am -pl gateway-service

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /workspace/gateway-service/target/gateway-service-*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
