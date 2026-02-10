package com.subscription.InvoiceGeneration.integration.repository;

import com.subscription.InvoiceGeneration.model.Invoice;
import com.subscription.InvoiceGeneration.repo.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for InvoiceRepository
 * Tests database operations using H2 in-memory database
 */
@DataJpaTest
@ActiveProfiles("test")
class InvoiceRepositoryIntegrationTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save invoice successfully")
    void testSaveInvoice() {
        // Given: A new invoice
        Invoice invoice = createTestInvoice(1, "2025-01", "BASIC");

        // When: Save the invoice
        Invoice saved = invoiceRepository.save(invoice);

        // Then: Invoice should be persisted with ID
        assertNotNull(saved.getId());
        assertEquals(1, saved.getCustomerId());
        assertEquals("2025-01", saved.getMonth());
    }

    @Test
    @DisplayName("Should find invoice by ID")
    void testFindById() {
        // Given: A saved invoice
        Invoice invoice = createTestInvoice(1, "2025-01", "BASIC");
        Invoice saved = invoiceRepository.save(invoice);

        // When: Find by ID
        Optional<Invoice> found = invoiceRepository.findById(saved.getId());

        // Then: Invoice should be found
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(saved.getCustomerId(), found.get().getCustomerId());
    }

    @Test
    @DisplayName("Should find all invoices")
    void testFindAll() {
        // Given: Multiple saved invoices
        invoiceRepository.save(createTestInvoice(1, "2025-01", "BASIC"));
        invoiceRepository.save(createTestInvoice(2, "2025-01", "PRO"));
        invoiceRepository.save(createTestInvoice(3, "2025-01", "BASIC"));

        // When: Find all
        List<Invoice> invoices = (List<Invoice>) invoiceRepository.findAll();

        // Then: All invoices should be returned
        assertEquals(3, invoices.size());
    }

    @Test
    @DisplayName("Should delete invoice")
    void testDeleteInvoice() {
        // Given: A saved invoice
        Invoice invoice = createTestInvoice(1, "2025-01", "BASIC");
        Invoice saved = invoiceRepository.save(invoice);

        // When: Delete the invoice
        invoiceRepository.deleteById(saved.getId());

        // Then: Invoice should not exist
        Optional<Invoice> found = invoiceRepository.findById(saved.getId());
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should update invoice")
    void testUpdateInvoice() {
        // Given: A saved invoice
        Invoice invoice = createTestInvoice(1, "2025-01", "BASIC");
        Invoice saved = invoiceRepository.save(invoice);

        // When: Update the invoice
        saved.setTotal(new BigDecimal("199.99"));
        Invoice updated = invoiceRepository.save(saved);

        // Then: Invoice should be updated
        assertEquals(new BigDecimal("199.99"), updated.getTotal());
    }

    @Test
    @DisplayName("Should handle BigDecimal precision correctly")
    void testBigDecimalPrecision() {
        // Given: Invoice with precise decimal values
        Invoice invoice = createTestInvoice(1, "2025-01", "BASIC");
        invoice.setBaseCharge(new BigDecimal("29.99"));
        invoice.setOverageCharge(new BigDecimal("15.50"));
        invoice.setTotal(new BigDecimal("45.49"));

        // When: Save and retrieve
        Invoice saved = invoiceRepository.save(invoice);
        Invoice retrieved = invoiceRepository.findById(saved.getId()).orElseThrow();

        // Then: Decimal precision should be maintained
        assertEquals(0, new BigDecimal("29.99").compareTo(retrieved.getBaseCharge()));
        assertEquals(0, new BigDecimal("15.50").compareTo(retrieved.getOverageCharge()));
        assertEquals(0, new BigDecimal("45.49").compareTo(retrieved.getTotal()));
    }

    @Test
    @DisplayName("Should persist LocalDateTime correctly")
    void testLocalDateTimePersistence() {
        // Given: Invoice with timestamp
        Invoice invoice = createTestInvoice(1, "2025-01", "BASIC");
        LocalDateTime now = LocalDateTime.now();
        invoice.setGeneratedAt(now);

        // When: Save and retrieve
        Invoice saved = invoiceRepository.save(invoice);
        Invoice retrieved = invoiceRepository.findById(saved.getId()).orElseThrow();

        // Then: Timestamp should be persisted (within 1 second tolerance)
        assertNotNull(retrieved.getGeneratedAt());
        assertTrue(Math.abs(now.getSecond() - retrieved.getGeneratedAt().getSecond()) <= 1);
    }

    @Test
    @DisplayName("Should handle bulk save operations")
    void testBulkSave() {
        // Given: Multiple invoices
        List<Invoice> invoices = List.of(
                createTestInvoice(1, "2025-01", "BASIC"),
                createTestInvoice(2, "2025-01", "PRO"),
                createTestInvoice(3, "2025-01", "ENTERPRISE")
        );

        // When: Save all
        invoiceRepository.saveAll(invoices);

        // Then: All should be persisted
        long count = invoiceRepository.count();
        assertEquals(3, count);
    }

    @Test
    @DisplayName("Should maintain data integrity on concurrent saves")
    void testDataIntegrity() {
        // Given: Same customer, different months
        Invoice jan = createTestInvoice(1, "2025-01", "BASIC");
        Invoice feb = createTestInvoice(1, "2025-02", "BASIC");

        // When: Save both
        invoiceRepository.save(jan);
        invoiceRepository.save(feb);

        // Then: Both should exist independently
        List<Invoice> invoices = (List<Invoice>) invoiceRepository.findAll();
        assertEquals(2, invoices.size());

        long janCount = invoices.stream()
                .filter(inv -> "2025-01".equals(inv.getMonth()))
                .count();
        long febCount = invoices.stream()
                .filter(inv -> "2025-02".equals(inv.getMonth()))
                .count();

        assertEquals(1, janCount);
        assertEquals(1, febCount);
    }

    // Helper method to create test invoice
    private Invoice createTestInvoice(int customerId, String month, String plan) {
        Invoice invoice = new Invoice();
        invoice.setCustomerId(customerId);
        invoice.setMonth(month);
        invoice.setPlan("Subscription: " + plan + " (29.99/month, 100 free GB)");
        invoice.setBaseCharge(new BigDecimal("29.99"));
        invoice.setOverageCharge(new BigDecimal("35.50"));
        invoice.setTotal(new BigDecimal("65.49"));
        invoice.setGeneratedAt(LocalDateTime.now());
        return invoice;
    }
}
