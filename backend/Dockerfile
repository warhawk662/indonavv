# Step 1: Build the application
FROM gradle:8.3-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

# Set environment variable to trigger minimal settings.gradle.kts
ENV DOCKER_BUILD=true

RUN gradle :backend:buildFatJar --no-daemon

# Step 2: Create the runtime image
FROM eclipse-temurin:17-jre-jammy
EXPOSE 8080

WORKDIR /app

# Copy the Fat JAR from the build stage
COPY --from=build /home/gradle/src/backend/build/libs/*-all.jar /app/ktor-backend.jar
# Copy the initial map data if it exists
COPY --from=build /home/gradle/src/backend/map_data.json /app/map_data.json

# Create uploads directory
RUN mkdir -p /app/uploads

CMD ["java", "-Xmx512m", "-jar", "/app/ktor-backend.jar"]
