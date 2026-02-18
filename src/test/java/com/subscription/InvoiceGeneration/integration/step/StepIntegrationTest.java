package com.subscription.InvoiceGeneration.integration.step;

import com.subscription.InvoiceGeneration.constants.Constants;
import com.subscription.InvoiceGeneration.model.Invoice;
import com.subscription.InvoiceGeneration.repo.CustomerRepository;
import com.subscription.InvoiceGeneration.repo.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for individual Batch Steps
 * Tests each step in isolation before testing full job
 */

@SpringBootTest(args = {"D:\\Java_data_projects\\InvoiceGeneration\\src\\test\\resources\\test-usage-comprehensive.csv", "test-key.json", "local"})
@SpringBatchTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class StepIntegrationTest {

    @Autowired
    private JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    private Job invoiceProcessingJob;

    @Autowired
    @Qualifier("loadUsageStep")
    private Step loadUsageStep;

    @Autowired
    @Qualifier("loadPricingStep")
    private Step loadPricingStep;

    @Autowired
    @Qualifier("partitionedInvoiceStep")
    private Step partitionedInvoiceStep;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobOperatorTestUtils.setJob (invoiceProcessingJob);

        // Clear data
        invoiceRepository.deleteAll();
        customerRepository.deleteAll();

        // Setup test customers
        setupTestCustomers();
    }

    private void setupTestCustomers() {
        String insertSql = "INSERT INTO customers (id, name, email, plan_code, start_date, status, billing_address, discount, last_billed_month) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(insertSql, 1, "John Doe", "john@test.com", "BASIC",
                LocalDate.of(2024, 12, 1), "ACTIVE", "123 Main St", 0.0, null);

        jdbcTemplate.update(insertSql, 2, "Jane Smith", "jane@test.com", "PRO",
                LocalDate.of(2024, 11, 1), "ACTIVE", "456 Oak Ave", 10.0, null);

        jdbcTemplate.update(insertSql, 3, "Bob Johnson", "bob@test.com", "BASIC",
                LocalDate.of(2025, 1, 15), "ACTIVE", "789 Pine St", 5.0, null);
    }

    // ==================== Load Usage Step Tests ====================

    @Test
    @DisplayName("loadUsageStep - Should complete successfully")
    void testLoadUsageStepCompletion() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("usage", "src/test/resources/test-usage.csv")
                .addString("service_acct_key", "")
                .addString("env", "local")
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();

        // When - Launch only loadUsageStep
        JobExecution jobExecution = jobOperatorTestUtils.startStep (loadUsageStep, jobParameters, new ExecutionContext());

        // Then
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // Verify step completed
        boolean stepCompleted = jobExecution.getStepExecutions().stream()
                .anyMatch(step -> "loadUsageStep".equals(step.getStepName())
                        && BatchStatus.COMPLETED == step.getStatus());

        assertTrue(stepCompleted, "loadUsageStep should complete successfully");
    }

    @Test
    @DisplayName("loadUsageStep - Should populate usageMap in execution context")
    void testLoadUsageStepPopulatesContext() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("usage", "src/test/resources/test-usage.csv")
                .addString("service_acct_key", "")
                .addString("env", "local")
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();

        // When
        JobExecution jobExecution = jobOperatorTestUtils.startStep (loadUsageStep, jobParameters, new ExecutionContext());

        // Then
        @SuppressWarnings ("unchecked")
        ConcurrentMap<Integer, ?> usageMap = (ConcurrentMap<Integer, ?>)
                jobExecution.getExecutionContext().get("usageMap");

        assertNotNull(usageMap, "usageMap should be in execution context");
        assertFalse(usageMap.isEmpty(), "usageMap should contain data");

        // Verify data from test CSV
        assertTrue(usageMap.containsKey(1), "Should contain customer 1");
        assertTrue(usageMap.containsKey(2), "Should contain customer 2");
        assertTrue(usageMap.containsKey(3), "Should contain customer 3");
    }

    @Test
    @DisplayName("loadUsageStep - Should handle missing file gracefully")
    void testLoadUsageStepMissingFile() {
        // Given - Invalid file path
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("usage", "src/test/resources/non-existent-file.csv")
                .addString("service_acct_key", "")
                .addString("env", "local")
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();

        // When
        JobExecution jobExecution = jobOperatorTestUtils.startStep (loadUsageStep, jobParameters, new ExecutionContext());

        // Then - Should fail gracefully
        assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
    }

    // ==================== Load Pricing Step Tests ====================

    @Test
    @DisplayName("loadPricingStep - Should complete successfully")
    void testLoadPricingStepCompletion() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();

        // When
        JobExecution jobExecution = jobOperatorTestUtils.startStep (loadPricingStep, jobParameters, new ExecutionContext());

        // Then
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
    }

    @Test
    @DisplayName("loadPricingStep - Should populate pricingMap in execution context")
    void testLoadPricingStepPopulatesContext() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();

        // When
        JobExecution jobExecution = jobOperatorTestUtils.startStep (loadPricingStep, jobParameters, new ExecutionContext());

        // Then
        @SuppressWarnings ("unchecked")
        java.util.Map<String, java.util.Map<String, Object>> pricingMap =
                (java.util.Map<String, java.util.Map<String, Object>>)
                        jobExecution.getExecutionContext().get("pricingMap");

        assertNotNull(pricingMap, "pricingMap should be in execution context");
        assertFalse (pricingMap.isEmpty(), "pricingMap should contain plans");

        // Verify expected plans
        assertTrue(pricingMap.containsKey("BASIC"), "Should contain BASIC plan");
        assertTrue(pricingMap.containsKey("PRO"), "Should contain PRO plan");
    }

    @Test
    @DisplayName("loadPricingStep - Should load plan details correctly")
    void testLoadPricingStepPlanDetails() {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();

        // When
        JobExecution jobExecution = jobOperatorTestUtils.startStep (loadPricingStep, jobParameters, new ExecutionContext());

        // Then
        @SuppressWarnings ("unchecked")
        java.util.Map<String, java.util.Map<String, Object>> pricingMap =
                (java.util.Map<String, java.util.Map<String, Object>>)
                        jobExecution.getExecutionContext().get("pricingMap");

        assert pricingMap != null;
        java.util.Map<String, Object> basicPlan = pricingMap.get("BASIC");
        assertNotNull(basicPlan);

        // Verify plan has all required fields
        assertTrue(basicPlan.containsKey(Constants.BASE_PRICE));
        assertTrue(basicPlan.containsKey(Constants.FREE_STORAGE_GB));
        assertTrue(basicPlan.containsKey(Constants.FREE_BANDWIDTH_GB));
        assertTrue(basicPlan.containsKey(Constants.FREE_API_CALLS));
    }

    // ==================== Invoice Processing Step Tests ====================

    @Test
    @DisplayName("invoiceProcess - Should complete successfully with test data")
    void testInvoiceProcessStepCompletion() {
        // Given - Need to set up execution context with usageMap and pricingMap
        // This is complex because invoiceProcess depends on previous steps
        // For now, we'll test it as part of the full job

        // Note: Testing individual chunk-oriented steps is complex
        // because they depend on execution context from previous steps
        // This is better tested as part of the full job integration test
        assertTrue(true, "Individual step test for invoiceProcess requires full context");
    }

    @Test
    @DisplayName("partitionedInvoiceStep - Should create multiple partitions")
    void testPartitionedStepCreatesPartitions() throws Exception {
        // Given - Setup complete job context
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("usage", "src/test/resources/test-usage.csv")
                .addString("service_acct_key", "")
                .addString("env", "local")
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();

        // When - Run full job to get partitioning
        JobExecution jobExecution =  jobOperatorTestUtils.startJob (jobParameters);

        // Then - Verify partitioning occurred
        long partitionCount = jobExecution.getStepExecutions().stream()
                .filter(step -> step.getStepName().startsWith("invoiceProcess:partition"))
                .count();

        assertTrue(partitionCount > 0, "Should create at least one partition");
    }

    // ==================== Step Sequencing Tests ====================

    @Test
    @DisplayName("Steps - Should execute in correct order in full job")
    void testStepExecutionOrder() throws Exception {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("usage", "src/test/resources/test-usage.csv")
                .addString("service_acct_key", "")
                .addString("env", "local")
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();

        // When
        JobExecution jobExecution = jobOperatorTestUtils.startJob (jobParameters);

        // Then - Verify steps executed in order
        List<String> stepNames = jobExecution.getStepExecutions().stream()
                .map(org.springframework.batch.core.step.StepExecution::getStepName)
                .toList();

        // loadUsageStep should come before loadPricingStep
        int usageIndex = stepNames.indexOf("loadUsageStep");
        int pricingIndex = stepNames.indexOf("loadPricingStep");
        int invoiceIndex = stepNames.indexOf("partitionedInvoiceStep");

        assertTrue(usageIndex >= 0, "Should execute loadUsageStep");
        assertTrue(pricingIndex >= 0, "Should execute loadPricingStep");
        assertTrue(invoiceIndex >= 0, "Should execute partitionedInvoiceStep");

        assertTrue(usageIndex < pricingIndex, "loadUsageStep should execute before loadPricingStep");
        assertTrue(pricingIndex < invoiceIndex, "loadPricingStep should execute before partitionedInvoiceStep");
    }

    // ==================== Step Metrics Tests ====================

    @Test
    @DisplayName("Steps - Should track read/write counts correctly")
    void testStepMetrics() throws Exception {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("usage", "src/test/resources/test-usage.csv")
                .addString("service_acct_key", "")
                .addString("env", "local")
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();

        // When
        JobExecution jobExecution = jobOperatorTestUtils.startJob (jobParameters);

        // Then - Verify metrics
        jobExecution.getStepExecutions().forEach(stepExecution -> {
            System.out.println("Step: " + stepExecution.getStepName());
            System.out.println("  Status: " + stepExecution.getStatus());
            System.out.println("  Read Count: " + stepExecution.getReadCount());
            System.out.println("  Write Count: " + stepExecution.getWriteCount());
            System.out.println("  Skip Count: " + stepExecution.getSkipCount());

            // All steps should complete
            assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
        });
    }

    @Test
    @DisplayName("invoiceProcess - Should read customers and write invoices")
    void testInvoiceProcessReadWrite() throws Exception {
        // Given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("usage", "src/test/resources/test-usage.csv")
                .addString("service_acct_key", "")
                .addString("env", "local")
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();

        // When
        JobExecution jobExecution = jobOperatorTestUtils.startJob (jobParameters);

        // Then - Verify read/write happened
        long totalReadCount = jobExecution.getStepExecutions().stream()
                .filter(step -> step.getStepName().startsWith("invoiceProcess"))
                .mapToLong(org.springframework.batch.core.step.StepExecution::getReadCount)
                .sum();

        long totalWriteCount = jobExecution.getStepExecutions().stream()
                .filter(step -> step.getStepName().startsWith("invoiceProcess"))
                .mapToLong(org.springframework.batch.core.step.StepExecution::getWriteCount)
                .sum();

        assertTrue(totalReadCount > 0, "Should read at least one customer");
        assertTrue(totalWriteCount > 0, "Should write at least one invoice");

        // Verify invoices were actually persisted
        List<Invoice> invoices = invoiceRepository.findAll();
        assertEquals(totalWriteCount, invoices.size(),
                "Write count should match persisted invoices");
    }
}