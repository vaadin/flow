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
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;
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
     * Resource path for importing dnd connector for compatibility mode.
     */
    public static final String DND_CONNECTOR_COMPATIBILITY = "frontend://dndConnector.js";

    /**
     * Resource path for importing dnd connector.
     */
    public static final String DND_CONNECTOR = "./dndConnector-es6.js";
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

    /**
     * Key for storing detach listener for a drop target to component data.
     */
    private static final String DETACH_LISTENER_FOR_DROP_TARGET = "_detachListenerForDropTarget";

    private DndUtil() {
        // no instances from this class
    }

    /**
     * Includes the dnd connector when the component is attached to a UI.
     * <p>
     * This is only for the case when the project is in compatibility mode and
     * the static methods in {@link com.vaadin.flow.component.dnd.DragSource
     * DragSource} and {@link com.vaadin.flow.component.dnd.DropTarget
     * DropTarget} are used, because otherwise the connector is not loaded.
     * 
     * @param component
     *            the component that should be attached and uses dnd connector
     */
    public static void addDndConnectorWhenComponentAttached(
            Component component) {
        component.getElement().getNode().runWhenAttached(ui -> {
            if (ComponentUtil.getData(ui, DND_CONNECTOR_COMPATIBILITY) == null
                    && ui.getSession().getConfiguration()
                            .isCompatibilityMode()) {
                ui.getPage().addJavaScript(DND_CONNECTOR_COMPATIBILITY,
                        LoadMode.EAGER);
                ComponentUtil.setData(ui, DND_CONNECTOR_COMPATIBILITY, true);
            }
        });
    }

    /**
     * Adds the mobile dnd polyfills when a iOS device is used. Calling this is
     * NOOP for non-iOS devices. The polyfills are only loaded once per page.
     * 
     * @param component
     *            the component using dnd
     */
    public static void addMobileDndPolyfillIfNeeded(Component component) {
        component.getElement().getNode().runWhenAttached(ui -> {
            if (ui.getSession().getBrowser().isIOS()) {
                ui.getPage().addJavaScript(
                        "context://webjars/mobile-drag-drop/2.3.0-rc.1/index.min.js");
                ui.getPage().addJavaScript(
                        "context://webjars/vaadin__vaadin-mobile-drag-drop/1.0.0/index.min.js");
            }
        });
    }

    /**
     * Triggers drag source activation method in JS connector once when the
     * component has been attached.
     * 
     * @param dragSource
     *            the drag source to update active status on
     * @param <T>
     *            the type of the drag source component
     */
    public static <T extends Component> void updateDragSourceActivation(
            DragSource<T> dragSource) {
        Command command = () -> dragSource.getDraggableElement().executeJs(
                "window.Vaadin.Flow.dndConnector.updateDragSource($0)",
                dragSource.getDraggableElement());
        runOnAttachBeforeResponse(dragSource.getDragSourceComponent(), command);
    }

    /**
     * Triggers drop target activation method in JS connector once when the
     * component has been attached. Will make sure the activation in JS is done
     * again when the component is detached and attached again, because
     * otherwise the element will not be a drop target again.
     * 
     * @param dropTarget
     *            the drop target to update active status on
     * @param <T>
     *            the type of the drop target component
     */
    public static <T extends Component> void updateDropTargetActivation(
            DropTarget<T> dropTarget) {
        Command command = () -> dropTarget.getElement().executeJs(
                "window.Vaadin.Flow.dndConnector.updateDropTarget($0)",
                dropTarget.getElement());

        runOnAttachBeforeResponse(dropTarget.getDropTargetComponent(), command);

        // add a detach listener which will make sure the activation is done
        // again on the client side if the component is removed and added again
        if (ComponentUtil.getData(dropTarget.getDropTargetComponent(),
                DETACH_LISTENER_FOR_DROP_TARGET) == null) {
            Registration detachRegistration = dropTarget.getElement()
                    .addDetachListener(event -> runOnAttachBeforeResponse(
                            dropTarget.getDropTargetComponent(), command));
            ComponentUtil.setData(dropTarget.getDropTargetComponent(),
                    DETACH_LISTENER_FOR_DROP_TARGET, detachRegistration);
        }
    }

    private static void runOnAttachBeforeResponse(Component component,
            Command command) {
        component.getElement().getNode().runWhenAttached(ui -> ui
                .beforeClientResponse(component, context -> command.execute()));
    }

    /**
     * Reports DnD feature usage from mixin interfaces.
     */
    public static void reportUsage() {
        UsageStatistics.markAsUsed("flow/generic-dnd", null);
    }
}
