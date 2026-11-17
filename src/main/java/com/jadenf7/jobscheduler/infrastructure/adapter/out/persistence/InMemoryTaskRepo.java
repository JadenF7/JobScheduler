package com.jadenf7.jobscheduler.infrastructure.adapter.out.persistence;

import com.jadenf7.jobscheduler.domain.model.Task;
import com.jadenf7.jobscheduler.domain.model.TaskId;
import com.jadenf7.jobscheduler.domain.model.TaskStatus;
import com.jadenf7.jobscheduler.domain.port.out.TaskRepository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryTaskRepo implements TaskRepository {
    private final Map<TaskId, Task> tasks = new LinkedHashMap<>();

    @Override
    public Optional<Task> findByTaskId(TaskId id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public void save (Task task) {
        tasks.put(task.getId(), task);
    }

    @Override
    public List<Task> claimPendingTasks(int limit, Instant now) {
        List<Task> claimed = tasks.values().stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING && !t.getNextAttemptAt().isAfter(now))
                .limit(limit)
                .toList();

        claimed.forEach(Task::startExecution);
        return claimed;
    }

    @Override
    public List<Task> claimStuckTasks(Instant cutoff, Instant now, int limit) {
        return tasks.values().stream()
                .filter(t -> t.getStatus() == TaskStatus.RUNNING && t.getStartedAt().isBefore(cutoff))
                .limit(limit)
                .toList();
    }
}
