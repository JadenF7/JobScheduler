package com.jadenf7.jobscheduler.domain.service;

import com.jadenf7.jobscheduler.domain.exception.NonRetryableTaskException;
import com.jadenf7.jobscheduler.domain.model.*;
import com.jadenf7.jobscheduler.domain.port.out.TaskHandler;
import com.jadenf7.jobscheduler.domain.port.out.TaskHandlerRegistry;
import com.jadenf7.jobscheduler.infrastructure.adapter.out.persistence.InMemoryTaskRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

class TaskOrchestratorTest {
    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");
    private static final TaskPayload PAYLOAD = TaskPayload.of("SEND_EMAIL", Map.of("to", "a@b.com"));

    private final RetryPolicy policy = new RetryPolicy(2, Duration.ofSeconds(5), Duration.ofMinutes(5));
    private final InMemoryTaskRepo repo = new InMemoryTaskRepo();
    private final TaskHandlerRegistry registry = mock(TaskHandlerRegistry.class);
    private final TaskHandler handler = mock(TaskHandler.class);
    private final TaskOrchestrator orchestrator =
            new TaskOrchestrator(repo, registry, policy, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireHandler() {
        when(registry.getHandler("SEND_EMAIL")).thenReturn(handler);
    }

    private Task seedPending(int retryCount) {
        Task task = Task.reconstitute(TaskId.generate(), PAYLOAD, TaskStatus.PENDING,
                NOW.minusSeconds(60), null, null, retryCount, null, NOW.minusSeconds(60));
        repo.save(task);
        return task;
    }

    private Task reload(Task task) { return repo.findByTaskId(task.getId()).orElseThrow(); }

    @Test
    void successfulTaskEndsCompleted() throws Exception {
        Task task = seedPending(0);

        int processed = orchestrator.executePendingTasks(10);

        assertThat(processed).isEqualTo(1);
        assertThat(reload(task).getStatus()).isEqualTo(TaskStatus.COMPLETED);
        verify(handler).handle(PAYLOAD);
    }

    @Test
    void transientFailureIsRescheduledWithBackoff() throws Exception {
        doThrow(new RuntimeException("caught")).when(handler).handle(any());
        Task task = seedPending(0);

        orchestrator.executePendingTasks(10);

        Task result = reload(task);
        assertThat(result.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(result.getRetryCount()).isEqualTo(1);
        assertThat(result.getFailureReason()).isEqualTo("caught");
        assertThat(result.getNextAttemptAt()).isBetween(NOW.plusMillis(2500), NOW.plusMillis(5000));
    }

    @Test
    void nonRetryableFailureStaysFailedImmediately() throws Exception {
        doThrow(new NonRetryableTaskException("missing 'to'")).when(handler).handle(any());
        Task task = seedPending(0);

        orchestrator.executePendingTasks(10);

        Task result = reload(task);
        assertThat(result.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(result.getRetryCount()).isZero();
    }

    @Test
    void exhaustedRetriesEndFailed() throws Exception {
        doThrow(new RuntimeException("caught")).when(handler).handle(any());
        Task task = seedPending(policy.maxRetries());

        orchestrator.executePendingTasks(10);

        Task result = reload(task);
        assertThat(result.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(result.getRetryCount()).isEqualTo(policy.maxRetries());
    }

    @Test
    void taskNotYetDueIsLeftAlone() throws Exception {
        Task task = Task.reconstitute(TaskId.generate(), PAYLOAD, TaskStatus.PENDING,
                NOW.minusSeconds(60), null, null, 1, "test", NOW.plusSeconds(30));
        repo.save(task);

        assertThat(orchestrator.executePendingTasks(10)).isZero();
        verify(handler, never()).handle(any());
    }
}
