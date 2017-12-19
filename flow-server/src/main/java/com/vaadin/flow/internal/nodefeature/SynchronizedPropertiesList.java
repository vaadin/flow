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

import java.util.Set;

import com.vaadin.flow.internal.StateNode;

/**
 * List of synchronized properties for an element.
 *
 * @author Vaadin Ltd
 */
public class SynchronizedPropertiesList extends SerializableNodeList<String> {

    private static class PropertiesSetView extends NodeList.SetView<String> {

        private PropertiesSetView(SynchronizedPropertiesList list) {
            super(list);
        }

        @Override
        protected void validate(String item) {
            if (item == null) {
                throw new IllegalArgumentException(
                        "Property name cannot be null");
            }
        }

    }

    /**
     * Creates a new synchronized properties list for the given node.
     *
     * @param node
     *            the node that the list belongs to
     */
    public SynchronizedPropertiesList(StateNode node) {
        super(node);
    }

    /**
     * Creates a view into this list.
     *
     * @return a view into this list
     */
    public Set<String> getSynchronizedProperties() {
        return new PropertiesSetView(this);
    }

}
