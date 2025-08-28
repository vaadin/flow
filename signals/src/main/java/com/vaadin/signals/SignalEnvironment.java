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
package com.vaadin.signals;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * The context in which signal operations are processed. Gives frameworks
 * control over how application code is executed to allow acquiring relevant
 * locks while running callbacks or to run potentially slow operations
 * asynchronously without holding irrelevant locks. It is assumed that the
 * environment is based on a {@link ThreadLocal} with regards to how results can
 * be cached between invocations.
 */
public abstract class SignalEnvironment {
    private static final List<SignalEnvironment> environments = new CopyOnWriteArrayList<>();

    private static final Executor IMMEDIATE_EXECUTOR = Runnable::run;

    private static final Executor DEFAULT_EFFECT_DISPATCHER = task -> {
        resolve(SignalEnvironment::getFallbackEffectDispatcher,
                IMMEDIATE_EXECUTOR).execute(task);
    };

    /**
     * Checks whether this environment is active on the current thread. No other
     * instance methods will be run while the environment is not active.
     *
     * @return <code>true</code> if this environment is active,
     *         <code>false</code> if it's inactive
     */
    public abstract boolean isActive();

    /**
     * Gets an executor to use for asynchronously notifying about operation
     * results. This method is run when an operation is submitted and the
     * returned executor is used to notify of the results when the operation has
     * been fully processed. The executor can thus be used to make sure the
     * notification is delivered in the same context as where the operation was
     * initiated. The notification is delivered immediately if no notifier is
     * provided. It is recommended that the executor preserves ordering of
     * submitted tasks within the same context so that notifications are
     * delivered in the order that operations have been applied.
     *
     * @return an executor to use for notifying about operation results, or
     *         <code>null</code> to notify about results immediately
     */
    public abstract Executor getResultNotifier();

    /**
     * Gets an executor to use for running the callback of an effect. This
     * method is run when an effect is created and the returned executor is used
     * for running the effect callback after any change has been detected. The
     * executor can thus be used to make sure the effect callback is invoked in
     * the same context as where the effect was created. It is recommended that
     * the executor is asynchronous so that the thread that submitted the change
     * can proceed without waiting for all affected effects to be dispatched. If
     * no dispatcher is provided when an effect is created, then the effect
     * callback will be run according to the
     * {@link #getFallbackEffectDispatcher()} of the environment that is active
     * when a change is applied. This executor does not need to preserve
     * ordering since the effect callback always uses the latest signal values
     * without concern for in which order values have been changed.
     *
     * @return an executor to use for invoking effect callbacks, or
     *         <code>null</code> to use the fallback dispatcher
     */
    public abstract Executor getEffectDispatcher();

    /**
     * Gets an executor to use for running the callback of an effect that
     * doesn't have its own dispatcher. This method is run when applying any
     * change to an effect that has no own dispatcher. The executor can thus be
     * used to make effect callback invocations asynchronous rather than
     * blocking the thread that applied the change until all affected effects
     * have been processed. This executor does not need to preserve ordering
     * since the effect callback always uses the latest signal values without
     * concern for in which order values have been changed.
     *
     * @return the executor to use for invoking affected effects that don't have
     *         their own dispatcher, or <code>null</code> to invoke the
     *         callbacks immediately
     */
    public abstract Executor getFallbackEffectDispatcher();

    /**
     * Registers a signal environment to consider when processing signal
     * operations. The environment should be unregistered using the returned
     * callback when it's no longer needed.
     *
     * @param environment
     *            the environment to register, not <code>null</code>
     * @return callback for unregistering the environment, not <code>null</code>
     */
    public static Runnable register(SignalEnvironment environment) {
        environments.add(Objects.requireNonNull(environment));

        return () -> environments.remove(environment);
    }

    private static <T> T resolve(Function<SignalEnvironment, T> getter,
            T defaultValue) {
        return environments.stream().filter(SignalEnvironment::isActive)
                .map(getter).filter(Objects::nonNull).findFirst()
                .orElse(defaultValue);
    }

    /**
     * Queries currently active environments for an executor to use for
     * notifying the results of an operation that is currently submitted. An
     * immediate executor is returned if no executor is provided by registered
     * environments.
     *
     * @see #getResultNotifier()
     *
     * @return the executor to use, not <code>null</code>
     */
    public static Executor getCurrentResultNotifier() {
        return resolve(SignalEnvironment::getResultNotifier,
                IMMEDIATE_EXECUTOR);
    }

    /**
     * Queries currently active environments for an executor to use for running
     * the callbacks of an effect that is currently being created. If no
     * registered environment provides an executor, then this method returns an
     * executor that will delegate to the environment that is active when a
     * change is applied and otherwise run the callback immediately.
     *
     * @see #getEffectDispatcher()
     * @see #getFallbackEffectDispatcher()
     *
     * @return the executor to use, not <code>null</code>
     */
    public static Executor getCurrentEffectDispatcher() {
        return resolve(SignalEnvironment::getEffectDispatcher,
                DEFAULT_EFFECT_DISPATCHER);
    }
}
