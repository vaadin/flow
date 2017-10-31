/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui.renderers;

import java.util.UUID;

import com.vaadin.flow.dom.Element;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

/**
 * Helper class for components that uses {@link ComponentRenderer} internally.
 * It contains utility methods for managing the renderer context at the
 * server-side.
 * 
 * @author Vaadin Ltd.
 *
 */
public class ComponentRendererUtil {

    /**
     * Creates a container to hold all rendered components at the client side.
     * The container is attached to the DOM, but it's no visible to the user.
     * <p>
     * The container is used by the `<flow-component-renderer>` webcomponent to
     * fetch the actual instances to be used inside the templates.
     * 
     * @param owner
     *            the owner of the container. The container is removed from the
     *            DOM whenever the owner is detached, and readded when the owner
     *            is reattached. The container is not a child of the owner
     *            though
     * @return an element that can be used as container for rendered components
     */
    public static Element createContainerForRenderers(Component owner) {
        Element container = new Element("div", false);
        container.getStyle().set("display", "none");

        String containerId = UUID.randomUUID().toString();
        container.setAttribute("id", containerId);

        owner.getElement().getNode().runWhenAttached(ui -> {
            ui.getElement().appendChild(container);
            owner.addAttachListener(
                    event -> event.getUI().getElement().appendChild(container));
        });

        owner.addDetachListener(event -> container.removeFromParent());
        return container;
    }

    /**
     * Removes a rendered component by selector. This removal doesn't affect the
     * server-side StateTree.
     * 
     * @param ui
     *            the current UI
     * @param container
     *            the container that holds the rendered component
     * @param selector
     *            the selector to fetch the correct element
     */
    public static void removeRendereredComponent(UI ui, Element container,
            String selector) {
        ui.getPage().executeJavaScript(
                "$0.removeChild($0.querySelector(\"" + selector + "\"));",
                container);
    }

}
