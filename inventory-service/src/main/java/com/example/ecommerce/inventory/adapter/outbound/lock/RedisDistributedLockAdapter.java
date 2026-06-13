package com.example.ecommerce.inventory.adapter.outbound.lock;

import com.example.ecommerce.inventory.domain.port.outbound.DistributedLockPort;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis-backed distributed lock via Redisson. Uses {@code tryLock} so we can
 * surface acquisition failures cleanly to callers instead of blocking forever.
 */
@Component
public class RedisDistributedLockAdapter implements DistributedLockPort {

    private final RedissonClient redisson;

    public RedisDistributedLockAdapter(RedissonClient redisson) {
        this.redisson = redisson;
    }

    @Override
    public <T> T withLock(String key, Duration waitFor, Duration holdFor, Supplier<T> action) {
        RLock lock = redisson.getLock(key);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitFor.toMillis(), holdFor.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new LockAcquisitionFailedException(key);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionFailedException(key);
        } catch (org.redisson.client.RedisException e) {
            // Connection severed / Redis unavailable — surface the canonical
            // domain failure so callers can fall back / retry instead of
            // leaking infrastructure exceptions out of the port.
            throw new LockAcquisitionFailedException(key);
        } finally {
            if (acquired) {
                try {
                    if (lock.isHeldByCurrentThread()) lock.unlock();
                } catch (org.redisson.client.RedisException ignored) {
                    // best effort; TTL will reap the key
                }
            }
        }
    }
}
