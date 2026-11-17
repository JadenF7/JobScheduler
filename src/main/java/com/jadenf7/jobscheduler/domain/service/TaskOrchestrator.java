package com.jadenf7.jobscheduler.domain.service;

import com.jadenf7.jobscheduler.domain.exception.NonRetryableTaskException;
import com.jadenf7.jobscheduler.domain.model.RetryPolicy;
import com.jadenf7.jobscheduler.domain.model.Task;
import com.jadenf7.jobscheduler.domain.model.TaskId;
import com.jadenf7.jobscheduler.domain.model.TaskPayload;
import com.jadenf7.jobscheduler.domain.port.in.ExecutePendingTasksUseCase;
import com.jadenf7.jobscheduler.domain.port.in.GetTaskUseCase;
import com.jadenf7.jobscheduler.domain.port.in.SubmitTaskUseCase;
import com.jadenf7.jobscheduler.domain.port.out.TaskHandler;
import com.jadenf7.jobscheduler.domain.port.out.TaskHandlerRegistry;
import com.jadenf7.jobscheduler.domain.port.out.TaskRepository;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

public class TaskOrchestrator implements GetTaskUseCase, SubmitTaskUseCase, ExecutePendingTasksUseCase {
    private final TaskRepository taskRepository;
    private final TaskHandlerRegistry taskHandlerRegistry;
    private final RetryPolicy retryPolicy;
    private final Clock clock;

    public TaskOrchestrator(TaskRepository taskRepository, TaskHandlerRegistry taskHandlerRegistry,
                            RetryPolicy retryPolicy, Clock clock) {
        this.taskRepository = taskRepository;
        this.taskHandlerRegistry = taskHandlerRegistry;
        this.retryPolicy = retryPolicy;
        this.clock = clock;
    }
    @Override
    public TaskId submitTask(TaskPayload payload) {
        Task task = Task.submit(payload);
        taskRepository.save(task);
        return task.getId();
    }

    @Override
    public Optional<Task> getTask(TaskId id) { return taskRepository.findByTaskId(id); }

    @Override
    public int executePendingTasks(int batchSize) {
        List<Task> claimed = taskRepository.claimPendingTasks(batchSize, clock.instant());

        for (Task task : claimed) {
            executeOne(task);
        }

        return claimed.size();
    }

    private void executeOne(Task task) {
        try {
            TaskHandler handler = taskHandlerRegistry.getHandler(task.getPayload().taskType());
            handler.handle(task.getPayload());
            task.markAsCompleted();
        } catch (NonRetryableTaskException e) {
            task.markAsFailed(e.getMessage());
        } catch (Exception e) {
            task.markAsFailed(e.getMessage());
            if(task.canRetry(retryPolicy)) {
                task.retry(retryPolicy, clock.instant());
            }
        } finally {
            taskRepository.save(task);
        }
    }

}
