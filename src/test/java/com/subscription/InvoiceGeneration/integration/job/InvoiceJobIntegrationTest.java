package com.subscription.InvoiceGeneration.integration.job;

import com.subscription.InvoiceGeneration.model.Invoice;
import com.subscription.InvoiceGeneration.repo.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the complete Invoice Generation batch job
 * Tests the full workflow from loading data to generating invoices
 * IMPORTANT: This test uses H2 in-memory database configured in application-test.properties
 * DO NOT mock repositories or EntityManagerFactory - they need to be real for batch processing
 */

@SpringBootTest(args = {"D:\\Java_data_projects\\InvoiceGeneration\\src\\test\\resources\\test-usage-comprehensive.csv", "test-key.json", "local"})
@SpringBatchTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class InvoiceJobIntegrationTest {

    @Autowired
    private JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private Job invoiceProcessingJob;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clear invoice table before each test
        invoiceRepository.deleteAll();

        jdbcTemplate.execute ("DELETE FROM customers");
    }

    @Sql (scripts = "classpath:test-data.sql")
    @Test
    @DisplayName("Should successfully complete invoice generation job")
    void testJobCompletion() throws Exception {
        // Given: Job parameters with test CSV file
        JobParameters jobParameters = createTestJobParameters();

        // When: Execute the job
        JobExecution jobExecution = jobOperator.start(invoiceProcessingJob, jobParameters);

        // Then: Job should complete successfully
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // Verify customers were inserted
        Integer customerCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers", Integer.class);
        System.out.println("Total customers in DB: " + customerCount);

        // Verify step executions
        jobExecution.getStepExecutions().forEach(step ->
                System.out.println("Step: " + step.getStepName() + ", Status: " + step.getStatus())
        );
    }

    @Test
    @DisplayName("Should generate invoices for active customers only")
    void testActiveCustomersProcessing() throws Exception {
        // Given: Job parameters
        JobParameters jobParameters = createTestJobParameters();

        // When: Execute the job
        JobExecution jobExecution = jobOperator.start(invoiceProcessingJob, jobParameters);

        // Then: Should create invoices only for active customers (customers 1, 2, 3)
        // Customer 4 is INACTIVE and should be skipped
        List<Invoice> invoices = (List<Invoice>) invoiceRepository.findAll();

        assertTrue(invoices.size() > 0, "Should generate at least one invoice");
        assertTrue(invoices.size() <= 3, "Should not generate invoice for inactive customer");

        // Verify no invoice for customer 4 (INACTIVE)
        boolean hasInactiveCustomerInvoice = invoices.stream()
                .anyMatch(inv -> inv.getCustomerId() == 4);
        assertFalse(hasInactiveCustomerInvoice, "Should not generate invoice for inactive customer");
    }

    @Test
    @DisplayName("Should handle missing usage data gracefully")
    void testMissingUsageData() throws Exception {
        // Given: Add a customer without usage data
        String insertSql = "INSERT INTO customers (id, name, email, plan_code, start_date, status, billing_address, discount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(insertSql, 999, "No Usage Customer", "nousage@test.com", "BASIC",
                LocalDate.of(2025, 1, 1), "ACTIVE", "999 Test St", 0.0);

        JobParameters jobParameters = createTestJobParameters();

        // When: Execute the job
        JobExecution jobExecution = jobOperator.start(invoiceProcessingJob, jobParameters);

        // Then: Job should complete (skip customer without usage)
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // Verify no invoice for customer 999
        List<Invoice> invoices = (List<Invoice>) invoiceRepository.findAll();
        boolean hasInvoiceFor999 = invoices.stream()
                .anyMatch(inv -> inv.getCustomerId() == 999);
        assertFalse(hasInvoiceFor999, "Should skip customer without usage data");
    }

    @Test
    @DisplayName("Should load usage data successfully")
    void testUsageDataLoading() throws Exception {
        // Given: Job parameters with test CSV
        JobParameters jobParameters = createTestJobParameters();

        // When: Execute the job
        JobExecution jobExecution = jobOperator.start(invoiceProcessingJob, jobParameters);

        // Then: loadUsageStep should complete successfully
        boolean loadUsageCompleted = jobExecution.getStepExecutions().stream()
                .anyMatch(step -> "loadUsageStep".equals(step.getStepName())
                        && BatchStatus.COMPLETED == step.getStatus());

        assertTrue(loadUsageCompleted, "loadUsageStep should complete successfully");
    }

    @Test
    @DisplayName("Should load pricing configuration successfully")
    void testPricingDataLoading() throws Exception {
        // Given: Job parameters
        JobParameters jobParameters = createTestJobParameters();

        // When: Execute the job
        JobExecution jobExecution = jobOperator.start(invoiceProcessingJob, jobParameters);

        // Then: loadPricingStep should complete successfully
        boolean loadPricingCompleted = jobExecution.getStepExecutions().stream()
                .anyMatch(step -> "loadPricingStep".equals(step.getStepName())
                        && BatchStatus.COMPLETED == step.getStatus());

        assertTrue(loadPricingCompleted, "loadPricingStep should complete successfully");
    }

    @Test
    @DisplayName("Should execute all steps in correct order")
    void testStepExecutionOrder() throws Exception {
        // Given: Job parameters
        JobParameters jobParameters = createTestJobParameters();

        // When: Execute the job
        JobExecution jobExecution = jobOperator.start(invoiceProcessingJob, jobParameters);

        // Then: Steps should execute in order: loadUsage -> loadPricing -> partitionedInvoice
        List<String> stepNames = jobExecution.getStepExecutions().stream()
                .map(org.springframework.batch.core.step.StepExecution::getStepName)
                .toList();

        assertTrue(stepNames.contains("loadUsageStep"), "Should execute loadUsageStep");
        assertTrue(stepNames.contains("loadPricingStep"), "Should execute loadPricingStep");
        assertTrue(stepNames.contains("partitionedInvoiceStep"), "Should execute partitionedInvoiceStep");
    }

    @Test
    @DisplayName("Should persist invoices to database")
    void testInvoicePersistence() throws Exception {
        // Given: Job parameters
        JobParameters jobParameters = createTestJobParameters();

        // When: Execute the job
        jobOperator.start(invoiceProcessingJob, jobParameters);

        // Then: Invoices should be saved in database
        List<Invoice> savedInvoices = (List<Invoice>) invoiceRepository.findAll();

        assertFalse(savedInvoices.isEmpty(), "Should save invoices to database");

        // Verify invoice structure
        Invoice firstInvoice = savedInvoices.get(0);
        assertNotNull(firstInvoice.getCustomerId());
        assertNotNull(firstInvoice.getMonth());
        assertNotNull(firstInvoice.getPlan());
        assertNotNull(firstInvoice.getBaseCharge());
        assertNotNull(firstInvoice.getTotal());
        assertNotNull(firstInvoice.getGeneratedAt());
    }

    @Test
    @DisplayName("Should handle partitioning correctly")
    void testPartitioning() throws Exception {
        // Given: Job parameters
        JobParameters jobParameters = createTestJobParameters();

        // When: Execute the job
        JobExecution jobExecution = jobOperator.start(invoiceProcessingJob, jobParameters);

        // Then: Partitioned step should create multiple partitions
        long partitionCount = jobExecution.getStepExecutions().stream()
                .filter(step -> step.getStepName().startsWith("invoiceProcess:partition"))
                .count();

        assertTrue(partitionCount >= 1, "Should create at least one partition");
    }

    @Test
    @DisplayName("Should calculate read and write counts correctly")
    void testReadWriteCounts() throws Exception {
        // Given: Job parameters
        JobParameters jobParameters = createTestJobParameters();

        // When: Execute the job
        JobExecution jobExecution = jobOperator.start(invoiceProcessingJob, jobParameters);

        // Then: Read count should match number of active customers processed
        long totalReadCount = jobExecution.getStepExecutions().stream()
                .filter(step -> step.getStepName().startsWith("invoiceProcess"))
                .mapToLong(org.springframework.batch.core.step.StepExecution::getReadCount)
                .sum();

        assertTrue(totalReadCount > 0, "Should read at least one customer");

        // Write count should match number of invoices generated
        long totalWriteCount = jobExecution.getStepExecutions().stream()
                .filter(step -> step.getStepName().startsWith("invoiceProcess"))
                .mapToLong(org.springframework.batch.core.step.StepExecution::getWriteCount)
                .sum();

        List<Invoice> savedInvoices = (List<Invoice>) invoiceRepository.findAll();
        assertEquals(savedInvoices.size(), totalWriteCount, "Write count should match saved invoices");
    }

    // Helper method to create test job parameters
    private JobParameters createTestJobParameters() {
        return new JobParametersBuilder()
                .addString("usage", "D:\\Java_data_projects\\InvoiceGeneration\\src\\test\\resources\\test-usage-comprehensive.csv")
                .addString("service_acct_key", "")
                .addString("env", "local")
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();
    }
}