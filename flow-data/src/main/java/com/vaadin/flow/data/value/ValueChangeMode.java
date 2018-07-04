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
     * then the provided eager event name is returned.
     *
     * @see HasValueChangeMode#setValueChangeMode(ValueChangeMode)
     * @see AbstractSinglePropertyField#setSynchronizedEvent(String)
     *
     * @param mode
     *            the value change mode
     * @param eagerEventName
     *            the event name to use for the eager mode
     * @return the event name
     */
    public static String eventForMode(ValueChangeMode mode,
            String eagerEventName) {
        if (mode == null) {
            return null;
        }

        switch (mode) {
        case EAGER:
            return eagerEventName;
        case ON_BLUR:
            return "blur";
        case ON_CHANGE:
            return "change";
        default:
            throw new IllegalArgumentException(
                    "Value change mode " + mode.name() + " not supported");
        }
    }
}
