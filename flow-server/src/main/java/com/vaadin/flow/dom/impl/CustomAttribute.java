/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.dom.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.dom.Element;

/**
 * Callback for handling attributes with special semantics. This is used for
 * e.g. <code>class</code> which is assembled from a separate list of tokens
 * instead of being stored as a regular attribute string.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.0
 */
public abstract class CustomAttribute implements Serializable {

    static {
        Map<String, CustomAttribute> map = new HashMap<>();

        map.put("class", new ClassAttributeHandler());
        map.put("style", new StyleAttributeHandler());

        customAttributes = Collections.unmodifiableMap(map);
    }

    private static final Map<String, CustomAttribute> customAttributes;

    /**
     * Gets the custom attribute with the provided name, if present.
     *
     * @param name
     *            the name of the attribute
     * @return and optional custom attribute, or an empty optional if there is
     *         no attribute with the given name
     */
    public static Optional<CustomAttribute> get(String name) {
        return Optional.ofNullable(customAttributes.get(name));
    }

    /**
     * Gets an unmodifiable set of custom attribute names.
     *
     * @return a set of attribute names
     */
    public static Set<String> getNames() {
        return customAttributes.keySet();
    }

    /**
     * Checks what {@link Element#hasAttribute(String)} should return for this
     * attribute.
     *
     * @param element
     *            the element to check, not <code>null</code>
     * @return <code>true</code> if the element has a value for this attribute,
     *         otherwise <code>false</code>
     */
    public abstract boolean hasAttribute(Element element);

    /**
     * Gets the value that should be returned by
     * {@link Element#getAttribute(String)} for this attribute.
     *
     * @param element
     *            the element to check, not <code>null</code>
     * @return the attribute value
     */
    public abstract String getAttribute(Element element);

    /**
     * Sets the value when {@link Element#setAttribute(String, String)} is
     * called for this attribute.
     *
     * @param element
     *            the element for which to set the value, not <code>null</code>
     * @param value
     *            the new attribute value, not <code>null</code>
     */
    public abstract void setAttribute(Element element, String value);

    /**
     * Removes the attribute when {@link Element#removeAttribute(String)} is
     * called for this attribute.
     *
     * @param element
     *            the element for which to remove the attribute, not
     *            <code>null</code>
     */
    public abstract void removeAttribute(Element element);
}
