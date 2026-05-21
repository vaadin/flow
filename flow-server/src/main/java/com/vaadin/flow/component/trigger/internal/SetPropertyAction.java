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

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;

/**
 * Assigns a value to a JavaScript property on a target element when the bound
 * trigger fires. Pure client-side — no server round-trip.
 * <p>
 * Symmetric with {@link PropertyInput}: the same property name space
 * (DOM/custom-element properties such as {@code value}, {@code checked},
 * {@code disabled}).
 * <p>
 * The value to assign can be either a literal (constant, serialised at build
 * time) or an {@link Action.Input} that produces the value on the client when
 * the trigger fires — for example, {@link ClickTrigger#screenX()
 * click.screenX()} feeds the click's screen coordinate.
 * <p>
 * Common idioms:
 * <ul>
 * <li>Disable a button: {@code new SetPropertyAction(button, "disabled", true)}
 * <li>Clear an input: {@code new SetPropertyAction(input, "value", "")}
 * <li>Mirror a click coordinate:
 * {@code new SetPropertyAction(field, "value", click.screenX())}
 * </ul>
 *
 * Server-side state is not updated by this action; the change lives in the
 * browser until the next sync from the client (if any).
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the runtime type of the value to assign
 */
public class SetPropertyAction<T> extends Action {

    private final Element target;
    private final String propertyName;
    private final Action.Input<? extends T> source;

    /**
     * Creates an action that assigns the given literal value to the given JS
     * property on {@code target} when the trigger fires.
     *
     * @param target
     *            the component whose root element to modify, not {@code null}
     * @param propertyName
     *            the JS property name, not {@code null}
     * @param value
     *            the value to assign — {@code String}, {@code Boolean},
     *            {@code Number}, or any Jackson-serialisable object; may be
     *            {@code null}
     */
    public SetPropertyAction(Component target, String propertyName,
            @Nullable T value) {
        this(target, propertyName, new LiteralInput<>(value));
    }

    /**
     * Creates an action that assigns the value produced by {@code source} to
     * the given JS property on {@code target} when the trigger fires.
     *
     * @param target
     *            the component whose root element to modify, not {@code null}
     * @param propertyName
     *            the JS property name, not {@code null}
     * @param source
     *            input that produces the value to assign, not {@code null}
     */
    public SetPropertyAction(Component target, String propertyName,
            Action.Input<? extends T> source) {
        this.target = Objects.requireNonNull(target).getElement();
        this.propertyName = Objects.requireNonNull(propertyName);
        this.source = Objects.requireNonNull(source);
    }

    @Override
    protected void appendStatement(JsBuilder builder, StringBuilder out) {
        out.append(builder.reference(target)).append("[")
                .append(JsBuilder.json(propertyName)).append("] = ");
        source.appendExpression(builder, out);
    }
}
