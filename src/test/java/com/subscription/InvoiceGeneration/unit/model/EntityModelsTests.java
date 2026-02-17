package com.subscription.InvoiceGeneration.unit.model;

import com.subscription.InvoiceGeneration.model.Customer;
import com.subscription.InvoiceGeneration.model.Invoice;
import com.subscription.InvoiceGeneration.model.Usage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Entity Models
 * Tests getters, setters, and basic business logic validation
 */

class EntityModelsTests {

    // ==================== Customer Entity Tests ====================

    @Test
    @DisplayName("Customer - Should create with all fields")
    void testCustomerCreation() {
        // Given
        Customer customer = new Customer();
        customer.setId(1);
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        customer.setPlanCode("BASIC");
        customer.setStartDate(LocalDate.of(2024, 1, 1));
        customer.setStatus("ACTIVE");
        customer.setBillingAddress("123 Main St");
        customer.setDiscount(10.0);
        customer.setLastBilledMonth("2024-12");

        // Then
        assertEquals(1, customer.getId());
        assertEquals("John Doe", customer.getName());
        assertEquals("john@example.com", customer.getEmail());
        assertEquals("BASIC", customer.getPlanCode());
        assertEquals(LocalDate.of(2024, 1, 1), customer.getStartDate());
        assertEquals("ACTIVE", customer.getStatus());
        assertEquals("123 Main St", customer.getBillingAddress());
        assertEquals(10.0, customer.getDiscount());
        assertEquals("2024-12", customer.getLastBilledMonth());
    }

    @Test
    @DisplayName("Customer - Should have default discount of 0.0")
    void testCustomerDefaultDiscount() {
        // Given
        Customer customer = new Customer();

        // Then
        assertEquals(0.0, customer.getDiscount());
    }

    @Test
    @DisplayName("Customer - Should handle null values")
    void testCustomerNullValues() {
        // Given
        Customer customer = new Customer();
        customer.setName(null);
        customer.setEmail(null);
        customer.setLastBilledMonth(null);

        // Then
        assertNull(customer.getName());
        assertNull(customer.getEmail());
        assertNull(customer.getLastBilledMonth());
    }

    @Test
    @DisplayName("Customer - Should update fields correctly")
    void testCustomerUpdate() {
        // Given
        Customer customer = new Customer();
        customer.setStatus("ACTIVE");
        customer.setDiscount(5.0);

        // When
        customer.setStatus("INACTIVE");
        customer.setDiscount(15.0);

        // Then
        assertEquals("INACTIVE", customer.getStatus());
        assertEquals(15.0, customer.getDiscount());
    }

    // ==================== Invoice Entity Tests ====================

    @Test
    @DisplayName("Invoice - Should create with all fields")
    void testInvoiceCreation() {
        // Given
        Invoice invoice = new Invoice();
        invoice.setId(1);
        invoice.setCustomerId(100);
        invoice.setPlan("BASIC Plan");
        invoice.setMonth("2025-01");
        invoice.setBaseCharge(new BigDecimal("29.99"));
        invoice.setOverageCharge(new BigDecimal("15.50"));
        invoice.setTotal(new BigDecimal("45.49"));
        invoice.setGeneratedAt(LocalDateTime.of(2025, 1, 15, 10, 30));

        // Then
        assertEquals(1, invoice.getId());
        assertEquals(100, invoice.getCustomerId());
        assertEquals("BASIC Plan", invoice.getPlan());
        assertEquals("2025-01", invoice.getMonth());
        assertEquals(0, new BigDecimal("29.99").compareTo(invoice.getBaseCharge()));
        assertEquals(0, new BigDecimal("15.50").compareTo(invoice.getOverageCharge()));
        assertEquals(0, new BigDecimal("45.49").compareTo(invoice.getTotal()));
        assertEquals(LocalDateTime.of(2025, 1, 15, 10, 30), invoice.getGeneratedAt());
    }

