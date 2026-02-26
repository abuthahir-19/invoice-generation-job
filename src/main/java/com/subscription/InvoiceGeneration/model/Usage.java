package com.subscription.InvoiceGeneration.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;


@Entity
@Table (name = "usage")
public class Usage {
    @Id
    private Integer customerId;
    private BigDecimal storageUsedGB;
    private BigDecimal bandwidthUsedGB;
    private Integer apiCalls;
    private String peakUsageDate;
    private Integer totalLogins;

    public Usage () {}

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getStorageUsedGB() {
        return storageUsedGB;
    }

    public void setStorageUsedGB(BigDecimal storageUsedGB) {
        this.storageUsedGB = storageUsedGB;
    }

    public BigDecimal getBandwidthUsedGB() {
        return bandwidthUsedGB;
    }

    public void setBandwidthUsedGB(BigDecimal bandwidthUsedGB) {
        this.bandwidthUsedGB = bandwidthUsedGB;
    }

    public Integer getApiCalls() {
        return apiCalls;
    }

    public void setApiCalls(Integer apiCalls) {
        this.apiCalls = apiCalls;
    }

    public String getPeakUsageDate() {
        return peakUsageDate;
    }

    public void setPeakUsageDate(String peakUsageDate) {
        this.peakUsageDate = peakUsageDate;
    }

    public Integer getTotalLogins() {
        return totalLogins;
    }

    public void setTotalLogins(Integer totalLogins) {
        this.totalLogins = totalLogins;
    }

    @Override
    public String toString() {
        return "Usage{" +
                "customerId=" + customerId +
                ", storageUsedGB=" + storageUsedGB +
                ", bandwidthUsedGB=" + bandwidthUsedGB +
                ", apiCalls=" + apiCalls +
                ", peakUsageDate='" + peakUsageDate + '\'' +
                ", totalLogins=" + totalLogins +
                '}';
    }
}
