# Stage 1: Build the application using Maven
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy the pom.xml file to download dependencies first (caching optimization)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the rest of the source code and build the package
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Expose the default backend port
EXPOSE 8081

# Command to execute the application
ENTRYPOINT ["java", "-jar", "app.jar"]
