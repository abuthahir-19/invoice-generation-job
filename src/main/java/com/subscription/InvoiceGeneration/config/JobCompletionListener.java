package com.subscription.InvoiceGeneration.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class JobCompletionListener implements JobExecutionListener {
    private static final Logger LOG = LogManager.getLogger();

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext context;

    @Override
    public void afterJob (@NonNull JobExecution jobExecution) {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isTestProfile = Arrays.asList (activeProfiles).contains ("test");

        LOG.info ("Active profiles : {}", Arrays.asList (activeProfiles));

        if (isTestProfile) {
            LOG.info ("Test profile detected. Skipping application shutdown");
            return;
        }

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            LOG.info ("All jobs completed successfully. Shutting down the application !!");
            SpringApplication.exit (context, () -> 0);
        } else {
            LOG.info ("May be some jobs could be failed. Manual intervention required..");
            SpringApplication.exit (context, () -> 1);
        }
    }
}
