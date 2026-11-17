package com.jadenf7.jobscheduler.domain.model;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class RetryPolicyTest {
    private final RetryPolicy policy = new RetryPolicy(5, Duration.ofSeconds(5), Duration.ofMinutes(5));

    @ParameterizedTest
    @CsvSource({"1, 5", "2, 10", "3, 20", "4, 40", "5, 80"})
    void delayDoublesEachRetry(int retryNumber, long expectedSeconds) {
        assertThat(policy.delayFor(retryNumber)).isEqualTo(Duration.ofSeconds(expectedSeconds));
    }

    @Test
    void delayIsCappedAtMax() {
        assertThat(policy.delayFor(10)).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void hugeRetryNumberDoesNotOverflow() {
        assertThat(policy.delayFor(500)).isEqualTo(Duration.ofMinutes(5));
    }

    @RepeatedTest(200)
    void jitterStaysBetweenHalfAndFullDelay() {
        Duration full = policy.delayFor(3);
        assertThat(policy.delayWithJitter(3)).isBetween(full.dividedBy(2), full);
    }
}
