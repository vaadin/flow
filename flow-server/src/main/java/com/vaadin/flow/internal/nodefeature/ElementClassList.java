/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.dom.ClassList;
import com.vaadin.flow.internal.StateNode;

/**
 * Handles CSS class names for an element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ElementClassList extends SerializableNodeList<String> {

    private static class ClassListView extends NodeList.SetView<String>
            implements ClassList {

        private ClassListView(ElementClassList elementClassList) {
            super(elementClassList);
        }

        @Override
        protected void validate(String className) {
            if (className == null) {
                throw new IllegalArgumentException("Class name cannot be null");
            }

            if ("".equals(className)) {
                throw new IllegalArgumentException(
                        "Class name cannot be empty");
            }
            if (className.indexOf(' ') != -1) {
                throw new IllegalArgumentException(
                        "Class name cannot contain spaces");
            }
        }
    }

    /**
     * Creates a new class list for the given node.
     *
     * @param node
     *            the node that the list belongs to
     */
    public ElementClassList(StateNode node) {
        super(node);
    }

    /**
     * Creates a view into this list.
     *
     * @return a view into this list
     */
    public ClassList getClassList() {
        return new ClassListView(this);
    }
}
