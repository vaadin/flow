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

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.function.TrackableSupplier;
import com.vaadin.flow.signals.function.ValueSupplier;

/**
 * Tracks signal value read operations while a task is run.
 */
public class UsageTracker {
    static final class CombinedUsage implements Usage {
        private final Collection<? extends Usage> usages;

        CombinedUsage(Collection<? extends Usage> usages) {
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

    private static final ThreadLocal<UsageRegistrar> currentTracker = new ThreadLocal<>();

    private UsageTracker() {
        // Only static methods
    }

    /**
     * Wraps the given task to track its usage of managed values through the
     * provided tracker.
     *
     * <p>
     * When this supplier is invoked, it temporarily sets the current usage
     * tracker to the provided one during the execution of the wrapped task.
     * This allows any signal value accessed within the task to be registered
     * with the tracker for later inspection of its dependencies.
     * </p>
     *
     * @param task
     *            the task whose usage should be tracked, not {@code null}
     * @param tracker
     *            a consumer that will receive notifications about all managed
     *            values used by the task, not {@code null}
     * @param <T>
     *            the type returned by the task
     * @return a new TrackableSupplier that wraps the given task and tracks its
     *         usage through the provided tracker
     */
    public static <T> TrackableSupplier<T> tracked(TrackableSupplier<T> task,
            UsageRegistrar tracker) {
        assert task != null;
        assert tracker != null;

        // avoid lambda to allow proper deserialization
        return new TrackableSupplier<T>() {
            @Override
            public @Nullable T supply() {
                var previousTracker = currentTracker.get();
                try {
                    currentTracker.set(tracker);
                    return task.supply();
                } finally {
                    currentTracker.set(previousTracker);
                }
            }
        };
    }

    /**
     * Wraps the given task to avoid tracking usage even when a usage tracker is
     * active.
     *
     * @param <T>
     *            the task result type
     * @param task
     *            the task to wrap, not <code>null</code>
     * @return the wrapped supplier
     */
    public static <T> ValueSupplier<T> untracked(ValueSupplier<T> task) {
        // avoid lambda to allow proper deserialization
        return new ValueSupplier<T>() {
            @Override
            public @Nullable T supply() {
                var previousTracker = currentTracker.get();
                if (previousTracker == null) {
                    return task.supply();
                }
                try {
                    currentTracker.remove();

                    return task.supply();
                } finally {
                    currentTracker.set(previousTracker);
                }
            }
        };
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
     * Checks whether a usage tracker is currently active.
     *
     * @return <code>true</code> if a usage tracker is active
     */
    public static boolean isActive() {
        return currentTracker.get() != null;
    }

}
