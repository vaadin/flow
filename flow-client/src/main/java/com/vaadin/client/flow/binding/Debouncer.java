/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.collection.JsMap.ForEachCallback;
import com.vaadin.client.flow.collection.JsSet;
import com.vaadin.flow.shared.JsonConstants;

import elemental.dom.Node;
import elemental.util.Timer;

/**
 * Manages debouncing of events. Use {@link #getOrCreate(Node, String, double)}
 * to either create a new instance or get an existing instance that currently
 * tracks a sequence of similar events.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class Debouncer {

    private static final JsMap<Node, JsMap<String, JsMap<Double, Debouncer>>> debouncers = new JsMap<>();

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
     *            a set of strings identifying the phases for which the
     *            triggered event should be considered.
     * @param command
     *            a consumer that will may be asynchronously invoked with a
     *            phase code if an associated phase is triggered
     * @return <code>true</code> if the event should be processed as-is without
     *         delaying
     */
    public boolean trigger(JsSet<String> phases, Consumer<String> command) {
        lastCommand = command;
        boolean triggerImmediately = false;
        if (idleTimer == null) {
            triggerImmediately = phases.has(JsonConstants.EVENT_PHASE_LEADING);
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

        if (intermediateTimer == null
                && phases.has(JsonConstants.EVENT_PHASE_INTERMEDIATE)) {
            intermediateTimer = new Timer() {
                @Override
                public void run() {
                    lastCommand.accept(JsonConstants.EVENT_PHASE_INTERMEDIATE);
                }
            };
            intermediateTimer.scheduleRepeating((int) timeout);
        }

        fireTrailing |= phases.has(JsonConstants.EVENT_PHASE_TRAILING);
        return triggerImmediately;
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

    /**
     * Flushes all pending changes.
     *
     * After command execution, Debouncer idle timers are rescheduled.
     *
     * @return the list command executed during flush operation.
     */
    public static List<Consumer<String>> flushAll() {
        ArrayList<Consumer<String>> executedCommands = new ArrayList<>();
        ForEachCallback<Node, JsMap<String, JsMap<Double, Debouncer>>> flusher = new ForEachCallback<Node, JsMap<String, JsMap<Double, Debouncer>>>() {

            @Override
            public void accept(JsMap<String, JsMap<Double, Debouncer>> jsmap,
                    Node key) {
                jsmap.mapValues().forEach(value -> {
                    value.mapValues().forEach(debouncer -> {
                        debouncer.lastCommand.accept(null);
                        executedCommands.add(debouncer.lastCommand);
                        // Reschedule the idleTimer so as if an event has been
                        // triggered
                        // and let the intermediateTimer continue to work as
                        // expected
                        if (debouncer.idleTimer != null) {
                            debouncer.idleTimer.cancel();
                            debouncer.idleTimer
                                    .schedule((int) debouncer.timeout);
                        }
                    });
                });
            }
        };
        debouncers.forEach(flusher);
        return executedCommands;
    }
}
