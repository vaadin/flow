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
package com.vaadin.flow.component.dnd;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

/**
 * HTML5 drag start event.
 *
 * @param <T>
 *            Type of the component that is dragged.
 * @author Vaadin Ltd
 * @see DragSourceExtension#addDragStartListener(DragStartListener)
 */
public class DragStartEvent<T extends Component> extends ComponentEvent<T> {
    private final EffectAllowed effectAllowed;

    /**
     * Creates a drag start event.
     *
     * @param source
     *            Component that is dragged.
     * @param effectAllowed
     *            Allowed effects from {@code DataTransfer.effectAllowed}
     *            object.
     */
    public DragStartEvent(T source, EffectAllowed effectAllowed) {
        super(source, true);

        this.effectAllowed = effectAllowed;
    }

    /**
     * Returns the {@code effectAllowed} parameter of this event.
     *
     * @return This event's {@code effectAllowed} parameter.
     */
    public EffectAllowed getEffectAllowed() {
        return effectAllowed;
    }

    /**
     * Returns the drag source component where the dragstart event occurred.
     *
     * @return Component which is dragged.
     */
    public T getComponent() {
        return getSource();
    }
}
