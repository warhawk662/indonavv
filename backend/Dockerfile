# Step 1: Build the application
FROM eclipse-temurin:17-jdk-jammy AS build

# Install findutils (needed for gradlew)
RUN apt-get update && apt-get install -y findutils && rm -rf /var/lib/apt/lists/*

COPY . /home/gradle/src
WORKDIR /home/gradle/src

# Set environment variable to trigger minimal settings.gradle.kts
ENV DOCKER_BUILD=true

# Ensure gradlew has execution permissions
RUN chmod +x gradlew

# Build the Fat JAR using the wrapper to ensure correct Gradle version
RUN ./gradlew :backend:buildFatJar --no-daemon --stacktrace

# Step 2: Create the runtime image
FROM eclipse-temurin:17-jre-jammy
EXPOSE 8080

WORKDIR /app

# Copy the Fat JAR from the build stage
# Note: The path might vary if the project name is different, but backend-all.jar is standard for Ktor plugin
COPY --from=build /home/gradle/src/backend/build/libs/*-all.jar /app/ktor-backend.jar
# Copy the initial map data if it exists
COPY --from=build /home/gradle/src/backend/map_data.json /app/map_data.json

# Create uploads directory
RUN mkdir -p /app/uploads

CMD ["java", "-Xmx512m", "-jar", "/app/ktor-backend.jar"]
