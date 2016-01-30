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

package com.vaadin.hummingbird.change;

import com.vaadin.hummingbird.namespace.Namespace;
import com.vaadin.hummingbird.namespace.NamespaceRegistry;
import com.vaadin.shared.JsonConstants;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Base class for all node changes related to a namespace.
 *
 * @since
 * @author Vaadin Ltd
 */
public abstract class NamespaceChange extends NodeChange {

    private final Class<? extends Namespace> namespace;

    /**
     * Creates a new change for the given namespace.
     *
     * @param namespace
     *            the namespace affected by the change
     */
    public NamespaceChange(Namespace namespace) {
        super(namespace.getNode());

        this.namespace = namespace.getClass();
    }

    /**
     * Gets the namespace affected by the change.
     *
     * @return the namespace
     */
    public Class<? extends Namespace> getNamespace() {
        return namespace;
    }

    @Override
    protected void populateJson(JsonObject json) {
        json.put(JsonConstants.CHANGE_NAMESPACE,
                Json.create(NamespaceRegistry.getId(namespace)));
    }
}
