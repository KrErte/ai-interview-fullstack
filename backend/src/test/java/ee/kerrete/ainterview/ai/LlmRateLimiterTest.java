package ee.kerrete.ainterview.ai;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmRateLimiterTest {

    @Test
    void perClientWindowRefusesTheNinthCallAndResetsAfterAnHour() {
        AtomicLong now = new AtomicLong(1_000_000L);
        LlmRateLimiter limiter = new LlmRateLimiter(8, 60, LlmRateLimiter.WINDOW_MILLIS, now::get);

        for (int i = 0; i < 8; i++) {
            assertTrue(limiter.tryAcquire("10.0.0.1"), "call " + i);
        }
        assertFalse(limiter.tryAcquire("10.0.0.1"));
        assertTrue(limiter.tryAcquire("10.0.0.2"));

        now.addAndGet(LlmRateLimiter.WINDOW_MILLIS);
        assertTrue(limiter.tryAcquire("10.0.0.1"));
    }

    @Test
    void globalWindowStopsEveryClient() {
        AtomicLong now = new AtomicLong(0L);
        LlmRateLimiter limiter = new LlmRateLimiter(8, 2, LlmRateLimiter.WINDOW_MILLIS, now::get);

        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("b"));
        assertFalse(limiter.tryAcquire("c"));
    }
}
