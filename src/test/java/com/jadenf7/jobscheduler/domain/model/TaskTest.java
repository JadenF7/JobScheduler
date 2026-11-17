package com.jadenf7.jobscheduler.domain.model;

import com.jadenf7.jobscheduler.domain.exception.IllegalTaskTransitionException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static com.jadenf7.jobscheduler.domain.model.TaskTestBuilder.aTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TaskTest {
    private static final Instant NOW = Instant.now();
    private static final RetryPolicy POLICY = new RetryPolicy(3, Duration.ofSeconds(5), Duration.ofMinutes(5));

    @Test
    void shouldCreateTaskWithPendingStatusNoRetries() {
        Task task = aTask().build();

        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(task.getRetryCount()).isZero();
    }

    @Test
    void shouldTransitionPendingTaskToRunning() {
        Task task = aTask().build();

        task.startExecution();

        assertThat(task.getStatus()).isEqualTo(TaskStatus.RUNNING);
    }

    @Test
    void shouldTransitionRunningTaskToCompleted() {
        Task task = aTask()
                .withStatus(TaskStatus.RUNNING)
                .build();

        task.markAsCompleted();

        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void shouldConfirmCompletedTaskIsTerminal() {
        Task task = aTask()
                .withStatus(TaskStatus.RUNNING)
                .build();

        task.markAsCompleted();

        assertThatThrownBy(() -> task.markAsFailed("late")).isInstanceOf(IllegalTaskTransitionException.class);
    }

    @Test
    void shouldTransitionRunningTaskToFailed() {
        Task task = aTask()
                .withStatus(TaskStatus.RUNNING)
                .build();

        task.markAsFailed("reason");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED).withFailMessage("reason");
    }

    @Test
    void shouldThrowIllegalStateExceptionIfTaskIsNotRunning() {
        Task task = aTask()
                .withStatus(TaskStatus.PENDING)
                .build();

        assertThrows(IllegalTaskTransitionException.class, task::markAsCompleted);
    }

    @Test
    void retryReturnsToPendingAndSchedulesFutureAttempts() {
        Task task = aTask()
                .withStatus(TaskStatus.FAILED)
                .build();

        task.retry(POLICY, NOW);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(task.getRetryCount()).isEqualTo(1);
        assertThat(task.getStartedAt()).isNull();
        assertThat(task.getCompletedAt()).isNull();
        assertThat(task.getNextAttemptAt()).isBetween(NOW.plusMillis(2500), NOW.plusMillis(5000));
    }

    @Test
    void retryBudgetIsEnforced() {
        Task task = aTask()
                .withStatus(TaskStatus.RUNNING)
                .build();
        for (int i = 0; i < POLICY.maxRetries(); i++) {
            task.markAsFailed("reason");
            task.retry(POLICY, NOW);
            task.startExecution();
        }
        task.markAsFailed("reason");

        assertThat(task.canRetry(POLICY)).isFalse();
        assertThatThrownBy(() -> task.retry(POLICY, NOW)).isInstanceOf(IllegalStateException.class);

    }

    @Test
    void cannotRetryATaskThatHasNotFailed() {
        Task task = aTask()
                .withStatus(TaskStatus.RUNNING)
                .build();

        assertThat(task.canRetry(POLICY)).isFalse();
    }

}
