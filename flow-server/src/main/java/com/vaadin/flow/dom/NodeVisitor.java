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
package com.vaadin.flow.dom;

/**
 * Element API node visitor interface.
 *
 * @see Node #accept(NodeVisitor, boolean)
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public interface NodeVisitor {

    /**
     * The type of the element.
     *
     * @author Vaadin Ltd
 * @since 1.0
     *
     */
    enum ElementType {
        /**
         * The type of the regular element: the element which has been created
         * via Element API and attached in a regular way.
         */
        REGULAR,
        /**
         * The type of the virtual element: the element which has been created
         * via Element API and attached using
         * {@link Element#appendVirtualChild(Element...)}.
         */
        VIRTUAL,
        /**
         * The type of the virtual element: the element which is created
         * automatically by the engine for the existing client side element and
         * attached to it.
         * <p>
         * It happens for injected elements via {@code @Id} and for the
         * sub-templates (templates in templates).
         */
        VIRTUAL_ATTACHED;
    }

    /**
     * Visit the {@code element} using provided element {@code type}.
     *
     * @param type
     *            the element type
     * @param element
     *            the element to visit
     * @return <code>true</code> to visit descendants, <code>false</code> to
     *         stop traversal
     */
    boolean visit(ElementType type, Element element);

    /**
     * Visit the shadow {@code root}.
     *
     * @param root
     *            the shadow root to visit
     * @return <code>true</code> to visit descendants, <code>false</code> to
     *         stop traversal
     */
    boolean visit(ShadowRoot root);

}
