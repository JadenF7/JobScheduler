package com.jadenf7.jobscheduler.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class TaskPayloadTest {

    @Test
    void shouldCreateValidTaskPayload() {
        Map<String, String> data = Map.of("key", "value");

        TaskPayload payload = TaskPayload.of("EMAIL_NOTIFICATION", data);

        assertThat(payload.taskType()).isEqualTo("EMAIL_NOTIFICATION");
        assertThat(payload.data()).containsEntry("key", "value");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"     ", "\t", "\n"})
    void shouldThrowWhenTaskTypeIsInvalid(String invalidTaskType) {
        assertThatThrownBy(() -> TaskPayload.of(invalidTaskType, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task payload cannot be null or empty.");
    }

    @Test
    void shouldHandleNullDataGracefully() {
        TaskPayload payload = TaskPayload.of("EMAIL_NOTIFICATION", null);

        assertThat(payload.data())
                .isNotNull()
                .isEmpty();
    }

    @Test
    void shouldBeImmutableFromSourceMapChanges() {
        Map<String, String> mutableData = new HashMap<>();
        mutableData.put("env", "prod");

        TaskPayload payload = TaskPayload.of("EMAIL_NOTIFICATION", mutableData);

        mutableData.put("env", "dev");

        assertThat(payload.data().get("env")).isEqualTo("prod");
    }

    @Test
    void shouldReturnUnmodifiableMap() {
        TaskPayload payload = TaskPayload.of("EMAIL_NOTIFICATION", Map.of("env", "prod"));

        assertThatThrownBy(() -> payload.data().put("newKey", "newValue"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

}
