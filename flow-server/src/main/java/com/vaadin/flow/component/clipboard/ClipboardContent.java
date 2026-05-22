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
package com.vaadin.flow.component.clipboard;

import java.io.Serializable;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.LiteralInput;
import com.vaadin.flow.component.trigger.internal.PropertyInput;

/**
 * Multi-format payload for {@link ClipboardBinding#copyFrom}. Any combination
 * of {@code text/plain} and {@code text/html} can be set; each accessor returns
 * {@code null} when the corresponding slot is empty.
 * <p>
 * Use the static factory:
 *
 * <pre>{@code
 * Clipboard.on(button).copyFrom(
 *         ClipboardContent.create().text("Hello").html("<b>Hello</b>"));
 * }</pre>
 *
 * Each setter has overloads accepting a literal value, a component (for value
 * fields), or a custom {@link Action.Input} for advanced cases.
 */
public final class ClipboardContent implements Serializable {

    private Action.@Nullable Input<String> textInput;
    private Action.@Nullable Input<String> htmlInput;

    private ClipboardContent() {
    }

    /**
     * Creates a new empty content builder.
     *
     * @return a new builder
     */
    public static ClipboardContent create() {
        return new ClipboardContent();
    }

    /**
     * Sets the {@code text/plain} payload to a literal value.
     *
     * @param literal
     *            the value, not {@code null}
     * @return this builder
     */
    public ClipboardContent text(String literal) {
        Objects.requireNonNull(literal, "literal must not be null");
        this.textInput = new LiteralInput<>(literal);
        return this;
    }

    /**
     * Sets the {@code text/plain} payload to the current {@code value} property
     * of the given component (typically an input field), read on the client
     * when the trigger fires.
     *
     * @param source
     *            the component whose {@code value} property should be read, not
     *            {@code null}
     * @param <C>
     *            component type implementing {@code HasValue<?, String>}
     * @return this builder
     */
    public <C extends Component & HasValue<?, String>> ClipboardContent textFromValue(
            C source) {
        Objects.requireNonNull(source, "source must not be null");
        this.textInput = new PropertyInput<>(source, "value", String.class);
        return this;
    }

    /**
     * Sets the {@code text/plain} payload to the value produced by a custom
     * input.
     *
     * @param source
     *            the input, not {@code null}
     * @return this builder
     */
    public ClipboardContent text(Action.Input<String> source) {
        Objects.requireNonNull(source, "source must not be null");
        this.textInput = source;
        return this;
    }

    /**
     * Sets the {@code text/html} payload to a literal value.
     *
     * @param literal
     *            the value, not {@code null}
     * @return this builder
     */
    public ClipboardContent html(String literal) {
        Objects.requireNonNull(literal, "literal must not be null");
        this.htmlInput = new LiteralInput<>(literal);
        return this;
    }

    /**
     * Sets the {@code text/html} payload to the value produced by a custom
     * input.
     *
     * @param source
     *            the input, not {@code null}
     * @return this builder
     */
    public ClipboardContent html(Action.Input<String> source) {
        Objects.requireNonNull(source, "source must not be null");
        this.htmlInput = source;
        return this;
    }

    /**
     * @return the text input, or {@code null} if no text was set
     */
    public Action.@Nullable Input<String> getTextInput() {
        return textInput;
    }

    /**
     * @return the html input, or {@code null} if no html was set
     */
    public Action.@Nullable Input<String> getHtmlInput() {
        return htmlInput;
    }
}
