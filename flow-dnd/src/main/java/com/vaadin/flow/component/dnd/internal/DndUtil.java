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

package com.vaadin.flow.component.dnd.internal;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.shared.ui.LoadMode;

/**
 * Internal class for drag and drop related utility methods. This class is not
 * meant for external usage and can be removed at any point.
 * 
 * @author Vaadin Ltd
 * @since 2.0
 */
public class DndUtil {

    /**
     * Resource path for importing dnd connector.
     */
    public static final String DND_CONNECTOR = "frontend://dndConnector";
    /**
     * Property name for storing the
     * {@link com.vaadin.flow.component.dnd.EffectAllowed} on element level.
     */
    public static final String EFFECT_ALLOWED_ELEMENT_PROPERTY = "__effectAllowed";

    /**
     * Key for storing server side drag data for a
     * {@link com.vaadin.flow.component.dnd.DragSource}.
     */
    public static final String DRAG_SOURCE_DATA_KEY = "drag-source-data";

    /**
     * Key for storing an internal drag start listener registration for a
     * {@link com.vaadin.flow.component.dnd.DragSource}.
     */
    public static final String START_LISTENER_REGISTRATION_KEY = "_startListenerRegistration";

    /**
     * Key for storing an internal drag end listener registration for a
     * {@link com.vaadin.flow.component.dnd.DragSource}.
     */
    public static final String END_LISTENER_REGISTRATION_KEY = "_endListenerRegistration";

    /**
     * Property name for storing drop target activity data for an element.
     */
    public static final String DROP_TARGET_ACTIVE_PROPERTY = "__active";

    /**
     * Property name for storing the
     * {@link com.vaadin.flow.component.dnd.DropEffect} on element level.
     */
    public static final String DROP_EFFECT_ELEMENT_PROPERTY = "__dropEffect";

    private DndUtil() {
        // no instances from this class
    }

    /**
     * Includes the dnd connector when the component is attached to a UI.
     * 
     * @param component
     *            the component that should be attached
     */
    public static void addDndConnectorWhenComponentAttached(
            Component component) {
        component.getElement().getNode().runWhenAttached(ui -> ui.getPage()
                .addJavaScript(DND_CONNECTOR, LoadMode.EAGER));
    }
}
