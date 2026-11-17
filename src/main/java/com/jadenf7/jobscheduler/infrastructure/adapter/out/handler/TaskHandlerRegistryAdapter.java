package com.jadenf7.jobscheduler.infrastructure.adapter.out.handler;

import com.jadenf7.jobscheduler.domain.exception.NoHandlerFoundException;
import com.jadenf7.jobscheduler.domain.port.out.TaskHandler;
import com.jadenf7.jobscheduler.domain.port.out.TaskHandlerRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TaskHandlerRegistryAdapter implements TaskHandlerRegistry {

    private final Map<String, TaskHandler> handlersByType;

    public TaskHandlerRegistryAdapter(List<TaskHandler> handlers) {
        this.handlersByType = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(TaskHandler::supportedType, Function.identity()));
    }

    @Override
    public TaskHandler getHandler(String taskType) {
        TaskHandler handler = handlersByType.get(taskType);
        if (handler == null) {
            throw new NoHandlerFoundException(taskType);
        }
        return handler;
    }

}