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
package com.vaadin.hummingbird.shared;

import com.vaadin.hummingbird.namespace.ElementAttributeNamespace;
import com.vaadin.hummingbird.namespace.ElementChildrenNamespace;
import com.vaadin.hummingbird.namespace.ElementDataNamespace;
import com.vaadin.hummingbird.namespace.ElementListenersNamespace;
import com.vaadin.hummingbird.namespace.ElementPropertiesNamespace;

/**
 * Registry of namespace id numbers and map keys shared between server and
 * client.
 *
 * @since
 * @author Vaadin Ltd
 */
public class Namespaces {
    /**
     * Id for {@link ElementDataNamespace}.
     */
    public static final int ELEMENT_DATA = 0;

    /**
     * Id for {@link ElementPropertiesNamespace}.
     */
    public static final int ELEMENT_PROPERTIES = 1;

    /**
     * Id for {@link ElementChildrenNamespace}.
     */
    public static final int ELEMENT_CHILDREN = 2;

    /**
     * Id for {@link ElementAttributeNamespace}.
     */
    public static final int ELEMENT_ATTRIBUTES = 3;

    /**
     * Id for {@link ElementListenersNamespace}.
     */
    public static final int ELEMENT_LISTENERS = 4;

    /**
     * Key for {@link ElementDataNamespace#getTag()}.
     */
    public static final String TAG = "tag";

    private Namespaces() {
        // Only static
    }
}
