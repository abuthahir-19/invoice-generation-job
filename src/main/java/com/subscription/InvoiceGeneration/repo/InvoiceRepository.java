package com.subscription.InvoiceGeneration.repo;

import com.subscription.InvoiceGeneration.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
}
