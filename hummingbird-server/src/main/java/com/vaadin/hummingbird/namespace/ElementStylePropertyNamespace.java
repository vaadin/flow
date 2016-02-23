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

import java.io.Serializable;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.ElementUtil;
import com.vaadin.hummingbird.dom.Style;
import com.vaadin.hummingbird.dom.impl.BasicElementStyle;

/**
 * Namespace for element style values.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ElementStylePropertyNamespace extends AbstractPropertyNamespace {

    /**
     * Creates a new element style namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    public ElementStylePropertyNamespace(StateNode node) {
        super(node);
    }

    @Override
    public void setProperty(String name, Serializable value) {
        assert ElementUtil.isValidStylePropertyValue(value);
        super.setProperty(name, value);
    }

    /**
     * Returns a style instance for managing element inline styles.
     *
     * @return a Style instance connected to this namespace
     */
    public Style getStyle() {
        return new BasicElementStyle(this);
    }

}
