package com.subscription.InvoiceGeneration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;

@SpringBootApplication
@EnableScheduling
public class InvoiceGenerationApplication implements CommandLineRunner {
	private static final Logger LOG = LogManager.getLogger();
	private static String filePath;
	private static String service_acct_key;
	private static String env;

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

	@Autowired (required = false)
	private JobOperator jobOperator;

	@Autowired (required = false)
	private Job invoiceProcessingJob;

	public static void main(String[] args) {
		SpringApplication.run(InvoiceGenerationApplication.class, args);
	}

	@Override
	public void run (String... args) throws Exception {
		LOG.info("=== RUNTIME CONFIG DEBUG - START ===");
		LOG.info("spring.cloud.gcp.sql.instance-connection-name = {}", cloudSqlInstance);
		LOG.info("spring.cloud.gcp.sql.database-name             = {}", databaseName);
		LOG.info("spring.datasource.username                    = {}", dbUsername);
		LOG.info("spring.datasource.url                         = {}", jdbcUrl);
		LOG.info("spring.datasource.password                         = {}", dbPassword);
		LOG.info("=== RUNTIME CONFIG DEBUG - END ===");

		Instant startTime = Instant.now();

		if (args.length != 3) {
			LOG.error ("Illegal Argument : Expected 3 but found {}, Args: <file_path><space><service_acct_key><space><env>", args.length);
		}
		filePath = args[0];
		service_acct_key = args[1];
		env  = args[2];
		LOG.info ("File : {}", filePath);
		LOG.info ("service_acct_key : {}", service_acct_key);
		LOG.info ("Environment : {}", env);
		LOG.info ("JVM Memory : {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);

		loadInvoiceGenerationJob();

		Instant endTime = Instant.now();
		Duration duration = Duration.between (startTime, endTime);

		long minutes = duration.toMinutes();
		long seconds = duration.toSeconds() % 60;

		System.out.println ("Total Execution Time : " + minutes + " mins " + seconds + " seconds.");
	}

	@Scheduled (cron = "0 10 0 * * ?")
	public void loadInvoiceGenerationJob () throws Exception {
		JobParameters params = new JobParametersBuilder()
				.addString ("usage", filePath)
				.addString ("service_acct_key", service_acct_key)
				.addString ("env", env)
				.addLong ("runTime", System.currentTimeMillis())
				.toJobParameters();

		LOG.info ("Starting the Batch Job with the file : {}", filePath);
		JobExecution jobExecution = jobOperator.start(invoiceProcessingJob, params);

		System.out.println ("======== Job Execution Summary ========");
		System.out.println ("Status: " + jobExecution.getStatus());
		System.out.println ("Exit Description : " + jobExecution.getExitStatus().getExitDescription());
		System.out.println ("Read count : " + jobExecution.getStepExecutions().stream().mapToDouble (StepExecution::getReadCount).sum());
		System.out.println ("Write Count : " + jobExecution.getStepExecutions().stream().mapToDouble (StepExecution::getWriteCount).sum());
		System.out.println ("Skip Count : " + jobExecution.getStepExecutions().stream().mapToDouble (StepExecution::getSkipCount).sum());
		System.out.println ("All Step details :");
		jobExecution.getStepExecutions().forEach(step ->
				System.out.println("  Step '" + step.getStepName() + "': " + step.getStatus() +
						", Read: " + step.getReadCount() + ", Write: " + step.getWriteCount() +
						", Skip: " + step.getSkipCount()));

		if (jobExecution.getStatus().isUnsuccessful()) {
			System.out.println("Job failed! Check logs for errors.");
			jobExecution.getAllFailureExceptions().forEach(Throwable::printStackTrace);
		} else {
			System.out.println("Job completed successfully! Check PostgresSQL tables for data.");
		}
	}
}
