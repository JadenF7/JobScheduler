package com.jadenf7.jobscheduler.infrastructure.adapter.out.persistence;

import com.jadenf7.jobscheduler.domain.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SpringDataTaskRepo extends JpaRepository<TaskEntity, String> {
    long countByStatus(String status);
    @Query(value = """
        SELECT id FROM tasks
        WHERE status = 'PENDING'
          AND COALESCE(next_attempt_at, created_at) <= :now
        ORDER BY COALESCE(next_attempt_at, created_at)
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<String> lockNextPendingTaskIds(@Param("limit") int limit, @Param("now") Instant now);

    @Query(value = """
        SELECT id FROM tasks
        WHERE status = 'RUNNING'
          AND started_at < :cutoff
        ORDER BY started_at
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<String> lockStuckTaskIds(@Param("cutoff") Instant cutoff, @Param("limit") int limit);
}
