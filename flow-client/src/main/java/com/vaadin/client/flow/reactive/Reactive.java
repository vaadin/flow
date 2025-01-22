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
package com.vaadin.client.flow.reactive;

import com.vaadin.client.Command;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsSet;

import elemental.events.EventRemover;

/**
 * Handles global features related to reactivity, such as keeping track of the
 * current {@link Computation}, providing a lazy flush cycle and registering
 * reactive event collectors.
 * <p>
 * With a reactive programming model, the dependencies needed for producing a
 * result are automatically registered when the result is computed. When any
 * dependency of a computation is changed, that computation is scheduled to be
 * recomputed. To reduce the number of recomputations performed when many
 * dependencies are updated, the recomputation is performed lazily the next time
 * {@link #flush()} is invoked.
 *
 * @see Computation
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Reactive {

    // Initializing static fields would cause nasty $clinit in the generated JS
    private static JsArray<FlushListener> flushListeners;

    private static JsArray<FlushListener> postFlushListeners;

    private static JsSet<ReactiveValueChangeListener> eventCollectors;

    private static Computation currentComputation = null;

    private static boolean flushing = false;

    private Reactive() {
        // Only static stuff in this class
    }

    /**
     * Adds a listener that will be invoked the next time {@link #flush()} is
     * invoked. A listener added during a flush will be invoked before that
     * flush finishes.
     *
     * @param flushListener
     *            the flush listener to add
     */
    public static void addFlushListener(FlushListener flushListener) {
        if (flushListeners == null) {
            flushListeners = JsCollections.array();
        }
        flushListeners.push(flushListener);
    }

    /**
     * Adds a listener that will be invoked during the next {@link #flush()},
     * after all regular flush listeners have been invoked. If a post flush
     * listener adds new flush listeners, those flush listeners will be invoked
     * before the next post flush listener is invoked.
     *
     * @param postFlushListener
     *            the listener to add
     */
    public static void addPostFlushListener(FlushListener postFlushListener) {
        if (postFlushListeners == null) {
            postFlushListeners = JsCollections.array();
        }
        postFlushListeners.push(postFlushListener);
    }

    /**
     * Flushes all flush listeners and post flush listeners. A listener is
     * discarded after it has been invoked once. This means that there will be
     * no listeners registered for the next flush at the time this method
     * returns.
     *
     * @see #addFlushListener(FlushListener)
     * @see #addPostFlushListener(FlushListener)
     */
    public static void flush() {
        if (flushing) {
            return;
        }
        try {
            flushing = true;
            while (hasFlushListeners() || hasPostFlushListeners()) {
                // Purge all flush listeners
                while (hasFlushListeners()) {
                    FlushListener oldestListener = flushListeners.remove(0);
                    oldestListener.flush();
                }

                // Purge one post flush listener, then look if there are new
                // flush
                // listeners to purge
                if (hasPostFlushListeners()) {
                    FlushListener oldestListener = postFlushListeners.remove(0);
                    oldestListener.flush();
                }
            }
        } finally {
            flushing = false;
        }
    }

    private static boolean hasPostFlushListeners() {
        return postFlushListeners != null && !postFlushListeners.isEmpty();
    }

    private static boolean hasFlushListeners() {
        return flushListeners != null && !flushListeners.isEmpty();
    }

    /**
     * Gets the currently active computation. Any reactive value that is
     * accessed when a computation is active should be added as a dependency to
     * that computation so that the computation will be invalidated if the value
     * changes.
     *
     * @return the current computation, or <code>null</code> if there is no
     *         current computation.
     */
    public static Computation getCurrentComputation() {
        return currentComputation;
    }

    /**
     * Runs a task with the given computation set as
     * {@link #getCurrentComputation()}. If another computation is set as the
     * current computation, it is temporarily replaced by the provided
     * computation, but restored again after the provided task has been run.
     *
     * @param computation
     *            the computation to set as current
     * @param command
     *            the command to run while the computation is set as current
     */
    public static void runWithComputation(Computation computation,
            Command command) {
        Computation oldComputation = currentComputation;
        currentComputation = computation;
        try {
            command.execute();
        } finally {
            currentComputation = oldComputation;
        }
    }

    /**
     * Adds a reactive change listener that will be invoked whenever a reactive
     * change event is fired from any reactive event router.
     *
     * @param reactiveValueChangeListener
     *            the listener to add
     * @return an event remover that can be used to remove the listener
     */
    public static EventRemover addEventCollector(
            ReactiveValueChangeListener reactiveValueChangeListener) {
        if (eventCollectors == null) {
            eventCollectors = JsCollections.set();
        }
        eventCollectors.add(reactiveValueChangeListener);

        return () -> eventCollectors.delete(reactiveValueChangeListener);
    }

    /**
     * Fires a reactive change event to all registered event collectors.
     *
     * @see #addEventCollector(ReactiveValueChangeListener)
     *
     * @param event
     *            the fired event
     */
    public static void notifyEventCollectors(ReactiveValueChangeEvent event) {
        if (eventCollectors != null) {
            JsSet<ReactiveValueChangeListener> copy = JsCollections
                    .set(eventCollectors);

            copy.forEach(listener -> listener.onValueChange(event));
        }
    }

    /**
     * Evaluates the given command whenever there is a change in any
     * {@link ReactiveValue} used in the command.
     *
     * @param command
     *            the command to run
     * @return A {@link Computation} object which can be used to control the
     *         evaluation
     */
    public static Computation runWhenDependenciesChange(Command command) {
        return new Computation() {
            @Override
            protected void doRecompute() {
                command.execute();
            }
        };
    }

    /**
     * Resets Reactive to the initial state.
     * <p>
     * Intended for test cases to call in setup to avoid having tests affect
     * each other as Reactive state is static and shared.
     * <p>
     * Should never be called from non-test code!
     *
     */
    public static void reset() {
        flushListeners = null;
        eventCollectors = null;
        currentComputation = null;
        postFlushListeners = null;
    }

}
