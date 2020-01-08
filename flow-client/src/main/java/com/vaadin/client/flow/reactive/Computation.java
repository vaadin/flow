/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsSet;

import elemental.events.EventRemover;

/**
 * Automatically reruns {@link #doRecompute()} whenever any reactive value used
 * by it changes. The recompute method is invoked by the next invocation of
 * {@link Reactive#flush()}, unless it has been invoked manually before the
 * global flush. A computation is also scheduled to for an initial
 * "recomputation" when it is created.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class Computation implements ReactiveValueChangeListener {

    private boolean invalidated = false;

    private boolean stopped = false;

    private final JsArray<EventRemover> dependencies = JsCollections.array();

    private JsSet<InvalidateListener> invalidateListeners = JsCollections.set();

    /**
     * Creates a new computation.
     */
    public Computation() {
        // Make sure a recompute is scheduled
        invalidate();
    }

    /**
     * Adds a dependency to a reactive value. This computation is scheduled for
     * recomputation when any dependency fires a change event. All previous
     * dependencies are cleared before recomputing.
     * <p>
     * This method is automatically called when a reactive value is used for
     * recomputing this computation. The developer is not expected to call this
     * method himself.
     *
     * @param dependency
     *            the reactive value to depend on
     */
    public void addDependency(ReactiveValue dependency) {
        if (!stopped) {
            EventRemover remover = dependency
                    .addReactiveValueChangeListener(this);
            dependencies.push(remover);
        }
    }

    @Override
    public void onValueChange(ReactiveValueChangeEvent changeEvent) {
        if (invalidated || stopped) {
            return;
        }

        invalidate();
    }

    private void invalidate() {
        invalidated = true;

        clearDependencies();

        if (!stopped) {
            Reactive.addFlushListener(this::recompute);
        }

        // Fire invalidate events
        if (invalidateListeners.size() != 0) {
            JsSet<InvalidateListener> oldListeners = invalidateListeners;
            invalidateListeners = JsCollections.set();

            InvalidateEvent invalidateEvent = new InvalidateEvent(this);

            oldListeners.forEach(
                    listener -> listener.onInvalidate(invalidateEvent));
        }
    }

    private void clearDependencies() {
        while (!dependencies.isEmpty()) {
            dependencies.remove(0).remove();
        }
    }

    /**
     * Stops this computation, so that it will no longer be recomputed.
     */
    public void stop() {
        stopped = true;

        invalidate();

        // Prevent firing more events
        invalidateListeners.clear();

        // Release memory
        clearDependencies();
    }

    /**
     * Checks whether this computation is invalidated. An invalidated
     * computation will eventually be recomputed (unless it has also been
     * stopped). Recomputation will happen the next time {@link #recompute()} or
     * {@link Reactive#flush()} is invoked.
     *
     * @return <code>true</code> if this computation is invalidated; otherwise
     *         <code>false</code>
     */
    public boolean isInvalidated() {
        return invalidated;
    }

    /**
     * Recomputes this computation.
     */
    public void recompute() {
        if (invalidated && !stopped) {
            try {
                Reactive.runWithComputation(this, this::doRecompute);
            } finally {
                invalidated = false;
            }
        }
    }

    /**
     * Does the actual recomputation. This method is run in a way that
     * automatically registers dependencies to any reactive value accessed.
     */
    protected abstract void doRecompute();

    /**
     * Adds an invalidate listener that will be invoked the next time this
     * computation is invalidated.
     *
     * @param listener
     *            the listener to run on the next invalidation
     */
    public void onNextInvalidate(InvalidateListener listener) {
        if (!stopped) {
            invalidateListeners.add(listener);
        }
    }
}
