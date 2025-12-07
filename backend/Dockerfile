# Multi-stage build for production Spring Boot backend
FROM amazoncorretto:17-alpine AS builder
WORKDIR /app

RUN apk add --no-cache bash unzip

COPY gradlew gradlew.bat settings.gradle build.gradle /app/
COPY gradle /app/gradle
COPY src /app/src

RUN chmod +x gradlew
RUN ./gradlew bootJar -x test --no-daemon

FROM amazoncorretto:17-alpine
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod

RUN addgroup -S app && adduser -S app -G app

COPY --from=builder /app/build/libs/*.jar /app/backend.jar

EXPOSE 8080
USER app

ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-Dspring.profiles.active=prod", "-jar", "/app/backend.jar"]

