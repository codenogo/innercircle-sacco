package com.innercircle.sacco.loan.batch;

import com.innercircle.sacco.loan.entity.LoanApplication;
import com.innercircle.sacco.loan.entity.LoanStatus;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class LoanBatchJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job monthlyLoanProcessingJob(Step loanProcessingStep, LoanBatchJobListener listener) {
        return new JobBuilder("monthlyLoanProcessingJob", jobRepository)
                .listener(listener)
                .start(loanProcessingStep)
                .build();
    }

    @Bean
    public Step loanProcessingStep(
            JpaPagingItemReader<LoanApplication> reader,
            ItemProcessor<LoanApplication, LoanApplication> processor,
            JpaItemWriter<LoanApplication> writer) {
        return new StepBuilder("loanProcessingStep", jobRepository)
                .<LoanApplication, LoanApplication>chunk(100, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<LoanApplication> loanItemReader() {
        return new JpaPagingItemReaderBuilder<LoanApplication>()
                .name("loanItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT l FROM LoanApplication l WHERE l.status = :status")
                .parameterValues(Collections.singletonMap("status", LoanStatus.REPAYING))
                .pageSize(100)
                .build();
    }

    @Bean
    public JpaItemWriter<LoanApplication> loanItemWriter() {
        return new JpaItemWriterBuilder<LoanApplication>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
