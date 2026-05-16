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
 * Base class for {@link Action} implementations.
 * <p>
 * Subclasses identify themselves with a namespaced type id
 * ({@code "flow:clipboard-copy"}, {@code "myapp:show-toast"}, …) which must
 * match a factory registered against {@code window.Vaadin.Flow.triggers} on the
 * client side. Subclasses override {@link #buildClientConfig} to ship
 * configuration with the action; if the action has a server-observable effect
 * that should stay in sync with what just ran in the browser, they also
 * override {@link #applyServerSideEffect()}.
 */
public abstract class AbstractAction implements Action {

    private final String typeId;

    /**
     * Creates a new action with the given namespaced type id.
     *
     * @param typeId
     *            type id matching a client factory, not {@code null}
     */
    protected AbstractAction(String typeId) {
        this.typeId = Objects.requireNonNull(typeId);
    }

    /**
     * The namespaced type id of this action.
     *
     * @return the type id, never {@code null}
     */
    public final String getTypeId() {
        return typeId;
    }

    /**
     * Produces the JSON configuration this action sends to the client. Default
     * is an empty object; override to add type-specific options.
     * <p>
     * Subclasses encode argument references by calling
     * {@link ConfigContext#registerArgument(Argument)} and element references
     * by calling
     * {@link ConfigContext#referenceElement(com.vaadin.flow.dom.Element)}.
     * Public so the internal framework can read the config without reflection;
     * subclasses just override.
     *
     * @param context
     *            the resolver for referenced elements and arguments, not
     *            {@code null}
     * @return a Jackson {@link ObjectNode}, never {@code null}
     */
    public ObjectNode buildClientConfig(ConfigContext context) {
        return JacksonUtils.createObjectNode();
    }

    /**
     * Mirrors the client-side effect on the server. Called on the UI thread, at
     * the start of the same server cycle that processes the triggering DOM
     * event, before any user-attached event listeners run, so that listener
     * code observes the post-action state.
     * <p>
     * Default is a no-op. Subclasses with a server-observable effect (e.g.
     * {@code SetEnabledAction}) override this.
     */
    public void applyServerSideEffect() {
        // No server-side mirror by default.
    }
}
