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
import java.util.List;
import java.util.Objects;

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
     * Runs the given task and tracks its usage of managed values through the
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
     * @param <T>
     *            the type returned by the task
     * @return a new TrackableSupplier that wraps the given task and tracks its
     *         usage through the provided tracker
     */
    public static <T> TrackedSupplier<T> tracked(TrackableSupplier<T> task) {
        assert task != null;
        return new TrackedSupplier<>(task);
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

    /**
     * Thrown when a signal callback did not read any other signals.
     * <p>
     * Indicates that the user's computed signal or effect action most likely
     * has an error, as we expect at least one signal value to be read in such
     * callbacks.
     */
    public static class MissingSignalUsageException
            extends IllegalStateException {
        /**
         * Thrown when a signal callback did not read any other signals.
         *
         * @param message
         *            the context message, not <code>null</code>
         */
        public MissingSignalUsageException(String message) {
            super(message + " " + """
                    Expected at least one signal value read in the callback, \
                    but no signal values were read.""");
        }
    }

    /**
     * Thrown when an invalid signal usage was detected.
     * <p>
     * This error indicates that a signal value was used where it is disallowed.
     */
    public static class DeniedSignalUsageException
            extends IllegalStateException {
        public DeniedSignalUsageException(String message) {
            super(message + " Using signals is denied in this context.");
        }
    }

    /**
     * Thrown when a circular signal usage is detected during dependency
     * tracking.
     * <p>
     * This exception indicates that a computation involves signals in a way
     * that creates a circular dependency, which creates infinite loops.
     */
    public static class CircularSignalUsageException
            extends IllegalStateException {
        public CircularSignalUsageException() {
            super("Infinite loop detected.");
        }
    }

    /**
     * A supplier that tracks and aggregates usage events from its underlying
     * {@link TrackableSupplier}. This class ensures that all signal usages
     * within the computation are recorded and can be queried or validated.
     * <p>
     * Each wrapped supplier is intended to be invoked once using
     * {@link TrackedSupplier#supply()}.
     * <p>
     * A usage listener could optionally be added before invoking the supplier
     * using the {@link TrackedSupplier#withUsageListener(UsageRegistrar)} call.
     * <p>
     * After the supplier invocation is finished, the collected usage could be
     * retrieved from {@link TrackedSupplier#dependencies()}.
     *
     * @param <T>
     *            the type of value supplied by the wrapped supplier
     */
    public static final class TrackedSupplier<T>
            implements UsageRegistrar, TrackableSupplier<T>, Serializable {
        private final TrackableSupplier<T> supplier;
        @Nullable
        private UsageRegistrar registrar;
        @Nullable
        private List<Usage> usages = new ArrayList<>();
        private UsageTracker.@Nullable Usage dependencies;

        TrackedSupplier(TrackableSupplier<T> supplier) {
            this.supplier = supplier;
        }

        /**
         * Sets up the {@link UsageRegistrar} for tracking signal usage within
         * this supplier.
         *
         * @param registrar
         *            the registrar to receive notifications about signal usage
         *            events.
         * @return the instance with the usage listener.
         */
        public TrackedSupplier<T> withUsageListener(UsageRegistrar registrar) {
            this.registrar = registrar;
            return this;
        }

        /**
         * Retrieves a {@link Usage} object representing the dependencies of
         * some computation.
         *
         * @return current dependencies of the computation, not
         *         <code>null</code>
         */
        public Usage dependencies() {
            if (dependencies != null) {
                return dependencies;
            }

            dependencies = switch (Objects.requireNonNull(usages).size()) {
            case 0 -> NO_USAGE;
            case 1 -> usages.iterator().next();
            default -> new CombinedUsage(usages);
            };

            return dependencies;
        }

        /**
         * Asserts that the current computation dependencies contain at least
         * one usage.
         *
         * <p>
         * This method is used to validate that a signal computation or effect
         * action tracked within
         * {@link UsageTracker#tracked(TrackableSupplier)}.
         *
         * @param message
         *            the error message to add for the exception
         * @throws MissingSignalUsageException
         *             if the current dependencies do not contain any signal
         *             usage (i.e., no signal values were read by the callback).
         */
        public void assertHasUsage(String message)
                throws MissingSignalUsageException {
            if (NO_USAGE.equals(dependencies())) {
                throw new MissingSignalUsageException(message);
            }
        }

        /**
         * Asserts that the current computation dependencies do not contain any
         * signal usage.
         *
         * <p>
         * This method ensures that no signal values are read within the
         * computation tracked within
         * {@link UsageTracker#tracked(TrackableSupplier)}.
         *
         * @throws DeniedSignalUsageException
         *             if the current dependencies include any signal usages,
         *             indicating that signals were improperly utilized in this
         *             context.
         */
        public void assertNoUsage(String message)
                throws DeniedSignalUsageException {
            if (!NO_USAGE.equals(dependencies())) {
                throw new DeniedSignalUsageException(message);
            }
        }

        @Override
        public void register(Usage usage) {
            if (usages == null) {
                throw new IllegalStateException(
                        "Dependencies were already collected.");
            }
            if (this.registrar != null) {
                this.registrar.register(usage);
            }
            usages.add(usage);
        }

        @Override
        public @Nullable T supply() {
            var previousTracker = currentTracker.get();
            try {
                currentTracker.set(this);
                return supplier.supply();
            } finally {
                currentTracker.set(previousTracker);
            }
        }
    }

    /**
     * Receives notifications about signal usage events.
     */
    @FunctionalInterface
    public static interface UsageRegistrar extends Serializable {
        /**
         * Called when a usage event occurs.
         *
         * @param usage
         *            the usage instance, not <code>null</code>
         */
        void register(Usage usage);
    }
}
