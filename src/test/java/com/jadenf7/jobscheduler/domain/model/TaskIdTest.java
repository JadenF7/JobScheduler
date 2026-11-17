package com.jadenf7.jobscheduler.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class TaskIdTest {

    @Test
    void shouldCreateValidTaskId() {
        String rawId = "task-123";

        TaskId taskId = TaskId.of(rawId);

        assertThat(taskId.value()).isEqualTo(rawId);
    }

    @Test
    void shouldGenerateUniqueTaskIds() {
        TaskId id1 = TaskId.generate();
        TaskId id2 = TaskId.generate();

        assertThat(id1.value()).isNotNull().isNotBlank();
        assertThat(id2.value()).isNotNull().isNotBlank();
        assertThat(id1).isNotEqualTo(id2);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings={"   ", "\t", "\n"})
    void shouldThrowWhenValueIsInvalid(String invalidValue) {
        assertThatThrownBy(() -> TaskId.of(invalidValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TaskId value must not be blank");
    }

    @Test
    void shouldSupportValueEquality() {
        TaskId id1 = TaskId.of("task-abc");
        TaskId id2 = TaskId.of("task-abc");
        TaskId id3 = TaskId.of("task-xyz");

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());

        assertThat(id1).isNotEqualTo(id3);
    }

    @Test
    void shouldReturnRawValueOnToString() {
        TaskId id = TaskId.of("task-123");

        assertThat(id.toString()).isEqualTo("task-123");
    }
}
