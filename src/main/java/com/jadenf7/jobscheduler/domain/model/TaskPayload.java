package com.jadenf7.jobscheduler.domain.model;

import java.util.Map;
import java.util.Objects;

public final class TaskPayload {
    private final String taskType;
    private final Map<String, String> data;

    private TaskPayload(String taskType, Map<String, String> data) {
        if (taskType == null || taskType.isBlank()) {
            throw new IllegalArgumentException("Task payload cannot be null or empty.");
        }
        this.taskType = taskType;
        this.data = Map.copyOf(data == null? Map.of() : data);
    }

    public static TaskPayload of(String taskType, Map<String, String> data) {return new TaskPayload(taskType, data);}

    public String taskType() { return taskType;}

    public Map<String, String> data() { return data;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskPayload)) return false;
        TaskPayload that = (TaskPayload) o;
        return taskType.equals(that.taskType) && data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskType, data);
    }

    @Override
    public String toString() {
        return "TaskPayload{taskType='" + taskType + "', data=" + data + '}';
    }
}
