/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
