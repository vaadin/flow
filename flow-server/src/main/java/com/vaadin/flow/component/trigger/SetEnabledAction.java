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
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Sets a target component's enabled state. Runs client-side by toggling the
 * {@code disabled} attribute so the change is visible the instant the
 * triggering DOM event handler returns — closing the latency window in which a
 * user could otherwise click the component a second time before the server
 * acknowledges the first click.
 * <p>
 * The server-side {@code Component.setEnabled(boolean)} mirror is applied in
 * the next server cycle so application code observes the same enabled state.
 */
public class SetEnabledAction extends AbstractAction {

    public static final String TYPE_ID = "flow:set-enabled";

    private final Element target;
    private final boolean enabled;

    /**
     * Creates a set-enabled action.
     *
     * @param target
     *            the component to enable or disable, not {@code null}
     * @param enabled
     *            {@code true} to enable, {@code false} to disable
     */
    public SetEnabledAction(Component target, boolean enabled) {
        super(TYPE_ID);
        this.target = Objects.requireNonNull(target).getElement();
        this.enabled = enabled;
    }

    /**
     * @return the target element
     */
    public Element getTarget() {
        return target;
    }

    /**
     * @return the value the action sets
     */
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public ObjectNode buildClientConfig(ConfigContext context) {
        ObjectNode node = JacksonUtils.createObjectNode();
        node.put("element", context.referenceElement(target));
        node.put("enabled", enabled);
        // Signal to the client that it needs to notify the server after
        // applying the local change, so the framework keeps server state
        // in sync.
        node.put("mirror", true);
        return node;
    }

    @Override
    public void applyServerSideEffect() {
        target.setEnabled(enabled);
    }
}
