package com.jadenf7.jobscheduler.domain.port.out;

import java.util.Optional;

public interface TaskHandlerRegistry {

    TaskHandler getHandler(String taskType);
}