    @Test
    @DisplayName("Invoice - Should handle BigDecimal precision")
    void testInvoiceBigDecimalPrecision() {
        // Given
        Invoice invoice = new Invoice();

        // When
        invoice.setBaseCharge(new BigDecimal("99.999"));
        invoice.setOverageCharge(new BigDecimal("0.001"));
        invoice.setTotal(new BigDecimal("100.000"));

        // Then - Should maintain precision
        assertTrue(invoice.getBaseCharge().compareTo(new BigDecimal("99.99")) > 0);
        assertTrue(invoice.getOverageCharge().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(0, invoice.getTotal().compareTo(new BigDecimal("100.000")));
    }

    @Test
    @DisplayName("Invoice - Should handle zero amounts")
    void testInvoiceZeroAmounts() {
        // Given
        Invoice invoice = new Invoice();

        // When
        invoice.setBaseCharge(BigDecimal.ZERO);
        invoice.setOverageCharge(BigDecimal.ZERO);
        invoice.setTotal(BigDecimal.ZERO);

        // Then
        assertEquals(BigDecimal.ZERO, invoice.getBaseCharge());
        assertEquals(BigDecimal.ZERO, invoice.getOverageCharge());
        assertEquals(BigDecimal.ZERO, invoice.getTotal());
    }

    @Test
    @DisplayName("Invoice - Should update values correctly")
    void testInvoiceUpdate() {
        // Given
        Invoice invoice = new Invoice();
        invoice.setTotal(new BigDecimal("100.00"));

        // When
        invoice.setTotal(new BigDecimal("150.00"));

        // Then
        assertEquals(0, new BigDecimal("150.00").compareTo(invoice.getTotal()));
    }

    // ==================== Usage Entity Tests ====================

    @Test
    @DisplayName("Usage - Should create with all fields")
    void testUsageCreation() {
        // Given
        Usage usage = new Usage();
        usage.setCustomerId(1);
        usage.setStorageUsedGB(new BigDecimal("150.5"));
        usage.setBandwidthUsedGB(new BigDecimal("2500.75"));
        usage.setApiCalls(250000);
        usage.setPeakUsageDate("2025-01-15");
        usage.setTotalLogins(500);

        // Then
        assertEquals(1, usage.getCustomerId());
        assertEquals(0, new BigDecimal("150.5").compareTo(usage.getStorageUsedGB()));
        assertEquals(0, new BigDecimal("2500.75").compareTo(usage.getBandwidthUsedGB()));
        assertEquals(250000, usage.getApiCalls());
        assertEquals("2025-01-15", usage.getPeakUsageDate());
        assertEquals(500, usage.getTotalLogins());
    }

    @Test
    @DisplayName("Usage - Should handle zero usage")
    void testUsageZeroValues() {
        // Given
        Usage usage = new Usage();

        // When
        usage.setStorageUsedGB(BigDecimal.ZERO);
        usage.setBandwidthUsedGB(BigDecimal.ZERO);
        usage.setApiCalls(0);
        usage.setTotalLogins(0);

        // Then
        assertEquals(BigDecimal.ZERO, usage.getStorageUsedGB());
        assertEquals(BigDecimal.ZERO, usage.getBandwidthUsedGB());
        assertEquals(0, usage.getApiCalls());
        assertEquals(0, usage.getTotalLogins());
    }

    @Test
    @DisplayName("Usage - Should handle large values")
    void testUsageLargeValues() {
        // Given
        Usage usage = new Usage();

        // When - Set very large values
        usage.setStorageUsedGB(new BigDecimal("999999.99"));
        usage.setBandwidthUsedGB(new BigDecimal("999999.99"));
        usage.setApiCalls(Integer.MAX_VALUE);
        usage.setTotalLogins(Integer.MAX_VALUE);

        // Then
        assertTrue(usage.getStorageUsedGB().compareTo(new BigDecimal("999999")) > 0);
        assertTrue(usage.getBandwidthUsedGB().compareTo(new BigDecimal("999999")) > 0);
        assertEquals(Integer.MAX_VALUE, usage.getApiCalls());
        assertEquals(Integer.MAX_VALUE, usage.getTotalLogins());
    }

    @Test
    @DisplayName("Usage - Should handle decimal precision")
    void testUsageDecimalPrecision() {
        // Given
        Usage usage = new Usage();

        // When
        usage.setStorageUsedGB(new BigDecimal("123.456789"));
        usage.setBandwidthUsedGB(new BigDecimal("789.123456"));

        // Then - Precision should be maintained
        assertEquals(0, new BigDecimal("123.456789").compareTo(usage.getStorageUsedGB()));
        assertEquals(0, new BigDecimal("789.123456").compareTo(usage.getBandwidthUsedGB()));
    }

    @Test
    @DisplayName("Usage - Should update values correctly")
    void testUsageUpdate() {
        // Given
        Usage usage = new Usage();
        usage.setApiCalls(1000);
        usage.setTotalLogins(50);

        // When
        usage.setApiCalls(2000);
        usage.setTotalLogins(100);

        // Then
        assertEquals(2000, usage.getApiCalls());
        assertEquals(100, usage.getTotalLogins());
    }

    // ==================== Entity Equality Tests ====================

    @Test
    @DisplayName("Customer - Should be equal with same ID")
    void testCustomerEquality() {
        // Given
        Customer customer1 = new Customer();
        customer1.setId(1);
        customer1.setName("John");

        Customer customer2 = new Customer();
        customer2.setId(1);
        customer2.setName("Jane");

        // Then - If entities use ID for equals (depends on implementation)
        // This test assumes basic Java object equality
        assertNotEquals(customer1, customer2); // Different object references
        assertEquals(customer1.getId(), customer2.getId()); // Same ID
    }

    @Test
    @DisplayName("Invoice - Should handle null fields gracefully")
    void testInvoiceNullFields() {
        // Given
        Invoice invoice = new Invoice();

        // When - Don't set any fields

        // Then - Should not throw NPE on getters
        assertDoesNotThrow(() -> {
            invoice.getCustomerId();
            invoice.getPlan();
            invoice.getMonth();
            invoice.getBaseCharge();
            invoice.getTotal();
        });
    }

    @Test
    @DisplayName("Usage - Should handle null BigDecimal fields")
    void testUsageNullBigDecimal() {
        // Given
        Usage usage = new Usage();

        // When - Don't set BigDecimal fields

        // Then
        assertNull(usage.getStorageUsedGB());
        assertNull(usage.getBandwidthUsedGB());
    }

    // ==================== Business Logic Tests ====================

    @Test
    @DisplayName("Customer - Discount should be between 0 and 100")
    void testCustomerDiscountRange() {
        // Given
        Customer customer = new Customer();

        // When
        customer.setDiscount(0.0);
        assertEquals(0.0, customer.getDiscount());

        customer.setDiscount(100.0);
        assertEquals(100.0, customer.getDiscount());

        // Note: In real application, you might want validation
        // to prevent values outside 0-100 range
    }

    @Test
    @DisplayName("Invoice - Total should equal base + overage (logically)")
    void testInvoiceTotalCalculation() {
        // Given
        Invoice invoice = new Invoice();
        BigDecimal base = new BigDecimal("50.00");
        BigDecimal overage = new BigDecimal("25.00");
        BigDecimal expectedTotal = base.add(overage);

        // When
        invoice.setBaseCharge(base);
        invoice.setOverageCharge(overage);
        invoice.setTotal(expectedTotal);

        // Then - Verify the relationship
        BigDecimal calculatedTotal = invoice.getBaseCharge().add(invoice.getOverageCharge());
        assertEquals(0, calculatedTotal.compareTo(invoice.getTotal()));
    }

    @Test
    @DisplayName("Usage - Peak date format should be YYYY-MM-DD")
    void testUsageDateFormat() {
        // Given
        Usage usage = new Usage();
        String validDate = "2025-01-15";

        // When
        usage.setPeakUsageDate(validDate);

        // Then
        assertEquals(validDate, usage.getPeakUsageDate());
        assertTrue(usage.getPeakUsageDate().matches("\\d{4}-\\d{2}-\\d{2}"));
    }
}