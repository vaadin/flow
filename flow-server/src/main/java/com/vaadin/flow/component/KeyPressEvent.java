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
package com.vaadin.flow.component;

import com.vaadin.flow.component.internal.KeyboardEvent;

/**
 * The event when a key is pressed.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@DomEvent("keypress")
public class KeyPressEvent extends KeyboardEvent {

    /**
     * Creates a new keyboard event.
     *
     * @param source
     *            the component that fired the event
     * @param fromClient
     *            <code>true</code> if the event was originally fired on the
     *            client, <code>false</code> if the event originates from
     *            server-side logic
     * @param key
     *            the string value representing the key
     * @param code
     *            the string value representing the code
     * @param location
     *            the integer value representing the location of the key
     * @param ctrlKey
     *            <code>true</code> if the control key was down when the event
     *            was fired, <code>false</code> otherwise
     * @param shiftKey
     *            <code>true</code> if the shift key was down when the event was
     *            fired, <code>false</code> otherwise
     * @param altKey
     *            <code>true</code> if the alt key was down when the event was
     *            fired, <code>false</code> otherwise
     * @param metaKey
     *            <code>true</code> if the meta key was down when the event was
     *            fired, <code>false</code> otherwise
     * @param repeat
     *            <code>true</code> if the key has been pressed in a sustained
     *            manner
     * @param composing
     *            <code>true</code> if the key event occurred as part of a
     *            composition session
     */
    public KeyPressEvent(Component source, boolean fromClient,
            @EventData("event.key") String key,
            @EventData("event.code") String code,
            @EventData("event.location") int location,
            @EventData("event.ctrlKey") boolean ctrlKey,
            @EventData("event.shiftKey") boolean shiftKey,
            @EventData("event.altKey") boolean altKey,
            @EventData("event.metaKey") boolean metaKey,
            @EventData("event.repeat") boolean repeat,
            @EventData("event.isComposing") boolean composing) {
        super(source, fromClient, key, code, location, ctrlKey, shiftKey,
                altKey, metaKey, repeat, composing);
    }

    /**
     * Creates a new server-side keyboard event with no additional information.
     *
     * @param source
     *            the component that fired the event
     * @param key
     *            the key for this event
     */
    public KeyPressEvent(Component source, String key) {
        super(source, key);
    }

    /**
     * Creates a new server-side keyboard event with no additional information.
     *
     * @param source
     *            the component that fired the event
     * @param key
     *            the key for this event
     * @param code
     *            the code for this event
     */
    public KeyPressEvent(Component source, String key, String code) {
        super(source, key, code);
    }
}
