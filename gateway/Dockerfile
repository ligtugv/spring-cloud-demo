FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 7573
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
