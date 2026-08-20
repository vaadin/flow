/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;

/**
 * Helpers for describing state nodes in messages logged by the framework.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class StateNodeUtil {

    private StateNodeUtil() {
        // Only static helpers
    }

    /**
     * Describes which part of the application the given node belongs to, for
     * inclusion in a log message.
     *
     * @param node
     *            the node to describe, may be <code>null</code>
     * @return a description of the node, never <code>null</code>
     */
    public static String describeTarget(StateNode node) {
        if (node == null) {
            return "unknown node";
        }

        StringBuilder targetInfo = new StringBuilder("node id=")
                .append(node.getId());
        // The node is not necessarily usable as an element even when it has
        // the feature, and a description for a log message must never throw
        if (BasicElementStateProvider.get().supports(node)) {
            Element element = Element.get(node);
            targetInfo.append(", element with tag '").append(element.getTag())
                    .append("'");
            Optional<Component> component = element.getComponent();
            if (component.isPresent()) {
                targetInfo.append(", component '")
                        .append(component.get().getClass().getName())
                        .append("'");
                /*
                 * The routing target is identified by its class since the path
                 * in its annotation is not necessarily the path it is served
                 * from: the path may be a placeholder for a name derived from
                 * the class, and it doesn't include the prefixes that parent
                 * layouts contribute.
                 */
                ComponentUtil.getRouteComponent(component.get()).filter(
                        routeComponent -> routeComponent != component.get())
                        .ifPresent(routeComponent -> targetInfo
                                .append(", used in '")
                                .append(routeComponent.getClass().getName())
                                .append("'"));
            }
        }
        return targetInfo.toString();
    }
}
