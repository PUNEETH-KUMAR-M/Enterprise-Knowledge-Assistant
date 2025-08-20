# Multi-stage Docker build for Spring Boot (Java 21)

# ===== Build stage =====
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# Leverage Docker layer caching
COPY pom.xml ./
RUN mvn -q -e -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

# ===== Runtime stage =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy fat jar
COPY --from=build /app/target/*.jar /app/app.jar

# Copy startup script for Render and general usage
COPY scripts/render-start.sh /usr/local/bin/render-start.sh
RUN chmod +x /usr/local/bin/render-start.sh

# Default runtime configuration (can be overridden at deploy time)
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080

EXPOSE 8080

# Start via script that adapts DATABASE_URL if present (Render)
ENTRYPOINT ["/usr/local/bin/render-start.sh"]
