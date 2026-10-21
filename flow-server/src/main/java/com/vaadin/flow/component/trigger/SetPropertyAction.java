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

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.ActionInput;
import com.vaadin.flow.component.trigger.internal.LiteralInput;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

/**
 * Assigns a value to a JavaScript property on a target element when the bound
 * trigger fires. Pure client-side — no server round-trip. The property name
 * space is that of DOM and custom-element properties such as {@code value},
 * {@code checked} or {@code disabled}.
 * <p>
 * Common idioms:
 * <ul>
 * <li>Disable a button:
 * {@code new SetPropertyAction<>(button, "disabled", true)}
 * <li>Clear an input: {@code new SetPropertyAction<>(input, "value", "")}
 * </ul>
 *
 * Server-side state is not updated by this action; the change lives in the
 * browser until the next sync from the client (if any).
 *
 * @param <T>
 *            the runtime type of the value to assign
 * @since 25.4
 */
public final class SetPropertyAction<T> extends Action {

    /**
     * Singleton input that yields a JS {@code null}. Used by the
     * {@link #SetPropertyAction(Component, String, Object) null-accepting
     * convenience constructor} so {@link LiteralInput} can stay non-null.
     */
    private static final ActionInput<Object> NULL_LITERAL = new ActionInput<>() {
        @Override
        public JsFunction toJs(Trigger trigger) {
            return JsFunction.of("return null");
        }
    };

    @SuppressWarnings("unchecked")
    private static <T> ActionInput<T> nullLiteral() {
        return (ActionInput<T>) NULL_LITERAL;
    }

    private final Element target;
    private final String propertyName;
    private final ActionInput<? extends T> source;

    /**
     * Creates an action that assigns the given literal value to the given JS
     * property on {@code target} when the trigger fires. Passing {@code null}
     * clears the property (renders {@code target[prop] = null;}).
     *
     * @param target
     *            the component whose root element to modify, not {@code null}
     * @param propertyName
     *            the JS property name, not {@code null}
     * @param value
     *            the value to assign — {@code String}, {@code Boolean},
     *            {@code Number}, or any Jackson-serialisable object; may be
     *            {@code null} to emit a JS {@code null} (e.g. to clear an
     *            input's value)
     */
    public SetPropertyAction(Component target, String propertyName,
            @Nullable T value) {
        this(target, propertyName,
                value == null ? nullLiteral() : new LiteralInput<>(value));
    }

    /**
     * Creates an action that assigns the value produced by {@code source} to
     * the given JS property on {@code target} when the trigger fires.
     * <p>
     * For internal use only. May be changed or removed in any release.
     *
     * @param target
     *            the component whose root element to modify, not {@code null}
     * @param propertyName
     *            the JS property name, not {@code null}
     * @param source
     *            input that produces the value to assign, not {@code null}
     */
    public SetPropertyAction(Component target, String propertyName,
            ActionInput<? extends T> source) {
        this.target = Objects.requireNonNull(target).getElement();
        this.propertyName = Objects.requireNonNull(propertyName);
        this.source = Objects.requireNonNull(source);
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        // $0 = target element (captured), $1 = property name (string
        // capture, Jackson-quoted on the client), $2 = source JsFunction
        // (invoked with event so handler-scoped inputs work).
        return JsFunction.of("$0[$1] = $2(event)", target, propertyName,
                source.toJs(trigger)).withArguments("event");
    }
}
