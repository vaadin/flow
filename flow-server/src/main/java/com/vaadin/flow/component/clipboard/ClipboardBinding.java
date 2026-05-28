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
import com.vaadin.flow.component.trigger.internal.ImageBlobInput;
import com.vaadin.flow.component.trigger.internal.LiteralInput;
import com.vaadin.flow.component.trigger.internal.PromiseAction.Error;
import com.vaadin.flow.component.trigger.internal.PropertyInput;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.component.trigger.internal.WriteToClipboardAction;
import com.vaadin.flow.function.SerializableConsumer;

/**
 * Fluent surface returned from {@link Clipboard#onClick}. Each {@code write*}
 * action attaches one {@link WriteToClipboardAction} to the underlying
 * {@link Trigger}.
 * <p>
 * Actions come in two flavours: fire-and-forget (one argument) and observed
 * (with {@code onCopied}/{@code onError} callbacks). {@code onCopied} receives
 * the string that was copied; {@code onError} receives the browser's error.
 * Both consumers are required in the observed form — pass {@code s -> {}} or
 * {@code err -> {}} to opt out of one.
 *
 * <pre>{@code
 * Button copy = new Button("Copy");
 * Clipboard.onClick(copy).writeText(textField);
 *
 * Clipboard.onClick(copy).writeText(textField,
 *         copied -> Notification.show("Copied " + copied),
 *         err -> Notification.show("Failed: " + err.message()));
 * }</pre>
 */
public final class ClipboardBinding implements Serializable {

    private final Trigger trigger;

    ClipboardBinding(Trigger trigger) {
        this.trigger = Objects.requireNonNull(trigger);
    }

    /**
     * Copies a literal string to the clipboard as {@code text/plain} when the
     * underlying trigger fires.
     *
     * @param literal
     *            the value to copy, not {@code null}
     */
    public void writeText(String literal) {
        Objects.requireNonNull(literal, "literal must not be null");
        bind(new WriteToClipboardAction(new LiteralInput<>(literal), null,
                null));
    }

    /**
     * Like {@link #writeText(String)} but reports the outcome of the write
     * promise back to the server.
     *
     * @param literal
     *            the value, not {@code null}
     * @param onCopied
     *            UI-thread callback receiving the copied string, not
     *            {@code null}
     * @param onError
     *            UI-thread callback receiving the browser's error, not
     *            {@code null}
     */
    public void writeText(String literal,
            SerializableConsumer<@Nullable String> onCopied,
            SerializableConsumer<Error> onError) {
        Objects.requireNonNull(literal, "literal must not be null");
        bind(new WriteToClipboardAction(new LiteralInput<>(literal), null, null,
                onCopied, onError));
    }

    /**
     * Copies the current {@code value} property of the given component
     * (typically an input field) to the clipboard as {@code text/plain} when
     * the underlying trigger fires. The value is read on the client at the
     * moment the trigger fires, so subsequent edits are reflected without any
     * server round-trip.
     *
     * @param source
     *            the component whose {@code value} should be copied, not
     *            {@code null}
     * @param <C>
     *            component type implementing {@code HasValue<?, String>}
     */
    public <C extends Component & HasValue<?, String>> void writeText(
            C source) {
        Objects.requireNonNull(source, "source must not be null");
        bind(new WriteToClipboardAction(
                new PropertyInput<>(source, "value", String.class), null,
                null));
    }

    /**
     * Like {@link #writeText(Component)} but reports the outcome back to the
     * server.
     *
     * @param source
     *            the component whose {@code value} should be copied, not
     *            {@code null}
     * @param onCopied
     *            UI-thread callback receiving the copied string, not
     *            {@code null}
     * @param onError
     *            UI-thread callback receiving the browser's error, not
     *            {@code null}
     * @param <C>
     *            component type implementing {@code HasValue<?, String>}
     */
    public <C extends Component & HasValue<?, String>> void writeText(C source,
            SerializableConsumer<@Nullable String> onCopied,
            SerializableConsumer<Error> onError) {
        Objects.requireNonNull(source, "source must not be null");
        bind(new WriteToClipboardAction(
                new PropertyInput<>(source, "value", String.class), null, null,
                onCopied, onError));
    }

