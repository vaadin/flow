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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.internal.StateNode;

/**
 * List of synchronized properties for an element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class SynchronizedPropertiesList extends SerializableNodeList<String> {

    /**
     * If property is in the list but is not in this map then its mode is
     * {@literal DisabledUpdateMode.ONLY_WHEN_ENABLED} by default (to avoid
     * wasting memory).
     */
    private Map<String, DisabledUpdateMode> disabledRpcModes;

    private static class PropertiesSetView extends NodeList.SetView<String> {

        private SynchronizedPropertiesList origin;

        private PropertiesSetView(SynchronizedPropertiesList list) {
            super(list);
            origin = list;
        }

        @Override
        protected void validate(String item) {
            if (item == null) {
                throw new IllegalArgumentException(
                        "Property name cannot be null");
            }
        }

        @Override
        public boolean remove(Object item) {
            if (origin.disabledRpcModes != null) {
                origin.disabledRpcModes.remove(item);
            }
            return super.remove(item);
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

    /**
     * Add the {@code property} to the synchronized properties list.
     *
     * @param property
     *            the property to synchronize
     * @param mode
     *            controls RPC from the client side to the server side when the
     *            element is disabled, not {@code null}
     */
    public void add(String property, DisabledUpdateMode mode) {
        Objects.requireNonNull(mode,
                "The argument which controls RPC for disabled element may not be null");
        getSynchronizedProperties().add(property);
        if (!DisabledUpdateMode.ONLY_WHEN_ENABLED.equals(mode)) {
            if (disabledRpcModes == null) {
                disabledRpcModes = new HashMap<>();
            }
            disabledRpcModes.put(property, mode);
        }
    }

    /**
     * Gets property update mode for disabled element.
     *
     * @param property
     *            the property to get update mode
     * @return the property update mode for disabled element
     */
    public DisabledUpdateMode getDisabledUpdateMode(String property) {
        DisabledUpdateMode mode = disabledRpcModes == null ? null
                : disabledRpcModes.get(property);
        if (mode == null) {
            return DisabledUpdateMode.ONLY_WHEN_ENABLED;
        }
        return mode;
    }

}
