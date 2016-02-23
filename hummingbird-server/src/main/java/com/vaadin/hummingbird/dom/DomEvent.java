/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.dom;

import java.util.EventObject;

import elemental.json.JsonObject;

/**
 * Server-side representation of a DOM event fired in the browser.
 *
 * @since
 * @author Vaadin Ltd
 */
public class DomEvent extends EventObject {

    private JsonObject eventData;
    private String eventType;

    /**
     * Creates a new DOM event.
     *
     * @param source
     *            the element for which the event is fired
     * @param eventType
     *            the type of the event
     * @param eventData
     *            additional data related to the event
     */
    public DomEvent(Element source, String eventType, JsonObject eventData) {
        super(source);
        assert source != null;
        assert eventType != null;
        assert eventData != null;

        this.eventType = eventType;
        this.eventData = eventData;
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
     * Gets additional data related to the event.
     *
     * @see Element#addEventListener(String, DomEventListener, String...)
     *
     * @return a JSON object containing event data
     */
    public JsonObject getEventData() {
        return eventData;
    }
}
