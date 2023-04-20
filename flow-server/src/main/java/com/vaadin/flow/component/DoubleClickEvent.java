/*
 * Copyright 2000-2023 Vaadin Ltd.
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

/**
 * Event fired when a component is double clicked.
 *
 * @author Vaadin Ltd
 * @since 24.1
 *
 * @param <C>
 *            The source component type
 */
@DomEvent("dblclick")
public class DoubleClickEvent<C extends Component>
        extends AbstractClickEvent<C> {

    /**
     * Creates a new click event.
     *
     * @param source
     *            the component that fired the event
     * @param fromClient
     *            <code>true</code> if the event was originally fired on the
     *            client, <code>false</code> if the event originates from
     *            server-side logic
     * @param screenX
     *            the x coordinate of the click event, relative to the upper
     *            left corner of the screen, -1 if unknown
     * @param screenY
     *            the y coordinate of the click event, relative to the upper
     *            left corner of the screen, -i if unknown
     * @param clientX
     *            the x coordinate of the click event, relative to the upper
     *            left corner of the browser viewport, -1 if unknown
     * @param clientY
     *            the y coordinate of the click event, relative to the upper
     *            left corner of the browser viewport, -1 if unknown
     * @param button
     *            the id of the pressed mouse button
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
     *
     */
    public DoubleClickEvent(Component source, boolean fromClient,
            @EventData("event.screenX") int screenX,
            @EventData("event.screenY") int screenY,
            @EventData("event.clientX") int clientX,
            @EventData("event.clientY") int clientY,
            @EventData("event.button") int button,
            @EventData("event.ctrlKey") boolean ctrlKey,
            @EventData("event.shiftKey") boolean shiftKey,
            @EventData("event.altKey") boolean altKey,
            @EventData("event.metaKey") boolean metaKey) {
        super(source, fromClient, screenX, screenY, clientX, clientY, button,
                ctrlKey, shiftKey, altKey, metaKey);
    }

    /**
     * Creates a new server-side click event with no additional information.
     *
     * @param source
     *            the component that fired the event
     */
    public DoubleClickEvent(Component source) {
        // source, notClient, 4 coordinates, button, 4 modifier
        // keys
        this(source, false, -1, -1, -1, -1, 1, false, false, false, false);
    }
}
