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

import java.util.Optional;

import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.template.angular.AbstractControlTemplateNode;

/**
 * Keeps track of an generator template node that might be needed for finding
 * the parent of an element.
 *
 * @author Vaadin Ltd
 */
public class ParentGeneratorHolder extends ServerSideFeature {

    private AbstractControlTemplateNode parentTemplate;

    /**
     * Creates a new feature for the given node.
     *
     * @param node
     *            the node which supports the feature
     */
    public ParentGeneratorHolder(StateNode node) {
        super(node);
    }

    /**
     * Sets the generator template node that should be used for finding the
     * parent of the element represented by this node.
     *
     * @param parentTemplate
     *            the parent generator, or <code>null</code> to remove a
     *            previous generator
     */
    public void setParentGenerator(AbstractControlTemplateNode parentTemplate) {
        this.parentTemplate = parentTemplate;
    }

    /**
     * Gets the parent generator.
     *
     * @return an optional parent generator, or an empty optional if no parent
     *         generator is defined
     */
    public Optional<AbstractControlTemplateNode> getParentGenerator() {
        return Optional.ofNullable(parentTemplate);
    }

}
