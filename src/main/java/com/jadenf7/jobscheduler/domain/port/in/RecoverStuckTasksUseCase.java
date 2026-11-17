package com.jadenf7.jobscheduler.domain.port.in;

public interface RecoverStuckTasksUseCase {
    int recoverStuckTasks(int batchSize);
}
