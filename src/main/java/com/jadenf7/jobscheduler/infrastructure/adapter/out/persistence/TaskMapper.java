package com.jadenf7.jobscheduler.infrastructure.adapter.out.persistence;

import com.jadenf7.jobscheduler.domain.model.Task;
import com.jadenf7.jobscheduler.domain.model.TaskId;
import com.jadenf7.jobscheduler.domain.model.TaskPayload;
import com.jadenf7.jobscheduler.domain.model.TaskStatus;

final class TaskMapper {

    private TaskMapper() {
    }

    static TaskEntity toEntity(Task task) {
        return new TaskEntity(
                task.getId().value(),
                task.getStatus().name(),
                task.getPayload().taskType(),
                task.getPayload().data(),
                task.getCreatedAt(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getRetryCount(),
                task.getFailureReason(),
                task.getNextAttemptAt()
        );
    }

    static Task toDomain(TaskEntity entity) {
        return Task.reconstitute(
                TaskId.of(entity.getId()),
                TaskPayload.of(entity.getTaskType(), entity.getPayloadData()),
                TaskStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getRetryCount(),
                entity.getFailureReason(),
                entity.getNextAttemptAt()
        );
    }

}