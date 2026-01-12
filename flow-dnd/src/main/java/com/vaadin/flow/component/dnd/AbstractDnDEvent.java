/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.component.dnd;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

/**
 * Abstract base class for HTML5 drag and drop events.
 * <p>
 * In the browser, drag events inherit from {@code MouseEvent}, so this class
 * provides access to common mouse event properties like cursor coordinates.
 *
 * @param <T>
 *            Type of the component associated with the event.
 * @author Vaadin Ltd
 * @since 24.8
 */
public abstract class AbstractDnDEvent<T extends Component>
        extends ComponentEvent<T> {

    private final int clientX;
    private final int clientY;

    /**
     * Creates a new drag and drop event.
     *
     * @param source
     *            the component that is the source of the event
     * @param fromClient
     *            {@code true} if the event originated from the client side,
     *            {@code false} otherwise
     * @param clientX
     *            the x coordinate of the mouse pointer relative to the
     *            viewport, excluding any scroll offset
     * @param clientY
     *            the y coordinate of the mouse pointer relative to the
     *            viewport, excluding any scroll offset
     */
    protected AbstractDnDEvent(T source, boolean fromClient, int clientX,
            int clientY) {
        super(source, fromClient);
        this.clientX = clientX;
        this.clientY = clientY;
    }

    /**
     * Gets the x coordinate of the mouse pointer relative to the viewport,
     * excluding any scroll offset.
     * <p>
     * This is useful for positioning elements based on where the drag/drop
     * operation occurred.
     *
     * @return the x coordinate relative to the viewport
     */
    public int getClientX() {
        return clientX;
    }

    /**
     * Gets the y coordinate of the mouse pointer relative to the viewport,
     * excluding any scroll offset.
     * <p>
     * This is useful for positioning elements based on where the drag/drop
     * operation occurred.
     *
     * @return the y coordinate relative to the viewport
     */
    public int getClientY() {
        return clientY;
    }

    /**
     * Returns the component associated with this event.
     *
     * @return the component associated with this event
     */
    public T getComponent() {
        return getSource();
    }
}
