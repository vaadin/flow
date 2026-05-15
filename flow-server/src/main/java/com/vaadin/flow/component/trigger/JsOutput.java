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
 * Output backed by an arbitrary JavaScript expression — the escape hatch for
 * cases not covered by a built-in {@link AbstractOutput}.
 * <p>
 * The expression runs at the moment the trigger fires and its return value
 * becomes the output. The expression executes in the global scope; use
 * {@code document.querySelector(...)} or other DOM globals to reach elements.
 *
 * <pre>{@code
 * Output<String> hostName = new JsOutput<>(String.class,
 *         "return window.location.hostname;");
 * }</pre>
 *
 * @param <T>
 *            the runtime type of the produced value
 */
public class JsOutput<T> extends AbstractOutput<T> {

    public static final String TYPE_ID = "flow:js";

    private final String expression;

    /**
     * Creates a JS-backed output.
     *
     * @param valueType
     *            the runtime type, not {@code null}
     * @param expression
     *            the JS source, not {@code null}
     */
    public JsOutput(Class<T> valueType, String expression) {
        super(TYPE_ID, valueType);
        this.expression = Objects.requireNonNull(expression);
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
