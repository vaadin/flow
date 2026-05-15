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
 * Trigger backed by an arbitrary JavaScript expression — the escape hatch for
 * cases not covered by a built-in {@link AbstractTrigger}.
 * <p>
 * The expression is evaluated once during {@code bind} with the host element as
 * {@code this} and a single named parameter {@code trigger} — a function the
 * expression must call (synchronously, inside a DOM event handler) to fire the
 * trigger. The expression may return a cleanup function; if it does, the
 * cleanup runs when the trigger is removed or when the host is re-bound.
 *
 * <pre>{@code
 * new JsTrigger(host, "this.addEventListener('dblclick', trigger);"
 *         + "return () => this.removeEventListener('dblclick', trigger);")
 *         .triggers(action);
 * }</pre>
 */
public class JsTrigger extends AbstractTrigger {

    public static final String TYPE_ID = "flow:js";

    private final String expression;

    /**
     * Creates a JS-backed trigger on the given host element.
     *
     * @param host
     *            the host element, not {@code null}
     * @param expression
     *            the JS source, not {@code null}
     */
    public JsTrigger(Element host, String expression) {
        super(TYPE_ID, host);
        this.expression = Objects.requireNonNull(expression);
    }

    /**
     * Creates a JS-backed trigger on the given component's root element.
     *
     * @param host
     *            the host component, not {@code null}
     * @param expression
     *            the JS source, not {@code null}
     */
    public JsTrigger(Component host, String expression) {
        this(Objects.requireNonNull(host).getElement(), expression);
    }

    /**
     * @return the JS expression
     */
    public String getExpression() {
        return expression;
    }

    @Override
    public ObjectNode buildClientConfig(ConfigContext context) {
        ObjectNode node = JacksonUtils.createObjectNode();
        node.put("expression", expression);
        return node;
    }
}
