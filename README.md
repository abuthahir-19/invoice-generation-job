# Spring Batch Invoice Generation Application

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Batch](https://img.shields.io/badge/Spring%20Batch-5.x-blue.svg)](https://spring.io/projects/spring-batch)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED.svg)](https://www.docker.com/)
[![GCP](https://img.shields.io/badge/GCP-Cloud%20Run-4285F4.svg)](https://cloud.google.com/run)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-Jenkins-D24939.svg)](https://www.jenkins.io/)

## Table of Contents

- [Overview](#overview)
- [Business Use-Case](#business-use-case)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [CI/CD Pipeline](#cicd-pipeline)
- [Setup and Installation](#setup-and-installation)
- [Local Setup](#local-setup)
- [Docker Containerization](#docker-containerization)
- [GCP Deployment](#gcp-deployment)
- [Jenkins CI/CD Setup](#jenkins-cicd-setup)
- [Testing](#testing)
- [Monitoring and Troubleshooting](#monitoring-and-troubleshooting)

---

## Overview

This project is a scalable Spring Batch application designed for automated monthly invoice generation in a subscription-based SaaS service (e.g., cloud storage similar to Dropbox or Google Drive). It efficiently processes large-scale customer data, usage metrics, and pricing rules to compute charges, apply discounts, and generate invoices. The app handles millions of records with fault tolerance, restartability, and parallelism, making it suitable for production environments.

The core functionality involves reading customer details from a PostgreSQL database, loading usage data from CSV files, parsing pricing from YAML, joining the data for calculations (prorated base charges, overages for storage/bandwidth/API calls), and outputting invoices to a database or files. It addresses real-world challenges like mid-month subscriptions, data validation, and scalability for high-volume billing.

To make this a production-ready solution, the application incorporates cloud-native features and **enterprise-grade CI/CD automation**:

- **Automated CI/CD Pipeline:** Jenkins-based continuous integration and deployment with automated testing, Docker image building, and Cloud Run deployment
- Integration with Google Cloud SQL (PostgreSQL) for managed, scalable storage
- Support for Google Cloud Storage (GCS) for remote CSV files in production
- Containerization using Docker with multi-stage builds for consistent deployments
- Image management in Google Artifact Registry for secure versioning
- Deployment as a Cloud Run Job on Google Cloud Platform (GCP), enabling scheduled or on-demand executions with serverless scaling
- **Environment isolation** with separate dev/prod configurations and manual approval gates

This demonstrates end-to-end DevOps practices, from local development to automated cloud deployment via Jenkins pipeline, serving as a practical example for building production-ready batch processing applications in a modern cloud ecosystem.

![Architecture Flow](./src/main/resources/Invoice-generation.png)

---

## Business Use-Case

In a subscription-based SaaS business, monthly invoicing for thousands to millions of customers involves aggregating subscription details, usage metrics (e.g., storage consumed, bandwidth, API calls), and applying tiered pricing. Manual processing is error-prone and unscalable, leading to delays or inaccuracies.

The batch job runs monthly (e.g., on the 1st) to:

1. **Load Data:** Read active customers from the database, usage from CSV (e.g., supplier logs), and pricing rules from YAML.
2. **Validate and Join:** Match customers with usage data, skip inactive or invalid records.
3. **Calculate Charges:** Prorate base fees based on start date (e.g., mid-month sign-ups), compute overages (e.g., excess storage at $1/GB), apply discounts.
4. **Generate Invoices:** Output itemized invoices with totals, storing in a database for auditing/emailing and optionally as files.
5. **Support Compliance:** Timestamps, breakdowns, and skips ensure audit-ready records.

This automates repetitive tasks, scales to 10M+ customers, reduces errors, and integrates with email systems for delivery. It promotes efficient billing, customer satisfaction, and revenue accuracy.

---

## Features

### Core Functionality
- **Batch Processing with Spring Batch:** Chunk-oriented steps for reading (DB/CSV), processing (calculations/joins), and writing (DB/files), with fault tolerance (skips up to 1000 exceptions)
- **Database Integration:** JPA/Hibernate for reading customers from PostgreSQL (paging for large datasets); supports local or Cloud SQL
- **File Handling:** Multithreaded CSV loading (ExecutorService for parallel parsing, reduced 1.7 min to 6s for 1M+ records); YAML for pricing
- **Calculations:** Proration, overages for multiple metrics, discounts; uses BigDecimal for precision
- **Validation:** Skips missing usage/invalid plans; dynamic JPQL for ranged queries in partitioning
- **Performance Optimizations:** Partitioning for parallel customer processing (by ID ranges, uses all cores); streaming batches for low-memory CSV loading

### Cloud-Native Features
- **Cloud Support:** GCS for remote CSV (with service account keys); Cloud SQL for DB; configurable via env/args (dev/prod)
- **Deployment:** Docker multi-stage build (Maven packaging inside image); Cloud Run Jobs for serverless execution (16 GB RAM, 8 vCPU)
- **Logging/Monitoring:** SLF4J for detailed logs (e.g., load times, skips); restartable via Batch repository
- **Security:** Credentials mounted as volumes; no hardcoding; environment variable injection

### CI/CD & DevOps
- **Jenkins Pipeline:** Automated build, test, Docker image creation, and deployment to Cloud Run
- **Automated Testing:** Unit and integration tests run automatically on every commit
- **Multi-Environment Support:** Separate dev and prod environments with different configurations
- **Manual Approval Gates:** Production deployments require explicit approval for safety
- **Docker Integration:** Automated image building, tagging, and pushing to Artifact Registry
- **Environment Variable Management:** Secure credential injection via Jenkins credentials
- **Zero Downtime Deployment:** Cloud Run job updates with no service interruption
- **Build Artifacts:** JAR files archived for traceability and rollback capability

---

## Technology Stack

### Core Framework
- **Framework:** Spring Boot 3.x, Spring Batch 5.x
- **Language:** Java 21 (JDK)
- **Build Tool:** Maven 3.9+

### Database & Storage
- **Database:** PostgreSQL 18 (via Cloud SQL in prod)
- **Data Access:** Spring Data JPA for ORM; JDBC optional for writes
- **File/Cloud:** Spring Cloud GCP for GCS; FlatFileItemReader for CSV

### Cloud & Infrastructure
- **Cloud Services:** Google Cloud Platform (Artifact Registry, Cloud Run Jobs, Cloud SQL, Cloud Storage)
- **Containerization:** Docker (multi-stage builds)
- **CI/CD:** Jenkins 2.54 with Pipeline plugin
- **Monitoring:** GCP Cloud Monitoring, Cloud Logging

### Libraries & Tools
- **Concurrency:** ExecutorService for multithreading; TaskExecutor for partitioning
- **Other Libraries:** Lombok, SLF4J, BigDecimal for finances, Google Cloud Storage SDK
- **Testing:** JUnit 5, Mockito, Spring Batch Test

---

## Architecture

The application follows a modular Spring Batch architecture:

1. **Job Configuration:** Single job with steps for loading usage/pricing, then partitioned processing.
2. **Step 1 - Load Usage:** Multithreaded CSV read into ConcurrentHashMap (streaming batches for low memory).
3. **Step 2 - Load Pricing:** YAML parse into map.
4. **Step 3 - Process Invoices:** Partitioned step: Reads active customers (JPA paging with ID ranges), joins with usage/pricing, calculates charges, outputs invoices.
5. **Job Execution:** Triggered via args (usage path, credentials, env); scheduled in Cloud Run.

For cloud deployment:

1. Built as JAR, containerized with Docker.
2. Pushed to Artifact Registry.
3. Deployed as Cloud Run Job, invocable manually or scheduled.

### CI/CD Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     JENKINS CI/CD PIPELINE                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. Git Checkout     → Clone repository                         │
│  2. Maven Build      → Compile source code                      │
│  3. Run Tests        → Execute unit & integration tests         │
│  4. Package          → Create executable JAR                    │
│  5. Docker Build     → Build container image                    │
│  6. Push to Registry → Upload to Artifact Registry              │
│  7. Deploy Dev       → Update Cloud Run job (dev)               │
│  8. Approval Gate    → Manual approval for production           │
│  9. Deploy Prod      → Update Cloud Run job (prod)              │
│                                                                 │
│  Duration: ~7 minutes (dev), ~10 minutes (with prod approval)   │
└─────────────────────────────────────────────────────────────────┘
```

---

## CI/CD Pipeline

### Pipeline Overview

The Jenkins pipeline automates the entire software delivery lifecycle from code commit to production deployment. Every push to the repository triggers the pipeline, ensuring consistent, reliable deployments.

**Pipeline Stages:**

1. **Checkout Code**
    - Clones source code from Git repository
    - Verifies branch and commit hash
    - Sets up build workspace

2. **Build & Run Tests**
    - Compiles Java 21 source code with Maven
    - Runs unit tests (processors, calculators, entities)
    - Runs integration tests (job execution, repository operations)
    - Generates test reports (JUnit XML)
    - Fails pipeline if any tests fail

3. **Build Docker Image**
    - Uses multi-stage Dockerfile for optimized image
    - Tags with build number and `latest`
    - Image size: ~250MB (Alpine-based)

4. **Push Image to Artifact Registry**
    - Authenticates with GCP service account
    - Pushes image to Google Artifact Registry
    - Tags: `build-{number}` and `latest`

5. **Deploy to Cloud Run Job (Dev)**
    - Updates dev Cloud Run job with new image
    - Injects development environment variables
    - Configures resources (2 vCPU, 2GB RAM)
    - Automatically deployed on every successful build

6. **Approval for Production (Manual Gate)**
    - Pauses pipeline for human review
    - Requires authorized user approval
    - 30-minute timeout (auto-rejects if no response)
    - Only runs for main/master branch

7. **Deploy to Production**
    - Updates production Cloud Run job with approved image
    - Injects production environment variables
    - Higher resources (4 vCPU, 4GB RAM)
    - Minimum 1 instance always running
    - Restricted access (authentication required)

### Environment Configurations

#### Development Environment
```yaml
Environment: dev
Cloud Run Job: invoice-generation-job-dev
Resources:
  CPU: 2 vCPU
  Memory: 2 GB
  Timeout: 15 minutes
  Max Instances: 10
  Min Instances: 0
Access: Public (for testing)
Database: invoice_dev
GCS Bucket: invoice-data-dev
Auto-Deploy: Yes (on every commit)
```

#### Production Environment
```yaml
Environment: prod
Cloud Run Job: invoice-generation-job-prod
Resources:
  CPU: 8 vCPU
  Memory: 16 GB
  Timeout: 15 minutes
  Max Instances: 20
  Min Instances: 1
Access: Authenticated only
Database: invoice_prod
GCS Bucket: invoice-data-prod
Auto-Deploy: No (requires manual approval)
```

### Key Features of Our Pipeline

✅ **Automated Testing:** All tests run automatically before deployment  
✅ **Environment Isolation:** Separate dev/prod with different resources  
✅ **Security:** Credentials managed via Jenkins, injected as environment variables  
✅ **Rollback Capability:** Previous images tagged and available in registry  
✅ **Build Artifacts:** JAR files archived for audit and rollback  
✅ **Health Checks:** Pipeline verifies successful deployment before marking complete  
✅ **Notifications:** Build status visible in Jenkins dashboard

---

## Setup and Installation

### Prerequisites

**For Local Development:**
- Java 21
- Maven 3.8+
- Docker
- GCP Account
- Google Cloud SDK installed in local environment
- PostgreSQL (Required to be installed in local machine for testing locally)
- Jenkins installed locally in Docker

**For CI/CD (Additional):**
- Jenkins 2.400+ installed and running
- Jenkins Plugins:
    - Git Plugin
    - Pipeline Plugin
    - Docker Pipeline Plugin
    - Google Cloud SDK Plugin
    - Maven Integration Plugin
    - JUnit Plugin
    - Credentials Plugin

---

## Local Setup

1. **Clone the repository:**
```shell
git clone https://github.com/abuthahir-19/invoice-generation.git
cd invoice-generation
```

2. **Configure application.properties (or use environment variables):**
```properties
spring.application.name=InvoiceGeneration
spring.datasource.url=${JDBC_URL}
spring.datasource.username=${USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# Disable Cloud SQL for local development
spring.cloud.gcp.sql.enabled=${CLOUD_SQL_ENABLED:false}
spring.cloud.gcp.sql.database-name=${DB_NAME:invoice_db}
spring.cloud.gcp.sql.instance-connection-name=${CLOUD_INSTANCE_CONNECTION_NAME:}

# Hibernate settings
spring.jpa.hibernate.ddl-auto=${DDL_AUTO:update}
spring.jpa.show-sql=${SHOW_SQL:false}

# Batch: Use PostgreSQL for JobRepository (auto-creates BATCH_* tables)
spring.batch.jdbc.initialize-schema=always
spring.batch.job.enabled=false  # Disables auto-run on startup (we control via runner)

# JPA Batching for performance
spring.jpa.properties.hibernate.jdbc.batch_size=2000
spring.datasource.hikari.maximum-pool-size=50
spring.jpa.properties.hibernate.order_inserts=true

# GCP Configuration
spring.cloud.gcp.project-id=${GCP_PROJECT_ID:}
spring.cloud.gcp.storage.enabled=${GCS_ENABLED:true}
```

3. **Set local environment variables:**
```shell
export JDBC_URL="jdbc:postgresql://localhost:5432/invoice_db"
export USERNAME="postgres"
export DB_PASSWORD="your_password"
export DB_NAME="invoice_db"
export CLOUD_SQL_ENABLED="false"
```

4. **Build the project:**
```shell
mvn clean install
```

5. **Run tests:**
```shell
mvn test
```

6. **Build the application:**
```shell
mvn clean package \
   -DJDBC_URL="${YOUR_JDBC_URL}" \
   -DDB_NAME="${YOUR_DB_NAME}" \
   -DCLOUD_INSTANCE_CONNECTION_NAME="${YOUR_CLOUD_INSTANCE_CONNECTION_NAME}" \
   -DUSERNAME="${YOUR_USERNAME}" \
   -DDB_PASSWORD="${YOUR_DB_PASSWORD}"

```

7. **Run the application:**
```shell
java -jar target/invoice-generation-0.0.1-SNAPSHOT.jar \
  /path/to/usage-data.csv \
  "" \
  local
```

**Arguments:**
- Arg 1: Path to usage CSV file
- Arg 2: Service account key path (empty for local)
- Arg 3: Environment (local/dev/prod)

---

## Docker Containerization

### Dockerfile

Our multi-stage Dockerfile optimizes image size and build time:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/InvoiceGeneration-0.0.1-SNAPSHOT.jar app.jar

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM options for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar ${0} ${@}"]
```

### Manual Docker Build (Without Jenkins)

1. **Build the Docker image:**
```shell
docker build -t invoice-generation-job:latest .
```

2. **Run docker image locally and verify:**
```shell
docker run \
  -e DATABASE_URL="jdbc:postgresql://host.docker.internal:5432/invoice_db" \
  -e DB_USERNAME="postgres" \
  -e DB_PASSWORD="your_password" \
  -e SPRING_PROFILES_ACTIVE="local" \
  -v <your-service-account-cred-file>:/app/creds.json \
  -v <your-local-csv-file-path>:/app/usage.csv \
  -e GOOGLE_APPLICATION_CREDENTIALS=/app/creds.json \
  invoice-generation-job:latest \
  /app/usage.csv "" local
```

3. **Create an Artifact Registry repository:**
```shell
gcloud artifacts repositories create my-batch-repo \
  --repository-format=docker \
  --location=us-central1 \
  --project=<gcp_project>
```

4. **Authenticate Docker to push:**
```shell
gcloud auth configure-docker us-central1-docker.pkg.dev
```

5. **Tag your local image for GCP:**
```shell
docker tag invoice-generation-job:latest \
  us-central1-docker.pkg.dev/<gcp_project>/my-batch-repo/invoice-generation-job:latest
```

6. **Push the image:**
```shell
docker push us-central1-docker.pkg.dev/<gcp_project>/my-batch-repo/invoice-generation-job:latest
```

7. **Verify in GCP Console:** Artifact Registry > Repositories > my-batch-repo > Images

---

## GCP Deployment

### Manual Deployment (Without Jenkins)

1. **Set up Google Cloud SQL PostgreSQL instance:**
```shell
gcloud sql instances create invoice-db \
  --database-version=POSTGRES_18 \
  --tier=db-g1-small \
  --region=us-central1 \
  --backup-start-time=03:00

# Create database
gcloud sql databases create invoice_prod --instance=invoice-db

# Create user
gcloud sql users create invoice_user \
  --instance=invoice-db \
  --password=secure_password
```

2. **Create a Cloud Run Job:**
```shell
gcloud run jobs create invoice-generation-job \
  --image us-central1-docker.pkg.dev/<gcp_project>/my-batch-repo/invoice-generation-job:latest \
  --region us-central1 \
  --project <your_gcp_project> \
  --service-account <your-service-account@gcp-project-name.iam.gserviceaccount.com> \
  --max-retries 2 \
  --task-timeout 15m \
  --memory 4Gi \
  --cpu 4 \
  --set-env-vars="DATABASE_URL=jdbc:postgresql://10.1.2.3:5432/invoice_prod,\
DB_USERNAME=invoice_user,\
DB_PASSWORD=secure_password,\
DB_NAME=invoice_prod,\
CLOUD_SQL_ENABLED=false,\
SPRING_AUTOCONFIGURE_EXCLUDE=com.google.cloud.spring.autoconfigure.sql.GcpCloudSqlAutoConfiguration,\
GCS_BUCKET=invoice-data-prod,\
SPRING_PROFILES_ACTIVE=prod"
```

3. **Run the Job manually:**
```shell
gcloud run jobs execute invoice-generation-job \
  --region us-central1 \
  --wait
```

4. **View logs:**
```shell
gcloud logging read \
  "resource.type=cloud_run_job AND resource.labels.job_name=invoice-generation-job" \
  --limit 50
```

---

## Jenkins CI/CD Setup

### Step 1: Install and Configure Jenkins

1. **Install Jenkins (if not already installed):**
```shell
# Ubuntu/Debian
wget -q -O - https://pkg.jenkins.io/debian-stable/jenkins.io.key | sudo apt-key add -
sudo sh -c 'echo deb https://pkg.jenkins.io/debian-stable binary/ > /etc/apt/sources.list.d/jenkins.list'
sudo apt-get update
sudo apt-get install jenkins

# Start Jenkins
sudo systemctl start jenkins
sudo systemctl enable jenkins
```

2. **Access Jenkins:**
    - Open browser: `http://localhost:8080`
    - Get initial password: `sudo cat /var/lib/jenkins/secrets/initialAdminPassword`
    - Complete setup wizard and install suggested plugins

### Step 2: Install Required Plugins

Navigate to: **Manage Jenkins** → **Manage Plugins** → **Available**

Install these plugins:
- Git Plugin
- Pipeline Plugin
- Docker Pipeline Plugin
- Google Cloud SDK Plugin
- Maven Integration Plugin
- JUnit Plugin
- Credentials Plugin

### Step 3: Configure Global Tools

**Manage Jenkins** → **Global Tool Configuration**

**Configure JDK:**
```
Name: JDK 21
JAVA_HOME: /usr/lib/jvm/java-21-openjdk-amd64
(or install automatically)
```

**Configure Maven:**
```
Name: Maven 3.9
Install automatically: Yes
Version: 3.9.6
```

### Step 4: Set Environment Variables

**Manage Jenkins** → **System** → **Global properties** → **Environment variables**

Add these variables:
```
GCP_PROJECT_ID: your-gcp-project-id
REGION: us-central1
```

### Step 5: Add Credentials

**Manage Jenkins** → **Manage Credentials** → **(global)** → **Add Credentials**

**1. GCP Service Account:**
```
Kind: Secret file
File: Upload your-gcp-service-account.json
ID: gcp-secret-key
Description: GCP Service Account for Cloud Run
```

**2. Database Credentials:**
```
Kind: Secret text
Secret: your_db_username
ID: USERNAME
Description: Database Username
```

```
Kind: Secret text
Secret: your_db_password
ID: DB_PASSWORD
Description: Database Password
```

```
Kind: Secret text
Secret: jdbc:postgresql://YOUR_HOST:5432/invoice_db
ID: JDBC_URL
Description: Database JDBC URL
```

```
Kind: Secret text
Secret: invoice_db
ID: DB_NAME
Description: Database Name
```

```
Kind: Secret text
Secret: (leave empty or provide if using Cloud SQL)
ID: CLOUD_INSTANCE_CONNECTION_NAME
Description: Cloud SQL Instance Connection Name
```

### Step 6: Create Jenkinsfile

Create `Jenkinsfile` in your repository root:

```groovy
pipeline {
    agent any

    environment {
        // Basic App Info
        APP_NAME          = 'invoice-generation-job'
        IMAGE_TAG         = "${env.BUILD_NUMBER}"

        // GCP Configuration
        GCP_PROJECT       = "${env.GCP_PROJECT_ID}"
        REGION            = "${env.REGION}"
        ARTIFACT_REGISTRY = 'us-central1-docker.pkg.dev'
        REPO_NAME         = 'my-batch-repo'
        CLOUD_RUN_JOB     = 'invoice-generation-job'
    }

    tools {
        maven 'Maven 3.9'
        jdk 'JDK 21'
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
                echo "✅ Code checked out successfully"
            }
        }

        stage('Build & Run Tests') {
            steps {
                withCredentials([
                    string(credentialsId: 'DB_NAME', variable: 'DB_NAME'),
                    string(credentialsId: 'CLOUD_INSTANCE_CONNECTION_NAME', variable: 'CLOUD_INSTANCE_CONNECTION_NAME'),
                    string(credentialsId: 'USERNAME', variable: 'USERNAME'),
                    string(credentialsId: 'DB_PASSWORD', variable: 'DB_PASSWORD'),
                    string(credentialsId: 'JDBC_URL', variable: 'JDBC_URL')
                ]) {
                    sh '''
                        mvn clean package \
                            -DJDBC_URL="${JDBC_URL}" \
                            -DDB_NAME="${DB_NAME}" \
                            -DCLOUD_INSTANCE_CONNECTION_NAME="${CLOUD_INSTANCE_CONNECTION_NAME}" \
                            -DUSERNAME="${USERNAME}" \
                            -DDB_PASSWORD="${DB_PASSWORD}" \
                            --no-transfer-progress
                    '''
                }
                echo "✅ Maven build and tests completed successfully"
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${APP_NAME}:${IMAGE_TAG} ."
                echo "✅ Docker image built: ${APP_NAME}:${IMAGE_TAG}"
            }
        }

        stage('Push Image to Artifact Registry') {
            steps {
                withCredentials([file(credentialsId: 'gcp-secret-key', variable: 'GOOGLE_APPLICATION_CREDENTIALS')]) {
                    sh '''
                        gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS
                        gcloud auth configure-docker ${ARTIFACT_REGISTRY}

                        docker tag ${APP_NAME}:${IMAGE_TAG} ${ARTIFACT_REGISTRY}/${GCP_PROJECT}/${REPO_NAME}/${APP_NAME}:${IMAGE_TAG}
                        docker push ${ARTIFACT_REGISTRY}/${GCP_PROJECT}/${REPO_NAME}/${APP_NAME}:${IMAGE_TAG}
                    '''
                }
                echo "✅ Image pushed to Artifact Registry"
            }
        }

        stage('Deploy to Cloud Run Job') {
            steps {
                withCredentials([
                    file(credentialsId: 'gcp-secret-key', variable: 'GOOGLE_APPLICATION_CREDENTIALS'),
                    string(credentialsId: 'JDBC_URL', variable: 'JDBC_URL'),
                    string(credentialsId: 'USERNAME', variable: 'USERNAME'),
                    string(credentialsId: 'DB_PASSWORD', variable: 'DB_PASSWORD'),
                    string(credentialsId: 'DB_NAME', variable: 'DB_NAME')
                ]) {
                    sh """
                        gcloud auth activate-service-account --key-file=\$GOOGLE_APPLICATION_CREDENTIALS
                        
                        gcloud run jobs update ${CLOUD_RUN_JOB} \\
                          --image ${ARTIFACT_REGISTRY}/${GCP_PROJECT}/${REPO_NAME}/${APP_NAME}:${IMAGE_TAG} \\
                          --region ${REGION} \\
                          --project ${GCP_PROJECT} \\
                          --set-env-vars="JDBC_URL=${JDBC_URL},USERNAME=${USERNAME},DB_PASSWORD=${DB_PASSWORD},DB_NAME=${DB_NAME},CLOUD_SQL_ENABLED=false,SPRING_AUTOCONFIGURE_EXCLUDE=com.google.cloud.spring.autoconfigure.sql.GcpCloudSqlAutoConfiguration,DDL_AUTO=update,HIKARI_MAX_POOL=50" \\
                          --quiet
                    """
                }
                echo "✅ Cloud Run Job updated successfully"
            }
        }
    }

    post {
        success {
            echo '🎉 Pipeline completed successfully!'
        }
        failure {
            echo '❌ Pipeline failed. Check logs above.'
        }
    }
}
```

### Step 7: Create Jenkins Pipeline Job

1. **Create new job:**
   ```
   Jenkins Dashboard → New Item
   Name: invoice-generation-pipeline
   Type: Pipeline
   Click OK
   ```

2. **Configure Pipeline:**
   ```
   General:
     ☑️ Discard old builds (Days: 30, Max: 10)
   
   Build Triggers:
     ☑️ Poll SCM (Schedule: H/5 * * * *)
   
   Pipeline:
     Definition: Pipeline script from SCM
     SCM: Git
     Repository URL: https://github.com/your-username/invoice-generation.git
     Credentials: (your Git credentials)
     Branch: */main
     Script Path: Jenkinsfile
   ```

3. **Save and Build:**
    - Click "Save"
    - Click "Build Now"
    - Monitor console output

### Pipeline Verification

After successful build:

1. **Check Jenkins Console Output:**
   ```
   ✅ Checkout Code: SUCCESS
   ✅ Build & Run Tests: SUCCESS
   ✅ Build Docker Image: SUCCESS
   ✅ Push to Registry: SUCCESS
   ✅ Deploy to Cloud Run: SUCCESS
   ```

2. **Verify Cloud Run Job:**
   ```shell
   gcloud run jobs describe invoice-generation-job \
     --region us-central1 \
     --format="value(status.conditions)"
   ```

3. **Execute Job:**
   ```shell
   gcloud run jobs execute invoice-generation-job \
     --region us-central1 \
     --wait
   ```

---

## Testing

### Running Tests Locally

```shell
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=InvoiceProcessorTest

# Run with coverage report
mvn test jacoco:report
```

### Test Structure

```
src/test/java/
├── unit/
│   ├── processor/InvoiceProcessorTest.java          (12 tests)
│   ├── model/EntityModelTests.java                  (30 tests)
│   ├── tasklet/TaskletTests.java                    (15 tests)
│   └── partitioner/PartitionerTests.java            (20 tests)
└── integration/
    ├── job/InvoiceJobIntegrationTest.java           (10 tests)
    └── repository/InvoiceRepositoryIntegrationTest.java (11 tests)
```

### Test Resources

```
src/test/resources/
├── application-test.properties    # H2 database config, GCS disabled
├── test-usage.csv                # Sample usage data
├── test-pricing.yaml             # Test pricing rules
└── test-data.sql                 # Database initialization
```

---

## Monitoring and Troubleshooting

### Cloud Run Monitoring

```shell
# View job executions
gcloud run jobs executions list \
  --job invoice-generation-job \
  --region us-central1

# View execution details
gcloud run jobs executions describe <execution-name> \
  --region us-central1

# View logs
gcloud logging read \
  "resource.type=cloud_run_job AND resource.labels.job_name=invoice-generation-job" \
  --limit 100 \
  --format json
```

### Common Issues and Solutions

#### Issue 1: Cloud SQL Connection Error
**Error:** `[${CLOUD_INSTANCE_CONNECTION_NAME}] Cloud SQL connection name is invalid`

**Solution:**
```shell
# Verify environment variables are set in Cloud Run
gcloud run jobs describe invoice-generation-job \
  --format="value(spec.template.spec.containers[0].env)"

# Should show: CLOUD_SQL_ENABLED=false
# If missing, update via Jenkins or manually
```

#### Issue 2: Jenkins Build Fails
**Error:** `Maven build failed` or `Tests failed`

**Solution:**
- Check Jenkins console output for specific error
- Verify all credentials are set correctly in Jenkins
- Ensure JDK 21 and Maven 3.9 are configured
- Run tests locally to reproduce issue

#### Issue 3: Docker Push Fails
**Error:** `denied: Permission denied`

**Solution:**
```shell
# Verify service account has permissions
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
  --member="serviceAccount:YOUR_SA@YOUR_PROJECT.iam.gserviceaccount.com" \
  --role="roles/artifactregistry.writer"
```

#### Issue 4: Out of Memory
**Error:** `java.lang.OutOfMemoryError`

**Solution:**
- Increase Cloud Run memory allocation
- Reduce batch chunk size in configuration
- Enable streaming for large files

### Jenkins Debugging

```shell
# View Jenkins logs
sudo journalctl -u jenkins -f

# Check Jenkins system info
Jenkins → Manage Jenkins → System Information

# View build artifacts
Jenkins → Job → Build #X → Artifacts
```

---

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Write tests for new functionality
4. Ensure all tests pass (`mvn test`)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

---

## Author

**Syed Abuthahir**
- GitHub: [Syed Abuthahir](https://github.com/abuthahir-19)

---

## Acknowledgments

- Spring Batch Team for the excellent framework
- Google Cloud Platform for reliable infrastructure
- Jenkins community for CI/CD tooling
- PostgreSQL community for the robust database

---

**⭐ If you found this project helpful, please give it a star!**