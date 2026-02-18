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
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.shared.Registration;

/**
 * Collects and provides access to dependencies of computations.
 */
public sealed abstract class UsageDetector
        implements UsageRegistrar, Serializable {
    /**
     * Retrieves a {@link UsageTracker.Usage} object representing the
     * dependencies of some computation.
     *
     * @return current dependencies of the computation, not <code>null</code>
     */
    public abstract UsageTracker.Usage dependencies();

    static sealed class CollectingUsageDetector extends UsageDetector
            implements Serializable {
        @Nullable
        private Collection<PreventPrematureChangeUsage> usages = new ArrayList<>();
        private UsageTracker.@Nullable Usage dependencies;

        @Override
        public UsageTracker.Usage dependencies() {
            if (dependencies != null) {
                return dependencies;
            }

            dependencies = switch (Objects.requireNonNull(usages).size()) {
            case 0 -> UsageTracker.NO_USAGE;
            case 1 -> usages.iterator().next();
            default -> new UsageTracker.CombinedUsage(usages);
            };

            // Check for circular dependencies: if a change was detected during
            // action execution, it indicates a circular write
            if (hasCircularDependency()) {
                throw new CircularSignalUsageException();
            }

            return dependencies;
        }

        /**
         * Checks if there's a circular dependency by examining if changes
         * occurred during action execution. A usage that had changes when it
         * was created indicates that a signal was written synchronously during
         * the action that read it.
         */
        protected boolean hasCircularDependency() {
            for (PreventPrematureChangeUsage usage : Objects
                    .requireNonNull(usages)) {
                if (usage.hadChangesAtCreation()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void register(UsageTracker.Usage usage) {
            if (usages == null) {
                throw new IllegalStateException(
                        "Dependencies were already collected.");
            }
            usages.add(new PreventPrematureChangeUsage(usage));
        }
    }

    static final class NecessaryUsageDetector extends CollectingUsageDetector {
        private final String message;

        public NecessaryUsageDetector(String message) {
            this.message = message;
        }

        @Override
        public UsageTracker.Usage dependencies() {
            var dependencies = super.dependencies();
            if (UsageTracker.NO_USAGE.equals(dependencies)) {
                throw new MissingSignalUsageException(message);
            }
            return dependencies;
        }
    }

    static final class DeniedUsageDetector extends CollectingUsageDetector {
        private final String message;

        DeniedUsageDetector(String message) {
            this.message = message;
        }

        @Override
        public void register(UsageTracker.Usage usage) {
            throw new DeniedSignalUsageException(message);
        }
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
     * Creates and returns a default implementation of the {@link UsageDetector}
     * interface. It does not impose any restrictions on signal usage.
     *
     * @return a new collector instance, not <code>null</code>
     */
    public static UsageDetector createCollecting() {
        return new CollectingUsageDetector();
    }

    /**
     * Creates a {@link UsageDetector} that will enforce signal usage. This
     * detector will throw an exception if signal usage is absent. The context
     * explanation message and the origin of the requirement are provided for
     * error reporting.
     *
     * @param reason
     *            a descriptive message explaining why using signals is
     *            necessary, which will be included in the exception message if
     *            thrown, not <code>null</code>
     * @return a new not <code>null</code>
     */
    public static UsageDetector createNecessary(String reason) {
        return new NecessaryUsageDetector(reason);
    }

    /**
     * Creates a {@link UsageDetector} that will deny any signal usage. This
     * detector throws an exception when attempting to register dependencies,
     * effectively disallowing all signal-based computations.
     * <p>
     * This is typically used in contexts where signals cannot be accessed, such
     * as during certain phases of initialization, cleanup, or change rendering,
     * ensuring that accidental signal usage doesn't occur.
     *
     * @param message
     *            a descriptive message explaining why signal usage is denied in
     *            the context, which will be included in the exception message
     *            if thrown, not <code>null</code>
     * @return a new non-null {@link DeniedUsageDetector} instance
     */
    public static UsageDetector createDenied(String message) {
        return new DeniedUsageDetector(message);
    }

    static final class PreventPrematureChangeUsage
            implements UsageTracker.Usage {
        private boolean hadChangesWhenCreated;

        private final UsageTracker.Usage delegate;

        public PreventPrematureChangeUsage(UsageTracker.Usage delegate) {
            this.delegate = delegate;
            this.hadChangesWhenCreated = delegate.hasChanges();
        }

        @Override
        public boolean hasChanges() {
            return delegate.hasChanges();
        }

        public boolean hadChangesAtCreation() {
            return hadChangesWhenCreated;
        }

        @Override
        public Registration onNextChange(TransientListener listener) {
            return this.delegate.onNextChange(listener);
        }
    }
}
