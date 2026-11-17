package com.jadenf7.jobscheduler.infrastructure.config;

import com.jadenf7.jobscheduler.domain.model.RetryPolicy;
import com.jadenf7.jobscheduler.domain.port.in.ExecutePendingTasksUseCase;
import com.jadenf7.jobscheduler.domain.port.in.GetTaskUseCase;
import com.jadenf7.jobscheduler.domain.port.in.RecoverStuckTasksUseCase;
import com.jadenf7.jobscheduler.domain.port.in.SubmitTaskUseCase;
import com.jadenf7.jobscheduler.domain.port.out.TaskHandlerRegistry;
import com.jadenf7.jobscheduler.domain.port.out.TaskRepository;
import com.jadenf7.jobscheduler.domain.service.StuckTaskRecoveryService;
import com.jadenf7.jobscheduler.domain.service.TaskOrchestrator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class DomainConfig {

    @Bean
    public TaskOrchestrator taskOrchestrator(TaskRepository taskRepository,
                                             TaskHandlerRegistry taskHandlerRegistry, RetryPolicy retryPolicy,
                                             Clock clock) {
        return new TaskOrchestrator(taskRepository, taskHandlerRegistry, retryPolicy, clock);
    }

    @Bean
    public RecoverStuckTasksUseCase recoverStuckTasksUseCase(TaskRepository taskRepository, RetryPolicy retryPolicy, Clock clock) {
        return new StuckTaskRecoveryService(taskRepository, retryPolicy, clock);
    }

    @Bean
    public RetryPolicy retryPolicy(@Value("${scheduler.retry.max-retries:5}") int max,
                                   @Value("${scheduler.retry.base-delay:5s}") Duration base,
                                   @Value("${scheduler.retry.max-delay:5m}") Duration cap) {
        return new RetryPolicy(max, base, cap);
    }

    @Bean
    public Clock clock() { return Clock.systemUTC(); }
}