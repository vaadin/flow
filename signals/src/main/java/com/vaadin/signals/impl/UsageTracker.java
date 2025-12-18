/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.signals.impl;

import java.util.ArrayList;
import java.util.Collection;

import com.vaadin.signals.function.CleanupCallback;
import com.vaadin.signals.function.ValueSupplier;

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
        public CleanupCallback onNextChange(TransientListener listener) {
            return new CleanupCallback() {
                /*
                 * Synchronize since listeners can fire at any time, e.g. before
                 * all listeners have been registered, or while running the
                 * action due to another listener being fired, or during cleanup
                 */
                final Object lock = new Object();
                final Collection<CleanupCallback> cleanups = new ArrayList<>();

                boolean closed = false;

                {
                    synchronized (lock) {
                        for (Usage usage : usages) {
                            CleanupCallback cleanup = usage
                                    .onNextChange(this::onChange);
                            if (closed) {
                                cleanup.cleanup();
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
                        closed = true;
                        cleanups.forEach(CleanupCallback::cleanup);
                        cleanups.clear();
                    }
                }

                @Override
                public void cleanup() {
                    close();
                }
            };
        }
    }

    /**
     * Tracks the state of some used value.
     */
    public interface Usage {
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
         * @return a callback for removing the listener, not <code>null</code>
         */
        CleanupCallback onNextChange(TransientListener listener);
    }

    /**
     * Receives notifications about signal usage events.
     */
    @FunctionalInterface
    public interface UsageRegistrar {
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
        public CleanupCallback onNextChange(TransientListener listener) {
            return () -> {
            };
        }
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
    public static Usage track(Runnable task) {
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
    public static void track(Runnable task, UsageRegistrar tracker) {
        assert task != null;
        assert tracker != null;

        var previousTracker = currentTracker.get();
        try {
            currentTracker.set(tracker);

            task.run();
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
    public static <T> T untracked(ValueSupplier<T> task) {
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
