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
import com.vaadin.flow.component.EventData;

/**
 * HTML5 drag end event, fired when the user stops dragging a drag source either
 * by dropping on top of a valid drop target or by canceling to drop.
 *
 * @param <T>
 *            Type of the component that was dragged.
 * @see DragSource#addDragEndListener(com.vaadin.flow.component.ComponentEventListener)
 * @author Vaadin Ltd
 * @since 2.0
 */
@DomEvent("dragend")
public class DragEndEvent<T extends Component> extends ComponentEvent<T> {
    private final DropEffect dropEffect;

    /**
     * Creates a drag end event.
     *
     * @param source
     *            Component that was dragged.
     * @param fromClient
     *            <code>true</code> if the event originated from the client
     *            side, <code>false</code> otherwise
     * @param dropEffect
     *            Drop effect from {@code DataTransfer.dropEffect} object.
     */
    public DragEndEvent(T source, boolean fromClient,
            @EventData("event.dataTransfer.dropEffect") String dropEffect) {
        super(source, fromClient);
        this.dropEffect = DropEffect.fromString(dropEffect);
    }

    /**
     * Get drop effect of the dragend event. The value will be in priority
     * order: the desired action set by the drop target, {@code effectAllowed}
     * parameter of the drag source and modifier keys the user presses.
     * <em>NOTE:</em> there are some browser specific differences to this -
     * Chrome does not change the drop effect based on modifier keys but only
     * what the drop target sets.
     * <p>
     * If the drop is not successful, the value will be {@code NONE}.
     * <p>
     * In case the desired drop effect is {@code MOVE}, the data being dragged
     * should be removed from the source.
     *
     * @return The {@code DataTransfer.dropEffect} parameter of the client side
     *         dragend event.
     * @see DragSource#setEffectAllowed(EffectAllowed)
     * @see DropTarget#setDropEffect(DropEffect)
     */
    public DropEffect getDropEffect() {
        return dropEffect;
    }

    /**
     * Returns whether the drop event succesful or was it cancelled or didn't
     * succeed. This is a shorthand for {@code dropEffect != NONE}.
     * <em>NOTE:</em> For Edge, Safari and IE11 this method will <b>always
     * report <code>false</code></b> due to bugs in the browsers!
     * 
     * @deprecated replaced with {@link #isSuccessful()} since 2.1 (v14.1), this
     *             method will be removed later.
     * @return {@code true} if the drop event succeeded, {@code false}
     *         otherwise.
     */
    @Deprecated
    public boolean isSuccesful() {
        return isSuccessful();
    }

    /**
     * Returns whether the drop event succesful or was it cancelled or didn't
     * succeed. This is a shorthand for {@code dropEffect != NONE}.
     * <em>NOTE:</em> For Edge, Safari and IE11 this method will <b>always
     * report <code>false</code></b> due to bugs in the browsers!
     * 
     * @return {@code true} if the drop event succeeded, {@code false}
     *         otherwise.
     * @since 2.1
     */
    public boolean isSuccessful() {
        return getDropEffect() != DropEffect.NONE;
    }

    /**
     * Returns the drag source component where the dragend event occurred.
     *
     * @return Component which was dragged.
     */
    public T getComponent() {
        return getSource();
    }

    /**
     * Clears the drag data for this drag operation (and the drag source
     * component).
     * <p>
     * This method is a shorthand for calling
     * {@link DragSource#setDragData(Object)} with {@code null} parameter.
     * 
     * @see DragStartEvent#setDragData(Object)
     * @see DragSource#setDragData(Object)
     */
    public void clearDragData() {
        DragSource.configure(getComponent()).setDragData(null);
    }
}
