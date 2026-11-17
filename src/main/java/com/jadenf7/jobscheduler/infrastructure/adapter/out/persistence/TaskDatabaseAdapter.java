package com.jadenf7.jobscheduler.infrastructure.adapter.out.persistence;

import com.jadenf7.jobscheduler.domain.model.Task;
import com.jadenf7.jobscheduler.domain.model.TaskId;
import com.jadenf7.jobscheduler.domain.model.TaskStatus;
import com.jadenf7.jobscheduler.domain.port.out.TaskRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class TaskDatabaseAdapter implements TaskRepository {

    private final SpringDataTaskRepo springDataTaskRepo;

    public TaskDatabaseAdapter(SpringDataTaskRepo springDataTaskRepo) {
        this.springDataTaskRepo = springDataTaskRepo;
    }

    @Override
    @Transactional
    public void save(Task task) {
        springDataTaskRepo.save(TaskMapper.toEntity(task));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Task> findByTaskId(TaskId id) {
        return springDataTaskRepo.findById(id.value()).map(TaskMapper::toDomain);
    }

    @Override
    @Transactional
    public List<Task> claimPendingTasks(int limit, Instant now) {
        List<String> lockedIds = springDataTaskRepo.lockNextPendingTaskIds(limit, now);
        if (lockedIds.isEmpty()) {
            return List.of();
        }

        List<TaskEntity> entities = springDataTaskRepo.findAllById(lockedIds);
        for (TaskEntity entity : entities) {
            entity.setStatus(TaskStatus.RUNNING.name());
            entity.setStartedAt(now);
        }
        springDataTaskRepo.saveAll(entities);

        return entities.stream().map(TaskMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public List<Task> claimStuckTasks(Instant cutoff, Instant now, int limit) {
        List<String> lockedIds = springDataTaskRepo.lockStuckTaskIds(cutoff, limit);
        if (lockedIds.isEmpty()) {
            return List.of();
        }

        List<TaskEntity> entities = springDataTaskRepo.findAllById(lockedIds);
        entities.forEach(e -> e.setStartedAt(now));
        springDataTaskRepo.saveAll(entities);
        return entities.stream().map(TaskMapper::toDomain).toList();
    }
}
