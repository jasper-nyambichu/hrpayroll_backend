# Stage 1: Build the JAR file
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the Application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Runs as an ordinary user, not root. /app/uploads (uploaded documents) belongs to it; docker-compose.prod.yml
# keeps that folder in a volume so uploads survive redeploys.
RUN addgroup -S app && adduser -S -G app app && mkdir -p /app/uploads && chown -R app:app /app
COPY --from=build --chown=app:app /app/target/*.jar app.jar
USER app
# docker-compose.prod.yml forwards to 8080. application.yaml says 8081 for local runs, so the image sets its own port.
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
