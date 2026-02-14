package com.subscription.InvoiceGeneration.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table (name = "invoice")
public class Invoice {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer customerId;
    private String plan;
    @Column (name = "billing_month")
    private String month;
    private BigDecimal baseCharge;
    private BigDecimal proratedPrice;
    private BigDecimal overageCharge;
    private BigDecimal total;
    private LocalDateTime generatedAt;

    public Invoice () { }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public BigDecimal getBaseCharge() {
        return baseCharge;
    }

    public void setBaseCharge(BigDecimal baseCharge) {
        this.baseCharge = baseCharge;
    }

    public BigDecimal getProratedPrice() {
        return proratedPrice;
    }

    public void setProratedPrice(BigDecimal proratedPrice) {
        this.proratedPrice = proratedPrice;
    }

    public BigDecimal getOverageCharge() {
        return overageCharge;
    }

    public void setOverageCharge(BigDecimal overageCharge) {
        this.overageCharge = overageCharge;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
