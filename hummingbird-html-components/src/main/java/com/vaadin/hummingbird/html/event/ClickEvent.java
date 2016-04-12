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
package com.vaadin.hummingbird.html.event;

import com.vaadin.annotations.DomEvent;
import com.vaadin.annotations.EventData;
import com.vaadin.hummingbird.html.HtmlComponent;
import com.vaadin.ui.ComponentEvent;

/**
 * Event fired when a component is clicked.
 *
 * @since
 * @author Vaadin Ltd
 */
@DomEvent("click")
public class ClickEvent extends ComponentEvent {

    private int clientX;
    private int clientY;

    /**
     * Creates a new change event.
     *
     * @param source
     *            the component that fired the event
     * @param fromClient
     *            <code>true</code> if the event was originally fired on the
     *            client, <code>false</code> if the event originates from
     *            server-side logic
     * @param clientX
     *            the x coordinate of the click event, relative to the upper
     *            left corner of the browser viewport
     * @param clientY
     *            the y coordinate of the click event, relative to the upper
     *            left corner of the browser viewport
     */
    public ClickEvent(HtmlComponent source, boolean fromClient,
            @EventData("event.clientX") int clientX,
            @EventData("event.clientY") int clientY) {
        super(source, fromClient);
        this.clientX = clientX;
        this.clientY = clientY;
    }

    @Override
    public HtmlComponent getSource() {
        return (HtmlComponent) super.getSource();
    }

    /**
     * Gets the x coordinate of the click event, relative to the upper left
     * corner of the browser viewport
     *
     * @return the x coordinate
     */
    public int getClientX() {
        return clientX;
    }

    /**
     * Gets the y coordinate of the click event, relative to the upper left
     * corner of the browser viewport
     *
     * @return the y coordinate
     */
    public int getClientY() {
        return clientY;
    }
}