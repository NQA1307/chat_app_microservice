FROM gradle:8.12-jdk21 AS build
WORKDIR /workspace
COPY . .
RUN gradle :api_gateway:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/api_gateway/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
