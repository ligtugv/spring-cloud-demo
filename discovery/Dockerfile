FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY target/*.jar app.jar
HEALTHCHECK --interval=30s --timeout=10s --retries=5 CMD curl -f http://localhost:8761/actuator/health || exit 1
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
