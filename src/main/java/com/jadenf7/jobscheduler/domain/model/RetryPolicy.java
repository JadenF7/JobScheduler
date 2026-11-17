package com.jadenf7.jobscheduler.domain.model;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public record RetryPolicy(int maxRetries, Duration baseDelay, Duration maxDelay) {

    public Duration delayFor(int retryNumber) {
        int exponent = Math.min(retryNumber - 1, 20);
        Duration delay = baseDelay.multipliedBy(1l << exponent);

        return delay.compareTo(maxDelay) > 0 ? maxDelay : delay;
    }

    public Duration delayWithJitter(int retryNumber) {
        long millis = delayFor(retryNumber).toMillis();
        long jittered = millis / 2 + ThreadLocalRandom.current().nextLong(millis / 2 + 1);

        return Duration.ofMillis(jittered);
    }
}
