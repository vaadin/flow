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
     * @return and optional custom attribute, or an emtpy optional if there is
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
