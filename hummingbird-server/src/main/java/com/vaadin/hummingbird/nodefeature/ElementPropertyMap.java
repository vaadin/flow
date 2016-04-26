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

package com.vaadin.hummingbird.nodefeature;

import java.io.Serializable;

import com.vaadin.hummingbird.StateNode;

/**
 * Map for element property values.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ElementPropertyMap extends AbstractPropertyMap {

    /**
     * Creates a new element property map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     */
    public ElementPropertyMap(StateNode node) {
        super(node);
    }

    @Override
    public void setProperty(String name, Serializable value,
            boolean emitChange) {
        assert !"textContent".equals(name);
        assert !"classList".equals(name);
        assert !"className".equals(name);

        super.setProperty(name, value, emitChange);
    }
}
