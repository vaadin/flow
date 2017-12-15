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
package com.vaadin.flow.nodefeature;

import java.util.Optional;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.polymertemplate.PolymerTemplate;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentUtil;
import com.vaadin.ui.Composite;

/**
 * A server side only node feature for mapping a node to a component.
 *
 * @author Vaadin Ltd
 */
public class ComponentMapping extends ServerSideFeature {

    private Component component = null;

    /**
     * Creates an instance of this node feature.
     *
     * @param node
     *            the node that the feature belongs to
     */
    protected ComponentMapping(StateNode node) {
        super(node);
    }

    /**
     * Assigns the given component to this node.
     * <p>
     * When assigning a component to the node, there must be no previously
     * assigned component.
     *
     * @param component
     *            the component to assign to this node, not {@code null}
     */
    public void setComponent(Component component) {
        assert component != null : "Component must not be null";
        assert this.component == null
                || component instanceof Composite : "Only a Composite is allowed to remap a component";
        this.component = component;

        if (getNode().hasFeature(ClientDelegateHandlers.class)) {
            getNode().getFeature(ClientDelegateHandlers.class)
                    .componentSet(component);
        }
        if (component instanceof PolymerTemplate<?>
                && getNode().hasFeature(PolymerServerEventHandlers.class)) {
            getNode().getFeature(PolymerServerEventHandlers.class)
                    .componentSet((PolymerTemplate<?>) component);
        }
    }

    /**
     * Gets the component this node has been mapped to, if any.
     *
     * @return an optional component, or an empty optional if no component has
     *         been mapped to this node
     */
    public Optional<Component> getComponent() {
        return Optional.ofNullable(component);
    }

    @Override
    public void onAttach(boolean initialAttach) {
        getComponent().ifPresent(
                c -> ComponentUtil.onComponentAttach(c, initialAttach));
    }

    @Override
    public void onDetach() {
        getComponent().ifPresent(ComponentUtil::onComponentDetach);
    }

}
