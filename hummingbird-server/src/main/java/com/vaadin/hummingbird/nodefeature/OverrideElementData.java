/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.nodefeature;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.template.TemplateNode;

/**
 * Metadata for a state node holding data to override configuration of an
 * element configured through a {@link TemplateNode}. This map only contains
 * meta data to keep track of the role of the state node, all the actual element
 * data is in the same feature type as used for regular elements, such as
 * {@link ElementChildrenList}.
 *
 * @author Vaadin Ltd
 */
public class OverrideElementData extends NodeMap {

    /**
     * Name of the key used for storing the id of the template node that the
     * overriden element corresponds to.
     */
    public static final String TEMPLATE_NODE_ID_PROPERTY = "node";

    /**
     * Creates a new override element data map for the given node.
     *
     * @param node
     *            the node that the list belongs to
     */
    public OverrideElementData(StateNode node) {
        super(node);
    }

    /**
     * Sets the template node that the overridden element corresponds to. The
     * template node is needed for defining the parent element for elements in
     * an {@link ElementChildrenList} in the same state node. The template node
     * can only be set once.
     *
     * @param templateNode
     *            the template node to set, not <code>null</code>
     */
    public void setTemplateNode(TemplateNode templateNode) {
        assert templateNode != null;
        assert !contains(TEMPLATE_NODE_ID_PROPERTY);

        put(TEMPLATE_NODE_ID_PROPERTY, Integer.valueOf(templateNode.getId()));
    }

    /**
     * Gets the template node that the overridden element corresponds to.
     *
     * @see #setTemplateNode(TemplateNode)
     *
     * @return the template node
     */
    public TemplateNode getTemplateNode() {
        Object nodeId = get(TEMPLATE_NODE_ID_PROPERTY);
        if (nodeId == null) {
            return null;
        }

        return TemplateNode.get(((Integer) nodeId).intValue());
    }

}
