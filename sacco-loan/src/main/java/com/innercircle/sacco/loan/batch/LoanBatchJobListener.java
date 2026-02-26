package com.innercircle.sacco.loan.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoanBatchJobListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String targetDate = jobExecution.getJobParameters().getString("targetDate");
        log.info("Starting loan processing job for date: {}", targetDate);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String targetDate = jobExecution.getJobParameters().getString("targetDate");
        log.info("Loan processing job completed for date: {} with status: {}",
                targetDate, jobExecution.getStatus());
    }
}
