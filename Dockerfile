FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR from builder stage
COPY target/InvoiceGeneration-0.0.1-SNAPSHOT.jar app.jar

# Set default JVM options (heap + heap dump on OOM)
ENV JAVA_OPTS="-Xms2g -Xmx6g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp"

# ENTRYPOINT that supports runtime arguments
# This allows: java $JAVA_OPTS -jar app.jar <usage> <service_acct_key> <env>
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar \"$@\"", "sh"]