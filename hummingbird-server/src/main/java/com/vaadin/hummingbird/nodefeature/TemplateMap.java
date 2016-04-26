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
import com.vaadin.hummingbird.shared.NodeFeatures;
import com.vaadin.hummingbird.template.TemplateNode;

/**
 * Map for nodes used as template roots.
 *
 * @since
 * @author Vaadin Ltd
 */
public class TemplateMap extends NodeMap {

    /**
     * Creates a new template map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     */
    public TemplateMap(StateNode node) {
        super(node);
    }

    /**
     * Gets the template node used for the state node.
     *
     * @return the template node
     */
    public TemplateNode getRootTemplate() {
        int rootId = getOrDefault(NodeFeatures.ROOT_TEMPLATE_ID, -1);
        return TemplateNode.get(rootId);
    }

    /**
     * Sets the template node used for the state node.
     *
     * @param rootTemplate
     *            the root template node, not <code>null</code>
     */
    public void setRootTemplate(TemplateNode rootTemplate) {
        assert rootTemplate != null;
        assert !rootTemplate.getParent()
                .isPresent() : "Root template node should not have any parent";

        put(NodeFeatures.ROOT_TEMPLATE_ID,
                Integer.valueOf(rootTemplate.getId()));
    }
}
