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

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.ContextInput;
import com.vaadin.flow.component.trigger.internal.LiteralInput;

/**
 * A value a {@link ClientAction} reads on the client when it runs, rather than
 * one captured on the server when it is bound.
 * <p>
 * This is what makes a single binding usable for many rendered elements: the
 * copy button in every grid row is one action, and
 * {@link #itemProperty(String)} resolves the row it was clicked in.
 *
 * <pre>{@code
 * Clipboard.write().text(ClientValue.itemProperty("email"));
 * }</pre>
 *
 * @param <T>
 *            the type of the value produced
 */
public final class ClientValue<T> implements Serializable {

    private final Action.Input<T> input;

    private ClientValue(Action.Input<T> input) {
        this.input = input;
    }

    /**
     * A property of the item the action fired for, read from the renderer's
     * client-side item data. The property must be one the renderer sends to the
     * client — for a {@code LitRenderer}, one declared with
     * {@code withProperty}.
     *
     * @param propertyName
     *            the item property to read, not {@code null}
     * @return a value resolving to that property of the item the action fired
     *         for
     */
    public static ClientValue<String> itemProperty(String propertyName) {
        Objects.requireNonNull(propertyName, "propertyName must not be null");
        return new ClientValue<>(
                new ContextInput<>("item", propertyName, String.class));
    }

    /**
     * A fixed value, the same for every element the action is rendered into.
     *
     * @param value
     *            the value, not {@code null}
     * @return a value resolving to {@code value}
     */
    public static ClientValue<String> of(String value) {
        Objects.requireNonNull(value, "value must not be null");
        return new ClientValue<>(new LiteralInput<>(value));
    }

    /**
     * The input backing this value.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return the input, never {@code null}
     */
    public Action.Input<T> getInput() {
        return input;
    }
}
