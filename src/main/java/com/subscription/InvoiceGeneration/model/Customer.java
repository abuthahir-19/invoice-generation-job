package com.subscription.InvoiceGeneration.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table (name = "customer_test")
public class Customer {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private String email;
    private String planCode;
    private LocalDate startDate;
    private String status;
    private String billingAddress;
    @Column (name = "discount", nullable = false)
    private Double discount = 0.0;
    private String lastBilledMonth;

    public Customer () { }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public Double getDiscount () {
        return discount;
    }

    public void setDiscount (Double discount) {
        this.discount = discount;
    }

    public String getLastBilledMonth() {
        return lastBilledMonth;
    }

    public void setLastBilledMonth(String lastBilledMonth) {
        this.lastBilledMonth = lastBilledMonth;
    }
}
