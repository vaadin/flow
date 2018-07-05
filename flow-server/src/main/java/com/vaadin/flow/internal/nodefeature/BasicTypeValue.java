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
package com.vaadin.flow.internal.nodefeature;

import java.io.Serializable;

import com.vaadin.flow.internal.StateNode;

/**
 * The feature contains a value of the basic type.
 * <p>
 * The value is wrapped into a {@link StateNode} and this feature instead being
 * sent directly. It allows to use basic types in lists.
 * 
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class BasicTypeValue extends NodeValue<Serializable> {

    /**
     * Creates a new value map for the given node.
     *
     * @param node
     *            the node that the map belongs to
     *
     */
    public BasicTypeValue(StateNode node) {
        super(node);
    }

    @Override
    protected String getKey() {
        return NodeProperties.VALUE;
    }

    /**
     * Sets the value of a basic type.
     *
     * @param value
     *            the value to set
     */
    @Override
    public void setValue(Serializable value) {
        super.setValue(value);
    }

    /**
     * Gets the value of a basic type.
     *
     * @return the value
     */
    @Override
    public Serializable getValue() {
        return super.getValue();
    }
}
