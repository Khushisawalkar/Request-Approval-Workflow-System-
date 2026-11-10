# Multi-stage build for optimal image size and security
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compile and package application
COPY src ./src
RUN mvn clean package -DskipTests

# Production runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root system group and user
RUN addgroup -S workflow && adduser -S workflow -G workflow
USER workflow:workflow

# Copy built artifact from builder
COPY --from=builder /workspace/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

