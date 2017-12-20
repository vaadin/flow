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
package com.vaadin.flow.internal.nodefeature;

import com.vaadin.flow.internal.StateNode;

/**
 * Data to make node visible/invisible.
 *
 * @author Vaadin Ltd
 *
 */
public class VisibilityData extends NodeValue<Boolean> {

    /**
     * Creates a new feature for the given node.
     *
     * @param node
     *            the node that the map belongs to
     *
     */
    public VisibilityData(StateNode node) {
        super(node);
    }

    @Override
    protected String getKey() {
        return NodeProperties.VISIBLE;
    }

    /**
     * Sets the node visibility value.
     *
     * @param visible
     *            the visibility value
     */
    public void setVisible(boolean visible) {
        setValue(visible);
    }

    /**
     * Checks whether the node is visible.
     *
     * @return {@code true} if the node is visible, {@code false} otherwise
     */
    public boolean isVisible() {
        return !Boolean.FALSE.equals(getValue());
    }

}
