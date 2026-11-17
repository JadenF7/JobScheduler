package com.jadenf7.jobscheduler.domain.port.out;

import com.jadenf7.jobscheduler.domain.model.Task;
import com.jadenf7.jobscheduler.domain.model.TaskId;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {
    Optional<Task> findByTaskId(TaskId id);
    void save(Task task);
    List<Task> claimPendingTasks(int limit, Instant now);
    List<Task> claimStuckTasks(Instant cutoff, Instant now, int limit);
}
