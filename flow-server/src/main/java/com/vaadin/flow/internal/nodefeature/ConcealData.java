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
 * Data to conceal node.
 *
 * @author Vaadin Ltd
 *
 */
public class ConcealData extends NodeValue<Boolean> {

    /**
     * Creates a new feature for the given node.
     *
     * @param node
     *            the node that the map belongs to
     *
     */
    public ConcealData(StateNode node) {
        super(node);
    }

    @Override
    protected String getKey() {
        return NodeProperties.CONCEAL;
    }

    public void setConcealed(boolean concealed) {
        setValue(concealed);
    }

    public boolean isConcealed() {
        return Boolean.TRUE.equals(getValue());
    }

}
