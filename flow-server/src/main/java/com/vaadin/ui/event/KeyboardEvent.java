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
package com.vaadin.ui.event;

import com.vaadin.ui.common.HtmlComponent;

/**
 * Abstract class for keyboard events.
 *
 * @author Vaadin Ltd
 */
public abstract class KeyboardEvent extends ComponentEvent<HtmlComponent> {

    private final String key;
    private final int location;

    private final boolean ctrlKey;
    private final boolean shiftKey;
    private final boolean altKey;
    private final boolean metaKey;

    private final boolean repeat;
    private final boolean composing;

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
    public KeyboardEvent(HtmlComponent source, boolean fromClient, String key,
            int location, boolean ctrlKey, boolean shiftKey, boolean altKey,
            boolean metaKey, boolean repeat, boolean composing) {
        super(source, fromClient);
        this.key = key;
        this.location = location;
        this.ctrlKey = ctrlKey;
        this.shiftKey = shiftKey;
        this.altKey = altKey;
        this.metaKey = metaKey;
        this.repeat = repeat;
        this.composing = composing;
    }

    /**
     * Creates a new server-side keyboard event with no additional information.
     *
     * @param source
     *            the component that fired the event
     * @param key
     *            the {@link Key} for this event
     */
    public KeyboardEvent(HtmlComponent source, Key key) {
        this(source, false, key.getKey(), 0, false, false, false, false, false,
                false);
    }

    /**
     * Gets the {@link Key} of the event.
     *
     * @return the {@link Key} of the event
     */
    public Key getKey() {
        return Key.of(key);
    }

    /**
     * Gets the {@link KeyLocation} of the event.
     *
     * @return the {@link KeyLocation} of the event
     */
    public KeyLocation getLocation() {
        return KeyLocation.of(location);
    }

    /**
     * Checks whether the ctrl key was was down when the event was fired.
     *
     * @return <code>true</code> if the ctrl key was down when the event was
     *         fired, <code>false</code> otherwise
     */
    public boolean isCtrlKey() {
        return ctrlKey;
    }

    /**
     * Checks whether the alt key was was down when the event was fired.
     *
     * @return <code>true</code> if the alt key was down when the event was
     *         fired, <code>false</code> otherwise
     */
    public boolean isAltKey() {
        return altKey;
    }

    /**
     * Checks whether the meta key was was down when the event was fired.
     *
     * @return <code>true</code> if the meta key was down when the event was
     *         fired, <code>false</code> otherwise
     */
    public boolean isMetaKey() {
        return metaKey;
    }

    /**
     * Checks whether the shift key was was down when the event was fired.
     *
     * @return <code>true</code> if the shift key was down when the event was
     *         fired, <code>false</code> otherwise
     */
    public boolean isShiftKey() {
        return shiftKey;
    }

    /**
     * Checks whether the key has been pressed in a sustained manner.
     *
     * @return <code>true</code> if the key has been pressed in a sustained
     *         manner
     */
    public boolean isRepeat() {
        return repeat;
    }

    /**
     * Checks whether the key event occurred as part of a composition session.
     *
     * @return <code>true</code> if the key event occurred as part of a
     *         composition session
     */
    public boolean isComposing() {
        return composing;
    }

}
