package com.jadenf7.jobscheduler.domain.model;

import com.jadenf7.jobscheduler.domain.exception.IllegalTaskTransitionException;

import java.time.Instant;

public class Task {
    private final TaskId id;
    private final TaskPayload payload;
    private final Instant createdAt;

    private TaskStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private int retryCount;
    private String failureReason;
    private Instant nextAttemptAt;

    private Task(TaskId id, TaskPayload payload, TaskStatus status, Instant createdAt,
                 Instant startedAt, Instant completedAt, int retryCount, String failureReason, Instant nextAttemptAt) {
        this.id = id;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.retryCount = retryCount;
        this.failureReason = failureReason;
        this.nextAttemptAt = nextAttemptAt;
    }

    public static Task submit(TaskPayload payload) {
        Instant now = Instant.now();
        return new Task(TaskId.generate(), payload, TaskStatus.PENDING, Instant.now(), null,
                null, 0, null, now);
    }

    public static Task reconstitute(TaskId id, TaskPayload payload, TaskStatus status,
                                    Instant createdAt, Instant startedAt, Instant completedAt, int retryCount,
                                    String failureReason, Instant nextAttemptAt) {
        return new Task(id, payload, status, createdAt, startedAt, completedAt, retryCount, failureReason, nextAttemptAt);
    }

    public void startExecution() {
        transitionTo(TaskStatus.RUNNING);
        this.startedAt = Instant.now();
    }

    public void markAsCompleted() {
        transitionTo(TaskStatus.COMPLETED);
        this.completedAt = Instant.now();
        this.failureReason = null;
    }

    public void markAsFailed(String reason) {
        transitionTo(TaskStatus.FAILED);
        this.completedAt = Instant.now();
        this.failureReason = reason;
    }

    private void transitionTo(TaskStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalTaskTransitionException(status, next);
        }
        this.status = next;
    }

    public boolean canRetry(RetryPolicy policy) {
        return status == TaskStatus.FAILED && retryCount < policy.maxRetries();
    }

    public void retry(RetryPolicy policy, Instant now) {
        if (!canRetry(policy)) {
            throw new IllegalStateException("Task" + id + " cannot be retried");
        }
        transitionTo(TaskStatus.PENDING);
        this.retryCount++;
        this.nextAttemptAt = now.plus(policy.delayWithJitter(retryCount));
        this.startedAt = null;
        this.completedAt = null;
    }

    public TaskId getId() {return id;}
    public TaskPayload getPayload() {return payload;}
    public TaskStatus getStatus() {return status;}
    public Instant getCreatedAt() {return createdAt;}
    public Instant getStartedAt() {return startedAt;}
    public Instant getCompletedAt() {return completedAt;}
    public int getRetryCount() {return retryCount;}
    public String getFailureReason() {return failureReason;}
    public Instant getNextAttemptAt() {return nextAttemptAt;}
}