    /**
     * Copies a literal HTML string to the clipboard as {@code text/html} when
     * the underlying trigger fires.
     *
     * @param literal
     *            the HTML, not {@code null}
     */
    public void writeHtml(String literal) {
        Objects.requireNonNull(literal, "literal must not be null");
        bind(new WriteToClipboardAction(null, new LiteralInput<>(literal),
                null));
    }

    /**
     * Like {@link #writeHtml(String)} but reports the outcome back to the
     * server.
     *
     * @param literal
     *            the HTML, not {@code null}
     * @param onCopied
     *            UI-thread callback receiving the copied HTML, not {@code null}
     * @param onError
     *            UI-thread callback receiving the browser's error, not
     *            {@code null}
     */
    public void writeHtml(String literal,
            SerializableConsumer<@Nullable String> onCopied,
            SerializableConsumer<Error> onError) {
        Objects.requireNonNull(literal, "literal must not be null");
        bind(new WriteToClipboardAction(null, new LiteralInput<>(literal), null,
                onCopied, onError));
    }

    /**
     * Copies the given component's root {@code <img>} element to the clipboard
     * as {@code image/png} when the underlying trigger fires. The image is
     * drawn on a canvas and exported as PNG on the client, so the source can be
     * any rasterisable format ({@code image/png}, {@code image/jpeg},
     * {@code image/svg+xml}, ...) as long as it has intrinsic dimensions.
     * <p>
     * Cross-origin images need {@code crossorigin="anonymous"} on the
     * {@code <img>} plus matching CORS headers; otherwise the canvas is tainted
     * and the write fails. Same-origin and {@code data:} URLs always work.
     *
     * @param source
     *            the component whose root {@code <img>} should be copied, not
     *            {@code null}
     */
    public void writeImage(Component source) {
        Objects.requireNonNull(source, "source must not be null");
        bind(new WriteToClipboardAction(null, null,
                new ImageBlobInput(source)));
    }

    /**
     * Like {@link #writeImage(Component)} but reports the outcome back to the
     * server. {@code onCopied} receives {@code null} — the image-only write has
     * no meaningful string value.
     *
     * @param source
     *            the component whose root {@code <img>} should be copied, not
     *            {@code null}
     * @param onCopied
     *            UI-thread callback receiving {@code null}, not {@code null}
     * @param onError
     *            UI-thread callback receiving the browser's error, not
     *            {@code null}
     */
    public void writeImage(Component source,
            SerializableConsumer<@Nullable String> onCopied,
            SerializableConsumer<Error> onError) {
        Objects.requireNonNull(source, "source must not be null");
        bind(new WriteToClipboardAction(null, null, new ImageBlobInput(source),
                onCopied, onError));
    }

    /**
     * Copies a multi-format payload to the clipboard, packed into a single
     * {@code ClipboardItem}.
     *
     * @param content
     *            the content, not {@code null}; must have at least one slot set
     * @throws IllegalArgumentException
     *             if {@code content} has no slots set
     */
    public void write(ClipboardContent content) {
        Objects.requireNonNull(content, "content must not be null");
        bind(new WriteToClipboardAction(content.getTextInput(),
                content.getHtmlInput(), content.getImageInput()));
    }

    /**
     * Like {@link #write(ClipboardContent)} but reports the outcome back to the
     * server.
     *
     * @param content
     *            the content, not {@code null}; must have at least one slot set
     * @param onCopied
     *            UI-thread callback receiving the {@code text/plain} value if
     *            present, otherwise the {@code text/html} value, not
     *            {@code null}
     * @param onError
     *            UI-thread callback receiving the browser's error, not
     *            {@code null}
     */
    public void write(ClipboardContent content,
            SerializableConsumer<@Nullable String> onCopied,
            SerializableConsumer<Error> onError) {
        Objects.requireNonNull(content, "content must not be null");
        bind(new WriteToClipboardAction(content.getTextInput(),
                content.getHtmlInput(), content.getImageInput(), onCopied,
                onError));
    }

    private void bind(WriteToClipboardAction action) {
        trigger.triggers(action);
    }
}
