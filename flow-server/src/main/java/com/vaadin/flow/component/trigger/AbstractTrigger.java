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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.ConfigContext;
import com.vaadin.flow.component.trigger.internal.ServerCallbackAction;
import com.vaadin.flow.component.trigger.internal.TriggerSupport;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Base class for {@link Trigger} implementations.
 * <p>
 * Subclasses identify themselves with a namespaced type id
 * ({@code "flow:click"}, {@code "myapp:double-tap"}, …) which must match a
 * factory registered against {@code window.Vaadin.Flow.triggers} on the client
 * side. Subclasses override
 * {@link #buildClientConfig(com.vaadin.flow.component.trigger.internal.ConfigContext)}
 * when they need to ship extra configuration with the trigger.
 */
public abstract class AbstractTrigger implements Trigger {

    private final String typeId;
    private final Element host;
    private final transient TriggerSupport support;
    private final int triggerId;

    /**
     * Creates a new trigger bound to the given host element.
     *
     * @param typeId
     *            namespaced type id matching a client factory, not {@code null}
     * @param host
     *            the element the trigger fires on, not {@code null}
     */
    protected AbstractTrigger(String typeId, Element host) {
        this.typeId = Objects.requireNonNull(typeId);
        this.host = Objects.requireNonNull(host);
        this.support = TriggerSupport.on(host);
        this.triggerId = support.registerTrigger(this);
    }

    /**
     * Creates a new trigger bound to the given host component's root element.
     *
     * @param typeId
     *            namespaced type id matching a client factory, not {@code null}
     * @param host
     *            the component whose root element the trigger fires on, not
     *            {@code null}
     */
    protected AbstractTrigger(String typeId, Component host) {
        this(typeId, Objects.requireNonNull(host).getElement());
    }

    /**
     * The host element this trigger fires on.
     *
     * @return the host element, never {@code null}
     */
    public final Element getHost() {
        return host;
    }

    /**
     * The namespaced type id of this trigger.
     *
     * @return the type id, never {@code null}
     */
    public final String getTypeId() {
        return typeId;
    }

    /**
     * Internal id for this trigger within its host's {@link TriggerSupport}.
     *
     * @return the id
     */
    public final int getTriggerId() {
        return triggerId;
    }

    /**
     * Produces the JSON configuration this trigger sends to the client. Default
     * is an empty object; override to add type-specific options.
     * <p>
     * The {@code context} lets subclasses encode element/argument references by
     * id when needed. Public so the internal framework can read the config
     * without reflection; subclasses just override.
     *
     * @param context
     *            the resolver for referenced elements and arguments, not
     *            {@code null}
     * @return a Jackson {@link ObjectNode}, never {@code null}
     */
    public ObjectNode buildClientConfig(ConfigContext context) {
        return JacksonUtils.createObjectNode();
    }

    @Override
    public final Trigger triggers(Action... actions) {
        Objects.requireNonNull(actions);
        support.bind(this, actions);
        return this;
    }

    @Override
    public final Trigger triggers(SerializableRunnable serverHandler) {
        Objects.requireNonNull(serverHandler);
        return triggers(new ServerCallbackAction(serverHandler));
    }

    @Override
    public final void remove() {
        support.removeTrigger(this);
    }
}
