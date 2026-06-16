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

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.LiteralInput;
import com.vaadin.flow.component.trigger.internal.PropertyInput;
import com.vaadin.flow.function.SerializableSupplier;

/**
 * Describes what a {@link ClipboardBinding} write action copies to the
 * clipboard: any combination of a {@code text/plain} value, a {@code text/html}
 * value and an image. It replaces the per-call construction of the internal
 * write action so the content is inspectable server-side — a browserless test
 * driver can read {@link #text()} / {@link #html()} / {@link #hasImage()}
 * without a DOM.
 * <p>
 * Text and HTML slots may be a literal string or a live binding to a
 * {@link HasValue} component's value. In both cases {@link #text()} /
 * {@link #html()} reflect the <em>current</em> value at the moment they are
 * called, matching the production behavior of reading the field's value on the
 * client when the trigger fires.
 *
 * @since 25.2
 */
public final class ClipboardWrite implements Serializable {

    private final Action.@Nullable Input<String> textInput;
    private final Action.@Nullable Input<String> htmlInput;
    private final Action.@Nullable Input<?> imageInput;

    // Server-side views of the slot values, evaluated live so a test driver
    // sees the same value the client would read at fire time. Null when the
    // corresponding MIME slot is unset.
    private final @Nullable SerializableSupplier<@Nullable String> textValue;
    private final @Nullable SerializableSupplier<@Nullable String> htmlValue;
    private final boolean hasImage;

    ClipboardWrite(Action.@Nullable Input<String> textInput,
            Action.@Nullable Input<String> htmlInput,
            Action.@Nullable Input<?> imageInput,
            @Nullable SerializableSupplier<@Nullable String> textValue,
            @Nullable SerializableSupplier<@Nullable String> htmlValue,
            boolean hasImage) {
        this.textInput = textInput;
        this.htmlInput = htmlInput;
        this.imageInput = imageInput;
        this.textValue = textValue;
        this.htmlValue = htmlValue;
        this.hasImage = hasImage;
    }

    static ClipboardWrite ofText(String literal) {
        return new ClipboardWrite(new LiteralInput<>(literal), null, null,
                () -> literal, null, false);
    }

    static <C extends Component & HasValue<?, String>> ClipboardWrite ofText(
            C source) {
        return new ClipboardWrite(
                new PropertyInput<>(source, "value", String.class), null, null,
                source::getValue, null, false);
    }

    static ClipboardWrite ofHtml(String literal) {
        return new ClipboardWrite(null, new LiteralInput<>(literal), null, null,
                () -> literal, false);
    }

    static ClipboardWrite ofImage(Action.Input<?> imageInput) {
        return new ClipboardWrite(null, null, imageInput, null, null, true);
    }

    static ClipboardWrite ofContent(ClipboardContent content) {
        return new ClipboardWrite(content.getTextInput(),
                content.getHtmlInput(), content.getImageInput(),
                content.getTextValue(), content.getHtmlValue(),
                content.hasImage());
    }

    /**
     * The current {@code text/plain} value this write would copy, or
     * {@code null} if the text slot is unset. For a value bound to a
     * {@link HasValue} source the result reflects the source's current value on
     * each call.
     *
     * @return the text value, or {@code null}
     */
    public @Nullable String text() {
        return textValue == null ? null : textValue.get();
    }

    /**
     * The current {@code text/html} value this write would copy, or
     * {@code null} if the HTML slot is unset.
     *
     * @return the HTML value, or {@code null}
     */
    public @Nullable String html() {
        return htmlValue == null ? null : htmlValue.get();
    }

    /**
     * Whether this write includes an image slot.
     *
     * @return {@code true} if an image is part of the payload
     */
    public boolean hasImage() {
        return hasImage;
    }

    Action.@Nullable Input<String> textInput() {
        return textInput;
    }

    Action.@Nullable Input<String> htmlInput() {
        return htmlInput;
    }

    Action.@Nullable Input<?> imageInput() {
        return imageInput;
    }
}
