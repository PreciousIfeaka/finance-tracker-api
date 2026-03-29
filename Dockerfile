# ---- Dependencies stage ----
FROM maven:3.9.0-eclipse-temurin-17 AS dependencies
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve dependency:resolve-plugins -B

# ---- Build stage ----
FROM dependencies AS build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Run stage ----
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 6002
EXPOSE 6003

ENTRYPOINT ["java", "-jar", "app.jar"]