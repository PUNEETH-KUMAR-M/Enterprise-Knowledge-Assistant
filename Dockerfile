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

# Default runtime configuration (can be overridden at deploy time)
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080

EXPOSE 8080

# Use shell form to allow env variable expansion for port and JVM opts
ENTRYPOINT ["/bin/sh","-c","java ${JAVA_OPTS} -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar --server.port=${PORT:-8080}"]
