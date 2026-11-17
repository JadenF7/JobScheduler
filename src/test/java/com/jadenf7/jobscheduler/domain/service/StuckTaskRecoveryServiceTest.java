package com.jadenf7.jobscheduler.domain.service;

import com.jadenf7.jobscheduler.domain.model.*;
import com.jadenf7.jobscheduler.infrastructure.adapter.out.persistence.InMemoryTaskRepo;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class StuckTaskRecoveryServiceTest {
    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");
    private static final Duration TIMEOUT = Duration.ofMinutes(10);
    private static final TaskPayload PAYLOAD = TaskPayload.of("SEND_EMAIL", Map.of());

    private final RetryPolicy policy = new RetryPolicy(2, Duration.ofSeconds(5), Duration.ofMinutes(5));
    private final InMemoryTaskRepo repo = new InMemoryTaskRepo();
    private final StuckTaskRecoveryService service =
            new StuckTaskRecoveryService(repo, policy, Clock.fixed(NOW, ZoneOffset.UTC));

    private Task seedRunning(Duration runningFor, int retryCount) {
        Instant started = NOW.minus(runningFor);
        Task task = Task.reconstitute(TaskId.generate(), PAYLOAD, TaskStatus.RUNNING,
                started.minusSeconds(5), started, null, retryCount, null, started.minusSeconds(5));
        repo.save(task);
        return task;
    }

    private Task reload(Task task) { return repo.findByTaskId(task.getId()).orElseThrow(); }

    @Test
    void staleRunningTaskIsRescheduled() {
        Task task = seedRunning(Duration.ofMinutes(20), 0);

        assertThat(service.recoverStuckTasks(TIMEOUT, 50)).isEqualTo(1);

        Task result = reload(task);
        assertThat(result.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(result.getRetryCount()).isEqualTo(1);
        assertThat(result.getNextAttemptAt()).isAfter(NOW);
    }

    @Test
    void recentlyStartedTaskIsNotTouched() {
        Task task = seedRunning(Duration.ofMinutes(1), 0);

        assertThat(service.recoverStuckTasks(TIMEOUT, 50)).isZero();
        assertThat(reload(task).getStatus()).isEqualTo(TaskStatus.RUNNING);
    }

    @Test
    void stuckTaskWithNoRetriesLeftEndsFailed() {
        Task task = seedRunning(Duration.ofMinutes(20), policy.maxRetries());

        service.recoverStuckTasks(TIMEOUT, 50);

        Task result = reload(task);
        assertThat(result.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(result.getFailureReason()).contains("Worker lost");
    }
}
