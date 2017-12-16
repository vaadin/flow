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
package com.vaadin.flow.internal.nodefeature;

import com.vaadin.flow.dom.impl.TemplateElementStateProvider;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.template.angular.TemplateNode;

/**
 * Keeps track of state nodes holding data that overrides configuration of an
 * element configured by a {@link TemplateNode}. The override state nodes has
 * the {@link OverrideElementData} feature as well as regular element features
 * such as {@link ElementChildrenList}.
 *
 * @author Vaadin Ltd
 */
public class TemplateOverridesMap extends NodeMap {

    /**
     * Creates a new template overrides map for the given node.
     *
     * @param node
     *            the node that the list belongs to
     */
    public TemplateOverridesMap(StateNode node) {
        super(node);
    }

    /**
     * Gets, or optionally creates, an override state node for the given
     * template node.
     *
     * @param templateNode
     *            the template node to get an override state node for
     * @param create
     *            <code>true</code> if a node should be created unless it
     *            already exists, <code>false</code> to return null if there is
     *            no existing node
     * @return the override state node, or <code>null</code> if there is no such
     *         node and <code>create</code> is <code>false</code>
     */
    public StateNode get(TemplateNode templateNode, boolean create) {
        String key = String.valueOf(templateNode.getId());
        StateNode overrideNode = (StateNode) get(key);

        if (overrideNode == null && create) {
            overrideNode = TemplateElementStateProvider.createOverrideNode();

            overrideNode.getFeature(OverrideElementData.class)
                    .setTemplateNode(templateNode);

            put(key, overrideNode);
        }

        assert overrideNode == null
                || overrideNode.getFeature(OverrideElementData.class)
                        .getTemplateNode() == templateNode;

        return overrideNode;
    }

}
