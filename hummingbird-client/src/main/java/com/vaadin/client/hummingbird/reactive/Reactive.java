/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.client.hummingbird.reactive;

import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsSet;

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
 * @since
 * @author Vaadin Ltd
 */
public class Reactive {
    /**
     * A task to be run in a reactive context where dependencies are
     * automatically registered to accessed reactive values.
     */
    @FunctionalInterface
    public interface ReactiveTask {
        /**
         * Runs the task.
         */
        public void run();
    }

    // Initializing static fields would cause nasty $clinit in the generated JS
    private static JsArray<FlushListener> flushListeners;

    private static JsSet<ReactiveChangeListener> eventCollectors;

    private static Computation currentComputation = null;

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
     * Flushes all flush listeners. A flush listener is discarded after it has
     * been invoked once. This means that there will be no flush listeners
     * registered for the next flush at the time this method return.
     *
     * @see #addFlushListener(FlushListener)
     */
    public static void flush() {
        while (flushListeners != null && !flushListeners.isEmpty()) {
            FlushListener oldestListener = flushListeners.remove(0);
            oldestListener.flush();
        }
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
     * @param task
     *            the task to run while the computation is set as current
     */
    public static void runWithComputation(Computation computation,
            ReactiveTask task) {
        Computation oldComputation = currentComputation;
        currentComputation = computation;
        try {
            task.run();
        } finally {
            currentComputation = oldComputation;
        }
    }

    /**
     * Adds a reactive change listener that will be invoked whenever a reactive
     * change event is fired from any reactive event router.
     *
     * @param listener
     *            the listener to add
     * @return an event remover that can be used to remove the listener
     */
    public static EventRemover addEventCollector(
            ReactiveChangeListener listener) {
        if (eventCollectors == null) {
            eventCollectors = JsCollections.set();
        }
        eventCollectors.add(listener);

        return () -> eventCollectors.delete(listener);
    }

    /**
     * Fires a reactive change event to all registered event collectors.
     *
     * @see #addEventCollector(ReactiveChangeListener)
     *
     * @param event
     *            the fired event
     */
    public static void notifyEventCollectors(ReactiveChangeEvent event) {
        if (eventCollectors != null) {
            JsSet<ReactiveChangeListener> copy = JsCollections
                    .set(eventCollectors);

            copy.forEach(listener -> listener.onChange(event));
        }
    }
}
