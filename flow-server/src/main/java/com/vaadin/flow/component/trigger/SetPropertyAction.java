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
import com.vaadin.flow.component.trigger.internal.ConfigContext;
import com.vaadin.flow.dom.Element;

/**
 * Assigns a value to a JavaScript property on a target element when the bound
 * trigger fires. Pure client-side — no server round-trip.
 * <p>
 * Symmetric with {@link PropertyArgument}: the same property name space
 * (DOM/custom-element properties such as {@code value}, {@code checked},
 * {@code disabled}).
 * <p>
 * Common idioms:
 * <ul>
 * <li>Disable a button: {@code new SetPropertyAction(button, "disabled", true)}
 * <li>Clear an input: {@code new SetPropertyAction(input, "value", "")}
 * <li>Toggle a custom property:
 * {@code new SetPropertyAction(panel, "expanded", false)}
 * </ul>
 *
 * Server-side state is not updated by this action; the change lives in the
 * browser until the next sync from the client (if any).
 *
 * @param <T>
 *            the runtime type of the value to assign
 */
public class SetPropertyAction<T> extends AbstractAction {

    public static final String TYPE_ID = "flow:set-property";

    private final Element target;
    private final String propertyName;
    private final @Nullable T value;

    /**
     * Creates an action that assigns {@code value} to the given JS property on
     * {@code target} when the trigger fires.
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
        super(TYPE_ID);
        this.target = Objects.requireNonNull(target).getElement();
        this.propertyName = Objects.requireNonNull(propertyName);
        this.value = value;
    }

    /**
     * @return the target element
     */
    public Element getTarget() {
        return target;
    }

    /**
     * @return the property name being written
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * @return the value to assign, may be {@code null}
     */
    public @Nullable T getValue() {
        return value;
    }

    @Override
    public void buildClientConfig(ConfigContext context) {
        context.put("element", context.referenceElement(target));
        context.put("property", propertyName);
        context.put("value", value);
    }
}
