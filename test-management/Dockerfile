FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/test-management-tool-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
