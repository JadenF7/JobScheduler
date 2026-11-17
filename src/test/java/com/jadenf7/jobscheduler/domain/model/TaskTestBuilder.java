package com.jadenf7.jobscheduler.domain.model;

import java.time.Instant;
import java.util.Map;

public class TaskTestBuilder {
    private TaskId id = TaskId.generate();
    private TaskPayload payload = TaskPayload.of("DEFAULT_TYPE", Map.of("key", "value"));
    private TaskStatus status = TaskStatus.PENDING;
    private Instant createdAt = Instant.now();
    private Instant startedAt = null;
    private Instant completedAt = null;
    private int retryCount = 0;
    private String failureReason = null;
    private Instant nextAttemptAt = Instant.now();

    public static TaskTestBuilder aTask() {
        return new TaskTestBuilder();
    }

    public TaskTestBuilder withPayload(TaskPayload payload) {
        this.payload = payload;
        return this;
    }

    // Convenience method so tests don't have to call TaskPayload.of(...) manually
    public TaskTestBuilder withPayload(String type, Map<String, String> data) {
        this.payload = TaskPayload.of(type, data);
        return this;
    }

    public TaskTestBuilder withStatus(TaskStatus status) {
        this.status = status;
        return this;
    }

    public TaskTestBuilder withStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    public TaskTestBuilder withFailureReason(String reason) {
        this.failureReason = reason;
        return this;
    }

    public Task build() {
        return Task.reconstitute(id, payload, status, createdAt, startedAt, completedAt, retryCount, failureReason, nextAttemptAt);
    }
}
