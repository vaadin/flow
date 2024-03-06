/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.flow.util;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * JsInterop bridge to the JavaScript <code>Object</code> type.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
public class JsObject {
    /**
     * A property descriptor that can be passed to
     * {@link JsObject#defineProperty(Object, String, PropertyDescriptor)}.
     */
    @JsType(isNative = true)
    public interface PropertyDescriptor {
        // Just a marker for now since we create the instance using JSNI to get
        // $entry support for the callbacks
    }

    /**
     * Adds a property to a JavaScript object.
     *
     * @param object
     *            the object to which the property should be added, not
     *            <code>null</code>
     * @param name
     *            the name of the property to add, not <code>null</code>
     * @param descriptor
     *            a descriptor for the property, not <code>null</code>
     */
    public static native void defineProperty(Object object, String name,
            PropertyDescriptor descriptor);
}
