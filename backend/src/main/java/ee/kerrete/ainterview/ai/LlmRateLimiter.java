package ee.kerrete.ainterview.ai;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Caps language-model calls so a public test or an open registration cannot run the API key
 * without a limit. One window for the whole process, and a smaller window per client address.
 * In production the address is the one Spring reads after {@code Forwarded} headers from the proxy.
 */
@Component
public class LlmRateLimiter {

    static final long WINDOW_MILLIS = 3_600_000L;

    private final int perClientMax;
    private final int globalMax;
    private final long windowMillis;
    private final LongSupplier clock;
    private final Deque<Long> global = new ArrayDeque<>();
    private final Map<String, Deque<Long>> clients = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    @Autowired
    public LlmRateLimiter(
            @Value("${app.llm.per-client-per-hour:8}") int perClientMax,
            @Value("${app.llm.global-per-hour:60}") int globalMax) {
        this(perClientMax, globalMax, WINDOW_MILLIS, System::currentTimeMillis);
    }

    LlmRateLimiter(int perClientMax, int globalMax, long windowMillis, LongSupplier clock) {
        this.perClientMax = perClientMax;
        this.globalMax = globalMax;
        this.windowMillis = windowMillis;
        this.clock = clock;
    }

    /** Tests and the no-arg client constructors. Never refuses. */
    public static LlmRateLimiter unlimited() {
        return new LlmRateLimiter(Integer.MAX_VALUE, Integer.MAX_VALUE, WINDOW_MILLIS, System::currentTimeMillis);
    }

    /** Tests. Refuses every call. */
    public static LlmRateLimiter blocking() {
        return new LlmRateLimiter(0, 0, WINDOW_MILLIS, System::currentTimeMillis);
    }

    /**
     * Records a call when both windows have room. A refused call is not recorded.
     */
    public boolean tryAcquire(String key) {
        String bucketKey = key == null || key.isBlank() ? "unknown" : key;
        long now = clock.getAsLong();
        synchronized (lock) {
            prune(global, now);
            Deque<Long> bucket = clients.computeIfAbsent(bucketKey, ignored -> new ArrayDeque<>());
            prune(bucket, now);
            if (global.size() >= globalMax || bucket.size() >= perClientMax) {
                if (bucket.isEmpty()) {
                    clients.remove(bucketKey, bucket);
                }
                return false;
            }
            global.addLast(now);
            bucket.addLast(now);
            if (clients.size() > 10_000) {
                clients.entrySet().removeIf(e -> {
                    prune(e.getValue(), now);
                    return e.getValue().isEmpty();
                });
            }
            return true;
        }
    }

    /**
     * Address of the current HTTP request, or {@code no-request} when the call has no request
     * (a test, or work started off the request thread).
     */
    public static String clientKey() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String remote = request.getRemoteAddr();
            if (remote != null && !remote.isBlank()) {
                return remote;
            }
        }
        return "no-request";
    }

    private void prune(Deque<Long> hits, long now) {
        while (!hits.isEmpty() && now - hits.peekFirst() >= windowMillis) {
            hits.removeFirst();
        }
    }
}
