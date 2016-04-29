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

import java.util.Optional;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.shared.NodeFeatures;
import com.vaadin.hummingbird.template.ChildSlotNode;
import com.vaadin.hummingbird.template.TemplateNode;

/**
 * Map for nodes used as template roots.
 *
 * @author Vaadin Ltd
 */
public class TemplateMap extends NodeMap {

    /**
     * Key for the state node defining <code>@child@</code> slot contents.
     */
    public static final String CHILD_SLOT_CONTENT = "child";

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
     * Sets the template node used for the state node. Must be called to
     * initialize an instance before calling any other methods.
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

    /**
     * Sets the root node of the element that occupies the <code>@child@</code>
     * slot in this template.
     *
     * @param child
     *            the state node of the element to put in the child slot, or
     *            <code>null</code> to remove the current child
     */
    public void setChild(StateNode child) {
        TemplateNode rootTemplate = getRootTemplate();
        if (rootTemplate == null) {
            throw new IllegalStateException(TemplateMap.class.getSimpleName()
                    + " must be initialized using setRootTemplate before using this method.");
        }

        Optional<ChildSlotNode> maybeSlot = ChildSlotNode.find(rootTemplate);
        if (!maybeSlot.isPresent()) {
            throw new IllegalStateException("Template has no child slot");
        }

        ChildSlotNode childTemplateNode = maybeSlot.get();

        // Reset bookkeeping for old child
        getChild().ifPresent(oldChild -> {
            ParentGeneratorHolder oldParentGeneratorHolder = oldChild
                    .getFeature(ParentGeneratorHolder.class);
            assert oldParentGeneratorHolder.getParentGenerator()
                    .get() == childTemplateNode;
            oldParentGeneratorHolder.setParentGenerator(null);
        });

        put(CHILD_SLOT_CONTENT, child);

        // Update bookkeeping for finding the parent of the new child
        if (child != null) {
            ParentGeneratorHolder parentGeneratorHolder = child
                    .getFeature(ParentGeneratorHolder.class);
            assert !parentGeneratorHolder.getParentGenerator().isPresent();
            parentGeneratorHolder.setParentGenerator(childTemplateNode);
        }
    }

    /**
     * Gets the root node of the element that occupies the <code>@child@</code>
     * slot in this template.
     *
     * @return an optional state node, or an empty optional if there is not
     *         child slot content
     */
    public Optional<StateNode> getChild() {
        return Optional.ofNullable((StateNode) get(CHILD_SLOT_CONTENT));
    }

}
