package com.subscription.InvoiceGeneration.config;

import com.google.cloud.spring.storage.GoogleStorageResource;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.subscription.InvoiceGeneration.model.Customer;
import com.subscription.InvoiceGeneration.model.Invoice;
import com.subscription.InvoiceGeneration.model.Usage;
import com.subscription.InvoiceGeneration.repo.CustomerRepository;
import com.subscription.InvoiceGeneration.repo.InvoiceRepository;
import com.subscription.InvoiceGeneration.util.GCSUtil;
import jakarta.persistence.EntityManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.data.RepositoryItemWriter;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableBatchProcessing
public class InvoiceGeneratorConfig {
    private static final Logger LOG = LogManager.getLogger();

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Bean
    @StepScope
    public Tasklet loadUsageTasklet (
            @Value ("#{jobParameters['usage']}") String usage,
            @Value ("#{jobParameters['service_acct_key']}") String service_acct_key
    ) {
        return (contribution, chunkContext) -> {

            // Dev
            Storage storage = StorageOptions
                    .newBuilder()
                    .setCredentials (GCSUtil.getCredentials(service_acct_key))
                    .build()
                    .getService();

            // Prod
//            Storage storage = StorageOptions
//                    .getDefaultInstance()
//                    .getService();

            FlatFileItemReader<Usage> reader = new FlatFileItemReaderBuilder<Usage>()
                    .name ("usageReader")
                    .resource (new GoogleStorageResource (storage, usage))
                    .delimited()
                    .names ("customer_id", "storage_used_gb", "bandwidth_used_gb", "api_calls", "peak_usage_date", "total_logins")
                    .fieldSetMapper (new BeanWrapperFieldSetMapper<>(){{
                        setTargetType (Usage.class);
                    }})
                    .linesToSkip (1)
                    .build();

            reader.open (chunkContext.getStepContext().getStepExecution().getExecutionContext());

            Map<Integer, Usage> usageMap = new HashMap<>();
            Usage usage1 = reader.read();
            while (usage1 != null) {
                usageMap.put (usage1.getCustomerId(), usage1);
                usage1 = reader.read();
            }

            reader.close();
            chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext()
                    .put ("usageMap", usageMap);

            LOG.info ("Loaded usage info details successfully !!");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step loadUsageStep () {
        return new StepBuilder ("loadUsageStep", jobRepository)
                .tasklet (loadUsageTasklet(null, null))
                .build();
    }

    @Bean
    public JpaPagingItemReader<Customer> customerReader () {
        LOG.info ("Reading customer information data from the database...");

        return new JpaPagingItemReaderBuilder<Customer>()
                .name ("customerReader")
                .entityManagerFactory (entityManagerFactory)
                .queryString ("SELECT c FROM Customer c WHERE c.status = 'ACTIVE'")
                .pageSize (100)
                .build();
    }

    @Bean
    public Tasklet loadPricingYamlTasklet () {
        return (contribution, chunkContext) -> {
            Yaml yaml = new Yaml();
            try (InputStream in = new ClassPathResource("pricing_details.yaml").getInputStream()) {
                Map<String, Map<String, Object>> pricingMap = yaml.load (in);

                chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put ("pricingMap", pricingMap);
            }

            LOG.info ("Pricing Plans Loaded successfully !!");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step loadPricingStep () {
        return new StepBuilder("loadPricingStep", jobRepository)
                .tasklet(loadPricingYamlTasklet())
                .build();
    }

    @Bean
    public ItemProcessor<Customer, Invoice> invoiceItemProcessor () {
        return new InvoiceProcessor();
    }

    @Bean
    public RepositoryItemWriter<Invoice> invoiceItemWriter () {
        RepositoryItemWriter<Invoice> writer = new RepositoryItemWriter<>(invoiceRepository);
        writer.setMethodName ("save");
        return writer;
    }

    @Bean
    public Step invoiceProcess (
            JpaPagingItemReader<Customer> customerReader,
            ItemProcessor <Customer, Invoice> invoiceItemProcessor,
            RepositoryItemWriter<Invoice> invoiceItemWriter
    ) {
        return new StepBuilder ("invoiceProcess", jobRepository)
                .<Customer, Invoice>chunk(100)
                .reader (customerReader)
                .processor (invoiceItemProcessor)
                .writer (invoiceItemWriter)
                .faultTolerant()
                .skip (Exception.class)
                .skipLimit (1000)
                .build();
    }

    @Bean
    public Job invoiceProcessingJob (
            @Qualifier ("loadUsageStep") Step loadUsageStep,
            @Qualifier ("loadPricingStep") Step loadPricingStep,
            @Qualifier ("invoiceProcess") Step invoiceProcess,
            JobCompletionListener listener
    ) {
        return new JobBuilder ("invoiceProcessingJob", jobRepository)
                .listener (listener)
                .start (loadUsageStep)
                .next (loadPricingStep)
                .next (invoiceProcess)
                .build();
    }
}
