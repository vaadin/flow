/*
 * Copyright 2000-2017 Vaadin Ltd.
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

package com.vaadin.flow.nodefeature;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.StateNode;

/**
 * Map for element property values.
 *
 * @author Vaadin Ltd
 */
public class ElementPropertyMap extends AbstractPropertyMap {

    private static final Set<String> forbiddenProperties = Stream
            .of("textContent", "classList", "className")
            .collect(Collectors.toSet());

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
        assert !forbiddenProperties.contains(name) : "Forbidden property name: "
                + name;

        super.setProperty(name, value, emitChange);
    }

    @Override
    protected boolean mayUpdateFromClient(String key, Serializable value) {
        return !forbiddenProperties.contains(key);
    }
}
