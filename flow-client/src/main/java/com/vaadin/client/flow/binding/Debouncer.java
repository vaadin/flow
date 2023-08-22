/*
 * Copyright 2000-2023 Vaadin Ltd.
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

    private Consumer<String> bufferedCommand;
    private Consumer<String> potentialTrailingWithBothTrailingAndIntermediate;

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
        // If "leading" events are requested and no timers created yet,
        // this is considered the leading event that is triggered immediately
        // and there is no need to save it
        final boolean triggerImmediately = phases
                .has(JsonConstants.EVENT_PHASE_LEADING) && idleTimer == null
                && intermediateTimer == null;
        if (!triggerImmediately
                && (phases.has(JsonConstants.EVENT_PHASE_TRAILING) || phases
                        .has(JsonConstants.EVENT_PHASE_INTERMEDIATE))) {
            // last command is saved for timers unless this is a "leading" event
            bufferedCommand = command;
            potentialTrailingWithBothTrailingAndIntermediate = null;
        }
        // idleTimer is used for trailing/leading, should always be there?
        if (phases.has(JsonConstants.EVENT_PHASE_LEADING)
                || phases.has(JsonConstants.EVENT_PHASE_TRAILING)) {
            if (idleTimer == null) {
                idleTimer = new Timer() {
                    @Override
                    public void run() {
                        if (bufferedCommand != null) {
                            bufferedCommand
                                    .accept(JsonConstants.EVENT_PHASE_TRAILING);
                            bufferedCommand = null;
                        } else if (potentialTrailingWithBothTrailingAndIntermediate != null) {
                            /*
                             * This happens if both trailing & intermediate are
                             * configured and e.g. typing has stopped. Then we
                             * wait for one additional timeout and if no new
                             * commands are there, we re-post the SAME event to
                             * the server. Documented in DebouncePhase. This is
                             * ugly but maybe handy for some people in some
                             * situations.
                             */
                            potentialTrailingWithBothTrailingAndIntermediate
                                    .accept(JsonConstants.EVENT_PHASE_TRAILING);
                        }
                        unregister(); // unregister to releease memory
                    }
                };
            }
            idleTimer.cancel();
            idleTimer.schedule((int) timeout);
        }

        if (intermediateTimer == null
                && phases.has(JsonConstants.EVENT_PHASE_INTERMEDIATE)) {
            intermediateTimer = new Timer() {
                @Override
                public void run() {
                    if (bufferedCommand != null) {
                        bufferedCommand
                                .accept(JsonConstants.EVENT_PHASE_INTERMEDIATE);
                        if (phases.has(JsonConstants.EVENT_PHASE_TRAILING)) {
                            potentialTrailingWithBothTrailingAndIntermediate = bufferedCommand;
                        }
                        bufferedCommand = null;
                    } else {
                        // no new last command during the period, stop timer
                        // and unregister to avoid memory leaks
                        unregister();
                    }
                }
            };
            intermediateTimer.scheduleRepeating((int) timeout);
        }
        return triggerImmediately;
    }

    private void unregister() {
        if (intermediateTimer != null) {
            intermediateTimer.cancel();
            intermediateTimer = null;
        }
        if (idleTimer != null) {
            idleTimer.cancel();
            idleTimer = null;
        }
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
                        if (debouncer.idleTimer != null) {
                            if (debouncer.bufferedCommand != null) {
                                // if there is trailing timer, consider as extra
                                // trailing event
                                debouncer.bufferedCommand.accept(
                                        JsonConstants.EVENT_PHASE_TRAILING);
                            } else {
                                // "Debouncer was in queue, but no command.
                                // Likely a leading only subscription."
                            }
                        } else {
                            // Otherwise, must be an extra intermediate event.
                            // Because of an other triggered event, this now
                            // comes bit early, but most likely this is better
                            // than in wrong order
                            debouncer.bufferedCommand.accept(
                                    JsonConstants.EVENT_PHASE_INTERMEDIATE);
                            // Restart intermediate timer so that there won't
                            // be triggering more than one event "quicker than
                            // orderd" in the orginal schedule.
                            debouncer.intermediateTimer
                                    .scheduleRepeating((int) debouncer.timeout);
                        }
                        if (debouncer.bufferedCommand != null) {
                            executedCommands.add(debouncer.bufferedCommand);
                            // clean so that idle timer can't fire it again
                            debouncer.bufferedCommand = null;
                        }
                    });
                });
            }
        };
        debouncers.forEach(flusher);
        return executedCommands;
    }
}
