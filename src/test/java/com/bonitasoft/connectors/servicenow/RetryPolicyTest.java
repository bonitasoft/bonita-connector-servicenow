package com.bonitasoft.connectors.servicenow;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryPolicyTest {

    @Test
    void shouldReturnResultOnFirstSuccess() throws ServiceNowException {
        RetryPolicy policy = new RetryPolicy(3);
        String result = policy.execute(() -> "ok");
        assertThat(result).isEqualTo("ok");
    }

    @Test
    void shouldRetryOnRetryableException() throws ServiceNowException {
        RetryPolicy policy = new TestRetryPolicy(3);

        AtomicInteger attempts = new AtomicInteger(0);
        String result = policy.execute(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new ServiceNowException("rate limited", 429, true);
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void shouldNotRetryOnNonRetryableException() {
        RetryPolicy policy = new TestRetryPolicy(3);

        assertThatThrownBy(() -> policy.execute(() -> {
            throw new ServiceNowException("not found", 404, false);
        })).isInstanceOf(ServiceNowException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void shouldExhaustRetriesAndThrow() {
        RetryPolicy policy = new TestRetryPolicy(2);

        assertThatThrownBy(() -> policy.execute(() -> {
            throw new ServiceNowException("server error", 500, true);
        })).isInstanceOf(ServiceNowException.class)
                .hasMessageContaining("server error");
    }

    @Test
    void shouldIdentifyRetryableStatusCodes() {
        assertThat(RetryPolicy.isRetryableStatusCode(429)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(500)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(502)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(503)).isTrue();
        assertThat(RetryPolicy.isRetryableStatusCode(404)).isFalse();
        assertThat(RetryPolicy.isRetryableStatusCode(400)).isFalse();
    }

    @Test
    void shouldCalculateExponentialBackoff() {
        RetryPolicy policy = new RetryPolicy(3);
        long wait0 = policy.calculateWait(0);
        long wait1 = policy.calculateWait(1);
        long wait2 = policy.calculateWait(2);

        // Exponential base: 1000, 2000, 4000 + jitter
        assertThat(wait0).isBetween(1000L, 1500L);
        assertThat(wait1).isBetween(2000L, 3000L);
        assertThat(wait2).isBetween(4000L, 6000L);
    }

    /** Test-only subclass that skips actual sleeping. */
    private static class TestRetryPolicy extends RetryPolicy {
        TestRetryPolicy(int maxRetries) {
            super(maxRetries);
        }

        @Override
        void sleep(long millis) {
            // no-op for tests
        }
    }
}
