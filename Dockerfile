# Lightweight Java 21 base image (matches Spring Boot)
FROM eclipse-temurin:21-jre-alpine

# Set working directory inside the container
WORKDIR /app

# Copy your built JAR (adjust filename if needed)
COPY target/InvoiceGeneration-0.0.1-SNAPSHOT.jar app.jar

# Run the JAR (your batch job starts here)
ENTRYPOINT ["java", "-jar", "app.jar"]