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
package com.vaadin.flow.dom;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.shared.JsonConstants;

/**
 * Defines phases for an event that is debounced.
 *
 * @see DomListenerRegistration#debounce(int, DebouncePhase, DebouncePhase...)
 *
 * @author Vaadin Ltd
 */
public enum DebouncePhase {
    /**
     * Debounce phase that happens immediately when the event is first
     * triggered. Another leading event will not be sent to the server until the
     * debounce timeout period has passed without any new events being fired.
     */
    LEADING(JsonConstants.EVENT_PHASE_LEADING),

    /**
     * Debounce phase for events that are periodically sent to the server while
     * events are being fired in rapid succession.
     */
    INTERMEDIATE(JsonConstants.EVENT_PHASE_INTERMEDIATE),

    /**
     * Debounce phase that is sent to the server once there have been at least
     * one debounce timeout period since the last event of the same type.
     */
    TRAILING(JsonConstants.EVENT_PHASE_TRAILING);

    private static Map<Character, DebouncePhase> INSTANCES = Stream
            .of(DebouncePhase.values()).collect(Collectors
                    .toMap(DebouncePhase::getCode, Function.identity()));

    private char code;

    DebouncePhase(String code) {
        assert code.length() == 1;
        this.code = code.charAt(0);
    }

    /**
     * Gets the character that is used to identify this phase.
     *
     * @see #forCode(char)
     *
     * @return the identifier character
     */
    public char getCode() {
        return code;
    }

    /**
     * Gets the phase that corresponds to the given identifier character.
     *
     * @see #getCode()
     * @param code
     *            the identifier character to look for
     * @return the debounce phase corresponding to the provided identifier
     *         character
     * @throws IllegalArgumentException
     *             if there is no corresponding character
     */
    public static DebouncePhase forCode(char code) {
        DebouncePhase phase = INSTANCES.get(Character.valueOf(code));
        if (phase == null) {
            throw new IllegalArgumentException(
                    "Unsupported debounce phase code: " + code);
        } else {
            return phase;
        }
    }
}
