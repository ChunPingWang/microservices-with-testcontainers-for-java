package com.example.ecommerce.inventory.domain.port.outbound;

import java.time.Duration;
import java.util.function.Supplier;

public interface DistributedLockPort {

    /**
     * Run {@code action} while holding the lock keyed by {@code key}. The lock
     * is automatically released. {@code waitFor} bounds how long we'll wait
     * to acquire; {@code holdFor} caps how long we hold once we acquire.
     *
     * @throws LockAcquisitionFailedException if the lock can't be obtained
     */
    <T> T withLock(String key, Duration waitFor, Duration holdFor, Supplier<T> action);

    class LockAcquisitionFailedException extends RuntimeException {
        public LockAcquisitionFailedException(String key) {
            super("failed to acquire distributed lock for key=" + key);
        }
    }
}
