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

import java.util.EventObject;
import java.util.Set;

import elemental.json.JsonObject;

/**
 * Server-side representation of a DOM event fired in the browser.
 *
 * @author Vaadin Ltd
 */
public class DomEvent extends EventObject {

    private final JsonObject eventData;

    private final String eventType;

    private final Set<String> matchedFilters;

    /**
     * Creates a new DOM event.
     *
     * @param source
     *            the element for which the event is fired, not
     *            <code>null</code>
     * @param eventType
     *            the type of the event, not <code>null</code>
     * @param eventData
     *            additional data related to the event, not <code>null</code>
     * @param matchedFilters
     *            the filters that matched for this event
     */
    public DomEvent(Element source, String eventType, JsonObject eventData,
            Set<String> matchedFilters) {
        super(source);
        assert source != null;
        assert eventType != null;
        assert eventData != null;

        this.eventType = eventType;
        this.eventData = eventData;
        this.matchedFilters = matchedFilters;
    }

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
     * Check whether the given filter string was matched on the client when this
     * event was sent to the server.
     *
     * @see DomListenerRegistration#setFilter(String)
     *
     * @param filter
     *            the filter string to check for
     *
     * @return <code>true</code> if the filter string was matched; otherwise
     *         <code>false</code>
     */
    public boolean matchesFilter(String filter) {
        return matchedFilters.contains(filter);
    }
}
