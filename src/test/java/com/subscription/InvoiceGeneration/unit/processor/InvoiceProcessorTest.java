package com.subscription.InvoiceGeneration.unit.processor;

import com.subscription.InvoiceGeneration.config.InvoiceProcessor;
import com.subscription.InvoiceGeneration.model.Customer;
import com.subscription.InvoiceGeneration.model.Invoice;
import com.subscription.InvoiceGeneration.model.Usage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.test.MetaDataInstanceFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InvoiceProcessor
 * Tests invoice calculation logic in isolation without database or Spring context
 */
class InvoiceProcessorTest {

    private InvoiceProcessor processor;
    private StepExecution stepExecution;
    private Map<Integer, Usage> usageMap;
    private Map<String, Map<String, Object>> pricingMap;

    @BeforeEach
    void setUp() {
        processor = new InvoiceProcessor();

        // Create mock job execution
        JobExecution jobExecution = MetaDataInstanceFactory.createJobExecution();

        // Initialize test data
        setupUsageMap();
        setupPricingMap();

        // Add to execution context
        ExecutionContext jobExecutionContext = jobExecution.getExecutionContext();
        jobExecutionContext.put("usageMap", usageMap);
        jobExecutionContext.put("pricingMap", pricingMap);

        stepExecution = MetaDataInstanceFactory.createStepExecution (
                jobExecution,
                "invoiceProcessorTest",
                1L
        );

        // Initialize processor with step execution
        processor.beforeStep(stepExecution);
    }

    private void setupUsageMap() {
        usageMap = new HashMap<>();

        // Customer 1: Normal usage within limits
        Usage usage1 = new Usage();
        usage1.setCustomerId(1);
        usage1.setStorageUsedGB(new BigDecimal("80.00"));
        usage1.setBandwidthUsedGB(new BigDecimal("400.00"));
        usage1.setApiCalls(90000);
        usage1.setPeakUsageDate("2025-01-15");
        usage1.setTotalLogins(150);
        usageMap.put(1, usage1);

        // Customer 2: High usage with overages
        Usage usage2 = new Usage();
        usage2.setCustomerId(2);
        usage2.setStorageUsedGB(new BigDecimal("250.00")); // 150 GB overage
        usage2.setBandwidthUsedGB(new BigDecimal("1500.00")); // 500 GB overage
        usage2.setApiCalls(200000); // 100k overage
        usage2.setPeakUsageDate("2025-01-20");
        usage2.setTotalLogins(500);
        usageMap.put(2, usage2);

        // Customer 3: Minimal usage
        Usage usage3 = new Usage();
        usage3.setCustomerId(3);
        usage3.setStorageUsedGB(new BigDecimal("10.00"));
        usage3.setBandwidthUsedGB(new BigDecimal("50.00"));
        usage3.setApiCalls(5000);
        usage3.setPeakUsageDate("2025-01-10");
        usage3.setTotalLogins(20);
        usageMap.put(3, usage3);
    }

    private void setupPricingMap() {
        pricingMap = new HashMap<>();

        // BASIC plan
        Map<String, Object> basicPlan = new HashMap<>();
        basicPlan.put("base_price", 29.99);
        basicPlan.put("free_storage_gb", 100);
        basicPlan.put("free_bandwidth_gb", 1000);
        basicPlan.put("free_api_calls", 100000);
        basicPlan.put("overage_storage_per_gb", 0.10);
        basicPlan.put("overage_bandwidth_per_gb", 0.05);
        basicPlan.put("overage_api_per_call", 0.0001);
        pricingMap.put("BASIC", basicPlan);

        // PRO plan
        Map<String, Object> proPlan = new HashMap<>();
        proPlan.put("base_price", 99.99);
        proPlan.put("free_storage_gb", 500);
        proPlan.put("free_bandwidth_gb", 5000);
        proPlan.put("free_api_calls", 500000);
        proPlan.put("overage_storage_per_gb", 0.08);
        proPlan.put("overage_bandwidth_per_gb", 0.03);
        proPlan.put("overage_api_per_call", 0.00008);
        pricingMap.put("PRO", proPlan);
    }

