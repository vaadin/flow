/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.component.trigger;

import java.util.Objects;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.trigger.internal.ConfigContext;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Base class for {@link Argument} implementations.
 * <p>
 * Subclasses identify themselves with a namespaced type id
 * ({@code "flow:property"}, {@code "myapp:caret-offset"}, …) which must match a
 * factory registered against {@code window.Vaadin.Flow.triggers} on the client
 * side. Subclasses override {@link #buildClientConfig} when they need to ship
 * configuration with the argument.
 *
 * @param <T>
 *            the runtime type of the value produced
 */
public abstract class AbstractArgument<T> implements Argument<T> {

    private final String typeId;
    private final Class<T> valueType;

    /**
     * Creates a new argument.
     *
     * @param typeId
     *            namespaced type id matching a client factory, not {@code null}
     * @param valueType
     *            runtime type of the produced value, not {@code null}
     */
    protected AbstractArgument(String typeId, Class<T> valueType) {
        this.typeId = Objects.requireNonNull(typeId);
        this.valueType = Objects.requireNonNull(valueType);
    }

    /**
     * The namespaced type id of this argument.
     *
     * @return the type id, never {@code null}
     */
    public final String getTypeId() {
        return typeId;
    }

    /**
     * The runtime type of the value this argument produces.
     *
     * @return the value type, never {@code null}
     */
    public final Class<T> getValueType() {
        return valueType;
    }

    /**
     * Produces the JSON configuration this argument sends to the client.
     * Default is an empty object; override to add type-specific options.
     * <p>
     * Subclasses encode element references by calling
     * {@link ConfigContext#referenceElement(com.vaadin.flow.dom.Element)}.
     * Public so the internal framework can read the config without reflection;
     * subclasses just override.
     *
     * @param context
     *            the resolver for referenced elements, not {@code null}
     * @return a Jackson {@link ObjectNode}, never {@code null}
     */
    public ObjectNode buildClientConfig(ConfigContext context) {
        return JacksonUtils.createObjectNode();
    }
}
