package com.subscription.InvoiceGeneration.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigDebugLogger {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigDebugLogger.class);

    // These will read from application.properties (which gets values from -D properties)
    @Value("${spring.cloud.gcp.sql.instance-connection-name:NOT_SET}")
    private String cloudSqlInstance;

    @Value("${spring.cloud.gcp.sql.database-name:NOT_SET}")
    private String databaseName;

    @Value("${spring.datasource.username:NOT_SET}")
    private String dbUsername;

    @Value("${spring.datasource.url:NOT_SET}")
    private String jdbcUrl;

    @Value("${spring.datasource.password:NOT_SET}")
    private String dbPassword;

    @PostConstruct
    public void logConfigAtStartup() {
        LOG.info("=== RUNTIME CONFIG DEBUG - START ===");
        LOG.info("spring.cloud.gcp.sql.instance-connection-name = {}", cloudSqlInstance);
        LOG.info("spring.cloud.gcp.sql.database-name             = {}", databaseName);
        LOG.info("spring.datasource.username                    = {}", dbUsername);
        LOG.info("spring.datasource.url                         = {}", jdbcUrl);
        LOG.info("spring.datasource.password                         = {}", dbPassword);
        LOG.info("=== RUNTIME CONFIG DEBUG - END ===");
    }
}