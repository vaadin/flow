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

import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.dnd.internal.DndUtil;

/**
 * Server side drop event. Fired when an HTML5 drop happens on a valid drop
 * target.
 *
 * @param <T>
 *            Type of the drop target component.
 * @author Vaadin Ltd
 * @see DropTarget#addDropListener(com.vaadin.flow.component.ComponentEventListener)
 * @author Vaadin Ltd
 * @since 2.0
 */
@DomEvent("drop")
public class DropEvent<T extends Component> extends ComponentEvent<T> {

    private final EffectAllowed effectAllowed;
    private final String dropEffect;
    private final Component dragSourceComponent;

    /**
     * Creates a server side drop event.
     *
     * @param source
     *            Component that received the drop.
     * @param fromClient
     *            <code>true</code> if the event originated from the client
     *            side, <code>false</code> otherwise
     * @param effectAllowed
     *            the effect allowed by the drag source
     */
    public DropEvent(T source, boolean fromClient,
            @EventData("event.dataTransfer.effectAllowed") String effectAllowed) {
        super(source, fromClient);

        this.effectAllowed = EffectAllowed.fromString(effectAllowed);
        // capture drop effect from server side, since it is meant for drag
        // end event
        dropEffect = source.getElement()
                .getProperty(DndUtil.DROP_EFFECT_ELEMENT_PROPERTY);
        // when the event is created, the drop target is always attached
        dragSourceComponent = getComponent().getUI()
                .orElseThrow(() -> new IllegalStateException(
                        "Drop target received a drop event but not attached "
                                + "to an UI."))
                .getActiveDragSourceComponent();
    }

    /**
     * Gets the server side drag data. This data can be set during the drag
     * start event on the server side and can be used to transfer data between
     * drag source and drop target when they are in the same UI.
     *
     * @return Optional server side drag data if set and the drag source and the
     *         drop target are in the same UI, otherwise empty {@code Optional}.
     */
    public Optional<Object> getDragData() {
        return getDragSourceComponent().map(component -> ComponentUtil
                .getData(component, DndUtil.DRAG_SOURCE_DATA_KEY));
    }

    /**
     * Get the desired {@code dropEffect} for the drop event.
     *
     * @return the drop effect set to the drop target, or null if nothing set
     * @see DropTarget#setDropEffect(DropEffect)
     */
    public DropEffect getDropEffect() {
        return dropEffect == null ? null : DropEffect.fromString(dropEffect);
    }

    /**
     * Get the {@code effectAllowed} set by the drag source.
     *
     * @return the effect allowed by the drag source
     * @see DragSource#setEffectAllowed(EffectAllowed)
     */
    public EffectAllowed getEffectAllowed() {
        return effectAllowed;
    }

    /**
     * Returns the drag source component if the drag originated from a component
     * in the same UI as the drop target component, or an empty optional.
     *
     * @return Drag source component from the same UI or an empty optional.
     */
    public Optional<Component> getDragSourceComponent() {
        return Optional.ofNullable(dragSourceComponent);
    }

    /**
     * Returns the drop target component where the drop event occurred.
     *
     * @return Component on which a drag source was dropped.
     */
    public T getComponent() {
        return getSource();
    }
}
