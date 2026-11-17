package com.jadenf7.jobscheduler.domain.service;

import com.jadenf7.jobscheduler.domain.model.RetryPolicy;
import com.jadenf7.jobscheduler.domain.model.Task;
import com.jadenf7.jobscheduler.domain.port.in.RecoverStuckTasksUseCase;
import com.jadenf7.jobscheduler.domain.port.out.TaskRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class StuckTaskRecoveryService implements RecoverStuckTasksUseCase {
    private final TaskRepository taskRepository;
    private final RetryPolicy retryPolicy;
    private final Clock clock;

    public StuckTaskRecoveryService(TaskRepository taskRepository, RetryPolicy retryPolicy, Clock clock) {
        this.taskRepository = taskRepository;
        this.retryPolicy = retryPolicy;
        this.clock = clock;
    }

    @Override
    public int recoverStuckTasks(Duration timeout, int batchSize) {
        Instant now = clock.instant();
        List<Task> stuck = taskRepository.claimStuckTasks(now.minus(timeout), now, batchSize);

        for (Task task : stuck) {
            task.markAsFailed("Worker lost: no completion within " + timeout);
            if (task.canRetry(retryPolicy)) {
                task.retry(retryPolicy, now);
            }
            taskRepository.save(task);
        }
        return stuck.size();
    }
}