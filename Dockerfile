# syntax=docker/dockerfile:1
# ---------- build stage ----------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY config config
COPY build.gradle settings.gradle gradle.properties ./
COPY src ./src
RUN ./gradlew bootJar --no-daemon

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl && addgroup -S app && adduser -S -G app app
COPY --from=build /app/build/libs/*.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
