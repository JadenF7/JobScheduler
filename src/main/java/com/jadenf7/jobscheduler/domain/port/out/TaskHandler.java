package com.jadenf7.jobscheduler.domain.port.out;

import com.jadenf7.jobscheduler.domain.model.TaskPayload;

public interface TaskHandler {
    String supportedType();

    void handle(TaskPayload taskPayload) throws Exception;
}
