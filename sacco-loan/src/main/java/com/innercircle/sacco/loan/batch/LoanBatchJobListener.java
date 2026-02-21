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
        String targetMonth = jobExecution.getJobParameters().getString("targetMonth");
        log.info("Starting monthly loan processing job for month: {}", targetMonth);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String targetMonth = jobExecution.getJobParameters().getString("targetMonth");
        log.info("Monthly loan processing job completed for month: {} with status: {}",
                targetMonth, jobExecution.getStatus());
    }
}
