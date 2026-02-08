# Stage 1: Build the JAR (multi-stage for clean runtime image)
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .
COPY src ./src

# Package the JAR (skip tests for speed; remove -DskipTests if you want tests)
RUN mvn clean package -DskipTests

# Stage 2: Lightweight runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/target/InvoiceGeneration-0.0.1-SNAPSHOT.jar app.jar

# Set default JVM options (heap + heap dump on OOM)
ENV JAVA_OPTS="-Xms2g -Xmx6g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp"

# ENTRYPOINT that supports runtime arguments
# This allows: java $JAVA_OPTS -jar app.jar <usage> <service_acct_key> <env>
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar \"$@\"", "sh"]