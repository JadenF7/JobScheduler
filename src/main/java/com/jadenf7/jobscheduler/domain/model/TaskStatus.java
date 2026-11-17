package com.jadenf7.jobscheduler.domain.model;

import java.util.Set;

public enum TaskStatus {
    PENDING {
        @Override
        public Set<TaskStatus> allowedNextStates() {return Set.of(RUNNING, CANCELLED); }
    },
    RUNNING {
        @Override
        public Set<TaskStatus> allowedNextStates() {return Set.of(COMPLETED, FAILED); }
    },
    COMPLETED {
        @Override
        public Set<TaskStatus> allowedNextStates() {return Set.of(); }
    },
    FAILED {
        @Override
        public Set<TaskStatus> allowedNextStates() {return Set.of(PENDING); }
    },
    CANCELLED {
        @Override
        public Set<TaskStatus> allowedNextStates() {return Set.of(); }
    };

    public abstract Set<TaskStatus> allowedNextStates();

    public boolean canTransitionTo(TaskStatus next) {
        return allowedNextStates().contains(next);
    }
}
