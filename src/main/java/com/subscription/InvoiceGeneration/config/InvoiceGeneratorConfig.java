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
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.ExecutionContext;
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
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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
            @Value ("#{jobParameters['service_acct_key']}") String service_acct_key,
            @Value ("#{jobParameters['env']}") String env
    ) {
        return (contribution, chunkContext) -> {
            Storage storage;
            Resource resource;

            if ("prod".equals (env)) {
                storage = StorageOptions
                        .getDefaultInstance()
                        .getService();
                resource = new GoogleStorageResource (storage, usage);
            } else if ("dev".equals (env)) {
                storage = StorageOptions
                        .newBuilder()
                        .setCredentials(GCSUtil.getCredentials (service_acct_key))
                        .build()
                        .getService();
                resource = new GoogleStorageResource (storage, usage);
            } else {
                resource = new FileSystemResource (usage);
            }

            FlatFileItemReader<Usage> reader = new FlatFileItemReaderBuilder<Usage>()
                    .name ("usageReader")
                    .resource (resource)
                    .delimited()
                    .names ("customer_id", "storage_used_gb", "bandwidth_used_gb", "api_calls", "peak_usage_date", "total_logins")
                    .fieldSetMapper (new BeanWrapperFieldSetMapper<>(){{
                        setTargetType (Usage.class);
                    }})
                    .linesToSkip (1)
                    .build();

            reader.open (chunkContext.getStepContext().getStepExecution().getExecutionContext());

            List<String> lines = new ArrayList<>();
            String line;
            BufferedReader bf = new BufferedReader (new InputStreamReader(resource.getInputStream()));
            bf.readLine();
            while ((line = bf.readLine()) != null) {
                lines.add (line);
            }
            bf.close();
            reader.close();

            int numThreads = Runtime.getRuntime().availableProcessors();
            ExecutorService executors = Executors.newFixedThreadPool (numThreads);
            ConcurrentMap<Integer, Usage> usageMap = new ConcurrentHashMap<>();

            int batchSize = lines.size() / numThreads;
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < numThreads; i++) {
                int start = i * batchSize;
                int end = (i == numThreads - 1) ? lines.size() : start + batchSize;

                List<String> batch = lines.subList (start, end);
                futures.add (executors.submit (() -> {
                    for (String l : batch) {
                        try {
                            String[] fields = l.split (",");
                            if (fields.length != 6) {
                                LOG.warn ("Skipping invalid line : {}", l);
                                return null;
                            };

                            Usage u = new Usage();
                            u.setCustomerId (Integer.parseInt (fields[0]));
                            u.setStorageUsedGB (new BigDecimal (fields[1]));
                            u.setBandwidthUsedGB (new BigDecimal (fields[2]));
                            u.setApiCalls (Integer.parseInt (fields[3]));
                            u.setPeakUsageDate (fields[4]);
                            u.setTotalLogins (Integer.parseInt (fields[5]));

                            usageMap.put (u.getCustomerId(), u);
                        } catch (Exception e) {
                            LOG.error ("Error parsing line {} : {}", l, e.getMessage());
                        }
                    }
                    return null;
                }));
            }

            for (Future<Void> f : futures) {
                f.get();
            }

            executors.shutdown();

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
                .tasklet (loadUsageTasklet(null, null, null))
                .build();
    }

    @Bean
    public Partitioner customerPartitioner () {
        return gridSize -> {
            Map<String, ExecutionContext> partitions = new HashMap<>();
            long minId = 1;
            long maxId = 10000000;
            long rangeSize = (maxId - minId) / gridSize;
            for (int i = 0; i < gridSize; i++) {
                ExecutionContext context = new ExecutionContext();
                long partitionMin = minId + (i * rangeSize);
                long partitionMax = (i == gridSize - 1) ? maxId : partitionMin + rangeSize - 1;
                context.putLong ("minId", partitionMin);
                context.putLong ("maxId", partitionMax);
                partitions.put ("partition" + i, context);
            }

            return partitions;
        };
    }

    @Bean
    public TaskExecutor batchTaskExecutor () {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("batch-");
        executor.setConcurrencyLimit (Runtime.getRuntime().availableProcessors());
        return executor;
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Customer> customerReader (
            @Value ("#{stepExecutionContext['minId']}") Long minId,
            @Value ("#{stepExecutionContext['maxId']}") Long maxId
    ) {
        LOG.info ("Reading customer information data from the database...");
        LOG.info ("Reading partition : minId = {}, maxId = {}", minId, maxId);

        return new JpaPagingItemReaderBuilder<Customer>()
                .name ("customerReader")
                .entityManagerFactory (entityManagerFactory)
                .queryString ("SELECT c FROM Customer c WHERE c.status = 'ACTIVE' AND c.id BETWEEN :minId AND :maxId ORDER BY c.id")
                .parameterValues (Map.of ("minId", minId, "maxId", maxId))
                .pageSize (1000)
                .build();
    }

    @Bean
    public Step partitionedInvoiceStep (
            Partitioner customerPartitioner,
            TaskExecutor batchTaskExecutor,
            @Qualifier ("invoiceProcess") Step invoiceProcess
    ) {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setGridSize(Runtime.getRuntime().availableProcessors());  // Partitions = cores
        handler.setTaskExecutor(batchTaskExecutor);
        handler.setStep(invoiceProcess);

        return new StepBuilder ("partitionedInvoiceStep", jobRepository)
                .partitioner ("invoiceProcess", customerPartitioner)
                .partitionHandler (handler)
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
                .<Customer, Invoice>chunk(10000)
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
            @Qualifier ("partitionedInvoiceStep") Step partitionedInvoiceStep,
            JobCompletionListener listener
    ) {
        return new JobBuilder ("invoiceProcessingJob", jobRepository)
                .listener (listener)
                .start (loadUsageStep)
                .next (loadPricingStep)
                .next (partitionedInvoiceStep)
                .build();
    }
}
