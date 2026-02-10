package com.subscription.InvoiceGeneration.config;

import com.subscription.InvoiceGeneration.model.Customer;
import com.subscription.InvoiceGeneration.model.Invoice;
import com.subscription.InvoiceGeneration.model.Usage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static com.subscription.InvoiceGeneration.constants.Constants.*;

public class InvoiceProcessor implements ItemProcessor<Customer, Invoice>, StepExecutionListener {
    private StepExecution stepExecution;
    private static final Logger LOG = LogManager.getLogger();

    @Override
    public void beforeStep (@NonNull StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Override
    public ExitStatus afterStep (@NonNull StepExecution stepExecution) {
        return null;
    }

    @Override
    public Invoice process (Customer customer) throws Exception {
        @SuppressWarnings ("unchecked")
        Map<Integer, Usage> usageMap = (Map<Integer, Usage>) stepExecution.getJobExecution().getExecutionContext().get ("usageMap");

        @SuppressWarnings ("unchecked")
        Map<String, Map<String, Object>> pricingMap = (Map<String, Map<String, Object>>) stepExecution.getJobExecution().getExecutionContext().get ("pricingMap");
        if (pricingMap == null) {
            LOG.error ("Skipping for the customer {}: Since pricingMap are null", customer.getId());
            return null;
        }

        Map<String, Object> planDetails = pricingMap.get (customer.getPlanCode());

        if (planDetails == null) {
            LOG.error ("Skipping for the customer {} and planCode {}: Since planDetails are null", customer.getId(), customer.getPlanCode());
            return null;
        }

        if (usageMap == null) {
            LOG.error ("Skipping for the customer {}: Since usageMap are null", customer.getId());
            return null;
        }

        Usage usage = usageMap.get (customer.getId());
        if (usage == null) {
//            LOG.error ("Skipping for the customer {}: Since usage are null", customer.getId());
            return null;
        }

        String billingMonthStr = usage.getPeakUsageDate().substring (0, 7);
        YearMonth month = YearMonth.parse (billingMonthStr);
        LocalDate billingStart = month.atDay (1);
        LocalDate billingEnd = month.atEndOfMonth();
        int daysInMonth = month.lengthOfMonth();

        LocalDate effectiveStart = customer.getStartDate().isBefore (billingStart) ? billingStart : customer.getStartDate();
        if (effectiveStart.isAfter (billingEnd)) {
//            LOG.error ("Skipping customer {}: Subscription started after the billing end", customer.getId());
            return null;
        }
        long daysActive = ChronoUnit.DAYS.between (effectiveStart, billingEnd) + 1;
        BigDecimal planPrice = new BigDecimal (planDetails.get (BASE_PRICE).toString());
        BigDecimal proratedPrice = planPrice
                .divide (BigDecimal.valueOf (daysInMonth), 2, RoundingMode.HALF_UP)
                .multiply (BigDecimal.valueOf (daysActive));

        BigDecimal storageOverage = calculateOverage (
                usage.getStorageUsedGB(),
                new BigDecimal (planDetails.get (FREE_STORAGE_GB).toString()),
                new BigDecimal (planDetails.get (OVERAGE_STORAGE_GB).toString())
        );

        BigDecimal bandwidthOverage = calculateOverage (
                usage.getBandwidthUsedGB(),
                new BigDecimal (planDetails.get (FREE_BANDWIDTH_GB).toString()),
                new BigDecimal (planDetails.get (OVERAGE_BANDWIDTH_GB).toString())
        );

        BigDecimal apiOverage = calculateOverage (
                new BigDecimal (usage.getApiCalls()),
                new BigDecimal (planDetails.get (FREE_API_CALLS).toString()),
                new BigDecimal (planDetails.get (OVERAGE_API_CALL).toString())
        );

        BigDecimal subTotal = proratedPrice.add (storageOverage).add (bandwidthOverage).add (apiOverage);
        BigDecimal discount = subTotal.multiply (new BigDecimal (customer.getDiscount()).divide (BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        BigDecimal total = subTotal.subtract (discount).setScale(2, RoundingMode.HALF_UP);

        Invoice invoice = new Invoice();
        invoice.setCustomerId (customer.getId());
        invoice.setMonth (usage.getPeakUsageDate().substring (0, 7));
        invoice.setPlan ("Subscription : "  + customer.getPlanCode() + " (" + planDetails.get (BASE_PRICE) + "/month,"+ planDetails.get (FREE_STORAGE_GB) + " free GB)");
        invoice.setBaseCharge (new BigDecimal (planDetails.get (BASE_PRICE).toString()));
        invoice.setProratedPrice (proratedPrice);
        invoice.setOverageCharge (subTotal);
        invoice.setTotal (total);
        invoice.setGeneratedAt (LocalDateTime.now());

        return invoice;
    }

    private static BigDecimal calculateOverage (BigDecimal used, BigDecimal free, BigDecimal rate) {
        return used.subtract (free).max (BigDecimal.ZERO).multiply (rate).setScale (2, RoundingMode.HALF_UP);
    }
}
