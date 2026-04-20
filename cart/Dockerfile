FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

COPY target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=docker

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
