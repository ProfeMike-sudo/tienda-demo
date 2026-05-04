# ---- Etapa 1: Compilación ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# ---- Etapa 2: Ejecución (Runtime) ----
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Exponer el puerto del consumidor
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]