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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Global configuration required by all kinds of signals. There is an object
 * mapper that is used for converting signal values to and from their underlying
 * JSON representation. There's an asynchronous dispatcher used for running
 * {@link Signal#effect(Runnable) effect} callbacks. The dispatcher does not
 * have to ensure tasks are executed in the order they were submitted. There can
 * optionally be dispatcher overrides that customize how tasks are dispatched in
 * specific cases.
 */
public class SignalEnvironment {
    private record InitializationState(ObjectMapper objectMapper,
            Executor dispatcher) {
    };

    private static final AtomicReference<InitializationState> state = new AtomicReference<>();

    private static final List<Supplier<Executor>> dispacherOverrides = new CopyOnWriteArrayList<>();

    private SignalEnvironment() {
        // Only static stuff
    }

    /**
     * Initializes the global signal environment if not already initialized. The
     * provided values are ignored if the environment is already initialized.
     * The environment must be initialized before signals are used.
     *
     * @param objectMapper
     *            the object mapper to use, not <code>null</code>
     * @param dispatcher
     *            the asynchronous dispatcher to use, not <code>null</code>
     * @return <code>true</code> if the provided values were used to initialize
     *         the environment, <code>false</code> if the environment was
     *         already initialized
     */
    public static boolean tryInitialize(ObjectMapper objectMapper,
            Executor dispatcher) {
        Objects.requireNonNull(objectMapper);
        Objects.requireNonNull(dispatcher);

        return state.compareAndSet(null,
                new InitializationState(objectMapper, dispatcher));
    }

    /**
     * Checks whether the environment is already initialized.
     *
     * @return <code>true</code> if initialized, <code>false</code> if not
     *         initialized
     */
    public static boolean initialized() {
        return state.get() != null;
    }

    /**
     * Adds a new supplier for override dispatchers. A supplier can inspect the
     * circumstances under in which it's run and optionally provide a dispatcher
     * to use for tasks initiated under those circumstances. The supplier is
     * expected to use its own thread local variables to understand the context.
     * <p>
     * The dispatcher is used both for asynchronously dispatched effect
     * callbacks and for resolving signal operation results. The dispatcher
     * needs to preserve ordering within the context that it belongs to (which
     * is up to the implementation to define) so that operation results are
     * published in order of confirmation.
     *
     * @param dispatcherOverride
     *            a supplier that can return a dispatcher to use or
     *            <code>null</code> to not provide any dispatcher under those
     *            circumstances. Not <code>null</code>.
     * @return a callback that can be used to unregister the supplier, not
     *         <code>null</code>
     */
    public static Runnable addDispatcherOverride(
            Supplier<Executor> dispatcherOverride) {
        dispacherOverrides.add(0, Objects.requireNonNull(dispatcherOverride));
        return () -> {
            dispacherOverrides.remove(dispatcherOverride);
        };
    }

    private static InitializationState getState() {
        InitializationState result = state.get();
        if (result == null) {
            throw new IllegalStateException(
                    "The environment must be initialized before used");
        }

        return result;
    }

    /**
     * Gets the object mapper to use for converting signal values to and from
     * their underlying JSON representation
     *
     * @return the object mapper, not <code>null</code>
     * @throws IllegalStateException
     *             if the environment has not yet been initialized
     */
    public static ObjectMapper objectMapper() {
        return getState().objectMapper;
    }

    /**
     * Gets the dispatcher that was set when the environment was initialized
     * without looking up overrides.
     *
     * @return the configured dispatcher, not <code>null</code>
     * @throws IllegalStateException
     *             if the environment has not yet been initialized
     */
    public static Executor defaultDispatcher() {
        return getState().dispatcher;
    }

    /**
     * Gets a dispatcher to use for asynchronous tasks. This is used e.g. for
     * running {@link Signal#effect(Runnable) effect} callbacks. The dispatcher
     * does not guarantee that tasks are run in the order they have been
     * submitted. This method uses a dispatcher override if any supplier matches
     * and otherwise uses the dispatcher provided when the environment was
     * initialized.
     *
     * @return the dispatcher to use, not <code>null</code>
     * @throws IllegalStateException
     *             if the environment has not yet been initialized
     */
    public static Executor asynchronousDispatcher() {
        return resolveDispatcher(defaultDispatcher());
    }

    /**
     * Gets a dispatcher to use for synchronous tasks. This is used e.g. for
     * resolving operation results. The dispatcher guarantees that tasks for the
     * same context are run in the order they have been submitted. This method
     * uses a dispatcher override if any supplier matches and otherwise uses a
     * dispatcher that runs tasks on the invoking thread.
     *
     * @return the dispatcher to use, not <code>null</code>
     * @throws IllegalStateException
     *             if the environment has not yet been initialized
     */
    public static Executor synchronousDispatcher() {
        return resolveDispatcher(Runnable::run);
    }

    private static Executor resolveDispatcher(Executor baseline) {
        for (Supplier<Executor> supplier : dispacherOverrides) {
            Executor override = supplier.get();
            if (override != null) {
                return override;
            }
        }
        return baseline;
    }
}
