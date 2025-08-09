#FROM openjdk:17-jdk-slim
#WORKDIR /app
#COPY target/* app.jar
#EXPOSE 8080
#ENTRYPOINT ["java", "-jar", "app.jar"]
# Stage 1: Build
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]