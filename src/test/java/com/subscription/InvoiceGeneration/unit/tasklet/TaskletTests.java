package com.subscription.InvoiceGeneration.unit.tasklet;

import com.subscription.InvoiceGeneration.config.InvoiceGeneratorConfig;
import com.subscription.InvoiceGeneration.constants.Constants;
import com.subscription.InvoiceGeneration.model.Usage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.test.MetaDataInstanceFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Tasklets
 * Tests loadUsageTasklet and loadPricingTasklet in isolation
 */
class TaskletTests {

    private InvoiceGeneratorConfig config;
    private StepExecution stepExecution;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        config = new InvoiceGeneratorConfig();
        stepExecution = MetaDataInstanceFactory.createStepExecution();
    }

    // ==================== Load Usage Tasklet Tests ====================

    @Test
    @DisplayName("LoadUsageTasklet - Should load usage data from local file")
    void testLoadUsageFromLocalFile() throws Exception {
        // Given: Create a test CSV file
        Path csvFile = createTestUsageFile();

        // When: Execute tasklet
        Tasklet tasklet = config.loadUsageTasklet(
                csvFile.toString(),  // usage
                "",                   // service_acct_key
                "local"              // env
        );

        ExecutionContext chunkContext = stepExecution.getExecutionContext();
        RepeatStatus status = tasklet.execute(null,
                new org.springframework.batch.core.scope.context.ChunkContext(
                        new org.springframework.batch.core.scope.context.StepContext(stepExecution)
                )
        );

        // Then: Should complete successfully
        assertEquals(RepeatStatus.FINISHED, status);

        // Verify usageMap is populated
        @SuppressWarnings ("unchecked")
        ConcurrentMap<Integer, Usage> usageMap = (ConcurrentMap<Integer, Usage>)
                stepExecution.getJobExecution().getExecutionContext().get("usageMap");

        assertNotNull(usageMap, "usageMap should be populated");
        assertFalse(usageMap.isEmpty(), "usageMap should contain data");
    }

    @Test
    @DisplayName("LoadUsageTasklet - Should handle CSV with header row")
    void testLoadUsageSkipsHeader() throws Exception {
        // Given: Create a test CSV with header
        Path csvFile = createTestUsageFile();

        // When: Execute tasklet
        Tasklet tasklet = config.loadUsageTasklet(
                csvFile.toString(),
                "",
                "local"
        );

        RepeatStatus status = tasklet.execute(null,
                new org.springframework.batch.core.scope.context.ChunkContext(
                        new org.springframework.batch.core.scope.context.StepContext(stepExecution)
                )
        );

        // Then: Header should be skipped, data rows should be loaded
        assertEquals(RepeatStatus.FINISHED, status);

        @SuppressWarnings ("unchecked")
        ConcurrentMap<Integer, ?> usageMap = (ConcurrentMap<Integer, ?>)
                stepExecution.getJobExecution().getExecutionContext().get("usageMap");

        assertNotNull(usageMap);
        // Should have data rows, not including header
        assertEquals(3, usageMap.size());
    }

    @Test
    @DisplayName("LoadUsageTasklet - Should handle empty file gracefully")
    void testLoadUsageEmptyFile() throws Exception {
        // Given: Create an empty CSV file (with header only)
        Path csvFile = tempDir.resolve("empty-usage.csv");
        Files.writeString(csvFile, "customer_id,storage_used_gb,bandwidth_used_gb,api_calls,peak_usage_date,total_logins\n");

        // When: Execute tasklet
        Tasklet tasklet = config.loadUsageTasklet(
                csvFile.toString(),
                "",
                "local"
        );

        RepeatStatus status = tasklet.execute(null,
                new org.springframework.batch.core.scope.context.ChunkContext(
                        new org.springframework.batch.core.scope.context.StepContext(stepExecution)
                )
        );

        // Then: Should complete but with empty map
        assertEquals(RepeatStatus.FINISHED, status);

        @SuppressWarnings ("unchecked")
        ConcurrentMap<Integer, ?> usageMap = (ConcurrentMap<Integer, ?>)
                stepExecution.getJobExecution().getExecutionContext().get("usageMap");

        assertNotNull(usageMap);
        assertEquals(0, usageMap.size(), "Empty file should result in empty usageMap");
    }

    @Test
    @DisplayName("LoadUsageTasklet - Should skip invalid lines")
    void testLoadUsageSkipsInvalidLines() throws Exception {
        // Given: CSV with some invalid lines
        Path csvFile = tempDir.resolve("invalid-usage.csv");
        String csvContent = """
            customer_id,storage_used_gb,bandwidth_used_gb,api_calls,peak_usage_date,total_logins
            1,100.5,500.0,50000,2025-01-15,100
            invalid,line,here,missing,fields
            2,200.0,1000.0,100000,2025-01-20,200
            another,bad,line
            3,150.0,750.0,75000,2025-01-18,150
            4,100.5,500.0,50000,2025-01-15,100
            invalid,line,here,missing,fields
            5,200.0,1000.0,100000,2025-01-20,200
            another,bad,line
            """;
        Files.writeString(csvFile, csvContent);

        // When: Execute tasklet
        Tasklet tasklet = config.loadUsageTasklet(
                csvFile.toString(),
                "",
                "local"
        );

        RepeatStatus status = tasklet.execute(null,
                new org.springframework.batch.core.scope.context.ChunkContext(
                        new org.springframework.batch.core.scope.context.StepContext(stepExecution)
                )
        );

        // Then: Should complete, loading only valid lines
        assertEquals(RepeatStatus.FINISHED, status);

        @SuppressWarnings ("unchecked")
        ConcurrentMap<Integer, ?> usageMap = (ConcurrentMap<Integer, ?>)
                stepExecution.getJobExecution().getExecutionContext().get("usageMap");

        assertNotNull(usageMap);
        assertEquals(5, usageMap.size(), "Should load only 3 valid lines");
        assertTrue(usageMap.containsKey(1));
        assertTrue(usageMap.containsKey(2));
        assertTrue(usageMap.containsKey(3));
    }

    @Test
    @DisplayName("LoadUsageTasklet - Should use concurrent processing")
    void testLoadUsageConcurrentProcessing() throws Exception {
        // Given: Large CSV file to test concurrency
        Path csvFile = createLargeUsageFile(1000); // 1000 records

        // When: Execute tasklet
        long startTime = System.currentTimeMillis();

        Tasklet tasklet = config.loadUsageTasklet(
                csvFile.toString(),
                "",
                "local"
        );

        RepeatStatus status = tasklet.execute(null,
                new org.springframework.batch.core.scope.context.ChunkContext(
                        new org.springframework.batch.core.scope.context.StepContext(stepExecution)
                )
        );

        long endTime = System.currentTimeMillis();

        // Then: Should complete successfully
        assertEquals(RepeatStatus.FINISHED, status);

        @SuppressWarnings ("unchecked")
        ConcurrentMap<Integer, ?> usageMap = (ConcurrentMap<Integer, ?>)
                stepExecution.getJobExecution().getExecutionContext().get("usageMap");

        assertNotNull(usageMap);
        assertEquals(1000, usageMap.size(), "Should load all 1000 records");

        // Verify it completed in reasonable time (concurrent processing should be faster)
        long duration = endTime - startTime;
        System.out.println("Loaded 1000 records in " + duration + "ms");
        assertTrue(duration < 5000, "Should complete within 5 seconds with concurrent processing");
    }

    // ==================== Load Pricing Tasklet Tests ====================

    @Test
    @DisplayName("LoadPricingTasklet - Should load pricing from YAML")
    void testLoadPricingFromYAML() throws Exception {
        // When: Execute tasklet
        Tasklet tasklet = config.loadPricingYamlTasklet();

        RepeatStatus status = tasklet.execute(null,
                new org.springframework.batch.core.scope.context.ChunkContext(
                        new org.springframework.batch.core.scope.context.StepContext(stepExecution)
                )
        );

        // Then: Should complete successfully
        assertEquals(RepeatStatus.FINISHED, status);

        // Verify pricingMap is populated
        @SuppressWarnings ("unchecked")
        Map<String, Map<String, Object>> pricingMap = (Map<String, Map<String, Object>>)
                stepExecution.getJobExecution().getExecutionContext().get("pricingMap");

        assertNotNull(pricingMap, "pricingMap should be populated");
        assertFalse(pricingMap.isEmpty(), "pricingMap should contain plans");
    }

    @Test
    @DisplayName("LoadPricingTasklet - Should load all plan details")
    void testLoadPricingAllPlans() throws Exception {
        // When: Execute tasklet
        Tasklet tasklet = config.loadPricingYamlTasklet();

        tasklet.execute(null,
                new org.springframework.batch.core.scope.context.ChunkContext(
                        new org.springframework.batch.core.scope.context.StepContext(stepExecution)
                )
        );

        // Then: Should contain all expected plans
        @SuppressWarnings ("unchecked")
        Map<String, Map<String, Object>> pricingMap = (Map<String, Map<String, Object>>)
                stepExecution.getJobExecution().getExecutionContext().get("pricingMap");

        assertNotNull(pricingMap);
        assertTrue(pricingMap.containsKey("BASIC"), "Should contain BASIC plan");
        assertTrue(pricingMap.containsKey("PRO"), "Should contain PRO plan");
        assertTrue(pricingMap.containsKey("ENTERPRISE"), "Should contain ENTERPRISE plan");
    }

    @Test
    @DisplayName("LoadPricingTasklet - Should load plan pricing details")
    void testLoadPricingPlanDetails() throws Exception {
        // When: Execute tasklet
        Tasklet tasklet = config.loadPricingYamlTasklet();

        tasklet.execute(null,
                new org.springframework.batch.core.scope.context.ChunkContext(
                        new org.springframework.batch.core.scope.context.StepContext(stepExecution)
                )
        );

        // Then: Verify plan details
        @SuppressWarnings ("unchecked")
        Map<String, Map<String, Object>> pricingMap = (Map<String, Map<String, Object>>)
                stepExecution.getJobExecution().getExecutionContext().get("pricingMap");

        Map<String, Object> basicPlan = pricingMap.get("BASIC");
        assertNotNull(basicPlan);
        assertTrue(basicPlan.containsKey(Constants.BASE_PRICE));
        assertTrue(basicPlan.containsKey(Constants.FREE_STORAGE_GB));
        assertTrue(basicPlan.containsKey(Constants.FREE_BANDWIDTH_GB));
        assertTrue(basicPlan.containsKey(Constants.FREE_API_CALLS));
        assertTrue(basicPlan.containsKey(Constants.OVERAGE_STORAGE_GB));
        assertTrue(basicPlan.containsKey(Constants.OVERAGE_BANDWIDTH_GB));
        assertTrue(basicPlan.containsKey(Constants.OVERAGE_API_CALL));
    }

    @Test
    @DisplayName("LoadPricingTasklet - Should return FINISHED status")
    void testLoadPricingReturnsFinished() throws Exception {
        // When
        Tasklet tasklet = config.loadPricingYamlTasklet();

        RepeatStatus status = tasklet.execute(null,
                new org.springframework.batch.core.scope.context.ChunkContext(
                        new org.springframework.batch.core.scope.context.StepContext(stepExecution)
                )
        );

        // Then
        assertEquals(RepeatStatus.FINISHED, status);
    }

    // ==================== Helper Methods ====================

    private Path createTestUsageFile() throws IOException {
        return Path.of ("D:\\Java_data_projects\\InvoiceGeneration\\src\\test\\resources\\test-usage.csv");
    }

    private Path createLargeUsageFile(int recordCount) throws IOException {
        Path csvFile = tempDir.resolve("large-usage.csv");
        StringBuilder content = new StringBuilder();
        content.append("customer_id,storage_used_gb,bandwidth_used_gb,api_calls,peak_usage_date,total_logins\n");

        for (int i = 1; i <= recordCount; i++) {
            content.append(String.format("%d,%d.5,%d.0,%d,2025-01-15,%d\n",
                    i, i * 10, i * 50, i * 1000, i * 10));
        }

        Files.writeString(csvFile, content.toString());
        return csvFile;
    }
}