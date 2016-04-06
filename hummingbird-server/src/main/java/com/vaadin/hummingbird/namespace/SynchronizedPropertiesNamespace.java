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

import java.util.Set;

import com.vaadin.hummingbird.StateNode;

/**
 * Namespace for synchronized properties for an element.
 * 
 * @author Vaadin Ltd
 * @since
 */
public class SynchronizedPropertiesNamespace
        extends SerializableListNamespace<String> {

    private static class PropertiesSetView
            extends ListNamespace.SetView<String> {

        private PropertiesSetView(SynchronizedPropertiesNamespace namespace) {
            super(namespace);
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
     * Creates a new synchronized properties set namespace for the given node.
     *
     * @param node
     *            the node that the namespace belongs to
     */
    public SynchronizedPropertiesNamespace(StateNode node) {
        super(node);
    }

    /**
     * Creates a view into this namespace.
     *
     * @return a view into this namespace
     */
    public Set<String> getSynchronizedProperties() {
        return new PropertiesSetView(this);
    }

}
