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
package com.vaadin.hummingbird.dom;

import java.util.Optional;

import com.vaadin.external.jsoup.nodes.Node;
import com.vaadin.external.jsoup.nodes.TextNode;
import com.vaadin.external.jsoup.parser.Tag;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentUtil;

/**
 * A class with helper methods related to pre-rendering elements.
 *
 * @author Vaadin Ltd
 */
public class Prerenderer {

    private static final String BASE_URI = "";

    private Prerenderer() {
        // Utility class
    }

    /**
     * Converts the given element to a JSoup node, which can be used for
     * pre-rendering.
     * <p>
     * <b>Does NOT include children</b>, use
     * {@link #prerenderElementTree(Element, boolean, boolean)} creating a JSoup
     * node with children.
     *
     * @param element
     *            the element to convert
     * @return the JSoup representation of the given element
     */
    public static Node toJsoup(Element element) {
        if (element.isTextNode()) {
            return new TextNode(element.getText(), BASE_URI);
        }

        com.vaadin.external.jsoup.nodes.Element target = new com.vaadin.external.jsoup.nodes.Element(
                Tag.valueOf(element.getTag()), BASE_URI);
        if (element.hasProperty("innerHTML")) {
            target.html((String) element.getPropertyRaw("innerHTML"));
        }

        element.getAttributeNames().forEach(name -> {
            String attributeValue = element.getAttribute(name);
            if ("".equals(attributeValue)) {
                target.attr(name, true);
            } else {
                target.attr(name, attributeValue);
            }
        });
        return target;
    }

    /**
     * Returns an optional pre-rendered version of the given element and its
     * children, or an empty optional if this element should not be included in
     * the pre-render HTML.
     * <p>
     * {@link ElementUtil#isCustomElement(Element) Web components} and scripts
     * are ignored.
     * <p>
     * This method is a short-hand for
     * {@link #prerenderElementTree(Element, boolean, boolean)}.
     *
     * @param element
     *            the element to pre-render
     * @return the node to pre-render, or an empty optional if the given element
     *         should not be pre-rendered
     * @see #prerenderElementTree(Element, boolean, boolean)
     */
    public static Optional<Node> prerenderElementTree(Element element) {
        return prerenderElementTree(element, true, true);
    }

    /**
     * Returns an optional pre-rendered version of the given element and its
     * children, or an empty optional if this element should not be included in
     * the pre-render HTML.
     *
     * @param element
     *            the element to pre-render
     * @param filterScripts
     *            should scripts be included or not
     * @param filterCustomElements
     *            should custom elements be included or not
     * @return the node to pre-render, or an empty optional if the given element
     *         should not be pre-rendered
     */
    public static Optional<Node> prerenderElementTree(Element element,
            boolean filterScripts, boolean filterCustomElements) {
        if (filterCustomElements && ElementUtil.isCustomElement(element)
                || filterScripts && ElementUtil.isScript(element)) {
            return Optional.empty();
        }

        Node target = toJsoup(element);
        if (target instanceof com.vaadin.external.jsoup.nodes.Element) {
            com.vaadin.external.jsoup.nodes.Element targetElement = (com.vaadin.external.jsoup.nodes.Element) target;
            element.getChildren().forEach(child -> {
                Optional<Component> component = ElementUtil.getComponent(child);
                // if there is a component mapping, let it handle the
                // pre-rendering, if not, just recurse with this method, which
                // is the same what component implementation does by default
                Optional<Node> childNode = component.isPresent()
                        ? ComponentUtil.prerender(component.get())
                        : prerenderElementTree(child, filterScripts,
                                filterCustomElements);
                childNode.ifPresent(targetElement::appendChild);
            });
        }
        return Optional.of(target);
    }
}
