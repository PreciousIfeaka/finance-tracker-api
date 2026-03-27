# ---- Build stage ----
FROM maven:3.9.0-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests

# ---- Run stage ----
FROM openjdk:17.0.1-jdk-slim
WORKDIR /app

COPY --from=build ./app/target/finance-tracker-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 6005

ENTRYPOINT ["java", "-jar", "app.jar"]
