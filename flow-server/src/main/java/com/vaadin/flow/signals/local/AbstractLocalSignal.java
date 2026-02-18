/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.signals.local;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.impl.TransientListener;
import com.vaadin.flow.signals.impl.UsageTracker;
import com.vaadin.flow.signals.impl.UsageTracker.Usage;

/**
 * Base class for local signals providing shared listener notification, usage
 * tracking, and value access logic.
 *
 * @param <T>
 *            the signal value type
 */
public abstract class AbstractLocalSignal<T> implements Signal<T> {

    private final List<TransientListener> listeners = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private int version;
    private @Nullable T signalValue;

    /**
     * Creates a new signal with the given initial value.
     *
     * @param initialValue
     *            the initial value
     */
    protected AbstractLocalSignal(@Nullable T initialValue) {
        this.signalValue = initialValue;
    }

    /**
     * Hook for subclasses to perform precondition checks before accessing the
     * value. Called while holding the lock. Default implementation does
     * nothing.
     */
    protected void checkPreconditions() {
    }

    @Override
    public @Nullable T get() {
        lock.lock();
        try {
            checkPreconditions();
            if (UsageTracker.isActive()) {
                UsageTracker.registerUsage(createUsage());
            }
            return signalValue;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public @Nullable T peek() {
        lock.lock();
        try {
            checkPreconditions();
            return signalValue;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Acquires the lock. Must be followed by {@link #unlock()} in a finally
     * block.
     */
    protected void lock() {
        lock.lock();
    }

    /**
     * Releases the lock.
     */
    protected void unlock() {
        lock.unlock();
    }

    /**
     * Attempts to acquire the lock without blocking.
     *
     * @return true if the lock was acquired, false otherwise
     */
    protected boolean tryLock() {
        return lock.tryLock();
    }

    /**
     * Asserts that the current thread holds the lock.
     */
    protected void assertLockHeld() {
        assert lock.isHeldByCurrentThread();
    }

    /**
     * Gets the current signal value. Must be called while holding the lock.
     *
     * @return the current value
     */
    protected @Nullable T getSignalValue() {
        assertLockHeld();
        return signalValue;
    }

    /**
     * Gets the current signal value without checking that the lock is held.
     * Only use when the caller ensures thread-safety through other means.
     *
     * @return the current value
     */
    protected @Nullable T getSignalValueUnsafe() {
        return signalValue;
    }

    /**
     * Sets the signal value and notifies all registered listeners. Must be
     * called while holding the lock.
     *
     * @param value
     *            the new value
     */
    protected void setSignalValue(@Nullable T value) {
        assertLockHeld();
        this.signalValue = value;
        version++;
        List<TransientListener> copy = List.copyOf(listeners);
        listeners.clear();
        for (var listener : copy) {
            if (listener.invoke(false)) {
                listeners.add(listener);
            }
        }
    }

    /**
     * Creates a Usage object for tracking changes from the current version.
     * Must be called while holding the lock.
     *
     * @return a Usage object for change detection
     */
    private Usage createUsage() {
        assertLockHeld();
        int originalVersion = version;
        return new Usage() {
            @Override
            public boolean hasChanges() {
                lock.lock();
                try {
                    return version != originalVersion;
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public Registration onNextChange(TransientListener listener) {
                lock.lock();
                try {
                    if (hasChanges()) {
                        if (!listener.invoke(true)) {
                            return () -> {
                            };
                        }
                    }
                    listeners.add(listener);
                    return () -> {
                        lock.lock();
                        try {
                            listeners.remove(listener);
                        } finally {
                            lock.unlock();
                        }
                    };
                } finally {
                    lock.unlock();
                }
            }
        };
    }

    /**
     * Returns the lock for testing purposes.
     *
     * @return the lock
     */
    ReentrantLock getLock() {
        return lock;
    }
}
