package com.jadenf7.jobscheduler.domain.port.in;

import com.jadenf7.jobscheduler.domain.model.TaskId;
import com.jadenf7.jobscheduler.domain.model.TaskPayload;

public interface SubmitTaskUseCase {

    TaskId submitTask(TaskPayload taskPayload);
}
