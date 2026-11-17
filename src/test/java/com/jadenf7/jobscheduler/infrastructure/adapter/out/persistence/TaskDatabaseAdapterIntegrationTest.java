package com.jadenf7.jobscheduler.infrastructure.adapter.out.persistence;

import com.jadenf7.jobscheduler.domain.model.*;
import com.jadenf7.jobscheduler.domain.port.out.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "scheduler.worker.enabled=false")
@Testcontainers
class TaskDatabaseAdapterIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");
    private static final TaskPayload PAYLOAD = TaskPayload.of("SEND_EMAIL", Map.of("to", "a@b.com"));

    @Autowired TaskRepository repository;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanTables() {
        jdbc.execute("TRUNCATE TABLE task_payload_data, tasks");
    }


    private Task seed(TaskStatus status, Instant createdAt, Instant startedAt, Instant nextAttemptAt) {
        Task task = Task.reconstitute(TaskId.generate(), PAYLOAD, status,
                createdAt, startedAt, null, 0, null, nextAttemptAt);
        repository.save(task);
        return task;
    }

    private Task seedPending(Instant createdAt) {
        return seed(TaskStatus.PENDING, createdAt, null, createdAt);
    }

    private Task seedStuck(java.time.Duration runningFor) {
        Instant started = NOW.minus(runningFor);
        return seed(TaskStatus.RUNNING, started.minusSeconds(5), started, started.minusSeconds(5));
    }

    private Task reload(Task task) {
        return repository.findByTaskId(task.getId()).orElseThrow();
    }

    private int countByStatus(String status) {
        return jdbc.queryForObject("SELECT count(*) FROM tasks WHERE status = ?", Integer.class, status);
    }

    private List<String> claimConcurrently(int threads, Supplier<List<Task>> claimer) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CyclicBarrier startTogether = new CyclicBarrier(threads);
        try {
            List<Future<List<String>>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    startTogether.await();
                    List<String> mine = new ArrayList<>();
                    List<Task> batch;
                    while (!(batch = claimer.get()).isEmpty()) {
                        batch.forEach(t -> mine.add(t.getId().value()));
                    }
                    return mine;
                }));
            }
            List<String> all = new ArrayList<>();
            for (Future<List<String>> f : futures) {
                all.addAll(f.get(30, TimeUnit.SECONDS));
            }
            return all;
        } finally {
            pool.shutdownNow();
        }

    }

    @Test
    void savedTaskRoundTripsThroughTheDatabase() {
        Task original = Task.reconstitute(TaskId.generate(),
                TaskPayload.of("SEND_EMAIL", Map.of("to", "a@b.com", "subject", "Hi")),
                TaskStatus.FAILED, NOW.minusSeconds(100), NOW.minusSeconds(50), NOW.minusSeconds(40),
                2, "boom", NOW.plusSeconds(30));
        repository.save(original);

        Task loaded = reload(original);

        assertThat(loaded.getId()).isEqualTo(original.getId());
        assertThat(loaded.getPayload()).isEqualTo(original.getPayload());
        assertThat(loaded.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(loaded.getRetryCount()).isEqualTo(2);
        assertThat(loaded.getFailureReason()).isEqualTo("boom");
        assertThat(loaded.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(30));
    }

    @Test
    void savingTheSameTaskTwiceUpdatesInsteadOfFailing() {
        Task task = seedPending(NOW.minusSeconds(60));
        task.startExecution();
        task.markAsCompleted();

        repository.save(task);

        assertThat(reload(task).getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(countByStatus("COMPLETED")).isEqualTo(1);
    }

    @Test
    void claimMarksTasksRunningAndPersistsIt() {
        Task task = seedPending(NOW.minusSeconds(60));

        List<Task> claimed = repository.claimPendingTasks(10, NOW);

        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).getStatus()).isEqualTo(TaskStatus.RUNNING);
        Task persisted = reload(task);
        assertThat(persisted.getStatus()).isEqualTo(TaskStatus.RUNNING);
        assertThat(persisted.getStartedAt()).isEqualTo(NOW);
    }

    @Test
    void claimHonorsLimitAndTakesOldestFirst() {
        Task oldest = seedPending(NOW.minusSeconds(180));
        Task middle = seedPending(NOW.minusSeconds(120));
        Task newest = seedPending(NOW.minusSeconds(60));

        List<Task> claimed = repository.claimPendingTasks(2, NOW);

        assertThat(claimed).extracting(Task::getId)
                .containsExactlyInAnyOrder(oldest.getId(), middle.getId());
        assertThat(reload(newest).getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void claimSkipsTasksThatAreNotYetDue() {
        Task notDue = seed(TaskStatus.PENDING, NOW.minusSeconds(60), null, NOW.plusSeconds(3600));
        Task due = seed(TaskStatus.PENDING, NOW.minusSeconds(60), null, NOW.minusSeconds(1));

        List<Task> claimed = repository.claimPendingTasks(10, NOW);

        assertThat(claimed).extracting(Task::getId).containsExactly(due.getId());
        assertThat(reload(notDue).getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void rowsWithNullNextAttemptAtFallBackToCreatedAt() {
        // simulates rows written before the column existed; the query's COALESCE should still pick them up
        Task legacy = seed(TaskStatus.PENDING, NOW.minusSeconds(60), null, null);

        assertThat(repository.claimPendingTasks(10, NOW)).extracting(Task::getId)
                .containsExactly(legacy.getId());
    }

    @Test
    void claimIgnoresTasksThatAreNotPending() {
        seed(TaskStatus.RUNNING, NOW.minusSeconds(60), NOW.minusSeconds(30), NOW.minusSeconds(60));
        seed(TaskStatus.COMPLETED, NOW.minusSeconds(60), NOW.minusSeconds(30), NOW.minusSeconds(60));
        seed(TaskStatus.FAILED, NOW.minusSeconds(60), NOW.minusSeconds(30), NOW.minusSeconds(60));

        assertThat(repository.claimPendingTasks(10, NOW)).isEmpty();
    }

    @Test
    void claimingTwiceNeverReturnsTheSameTask() {
        seedPending(NOW.minusSeconds(60));

        assertThat(repository.claimPendingTasks(10, NOW)).hasSize(1);
        assertThat(repository.claimPendingTasks(10, NOW)).isEmpty();
    }

    @Test
    void concurrentWorkersNeverClaimTheSameTask() throws Exception {
        int total = 100;
        for (int i = 0; i < total; i++) {
            seedPending(NOW.minusSeconds(60));
        }

        List<String> claimed = claimConcurrently(8, () -> repository.claimPendingTasks(7, NOW));

        assertThat(claimed).doesNotHaveDuplicates().hasSize(total);
        assertThat(countByStatus("RUNNING")).isEqualTo(total);
    }

    @Test
    void stuckClaimFindsOnlyStaleRunningTasks() {
        Task stale = seedStuck(Duration.ofMinutes(20));
        Task fresh = seedStuck(Duration.ofMinutes(1));
        Task pending = seedPending(NOW.minusSeconds(60));

        List<Task> claimed = repository.claimStuckTasks(NOW.minus(Duration.ofMinutes(10)), NOW, 10);

        assertThat(claimed).extracting(Task::getId).containsExactly(stale.getId());
        assertThat(reload(fresh).getStatus()).isEqualTo(TaskStatus.RUNNING);
        assertThat(reload(pending).getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void stuckClaimBumpsStartedAtSoASecondSweepSkipsIt() {
        Task stale = seedStuck(Duration.ofMinutes(20));
        Instant cutoff = NOW.minus(Duration.ofMinutes(10));

        assertThat(repository.claimStuckTasks(cutoff, NOW, 10)).hasSize(1);
        assertThat(reload(stale).getStartedAt()).isEqualTo(NOW);
        assertThat(repository.claimStuckTasks(cutoff, NOW, 10)).isEmpty();
    }

    @Test
    void concurrentStuckSweepsRecoverEachTaskOnce() throws Exception {
        int total = 40;
        for (int i = 0; i < total; i++) {
            seedStuck(Duration.ofMinutes(20));
        }
        Instant cutoff = NOW.minus(Duration.ofMinutes(10));

        List<String> recovered = claimConcurrently(6, () -> repository.claimStuckTasks(cutoff, NOW, 5));

        assertThat(recovered).doesNotHaveDuplicates().hasSize(total);
    }
}