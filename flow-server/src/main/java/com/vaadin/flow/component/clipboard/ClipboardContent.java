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
import com.vaadin.flow.component.trigger.internal.ImageBlobInput;
import com.vaadin.flow.component.trigger.internal.LiteralInput;
import com.vaadin.flow.component.trigger.internal.PropertyInput;

/**
 * Multi-format payload for {@link ClipboardBinding#write}. Any combination of
 * {@code text/plain}, {@code text/html} and an image source can be set; each
 * accessor returns {@code null} when the corresponding slot is empty.
 * <p>
 * Use the static factory:
 *
 * <pre>{@code
 * Clipboard.onClick(button).write(ClipboardContent.create().text("Hello")
 *         .html("<b>Hello</b>").image(previewImage));
 * }</pre>
 *
 * @since 25.2
 */
public final class ClipboardContent implements Serializable {

    private Action.@Nullable Input<String> textInput;
    private Action.@Nullable Input<String> htmlInput;
    private Action.@Nullable Input<?> imageInput;

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
     * Sets the plain text to be written to the clipboard.
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
     * Sets the plain text to be written to the clipboard, taken from the
     * {@code value} property of the given component (typically an input field).
     * The value is read on the client when the trigger fires.
     *
     * @param source
     *            the component whose {@code value} property should be read, not
     *            {@code null}
     * @param <C>
     *            component type implementing {@code HasValue<?, String>}
     * @return this builder
     */
    public <C extends Component & HasValue<?, String>> ClipboardContent text(
            C source) {
        Objects.requireNonNull(source, "source must not be null");
        this.textInput = new PropertyInput<>(source, "value", String.class);
        return this;
    }

    /**
     * Sets the HTML to be written to the clipboard.
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
     * Sets the {@code image/png} payload to a PNG re-encoding of the given
     * component's root {@code <img>} element, produced on the client when the
     * trigger fires. The source can be any rasterisable image
     * ({@code image/png}, {@code image/jpeg}, {@code image/svg+xml}, ...) with
     * intrinsic dimensions; cross-origin sources need
     * {@code crossorigin="anonymous"} on the {@code <img>} plus matching CORS
     * headers, otherwise the canvas is tainted and the write fails.
     *
     * @param source
     *            the component whose root {@code <img>} should be copied, not
     *            {@code null}
     * @return this builder
     * @throws IllegalArgumentException
     *             if the source's root element is not an {@code <img>}
     */
    public ClipboardContent image(Component source) {
        Objects.requireNonNull(source, "source must not be null");
        this.imageInput = new ImageBlobInput(source);
        return this;
    }

    Action.@Nullable Input<String> getTextInput() {
        return textInput;
    }

    Action.@Nullable Input<String> getHtmlInput() {
        return htmlInput;
    }

    Action.@Nullable Input<?> getImageInput() {
        return imageInput;
    }
}
