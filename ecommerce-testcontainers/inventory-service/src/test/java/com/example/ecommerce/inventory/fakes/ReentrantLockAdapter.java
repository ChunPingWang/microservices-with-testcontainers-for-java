package com.example.ecommerce.inventory.fakes;

import com.example.ecommerce.inventory.domain.port.outbound.DistributedLockPort;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * In-process {@link DistributedLockPort} useful in unit tests. NOT safe for
 * multi-JVM use — for that, use the Redis adapter.
 */
public class ReentrantLockAdapter implements DistributedLockPort {

    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T withLock(String key, Duration waitFor, Duration holdFor, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitFor.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new LockAcquisitionFailedException(key);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionFailedException(key);
        } finally {
            if (acquired) lock.unlock();
        }
    }
}
