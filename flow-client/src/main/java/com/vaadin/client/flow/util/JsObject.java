/*
 * Copyright 2000-2019 Vaadin Ltd.
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
