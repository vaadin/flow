/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.nodefeature;

import com.vaadin.flow.dom.ClassList;
import com.vaadin.flow.internal.StateNode;

/**
 * Handles CSS class names for an element.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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
