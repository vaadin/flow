/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.client.flow.binding;

import java.util.function.Consumer;

import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.collection.JsWeakMap;
import com.vaadin.flow.shared.JsonConstants;

import elemental.dom.Node;
import elemental.util.Timer;

/**
 * Manages debouncing of events. Use {@link #getOrCreate(Node, String, double)}
 * to either create a new instance or get an existing instance that currently
 * tracks a sequence of similar events.
 *
 * @author Vaadin Ltd
 */
public class Debouncer {

    private static final JsWeakMap<Node, JsMap<String, JsMap<Double, Debouncer>>> debouncers = JsCollections
            .weakMap();

    private final double timeout;
    private final Node element;
    private final String identifier;

    private Timer idleTimer;
    private Timer intermediateTimer;

    private boolean fireTrailing = false;
    private Consumer<String> lastCommand;

    private Debouncer(Node element, String identifier, double timeout) {
        this.element = element;
        this.identifier = identifier;
        this.timeout = timeout;
    }

    /**
     * Informs this debouncer that an event has occurred.
     *
     * @param phases
     *            a string containing character codes identifying the phases for
     *            which the triggered event should be considered.
     * @param command
     *            a consumer that will may be asynchronously invoked with a
     *            phase code if an associated phase is triggered
     * @return <code>true</code> if the event should be processed as-is without
     *         delaying
     */
    public boolean trigger(String phases, Consumer<String> command) {
        lastCommand = command;

        boolean triggerImmediately = false;
        if (idleTimer == null) {
            triggerImmediately = isLeading(phases);

            idleTimer = new Timer() {
                @Override
                public void run() {
                    if (intermediateTimer != null) {
                        intermediateTimer.cancel();
                    }

                    if (fireTrailing) {
                        lastCommand.accept(JsonConstants.EVENT_PHASE_TRAILING);
                    }

                    unregister();
                }
            };
        }

        idleTimer.cancel();
        idleTimer.schedule((int) timeout);

        if (intermediateTimer == null && isIntermediate(phases)) {
            intermediateTimer = new Timer() {
                @Override
                public void run() {
                    lastCommand.accept(JsonConstants.EVENT_PHASE_INTERMEDIATE);
                }
            };
            intermediateTimer.scheduleRepeating((int) timeout);
        }

        fireTrailing |= isTrailing(phases);

        return triggerImmediately;
    }

    private static boolean isLeading(String phases) {
        return phases.contains(JsonConstants.EVENT_PHASE_LEADING);
    }

    private static boolean isTrailing(String phases) {
        return phases.contains(JsonConstants.EVENT_PHASE_TRAILING);
    }

    private static boolean isIntermediate(String phases) {
        return phases.contains(JsonConstants.EVENT_PHASE_INTERMEDIATE);
    }

    private void unregister() {
        JsMap<String, JsMap<Double, Debouncer>> elementMap = debouncers
                .get(element);
        if (elementMap == null) {
            return;
        }

        JsMap<Double, Debouncer> identifierMap = elementMap.get(identifier);
        if (identifierMap == null) {
            return;
        }

        identifierMap.delete(Double.valueOf(timeout));

        if (identifierMap.isEmpty()) {
            elementMap.delete(identifier);

            if (elementMap.isEmpty()) {
                debouncers.delete(element);
            }
        }
    }

    /**
     * Gets an existing debouncer or creates a new one associated with the given
     * DOM node, identifier and debounce timeout.
     *
     * @param element
     *            the DOM node to which this debouncer is bound
     * @param identifier
     *            a unique identifier string in the scope of the provided
     *            element
     * @param debounce
     *            the debounce timeout
     * @return a debouncer instance
     */
    public static Debouncer getOrCreate(Node element, String identifier,
            double debounce) {
        JsMap<String, JsMap<Double, Debouncer>> elementMap = debouncers
                .get(element);
        if (elementMap == null) {
            elementMap = JsCollections.map();
            debouncers.set(element, elementMap);
        }

        JsMap<Double, Debouncer> identifierMap = elementMap.get(identifier);
        if (identifierMap == null) {
            identifierMap = JsCollections.map();
            elementMap.set(identifier, identifierMap);
        }

        Debouncer debouncer = identifierMap.get(Double.valueOf(debounce));
        if (debouncer == null) {
            debouncer = new Debouncer(element, identifier, debounce);
            identifierMap.set(Double.valueOf(debounce), debouncer);
        }

        return debouncer;
    }
}
