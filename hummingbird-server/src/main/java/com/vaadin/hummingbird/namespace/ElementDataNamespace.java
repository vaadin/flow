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

package com.vaadin.hummingbird.namespace;

import com.vaadin.hummingbird.StateNode;

/**
 * Namespace for basic element information.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ElementDataNamespace extends MapNamespace {

    private static final String TAG = "tag";
    private static final String IS = "is";

    /**
     * Creates a new element data namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     *
     */
    public ElementDataNamespace(StateNode node) {
        super(node);
    }

    /**
     * Sets the tag name of the element.
     *
     * @param tag
     *            the tag name
     */
    public void setTag(String tag) {
        put(TAG, tag);
    }

    /**
     * Gets the tag name of the element.
     *
     * @return the tag name
     */
    public String getTag() {
        return (String) get(TAG);
    }

    /**
     * Sets the type of the element as defined by the "is" attribute.
     * <p>
     * Note that this cannot be changed after initialization.
     *
     * @param is
     *            the is attribute
     */
    public void setIs(String is) {
        put(IS, is);
    }

    /**
     * Gets the type of the element as defined by the "is" attribute.
     *
     * @return the is attribute
     */
    public String getIs() {
        return (String) get(IS);
    }
}
