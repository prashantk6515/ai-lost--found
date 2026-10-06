# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Lightweight runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create directories for persistent DB & uploads
RUN mkdir -p /app/data /app/uploads

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8085

# Environment variables for dynamic port (useful for Render/Railway)
ENV PORT=8085
ENTRYPOINT ["sh", "-c", "java -jar -Dserver.port=${PORT:-8085} app.jar"]
