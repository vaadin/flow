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

package com.vaadin.flow.data.value;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.dom.DomListenerRegistration;

/**
 * All possible value change modes that can be set for any component extending
 * {@link HasValueChangeMode}. Depending on the mode used, the component's
 * {@code value} is synced differently from the client to the server.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public enum ValueChangeMode {
    /**
     * Syncs the value to the server each time it's changed on the client. The
     * event that triggers the synchronization is defined by the component.
     */
    EAGER,

    /**
     * On every user event, schedule a synchronization after a defined interval,
     * cancelling the currently-scheduled event if any.
     * <p>
     * The recommended default timeout for input fields is
     * {@link HasValueChangeMode#DEFAULT_CHANGE_TIMEOUT}.
     */
    LAZY,

    /**
     * Syncs the value at defined intervals
     * as long as the value changes from one event to the next.
     */
    TIMEOUT,

    /**
     * Syncs the value to the server on {@code blur} event, i.e. when the
     * component looses focus.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/Events/blur">Blur
     *      event description</a>
     */
    ON_BLUR,

    /**
     * Syncs the value to the server on {@code change} event, i.e. when the
     * component value is committed.
     *
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/Events/change"> Change
     *      event description</a>
     */
    ON_CHANGE,;

    /**
     * Gets the name of the event associated with the given mode. If the mode is
     * <code>null</code>, then null is returned. If the mode is {@link #EAGER},
     * {@link #LAZY}, or {@link #TIMEOUT},
     * then the provided immediate event name is returned.
     *
     * @see HasValueChangeMode#setValueChangeMode(ValueChangeMode)
     * @see AbstractSinglePropertyField#setSynchronizedEvent(String)
     *
     * @param mode
     *            the value change mode
     * @param immediateEventName
     *            the name of the event that is fired immediately on value change
     * @return the event name
     */
    public static String eventForMode(ValueChangeMode mode,
                                      String immediateEventName) {
        if (mode == null) {
            return null;
        }

        switch (mode) {
        case EAGER:
        case LAZY:
        case TIMEOUT:
            return immediateEventName;
        case ON_BLUR:
            return "blur";
        case ON_CHANGE:
            return "change";
        default:
            throwModeNotSupported(mode);
            return null;
        }
    }

    /**
     * Applies the value change timeout of the given mode on the registration
     * of the DOM event listener that synchronizes.
     * It has any effect only for {@link #LAZY}, or {@link #TIMEOUT}.

     * @see HasValueChangeMode#getValueChangeTimeout()
     * @see AbstractSinglePropertyField#getSynchronizationRegistration()
     *
     * @param mode
     *            the value change mode
     * @param timeout
     *            Value change timeout in milliseconds.
     *            <code>0</code> means timeout is disabled,
     *            so the change mode will behave like {@link #EAGER}
     * @param registration
     *            The registration of the DOM event listener that synchronizes.
     */
    public static void applyChangeTimeout(ValueChangeMode mode, int timeout,
                                          DomListenerRegistration registration) {
        if (mode == null || registration == null) {
            return;
        }

        switch (mode) {
            case LAZY:
                registration.debounce(timeout);
                break;
            case TIMEOUT:
                registration.throttle(timeout);
                break;
            case EAGER:
            case ON_BLUR:
            case ON_CHANGE:
                break;
            default:
                throwModeNotSupported(mode);
        }
    }

    private static void throwModeNotSupported(ValueChangeMode mode) {
        throw new IllegalArgumentException(
                "Value change mode " + mode.name() + " not supported");
    }
}
