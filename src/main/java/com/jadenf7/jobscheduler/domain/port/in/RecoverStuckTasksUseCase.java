package com.jadenf7.jobscheduler.domain.port.in;

import java.time.Duration;

public interface RecoverStuckTasksUseCase {
    int recoverStuckTasks(Duration timeout, int batchSize);
}
