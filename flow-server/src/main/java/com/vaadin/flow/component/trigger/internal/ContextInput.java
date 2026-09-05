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
package com.vaadin.flow.component.trigger.internal;

import java.util.Objects;

import com.vaadin.flow.dom.JsFunction;

/**
 * Reads a value out of the trigger context — the object a trigger supplies to
 * describe what it fired for. A {@link SinkTrigger} rendered into a row
 * renderer supplies {@code {item, index, key}} for the row the event came from,
 * so {@code new ContextInput<>("item", "email", String.class)} yields that
 * row's {@code email} property at fire time.
 * <p>
 * This is what lets one action serve a whole column: the value is not captured
 * on the server per row, it is read from the row's own client-side data when
 * the action runs.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the runtime type of the value produced
 */
public class ContextInput<T> extends Action.Input<T> {

    private final String contextProperty;
    private final String valueProperty;

    /**
     * Creates an input reading {@code context[contextProperty][valueProperty]}
     * at fire time.
     *
     * @param contextProperty
     *            the context member to read from (e.g. {@code "item"}), not
     *            {@code null}
     * @param valueProperty
     *            the property to read off that member, not {@code null}
     * @param valueType
     *            runtime type of the produced value, not {@code null}
     */
    public ContextInput(String contextProperty, String valueProperty,
            Class<T> valueType) {
        this.contextProperty = Objects.requireNonNull(contextProperty);
        this.valueProperty = Objects.requireNonNull(valueProperty);
        Objects.requireNonNull(valueType);
    }

    @Override
    public JsFunction toJs(Trigger trigger) {
        if (!trigger.suppliesContext()) {
            throw new IllegalArgumentException("Input reads the trigger "
                    + "context, but a " + trigger.getClass().getSimpleName()
                    + " supplies none. Use a value source that does not depend "
                    + "on the context, or bind the action to something that "
                    + "renders repeating elements.");
        }
        // Both names are captures, so nothing is concatenated into the body.
        return JsFunction
                .of("return context[$0][$1]", contextProperty, valueProperty)
                .withArguments("event", "context");
    }
}
