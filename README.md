# Spring Batch Invoice Generation Application

## Overview

This project is a scalable Spring Batch application designed for automated monthly invoice generation in a subscription-based SaaS service (e.g., cloud storage similar to Dropbox or Google Drive). It efficiently processes large-scale customer data, usage metrics, and pricing rules to compute charges, apply discounts, and generate invoices. The app handles millions of records with fault tolerance, restartability, and parallelism, making it suitable for production environments.

The core functionality involves reading customer details from a PostgreSQL database, loading usage data from CSV files, parsing pricing from YAML, joining the data for calculations (prorated base charges, overages for storage/bandwidth/API calls), and outputting invoices to a database or files. It addresses real-world challenges like mid-month subscriptions, data validation, and scalability for high-volume billing.

To make this a production-ready solution, the application incorporates cloud-native features:

- Integration with Google Cloud SQL (PostgreSQL) for managed, scalable storage.
- Support for Google Cloud Storage (GCS) for remote CSV files in production.
- Containerization using Docker with multi-stage builds for consistent deployments.
- Image management in Google Artifact Registry for secure versioning.
- Deployment as a Cloud Run Job on Google Cloud Platform (GCP), enabling scheduled or on-demand executions with serverless scaling.

This demonstrates end-to-end DevOps practices, from local development to cloud deployment, serving as a practical example for building batch processing applications in a modern cloud ecosystem.

![Architecture Flow](./src/main/resources/Invoice-generation.png)
## Business Use-Case

In a subscription-based SaaS business, monthly invoicing for thousands to millions of customers involves aggregating subscription details, usage metrics (e.g., storage consumed, bandwidth, API calls), and applying tiered pricing. Manual processing is error-prone and unscalable, leading to delays or inaccuracies.

The batch job runs monthly (e.g., on the 1st) to:

1. **Load Data:** Read active customers from the database, usage from CSV (e.g., supplier logs), and pricing rules from YAML.
2. **Validate and Join:** Match customers with usage data, skip inactive or invalid records.
3. **Calculate Charges:** Prorate base fees based on start date (e.g., mid-month sign-ups), compute overages (e.g., excess storage at $1/GB), apply discounts.
4. **Generate Invoices:** Output itemized invoices with totals, storing in a database for auditing/emailing and optionally as files.
5. **Support Compliance:** Timestamps, breakdowns, and skips ensure audit-ready records.

This automates repetitive tasks, scales to 10M+ customers, reduces errors, and integrates with email systems for delivery. It promotes efficient billing, customer satisfaction, and revenue accuracy.

## Features

- **Batch Processing with Spring Batch:** Chunk-oriented steps for reading (DB/CSV), processing (calculations/joins), and writing (DB/files), with fault tolerance (skips up to 1000 exceptions).
- **Database Integration:** JPA/Hibernate for reading customers from PostgreSQL (paging for large datasets); supports local or Cloud SQL.
- **File Handling:** Multithreaded CSV loading (ExecutorService for parallel parsing, reduced 1.7 min to 6s for 1M+ records); YAML for pricing.
- **Calculations:** Proration, overages for multiple metrics, discounts; uses BigDecimal for precision.
- **Validation:** Skips missing usage/invalid plans; dynamic JPQL for ranged queries in partitioning.
- **Performance Optimizations:** Partitioning for parallel customer processing (by ID ranges, uses all cores); streaming batches for low-memory CSV loading.
- **Cloud Support:** GCS for remote CSV (with service account keys); Cloud SQL for DB; configurable via env/args (dev/prod).
- **Deployment:** Docker multi-stage build (Maven packaging inside image); Cloud Run Jobs for serverless execution (16 GB RAM, 8 vCPU).
- **Logging/Monitoring:** SLF4J for detailed logs (e.g., load times, skips); restartable via Batch repository.
- **Security:** Credentials mounted as volumes; no hardcoding.

## Technology Stack

- **Framework:** Spring Boot 3.x, Spring Batch 5.x
- **Language:** Java 21 (JDK)
- **Database:** PostgreSQL (via Cloud SQL in prod)
- **Data Access:** Spring Data JPA for ORM; JDBC optional for writes.
- **File/Cloud:** Spring Cloud GCP for GCS; FlatFileItemReader for CSV.
- **Concurrency:** ExecutorService for multithreading; TaskExecutor for partitioning.
- **Build Tool:** Maven
- **Containerization:** Docker (multi-stage)
- **Cloud Services:** Google Cloud Platform (Artifact Registry, Cloud Run Jobs, Cloud SQL, GCS)
- **Other Libraries:** Lombok, SLF4J, BigDecimal for finances, Google Cloud Storage SDK.

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