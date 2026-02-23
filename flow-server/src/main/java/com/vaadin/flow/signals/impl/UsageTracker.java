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
package com.vaadin.flow.signals.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.function.ValueSupplier;

/**
 * Tracks signal value read operations while a task is run.
 */
public class UsageTracker {
    static final class CombinedUsage implements Usage {
        private final Collection<Usage> usages;

        CombinedUsage(Collection<Usage> usages) {
            this.usages = usages;
        }

        @Override
        public boolean hasChanges() {
            return usages.stream().anyMatch(Usage::hasChanges);
        }

        @Override
        public Registration onNextChange(TransientListener listener) {
            return new Registration() {
                /*
                 * Synchronize since listeners can fire at any time, e.g. before
                 * all listeners have been registered, or while running the
                 * action due to another listener being fired, or during cleanup
                 */
                final Object lock = new Object();
                final Collection<Registration> cleanups = new ArrayList<>();

                boolean closed = false;

                {
                    synchronized (lock) {
                        for (Usage usage : usages) {
                            // avoid lambda to allow proper deserialization
                            TransientListener usageListener = new TransientListener() {
                                @Override
                                public boolean invoke(boolean immediate) {
                                    return onChange(immediate);
                                }
                            };
                            Registration cleanup = usage
                                    .onNextChange(usageListener);
                            if (closed) {
                                cleanup.remove();
                                break;
                            } else {
                                cleanups.add(cleanup);
                            }
                        }
                    }
                }

                private boolean onChange(boolean immediate) {
                    synchronized (lock) {
                        if (closed) {
                            return false;
                        }
                        boolean listenToNext = listener.invoke(immediate);
                        if (!listenToNext) {
                            close();
                        }
                        return listenToNext;
                    }
                }

                private void close() {
                    synchronized (lock) {
                        if (closed) {
                            return;
                        }
                        closed = true;
                    }
                    // Important release the lock before calling signal methods
                    cleanups.forEach(Registration::remove);
                    cleanups.clear();
                }

                @Override
                public void remove() {
                    close();
                }
            };
        }
    }

    /**
     * Tracks the state of some used value.
     */
    public interface Usage extends Serializable {
        /**
         * Checks whether the used value has subsequently been changed.
         *
         * @return <code>true</code> if the value has been changed,
         *         <code>false</code> if there is no change
         */
        boolean hasChanges();

        /**
         * Registers a listener that will be invoked the next time there's a
         * change to the used value. If this usage already has changes, then the
         * listener is invoked immediately.
         *
         * @param listener
         *            the listener to use, not <code>null</code>
         * @return a {@link Registration} for removing the listener, not
         *         <code>null</code>
         */
        Registration onNextChange(TransientListener listener);
    }

    /**
     * Receives notifications about signal usage events.
     */
    @FunctionalInterface
    public interface UsageRegistrar extends Serializable {
        /**
         * Called when a usage event occurs.
         *
         * @param usage
         *            the usage instance, not <code>null</code>
         */
        void register(Usage usage);
    }

    /**
     * A usage that doesn't have any changes and never fires any events.
     */
    public static final Usage NO_USAGE = new Usage() {
        @Override
        public boolean hasChanges() {
            return false;
        }

        @Override
        public Registration onNextChange(TransientListener listener) {
            return () -> {
            };
        }
    };

    /**
     * Sentinel registrar that represents a deliberately untracked context,
     * distinguishing it from having no tracking context at all.
     */
    private static final UsageRegistrar DELIBERATELY_UNTRACKED = usage -> {
    };

    private static final ThreadLocal<UsageRegistrar> currentTracker = new ThreadLocal<>();

    private UsageTracker() {
        // Only static methods
    }

    /**
     * Runs the given task while collecting all cases where a managed value is
     * used.
     *
     * @param task
     *            the task to run, not <code>null</code>
     * @return a usage instance that combines all used managed values, not
     *         <code>null</code>
     */
    public static Usage track(SerializableRunnable task) {
        Collection<Usage> usages = new ArrayList<>();

        track(task, usages::add);

        int usageSize = usages.size();
        if (usageSize == 0) {
            return NO_USAGE;
        } else if (usageSize == 1) {
            return usages.iterator().next();
        } else {
            return new CombinedUsage(usages);
        }
    }

    /**
     * Runs the given task while reacting to all cases where a managed value is
     * used.
     *
     * @param task
     *            the task to run, not <code>null</code>
     * @param tracker
     *            a consumer that receives all usages as they happen, not
     *            <code>null</code>
     */
    public static void track(SerializableRunnable task,
            UsageRegistrar tracker) {
        assert task != null;
        assert tracker != null;

        track(() -> {
            task.run();
            return null;
        }, tracker);
    }

    /**
     * Runs the given task with return value while reacting to all cases where a
     * managed value is used.
     *
     * @param task
     *            the task to run, not <code>null</code>
     * @param tracker
     *            a consumer that receives all usages as they happen, not
     *            <code>null</code>
     * @param <T>
     *            the task return type
     * @return the value returned from the task
     */
    public static <T> T track(Supplier<T> task, UsageRegistrar tracker) {
        assert task != null;
        assert tracker != null;

        var previousTracker = currentTracker.get();
        try {
            currentTracker.set(tracker);

            return task.get();
        } finally {
            currentTracker.set(previousTracker);
        }
    }

    /**
     * Runs the given supplier without tracking usage even if a usage tracker is
     * active.
     *
     * @param <T>
     *            the supplier type
     * @param task
     *            the supplier task to run, not <code>null</code>
     * @return the value returned from the supplier
     */
    public static <T> @Nullable T untracked(ValueSupplier<T> task) {
        var previousTracker = currentTracker.get();
        if (previousTracker == DELIBERATELY_UNTRACKED) {
            return task.supply();
        }

        try {
            currentTracker.set(DELIBERATELY_UNTRACKED);

            return task.supply();
        } finally {
            if (previousTracker == null) {
                currentTracker.remove();
            } else {
                currentTracker.set(previousTracker);
            }
        }
    }

    /**
     * Registers a usage with the current usage tracker. This method should be
     * run only if usage tracking is {@link #isActive() active}.
     *
     * @param usage
     *            the usage instance to register, not <code>null</code>
     */
    public static void registerUsage(Usage usage) {
        UsageRegistrar tracker = currentTracker.get();
        assert tracker != null;
        tracker.register(usage);
    }

    /**
     * Checks whether calling {@code Signal.get()} is allowed in the current
     * context. Returns {@code true} when a real usage tracker is active or when
     * inside a deliberately untracked context (e.g. via
     * {@code Signal.untracked()} or {@code Signal.peek()}).
     *
     * @return {@code true} if calling {@code get()} is allowed
     */
    public static boolean isGetAllowed() {
        return currentTracker.get() != null;
    }

    /**
     * Checks whether a usage tracker is currently active. Returns {@code true}
     * only for real trackers, not the deliberately untracked sentinel.
     *
     * @return <code>true</code> if a usage tracker is active
     */
    public static boolean isActive() {
        UsageRegistrar tracker = currentTracker.get();
        return tracker != null && tracker != DELIBERATELY_UNTRACKED;
    }

}
