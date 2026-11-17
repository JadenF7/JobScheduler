package com.jadenf7.jobscheduler.domain.port.in;

import com.jadenf7.jobscheduler.domain.model.Task;
import com.jadenf7.jobscheduler.domain.model.TaskId;

import java.util.Optional;

public interface GetTaskUseCase {
    Optional<Task> getTask(TaskId id);
}
