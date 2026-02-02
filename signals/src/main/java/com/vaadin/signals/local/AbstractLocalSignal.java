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
package com.vaadin.signals.local;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.vaadin.signals.function.CleanupCallback;
import com.vaadin.signals.impl.TransientListener;
import com.vaadin.signals.impl.UsageTracker.Usage;

/**
 * Base class for local signals providing shared listener notification and usage
 * tracking logic.
 */
abstract class AbstractLocalSignal {

    /**
     * Listeners to notify on changes.
     */
    protected final List<TransientListener> listeners = new ArrayList<>();

    /**
     * Lock for thread-safe access to signal state.
     */
    // package-protected for testing
    final ReentrantLock lock = new ReentrantLock();

    /**
     * Version counter incremented on each change.
     */
    protected int version;

    /**
     * Notifies all registered listeners of a change and increments the version.
     * Must be called while holding the lock.
     */
    protected void notifyListeners() {
        assert lock.isHeldByCurrentThread();
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
     * Creates a Usage object for tracking changes from the given version.
     *
     * @param originalVersion
     *            the version at the time of usage registration
     * @return a Usage object for change detection
     */
    protected Usage createUsage(int originalVersion) {
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
            public CleanupCallback onNextChange(TransientListener listener) {
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
}
