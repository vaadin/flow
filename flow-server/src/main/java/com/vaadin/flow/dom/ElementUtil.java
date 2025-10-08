/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.dom;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.dom.impl.BasicTextElementStateProvider;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementData;
import com.vaadin.flow.internal.nodefeature.InertData;
import com.vaadin.flow.internal.nodefeature.TextNodeMap;

/**
 * Provides utility methods for {@link Element}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ElementUtil {

    /**
     * Pattern for matching valid tag names, according to
     * https://www.w3.org/TR/html-markup/syntax.html#tag-name "HTML elements all
     * have names that only use characters in the range 0–9, a–z, and A–Z."
     */
    private static Pattern tagNamePattern = Pattern
            .compile("^[a-zA-Z][a-zA-Z0-9-_\\.]*$");

    private ElementUtil() {
        // Util methods only
    }

    /**
     * Checks if the given tag name is valid.
     *
     * @param tag
     *            the tag name
     * @return true if the string is valid as a tag name, false otherwise
     */
    public static boolean isValidTagName(String tag) {
        return tag != null && tagNamePattern.matcher(tag).matches();
    }

    /**
     * Checks if the given attribute name is valid.
     *
     * @param attribute
     *            the name of the attribute in lower case
     * @return true if the name is valid, false otherwise
     */
    public static boolean isValidAttributeName(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return false;
        }
        assert attribute.equals(attribute.toLowerCase(Locale.ENGLISH));

        // https://html.spec.whatwg.org/multipage/syntax.html#attributes-2
        // Attribute names must consist of one or more characters other than the
        // space characters, U+0000 NULL, U+0022 QUOTATION MARK ("), U+0027
        // APOSTROPHE ('), U+003E GREATER-THAN SIGN (>), U+002F SOLIDUS (/), and
        // U+003D EQUALS SIGN (=) characters, the control characters, and any
        // characters that are not defined by Unicode.
        char[] illegalCharacters = new char[] { 0, ' ', '"', '\'', '>', '/',
                '=' };
        for (char c : illegalCharacters) {
            if (attribute.indexOf(c) != -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates the given style property name and throws an exception if the
     * name is invalid.
     *
     * @param name
     *            the style property name to validate
     */
    public static void validateStylePropertyName(String name) {
        String reason = getInvalidStylePropertyNameError(name);
        if (reason != null) {
            throw new IllegalArgumentException(reason);
        }
    }

    private static String getInvalidStylePropertyNameError(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "A style property name cannot be null or empty";
        }

        if (name.startsWith(" ") || name.endsWith(" ")) {
            return String.format(
                    "Invalid style property name '%s': a style property name cannot start or end in whitespace",
                    name);
        }

        if (name.contains(":")) {
            return String.format(
                    "Invalid style property name '%s': a style property name cannot contain colons",
                    name);
        }

        return null;
    }

    /**
     * Checks if the given style property name is valid.
     *
     * @param name
     *            the name to validate
     * @return true if the name is valid, false otherwise
     */
    public static boolean isValidStylePropertyName(String name) {
        return getInvalidStylePropertyNameError(name) == null;
    }

    /**
     * Checks if the given style property value is valid.
     *
     * @param value
     *            the value to validate
     * @return true if the value is valid, false otherwise
     */
    public static boolean isValidStylePropertyValue(String value) {
        return getInvalidStylePropertyValueError(value) == null;
    }

    /**
     * Checks if the given style property value is valid.
     * <p>
     * Throws an exception if it's certain the value is invalid
     *
     * @param value
     *            the value to validate
     */
    public static void validateStylePropertyValue(String value) {
        String reason = getInvalidStylePropertyValueError(value);
        if (reason != null) {
            throw new IllegalArgumentException(reason);
        }
    }

    private static String getInvalidStylePropertyValueError(String value) {
        if (value.endsWith(";")) {
            return "A style value cannot end in semicolon";
        }
        return null;
    }

    /**
     * Defines a mapping between this element and the given {@link Component}.
     * <p>
     * An element can only be mapped to one component and the mapping cannot be
     * changed. The only exception is {@link Composite} which can overwrite the
     * mapping for its content.
     *
     * @param element
     *            the element to map to the component
     * @param component
     *            the component this element is attached to
     */
    public static void setComponent(Element element, Component component) {
        if (element == null) {
            throw new IllegalArgumentException("Element must not be null");
        }
        if (component == null) {
            throw new IllegalArgumentException("Component must not be null");
        }

        Optional<Component> currentComponent = element.getComponent();
        if (currentComponent.isPresent()) {
            // Composite can replace its content
            boolean isCompositeReplacingItsContent = component instanceof Composite
                    && component.getChildren().findFirst()
                            .get() == currentComponent.get();
            if (!isCompositeReplacingItsContent) {
                throw new IllegalStateException("A component of type "
                        + currentComponent.get().getClass().getName()
                        + " is already attached to this element");
            }
        }
        element.getStateProvider().setComponent(element.getNode(), component);
    }

    /**
     * Converts the given element and its children to a JSoup node with
     * children.
     *
     * @param document
     *            A JSoup document
     * @param element
     *            The element to convert
     * @return A JSoup node containing the converted element
     */
    public static Node toJsoup(Document document, Element element) {
        if (element.isTextNode()) {
            return new TextNode(element.getText());
        }

        org.jsoup.nodes.Element target = document
                .createElement(element.getTag());
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

        element.getChildren()
                .forEach(child -> target.appendChild(toJsoup(document, child)));

        return target;
    }

    /**
     * Converts a given JSoup {@link org.jsoup.nodes.Node} and its children into
     * a matching {@link com.vaadin.flow.dom.Element} hierarchy.
     * <p>
     * Only nodes of type {@link org.jsoup.nodes.TextNode} and
     * {@link org.jsoup.nodes.Element} are converted - other node types return
     * an empty optional.
     *
     * @param node
     *            JSoup node to convert
     * @return element with the matching hierarchy as the given node, or empty
     */
    public static Optional<Element> fromJsoup(Node node) {
        Element ret;
        if (node instanceof TextNode) {
            return Optional.of(Element.createText(((TextNode) node).text()));
        } else if (node instanceof org.jsoup.nodes.Element) {
            ret = new Element(((org.jsoup.nodes.Element) node).tagName());
        } else {
            LoggerFactory.getLogger(ElementUtil.class).error(
                    "Could not convert a {}, '{}' into {}!",
                    Node.class.getName(), node, Element.class.getName());
            return Optional.empty();
        }

        node.attributes().asList().forEach(attribute -> ret
                .setAttribute(attribute.getKey(), attribute.getValue()));

        List<Node> childNodes = node.childNodes();
        if (!childNodes.isEmpty()) {
            childNodes.forEach(
                    child -> fromJsoup(child).ifPresent(ret::appendChild));
        }

        return Optional.of(ret);
    }

    /**
     * Checks whether the given element is a custom element or not.
     * <p>
     * Custom elements (Web Components) are recognized by having at least one
     * dash in the tag name.
     *
     * @param element
     *            the element to check
     * @return <code>true</code> if a custom element, <code>false</code> if not
     */
    public static boolean isCustomElement(Element element) {
        return !element.isTextNode() && element.getTag().contains("-");
    }

    /**
     * Checks whether the given element is a <code>script</code> or not.
     *
     * @param element
     *            the element to check
     * @return <code>true</code> if a script, <code>false</code> if not
     */
    public static boolean isScript(Element element) {
        return !element.isTextNode()
                && "script".equalsIgnoreCase(element.getTag());
    }

    /**
     * Sets whether or not the element should inherit or not inherit its
     * parent's inert state. Default value is {@code false}.
     *
     * @param element
     *            the element to update
     * @param ignoreParentInert
     *            {@code true} for ignoring parent inert, {@code false} for not
     *            ignoring
     * @see #setInert(Element, boolean)
     */
    public static void setIgnoreParentInert(Element element,
            boolean ignoreParentInert) {
        final Optional<InertData> optionalInertData = element.getNode()
                .getFeatureIfInitialized(InertData.class);
        if (ignoreParentInert) {
            optionalInertData
                    .orElse(element.getNode().getFeature(InertData.class))
                    .setIgnoreParentInert(true);
        } else { // by default InertData not present
            optionalInertData.ifPresent(
                    inertData -> inertData.setIgnoreParentInert(false));
        }
    }

    /**
     * Sets whether or not the given element is inert. When an element is inert,
     * it does not receive any updates or interaction from the client side. The
     * inert state is inherited to all child elements, unless those are ignoring
     * the inert state.
     *
     * @param element
     *            the element to update
     * @param inert
     *            {@code true} for inert
     * @see #setIgnoreParentInert(Element, boolean)
     */
    public static void setInert(Element element, boolean inert) {
        final Optional<InertData> optionalInertData = element.getNode()
                .getFeatureIfInitialized(InertData.class);
        if (inert) {
            optionalInertData
                    .orElse(element.getNode().getFeature(InertData.class))
                    .setInertSelf(true);
        } else { // default when no inert data present
            optionalInertData
                    .ifPresent(inertData -> inertData.setInertSelf(false));
        }
    }

    /**
     * Gets the element mapped to the given state node.
     *
     * @param node
     *            the state node, not <code>null</code>
     * @return the element for the node, or an empty Optional if the state node
     *         is not mapped to any particular element.
     */
    public static Optional<Element> from(StateNode node) {
        assert node != null;

        if (node.hasFeature(TextNodeMap.class)) {
            return Optional
                    .of(Element.get(node, BasicTextElementStateProvider.get()));
        } else if (node.hasFeature(ElementData.class)) {
            return Optional
                    .of(Element.get(node, BasicElementStateProvider.get()));
        } else {
            return Optional.empty();
        }
    }

}
