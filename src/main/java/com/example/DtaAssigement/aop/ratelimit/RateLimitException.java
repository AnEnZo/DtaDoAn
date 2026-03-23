package com.example.DtaAssigement.aop.ratelimit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

/**
 * Exception thrown when rate limit is exceeded
 */
@Getter
@RequiredArgsConstructor
public class RateLimitException extends RuntimeException {

    private final int maxRequests;
    private final int windowSeconds;
    private final Instant retryAfter;

    @Override
    public String getMessage() {
        long secondsUntilRetry = retryAfter.getEpochSecond() - Instant.now().getEpochSecond();
        return String.format("Rate limit exceeded. Max %d requests per %d seconds. Retry after %d seconds.",
                maxRequests, windowSeconds, Math.max(0, secondsUntilRetry));
    }
}
