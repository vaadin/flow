/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import com.vaadin.flow.component.DomEvent;

/**
 * HTML5 drag start event, fired when the user starts dragging a drag source.
 *
 * @param <T>
 *            Type of the component that is dragged.
 * @author Vaadin Ltd
 * @see DragSource#addDragStartListener(com.vaadin.flow.component.ComponentEventListener)
 * @author Vaadin Ltd
 * @since 2.0
 */
@DomEvent("dragstart")
public class DragStartEvent<T extends Component> extends ComponentEvent<T> {

    /**
     * Creates a drag start event.
     *
     * @param source
     *            Component that is dragged.
     * @param fromClient
     *            <code>true</code> if the event originated from the client
     *            side, <code>false</code> otherwise
     */
    public DragStartEvent(T source, boolean fromClient) {
        super(source, fromClient);
    }

    /**
     * Returns the drag source component where the dragstart event occurred.
     *
     * @return Component which is dragged.
     */
    public T getComponent() {
        return getSource();
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
