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

import java.util.EventObject;

import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Server-side representation of a DOM event fired in the browser.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DomEvent extends EventObject {

    private final JsonObject eventData;

    private final String eventType;

    private final DebouncePhase phase;

    /**
     * Creates a new DOM event.
     *
     * @param source
     *            the element on which the listener has been attached, not
     *            <code>null</code>
     * @param eventType
     *            the type of the event, not <code>null</code>
     * @param eventData
     *            additional data related to the event, not <code>null</code>
     *
     * @see Element#addEventListener(String, DomEventListener)
     * @see DomEventListener
     */
    public DomEvent(Element source, String eventType, JsonObject eventData) {
        super(source);
        assert source != null;
        assert eventType != null;
        assert eventData != null;

        this.eventType = eventType;
        this.eventData = eventData;

        phase = extractPhase(eventData);
    }

    private static DebouncePhase extractPhase(JsonObject eventData) {
        JsonValue jsonValue = eventData.get(JsonConstants.EVENT_DATA_PHASE);
        if (jsonValue == null) {
            return DebouncePhase.LEADING;
        } else {
            return DebouncePhase.forIdentifier(jsonValue.asString());
        }
    }

    /**
     * Returns the element on which the listener has been attached.
     *
     * @return The element on which the listener has been attached.
     *
     * @see Element#addEventListener(String, DomEventListener)
     * @see Element#addEventListener(String, DomEventListener, String...)
     */
    @Override
    public Element getSource() {
        return (Element) super.getSource();
    }

    /**
     * Gets the type of the event.
     *
     * @return the type of the event
     */
    public String getType() {
        return eventType;
    }

    /**
     * Gets additional data related to the event. An empty JSON object is
     * returned if no event data is available.
     *
     * @see DomListenerRegistration#addEventData(String)
     *
     * @return a JSON object containing event data, never <code>null</code>
     */
    public JsonObject getEventData() {
        return eventData;
    }

    /**
     * Gets the debounce phase for which this event is fired. This is used
     * internally to only deliver the event to the appropriate listener in cases
     * where there are multiple listeners for the same event with different
     * debounce settings.
     *
     * @return the debounce phase
     */
    public DebouncePhase getPhase() {
        return phase;
    }
}
