package com.jadenf7.jobscheduler.infrastructure.adapter.in.web.dto;

import com.jadenf7.jobscheduler.domain.model.Task;

import java.time.Instant;

public record TaskResponseDto(
        String id,
        String status,
        String taskType,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        int retryCount,
        String failureReason
) {
    public static TaskResponseDto from(Task task) {
        return new TaskResponseDto(
                task.getId().value(),
                task.getStatus().name(),
                task.getPayload().taskType(),
                task.getCreatedAt(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getRetryCount(),
                task.getFailureReason()
        );
    }
}
