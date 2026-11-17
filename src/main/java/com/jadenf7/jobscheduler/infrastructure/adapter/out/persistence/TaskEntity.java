package com.jadenf7.jobscheduler.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "tasks")
public class TaskEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "task_type", nullable = false, length = 100)
    private String taskType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_payload_data", joinColumns = @JoinColumn(name = "task_id"))
    @MapKeyColumn(name = "data_key")
    @Column(name = "data_value")
    private Map<String, String> payloadData = new HashMap<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "retry count", nullable = false)
    private int retryCount;

    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    protected TaskEntity() {
    }

    public TaskEntity(String id, String status, String taskType, Map<String, String> payloadData,
                      Instant createdAt, Instant startedAt, Instant completedAt, int retryCount,
                      String failureReason, Instant nextAttemptAt) {
        this.id = id;
        this.status = status;
        this.taskType = taskType;
        this.payloadData = payloadData == null ? new HashMap<>() : new HashMap<>(payloadData);
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.retryCount = retryCount;
        this.failureReason = failureReason;
        this.nextAttemptAt = nextAttemptAt;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTaskType() {
        return taskType;
    }

    public Map<String, String> getPayloadData() {
        return payloadData;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public int getRetryCount() { return retryCount; }

    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getNextAttemptAt() {return nextAttemptAt;}
}
