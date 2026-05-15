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

import java.util.List;
import java.util.Objects;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.trigger.internal.ConfigContext;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Action backed by an arbitrary JavaScript expression — the escape hatch for
 * cases not covered by a built-in {@link AbstractAction}.
 * <p>
 * The expression runs every time the trigger fires. A single helper is in
 * scope: {@code output(i)} returns the resolved value of the i-th declared
 * output (in the order passed to this constructor).
 *
 * <pre>{@code
 * Output<String> who = new PropertyOutput<>(field, "value", String.class);
 * new JsAction("alert('Hello ' + output(0));", who);
 * }</pre>
 */
public class JsAction extends AbstractAction {

    public static final String TYPE_ID = "flow:js";

    private final String expression;
    private final List<Output<?>> outputs;

    /**
     * Creates a JS-backed action.
     *
     * @param expression
     *            the JS source, not {@code null}
     * @param outputs
     *            outputs available to the expression via {@code output(i)}, in
     *            the order passed
     */
    public JsAction(String expression, Output<?>... outputs) {
        super(TYPE_ID);
        this.expression = Objects.requireNonNull(expression);
        this.outputs = List.of(outputs);
    }

    /**
     * @return the JS expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * @return the declared outputs in order
     */
    public List<Output<?>> getOutputs() {
        return outputs;
    }

    @Override
    public ObjectNode buildClientConfig(ConfigContext context) {
        ObjectNode node = JacksonUtils.createObjectNode();
        node.put("expression", expression);
        ArrayNode ids = JacksonUtils.createArrayNode();
        for (Output<?> output : outputs) {
            ids.add(context.registerOutput(output));
        }
        node.set("outputs", ids);
        return node;
    }
}
