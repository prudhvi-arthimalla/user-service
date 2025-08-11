# Stage 1: Build the application
FROM gradle:8.7.0-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# Stage 2: Run the application
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the jar from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Env vars (can also be overridden by docker-compose or k8s)
ENV SPRING_PROFILES_ACTIVE=local \
    SPRING_DATA_MONGODB_URI=mongodb://mongo:27017/user-service \
    JWT_SECRET=change-me-in-prod

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]