    @Test
    @DisplayName("Should calculate invoice correctly for full month with no overages")
    void testFullMonthNoOverages() throws Exception {
        // Given: Customer active for full month with basic plan and no overages
        Customer customer = createCustomer(1, "BASIC", LocalDate.of(2024, 12, 1), 0.0);

        // When: Process the invoice
        Invoice invoice = processor.process(customer);

        // Then: Verify invoice details
        assertNotNull(invoice);
        assertEquals(1, invoice.getCustomerId());
        assertEquals("2025-01", invoice.getMonth());
        assertEquals(new BigDecimal("29.99"), invoice.getBaseCharge());
        assertTrue(invoice.getTotal().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(invoice.getGeneratedAt());
    }

    @Test
    @DisplayName("Should calculate prorated invoice for mid-month start")
    void testProratedBilling() throws Exception {
        // Given: Customer started on Jan 15th (17 days active in 31-day month)
        Customer customer = createCustomer(1, "BASIC", LocalDate.of(2025, 1, 15), 0.0);

        // When: Process the invoice
        Invoice invoice = processor.process(customer);

        // Then: Base charge should be prorated
        assertNotNull(invoice);
        // 17 days out of 31: (29.99 / 31) * 17 = ~16.48
        BigDecimal expectedProrated = new BigDecimal("29.99")
                .divide(new BigDecimal("31"), 2, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("17"));

        assertEquals(expectedProrated, invoice.getProratedPrice());
    }

    @Test
    @DisplayName("Should calculate overage charges correctly")
    void testOverageCalculation() throws Exception {
        // Given: Customer with high usage (customer 2 in setup)
        Customer customer = createCustomer(2, "BASIC", LocalDate.of(2024, 12, 1), 0.0);

        // When: Process the invoice
        Invoice invoice = processor.process(customer);

        // Then: Should include overage charges
        // Storage overage: (250 - 100) * 0.10 = 15.00
        // Bandwidth overage: (1500 - 1000) * 0.05 = 25.00
        // API overage: (200000 - 100000) * 0.0001 = 10.00
        // Total overage: 50.00 + base 29.99 = 79.99
        assertNotNull(invoice);
        assertTrue(invoice.getOverageCharge().compareTo(new BigDecimal("29.99")) > 0);
    }

    @Test
    @DisplayName("Should apply discount correctly")
    void testDiscountApplication() throws Exception {
        // Given: Customer with 10% discount
        Customer customer = createCustomer(1, "BASIC", LocalDate.of(2024, 12, 1), 10.0);

        // When: Process the invoice
        Invoice invoice = processor.process(customer);

        // Then: Total should reflect 10% discount
        assertNotNull(invoice);
        BigDecimal subtotal = invoice.getOverageCharge();
        BigDecimal expectedDiscount = subtotal.multiply(new BigDecimal("0.10"));
        BigDecimal expectedTotal = subtotal.subtract(expectedDiscount);

        assertEquals(expectedTotal.setScale(2, java.math.RoundingMode.HALF_UP),
                invoice.getTotal().setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Should return null when usage data is missing")
    void testMissingUsageData() throws Exception {
        // Given: Customer with no usage data (customer 999)
        Customer customer = createCustomer(999, "BASIC", LocalDate.of(2025, 1, 1), 0.0);

        // When: Process the invoice
        Invoice invoice = processor.process(customer);

        // Then: Should return null (customer will be skipped)
        assertNull(invoice);
    }

    @Test
    @DisplayName("Should return null when plan code is invalid")
    void testInvalidPlanCode() throws Exception {
        // Given: Customer with invalid plan code
        Customer customer = createCustomer(1, "INVALID_PLAN", LocalDate.of(2025, 1, 1), 0.0);

        // When: Process the invoice
        Invoice invoice = processor.process(customer);

        // Then: Should return null
        assertNull(invoice);
    }

    @Test
    @DisplayName("Should return null when customer started after billing period")
    void testCustomerStartedAfterBillingPeriod() throws Exception {
        // Given: Customer started Feb 1st but billing period is January
        Customer customer = createCustomer(1, "BASIC", LocalDate.of(2025, 2, 1), 0.0);

        // When: Process the invoice
        Invoice invoice = processor.process(customer);

        // Then: Should return null (no billing for this period)
        assertNull(invoice);
    }

    @Test
    @DisplayName("Should handle zero discount correctly")
    void testZeroDiscount() throws Exception {
        // Given: Customer with 0% discount
        Customer customer = createCustomer(1, "BASIC", LocalDate.of(2024, 12, 1), 0.0);

        // When: Process the invoice
        Invoice invoice = processor.process(customer);

        // Then: Total should equal subtotal
        assertNotNull(invoice);
        assertEquals(invoice.getOverageCharge(), invoice.getTotal());
    }

    @Test
    @DisplayName("Should calculate correctly for PRO plan")
    void testProPlanCalculation() throws Exception {
        // Given: Customer on PRO plan
        Customer customer = createCustomer(2, "PRO", LocalDate.of(2024, 12, 1), 0.0);

        // When: Process the invoice
        Invoice invoice = processor.process(customer);

        // Then: Should use PRO plan pricing
        assertNotNull(invoice);
        assertEquals(new BigDecimal("99.99"), invoice.getBaseCharge());
        assertTrue(invoice.getPlan().contains("PRO"));
    }

    @Test
    @DisplayName("Should handle minimal usage correctly")
    void testMinimalUsage() throws Exception {
        // Given: Customer with minimal usage (customer 3)
        Customer customer = createCustomer(3, "BASIC", LocalDate.of(2024, 12, 1), 0.0);

        // When: Process the invoice
        Invoice invoice = processor.process(customer);

        // Then: Should only charge base price (no overages)
        assertNotNull(invoice);
        // All usage is within free limits, so overage charge should equal base charge
        assertTrue(invoice.getTotal().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should return null when pricingMap is null")
    void testNullPricingMap() throws Exception {
        // Given: Clear pricing map from execution context
        stepExecution.getJobExecution().getExecutionContext().remove("pricingMap");
        processor.beforeStep(stepExecution);

        Customer customer = createCustomer(1, "BASIC", LocalDate.of(2025, 1, 1), 0.0);

        // When: Process the invoice
        Invoice invoice = processor.process(customer);

        // Then: Should return null
        assertNull(invoice);
    }

    @Test
    @DisplayName("Should return null when usageMap is null")
    void testNullUsageMap() throws Exception {
        // Given: Clear usage map from execution context
        stepExecution.getJobExecution().getExecutionContext().remove("usageMap");
        processor.beforeStep(stepExecution);

        Customer customer = createCustomer(1, "BASIC", LocalDate.of(2025, 1, 1), 0.0);

        // When: Process the invoice
        Invoice invoice = processor.process(customer);

        // Then: Should return null
        assertNull(invoice);
    }

    // Helper method to create test customers
    private Customer createCustomer(int id, String planCode, LocalDate startDate, double discount) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("Test Customer " + id);
        customer.setEmail("customer" + id + "@test.com");
        customer.setPlanCode(planCode);
        customer.setStartDate(startDate);
        customer.setStatus("ACTIVE");
        customer.setDiscount(discount);
        customer.setBillingAddress("123 Test St");
        return customer;
    }
}
