# ---- Etapa 1: Compilación ----
# Se utiliza una imagen que incluye Maven y el JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar el archivo pom.xml primero para descargar las dependencias y aprovechar el caché de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar omitiendo las pruebas para agilizar el proceso
COPY src ./src
RUN mvn clean package -DskipTests

# ---- Etapa 2: Ejecución (Runtime) ----
# Se utiliza una imagen más ligera que solo contiene el JRE
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copiar el archivo .jar generado en la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Exponer el puerto del proveedor
EXPOSE 8081

# Comando de inicio del contenedor
ENTRYPOINT ["java", "-jar", "app.jar"]