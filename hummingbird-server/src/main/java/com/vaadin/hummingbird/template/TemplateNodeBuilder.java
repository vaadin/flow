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
package com.vaadin.hummingbird.template;

import java.util.Collection;

/**
 * Mutable builder for immutable template nodes.
 *
 * @author Vaadin Ltd
 */
@FunctionalInterface
public interface TemplateNodeBuilder {
    /**
     * Creates a collection of template nodes (usually only one) based on the
     * current configuration of this builder. The provided parent reference
     * might not be fully constructed when this method is called, so the
     * implementation should only store the reference for later use.
     *
     * @param parent
     *            the node to set as the parent of the built node, or
     *            <code>null</code> if the if the created node will be the root
     *            of a template tree
     *
     * @return a collection of new template nodes, not <code>null</code>
     */
    Collection<TemplateNode> build(TemplateNode parent);
}
