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
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;

/**
 * HTML5 drag start event, fired when the user starts dragging a drag source.
 *
 * @param <T>
 *            Type of the component that is dragged.
 * @author Vaadin Ltd
 * @see DragSource#addDragStartListener(com.vaadin.flow.component.ComponentEventListener)
 * @since 2.0
 */
@DomEvent("dragstart")
public class DragStartEvent<T extends Component> extends AbstractDnDEvent<T> {

    private final int offsetX;
    private final int offsetY;

    /**
     * Creates a drag start event.
     *
     * @param source
     *            Component that is dragged.
     * @param fromClient
     *            <code>true</code> if the event originated from the client
     *            side, <code>false</code> otherwise
     * @param clientX
     *            the x coordinate of the mouse pointer relative to the viewport
     * @param clientY
     *            the y coordinate of the mouse pointer relative to the viewport
     * @param offsetX
     *            the x coordinate of the mouse pointer relative to the drag
     *            source element
     * @param offsetY
     *            the y coordinate of the mouse pointer relative to the drag
     *            source element
     */
    public DragStartEvent(T source, boolean fromClient,
            @EventData("event.clientX") int clientX,
            @EventData("event.clientY") int clientY,
            @EventData("event.offsetX") int offsetX,
            @EventData("event.offsetY") int offsetY) {
        super(source, fromClient, clientX, clientY);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    /**
     * Gets the x coordinate of the mouse pointer relative to the drag source
     * element when the drag started.
     * <p>
     * This is useful for maintaining the relative grab position when
     * positioning dropped items.
     *
     * @return the x coordinate relative to the drag source element
     * @since 25.1
     */
    public int getOffsetX() {
        return offsetX;
    }

    /**
     * Gets the y coordinate of the mouse pointer relative to the drag source
     * element when the drag started.
     * <p>
     * This is useful for maintaining the relative grab position when
     * positioning dropped items.
     *
     * @return the y coordinate relative to the drag source element
     * @since 25.1
     */
    public int getOffsetY() {
        return offsetY;
    }

    /**
     * Set server side drag data for this started drag operation. This data is
     * available in the drop event and can be used to transfer data between drag
     * source and {@link DropTarget} if they are in the same UI.
     * <p>
     * This method is a shorthand for {@link DragSource#setDragData(Object)} and
     * overrides any previously set drag data.
     *
     * @param data
     *            Data to transfer to drop event.
     * @see DropEvent#getDragData()
     * @see DragEndEvent#clearDragData()
     */
    public void setDragData(Object data) {
        DragSource.configure(getComponent()).setDragData(data);
    }
}
