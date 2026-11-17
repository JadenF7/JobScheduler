package com.jadenf7.jobscheduler.domain.port.in;

public interface ExecutePendingTasksUseCase {
    int executePendingTasks(int batchSize);
}
