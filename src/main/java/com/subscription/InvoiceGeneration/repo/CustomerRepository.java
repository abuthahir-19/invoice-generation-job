package com.subscription.InvoiceGeneration.repo;

import com.subscription.InvoiceGeneration.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
}
