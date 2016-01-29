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

package com.vaadin.hummingbird.namespace;

import com.vaadin.hummingbird.StateNode;

/**
 * Namespace for nodes describing the child elements of an element.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ElementChildrenNamespace extends ListNamespace {

    /**
     * Creates a new element children namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    public ElementChildrenNamespace(StateNode node) {
        super(node);
    }

}
