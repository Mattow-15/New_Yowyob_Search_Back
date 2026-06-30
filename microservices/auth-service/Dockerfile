# Runtime stage (Local build workaround)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy locally built JAR
COPY target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
