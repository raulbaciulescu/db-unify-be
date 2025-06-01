# ===== Build stage =====
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /app

# Copiem fișierele necesare
COPY pom.xml .
COPY src ./src

# Compilăm proiectul (fără teste)
RUN mvn clean package -DskipTests

# ===== Runtime stage =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiem JAR-ul construit
COPY --from=build /app/target/*.jar app.jar

# Copiem fișierul de configurare dedicat pentru container
COPY src/main/resources/application-docker.yaml /app/config/application-docker.yaml

# Expunem portul aplicației
EXPOSE 8080

# Pornim aplicația cu profilul `docker` și configurare suplimentară
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-Dspring.config.additional-location=classpath:/,file:/app/config/", "-jar", "app.jar"]
