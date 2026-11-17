package com.jadenf7.jobscheduler.infrastructure.adapter.in.worker;

import com.jadenf7.jobscheduler.domain.port.in.ExecutePendingTasksUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "scheduler.worker.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncPollingWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncPollingWorker.class);
    private final ExecutePendingTasksUseCase executePendingTasksUseCase;
    private final int batchSize;

    public AsyncPollingWorker(ExecutePendingTasksUseCase executePendingTasksUseCase,
                              @Value("10") int batchSize) {
        this.executePendingTasksUseCase = executePendingTasksUseCase;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "2000")
    public void pollAndExecute() {
        try {
            int processed = executePendingTasksUseCase.executePendingTasks(batchSize);
            if (processed > 0) {
                LOGGER.info("Processed {} tasks", processed);
            }
        }  catch (Exception e) {
            LOGGER.error("Polling cycle failed", e);
        }
    }


}
