# ---- Build stage ----
FROM maven:3.9.0-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# ---- Run stage ----
FROM openjdk:17.0.1-jdk-slim
COPY --from=build /app/target/*.jar finance-tracker.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "finance-tracker.jar"]
