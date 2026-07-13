# Stage 1: Build (Use official Maven + JDK image)
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /usr/src/app

# Copy project files
COPY pom.xml .
COPY src ./src

# Build the project
RUN mvn clean package -DskipTests

# Runtime image (Using jre as we are already using jdk in build stage)
FROM eclipse-temurin:17-jre

WORKDIR /usr/src/app

# Copy built jar from build stage
COPY --from=build /usr/src/app/target/*.jar app.jar

# Expose Spring Boot default port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
