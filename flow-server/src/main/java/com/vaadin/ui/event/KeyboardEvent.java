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

import java.util.EnumSet;

import com.vaadin.ui.common.HtmlComponent;

/**
 * Abstract class for keyboard events.
 *
 * @author Vaadin Ltd
 */
public abstract class KeyboardEvent extends ComponentEvent<HtmlComponent> {

    private final String key;
    private final KeyLocation location;

    private final boolean repeat;
    private final boolean composing;

    /**
     * The set of {@link KeyModifier} of the event.
     */
    protected final EnumSet<KeyModifier> modifiers;

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
        this.location = KeyLocation.of(location);
        this.repeat = repeat;
        this.composing = composing;
        this.modifiers = EnumSet.noneOf(KeyModifier.class);
        if (ctrlKey) {
            this.modifiers.add(KeyModifier.CONTROL);
        }
        if (shiftKey) {
            this.modifiers.add(KeyModifier.SHIFT);
        }
        if (altKey) {
            this.modifiers.add(KeyModifier.ALT);
        }
        if (metaKey) {
            this.modifiers.add(KeyModifier.META);
        }
    }

    /**
     * Creates a new server-side keyboard event with no additional information.
     *
     * @param source
     *            the component that fired the event
     * @param key
     *            the key for this event
     */
    public KeyboardEvent(HtmlComponent source, String key) {
        this(source, false, key, 0, false, false, false, false, false, false);
    }

    /**
     * Gets the key of the event.
     *
     * @return the key of the event
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the {@link KeyLocation} of the event.
     *
     * @return the {@link KeyLocation} of the event
     */
    public KeyLocation getLocation() {
        return location;
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

    /**
     * Gets the set of {@link KeyModifier} of the event.
     *
     * @return the set of {@link KeyModifier}
     */
    public EnumSet<KeyModifier> getModifiers() {
        return modifiers;
    }

}
