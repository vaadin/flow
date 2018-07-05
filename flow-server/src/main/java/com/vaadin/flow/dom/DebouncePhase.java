/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.dom;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.shared.JsonConstants;

/**
 * Defines phases for an event that is debounced. For most cases, high level
 * methods such as {@link DomListenerRegistration#debounce(int)} or
 * {@link DomListenerRegistration#throttle(int)} should be used instead.
 *
 * @see DomListenerRegistration#debounce(int, DebouncePhase, DebouncePhase...)
 * @see DomEvent#getPhase()
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public enum DebouncePhase {
    /**
     * Debounce phase that happens immediately when the event is first
     * triggered. Another leading event will not be sent to the server until the
     * debounce timeout period has passed without any new events being fired.
     * This is useful for cases such as button click where double submitting
     * should be avoided.
     */
    LEADING(JsonConstants.EVENT_PHASE_LEADING),

    /**
     * Debounce phase for events that are periodically sent to the server while
     * events are being fired in rapid succession. This is useful for cases such
     * as text input when you don't want to receive an event for each individual
     * keystroke, but still want periodic updates. This is sometimes useful in
     * combination with {@link #LEADING} so that the first event is sent
     * immediately. Can also be combined with {@link #TRAILING} to get a
     * separate event when the input has stopped.
     */
    INTERMEDIATE(JsonConstants.EVENT_PHASE_INTERMEDIATE),

    /**
     * Debounce phase that is sent to the server once there have been at least
     * one debounce timeout period since the last event of the same type. This
     * is useful for cases such as text input when you are only want to react to
     * the text when the user pauses typing.
     */
    TRAILING(JsonConstants.EVENT_PHASE_TRAILING);

    private static final Map<String, DebouncePhase> INSTANCES = Stream
            .of(DebouncePhase.values()).collect(Collectors
                    .toMap(DebouncePhase::getIdentifier, Function.identity()));

    private String identifier;

    DebouncePhase(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Gets the string that is used to identify this phase.
     *
     * @see #forIdentifier(String)
     *
     * @return the identifier string
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Gets the phase that corresponds to the given identifier character.
     *
     * @see #getIdentifier()
     * @param identifier
     *            the identifier character to look for
     * @return the debounce phase corresponding to the provided identifier
     *         character
     * @throws IllegalArgumentException
     *             if there is no corresponding character
     */
    public static DebouncePhase forIdentifier(String identifier) {
        DebouncePhase phase = INSTANCES.get(identifier);
        if (phase == null) {
            throw new IllegalArgumentException(
                    "Unsupported debounce phase identifier: " + identifier);
        } else {
            return phase;
        }
    }
}
