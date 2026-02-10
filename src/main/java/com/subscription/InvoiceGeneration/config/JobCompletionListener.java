package com.subscription.InvoiceGeneration.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class JobCompletionListener implements JobExecutionListener {
    private static final Logger LOG = LogManager.getLogger();

    @Value ("${spring.profiles.active:default}")
    private String activeProfile;

    @Autowired
    private ApplicationContext context;

    @Override
    public void afterJob (@NonNull JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            LOG.info ("All jobs completed successfully. Shutting down the application !!");
            SpringApplication.exit (context, () -> 0);
        } else {
            LOG.info ("May be some jobs could be failed. Manual intervention required..");
            SpringApplication.exit (context, () -> 1);
        }
    }
}
