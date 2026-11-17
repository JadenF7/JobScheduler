package com.jadenf7.jobscheduler.infrastructure.adapter.in.worker;

import com.jadenf7.jobscheduler.domain.port.in.ExecutePendingTasksUseCase;
import com.jadenf7.jobscheduler.domain.port.in.RecoverStuckTasksUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "scheduler.worker.enabled", havingValue = "true", matchIfMissing = true)
public class StuckTaskRecoveryWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncPollingWorker.class);

    private final RecoverStuckTasksUseCase recoverStuckTasksUseCase;
    private final Duration timeout;
    private final int batchSize;

    public StuckTaskRecoveryWorker(RecoverStuckTasksUseCase recoverStuckTasksUseCase,
                                   @Value("${scheduler.recovery.timeout:10m}") Duration timeout,
                                   @Value("${scheduler.recovery.batch-size:50}") int batchSize) {
        this.recoverStuckTasksUseCase = recoverStuckTasksUseCase;
        this.timeout = timeout;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${scheduler.recovery.interval-ms:60000}")
    public void sweep() {
        try {
            int recovered = recoverStuckTasksUseCase.recoverStuckTasks(timeout, batchSize);
            if (recovered > 0) {
                LOGGER.warn("Recovered stuck tasks in {} ms", recovered);
            }
        }  catch (Exception e) {
            LOGGER.error("Stuck task sweep failed");
        }
    }
}
