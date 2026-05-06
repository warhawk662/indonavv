# Step 1: Build the application
FROM gradle:8.3-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle :backend:buildFatJar --no-daemon

# Step 2: Create the runtime image
FROM openjdk:17-slim
EXPOSE 8080
RUN mkdir /app
# Copy the Fat JAR from the build stage
COPY --from=build /home/gradle/src/backend/build/libs/*-all.jar /app/ktor-backend.jar
# Ensure map_data.json exists or is copied if you want to include default data
COPY --from=build /home/gradle/src/backend/map_data.json /app/map_data.json

WORKDIR /app
CMD ["java", "-jar", "ktor-backend.jar"]
