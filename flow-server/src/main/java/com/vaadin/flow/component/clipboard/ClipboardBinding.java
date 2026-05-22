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

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.ClipboardCopyAction;
import com.vaadin.flow.component.trigger.internal.LiteralInput;
import com.vaadin.flow.component.trigger.internal.PropertyInput;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * Fluent surface returned from {@link Clipboard#on}. Each {@code copy*From}
 * verb attaches one {@link ClipboardCopyAction} to the underlying
 * {@link Trigger} and returns a {@link ClipboardWrite} for removal.
 * <p>
 * Verbs come in two flavours: fire-and-forget (one argument) and observed (with
 * {@code onSuccess}/{@code onError} callbacks). Both consumers are required in
 * the observed form — pass {@code () -> {}} or {@code err -> {}} to opt out of
 * one.
 *
 * <pre>{@code
 * Button copy = new Button("Copy");
 * Clipboard.on(copy).copyTextFromValue(textField);
 *
 * Clipboard.on(copy).copyTextFromValue(textField,
 *         () -> Notification.show("Copied"),
 *         err -> Notification.show("Failed: " + err));
 * }</pre>
 */
public final class ClipboardBinding {

    private final Trigger trigger;

    ClipboardBinding(Trigger trigger) {
        this.trigger = Objects.requireNonNull(trigger);
    }

    // --- text -----------------------------------------------------------

    /**
     * Copies a literal string to the clipboard as {@code text/plain} when the
     * underlying trigger fires.
     *
     * @param literal
     *            the value to copy, not {@code null}
     * @return a registration that removes the trigger when removed
     */
    public ClipboardWrite copyTextFrom(String literal) {
        Objects.requireNonNull(literal, "literal must not be null");
        return copyTextFrom(new LiteralInput<>(literal));
    }

    /**
     * Like {@link #copyTextFrom(String)} but reports the outcome of the
     * {@code writeText} promise back to the server.
     *
     * @param literal
     *            the value, not {@code null}
     * @param onSuccess
     *            UI-thread callback on success, not {@code null}
     * @param onError
     *            UI-thread callback on failure with the browser's error
     *            message, not {@code null}
     * @return a registration that removes the trigger when removed
     */
    public ClipboardWrite copyTextFrom(String literal,
            SerializableRunnable onSuccess,
            SerializableConsumer<String> onError) {
        Objects.requireNonNull(literal, "literal must not be null");
        return copyTextFrom(new LiteralInput<>(literal), onSuccess, onError);
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
     * @return a registration that removes the trigger when removed
     */
    public <C extends Component & HasValue<?, String>> ClipboardWrite copyTextFromValue(
            C source) {
        Objects.requireNonNull(source, "source must not be null");
        return copyTextFrom(new PropertyInput<>(source, "value", String.class));
    }

    /**
     * Like {@link #copyTextFromValue(Component)} but reports the outcome back
     * to the server.
     */
    public <C extends Component & HasValue<?, String>> ClipboardWrite copyTextFromValue(
            C source, SerializableRunnable onSuccess,
            SerializableConsumer<String> onError) {
        Objects.requireNonNull(source, "source must not be null");
        return copyTextFrom(new PropertyInput<>(source, "value", String.class),
                onSuccess, onError);
    }

    /**
     * Copies the value produced by a custom {@link Action.Input} to the
     * clipboard as {@code text/plain} when the underlying trigger fires.
     *
     * @param source
     *            the input, not {@code null}
     * @return a registration that removes the trigger when removed
     */
    public ClipboardWrite copyTextFrom(Action.Input<String> source) {
        Objects.requireNonNull(source, "source must not be null");
        return bind(new ClipboardCopyAction(source, null));
    }

    /**
     * Like {@link #copyTextFrom(Action.Input)} but reports the outcome back to
     * the server.
     */
    public ClipboardWrite copyTextFrom(Action.Input<String> source,
            SerializableRunnable onSuccess,
            SerializableConsumer<String> onError) {
        Objects.requireNonNull(source, "source must not be null");
        return bind(new ClipboardCopyAction(source, null, onSuccess, onError));
    }

    // --- html -----------------------------------------------------------

    /**
     * Copies a literal HTML string to the clipboard as {@code text/html}.
     */
    public ClipboardWrite copyHtmlFrom(String literal) {
        Objects.requireNonNull(literal, "literal must not be null");
        return copyHtmlFrom(new LiteralInput<>(literal));
    }

    /**
     * Like {@link #copyHtmlFrom(String)} but reports the outcome back to the
     * server.
     */
    public ClipboardWrite copyHtmlFrom(String literal,
            SerializableRunnable onSuccess,
            SerializableConsumer<String> onError) {
        Objects.requireNonNull(literal, "literal must not be null");
        return copyHtmlFrom(new LiteralInput<>(literal), onSuccess, onError);
    }

    /**
     * Copies the value produced by a custom {@link Action.Input} to the
     * clipboard as {@code text/html} when the underlying trigger fires.
     */
    public ClipboardWrite copyHtmlFrom(Action.Input<String> source) {
        Objects.requireNonNull(source, "source must not be null");
        return bind(new ClipboardCopyAction(null, source));
    }

    /**
     * Like {@link #copyHtmlFrom(Action.Input)} but reports the outcome back to
     * the server.
     */
    public ClipboardWrite copyHtmlFrom(Action.Input<String> source,
            SerializableRunnable onSuccess,
            SerializableConsumer<String> onError) {
        Objects.requireNonNull(source, "source must not be null");
        return bind(new ClipboardCopyAction(null, source, onSuccess, onError));
    }

    // --- multi-format ---------------------------------------------------

    /**
     * Copies a multi-format payload to the clipboard, packed into a single
     * {@code ClipboardItem}.
     *
     * @param content
     *            the content, not {@code null}; must have at least one slot set
     * @return a registration that removes the trigger when removed
     * @throws IllegalArgumentException
     *             if {@code content} has no slots set
     */
    public ClipboardWrite copyFrom(ClipboardContent content) {
        Objects.requireNonNull(content, "content must not be null");
        return bind(new ClipboardCopyAction(content.getTextInput(),
                content.getHtmlInput()));
    }

    /**
     * Like {@link #copyFrom(ClipboardContent)} but reports the outcome back to
     * the server.
     */
    public ClipboardWrite copyFrom(ClipboardContent content,
            SerializableRunnable onSuccess,
            SerializableConsumer<String> onError) {
        Objects.requireNonNull(content, "content must not be null");
        return bind(new ClipboardCopyAction(content.getTextInput(),
                content.getHtmlInput(), onSuccess, onError));
    }

    // --- internal -------------------------------------------------------

    private ClipboardWrite bind(ClipboardCopyAction action) {
        trigger.triggers(action);
        return trigger::remove;
    }
}
