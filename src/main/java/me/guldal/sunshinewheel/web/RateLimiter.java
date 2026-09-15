package me.guldal.sunshinewheel.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Tiny per-IP fixed-window rate limiter (defense in depth for direct exposure). */
final class RateLimiter {

    private final Map<String, long[]> buckets = new ConcurrentHashMap<>();

    /** @return true if the request is within maxPerMinute for this key. */
    boolean allow(String key, int maxPerMinute) {
        long window = System.currentTimeMillis() / 60_000L;
        long[] slot = buckets.compute(key,
                (k, old) -> old == null || old[0] != window ? new long[] {window, 1} : new long[] {window, old[1] + 1});
        return slot[1] <= maxPerMinute;
    }

    void prune() {
        long current = System.currentTimeMillis() / 60_000L;
        buckets.entrySet().removeIf(entry -> entry.getValue()[0] < current - 1);
    }
}
