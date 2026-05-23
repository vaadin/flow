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

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * Writes a {@code ClipboardItem} to the user's clipboard via
 * {@code navigator.clipboard.write}. Supports two concurrent text MIME types in
 * one item: {@code text/plain} and {@code text/html}. Any combination is
 * allowed; at least one slot must be set.
 * <p>
 * The Clipboard API requires the {@code write} call to happen inside a
 * short-lived user gesture (click, key press, ...). Bind this action to a
 * trigger that fires during such a gesture.
 * <p>
 * Outcome handling is inherited from {@link PromiseAction}: use the no-arg
 * outcome constructor for fire-and-forget, or the overload taking
 * {@code onSuccess}/{@code onError} to react on the UI thread.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class ClipboardCopyAction extends PromiseAction {

    private final Action.@Nullable Input<String> textInput;
    private final Action.@Nullable Input<String> htmlInput;

    /**
     * Creates a fire-and-forget clipboard-copy action.
     *
     * @param textInput
     *            input producing the {@code text/plain} payload, or
     *            {@code null} to omit
     * @param htmlInput
     *            input producing the {@code text/html} payload, or {@code null}
     *            to omit
     * @throws IllegalArgumentException
     *             if both inputs are {@code null}
     */
    public ClipboardCopyAction(Action.@Nullable Input<String> textInput,
            Action.@Nullable Input<String> htmlInput) {
        super();
        validate(textInput, htmlInput);
        this.textInput = textInput;
        this.htmlInput = htmlInput;
    }

    /**
     * Creates a clipboard-copy action whose outcome is reported back to the
     * server. {@code navigator.clipboard.write} resolves to {@code undefined}
     * so {@code onSuccess} carries no value (a {@link SerializableRunnable});
     * {@code onError} receives the browser's rejection message.
     *
     * @param textInput
     *            input producing the {@code text/plain} payload, or
     *            {@code null} to omit
     * @param htmlInput
     *            input producing the {@code text/html} payload, or {@code null}
     *            to omit
     * @param onSuccess
     *            invoked on the UI thread after the write resolves, not
     *            {@code null}
     * @param onError
     *            invoked on the UI thread with the browser's error message
     *            after the write rejects, not {@code null}
     * @throws IllegalArgumentException
     *             if both inputs are {@code null}
     */
    public ClipboardCopyAction(Action.@Nullable Input<String> textInput,
            Action.@Nullable Input<String> htmlInput,
            SerializableRunnable onSuccess,
            SerializableConsumer<String> onError) {
        super(adaptOnSuccess(onSuccess), adaptOnError(onError));
        validate(textInput, htmlInput);
        this.textInput = textInput;
        this.htmlInput = htmlInput;
    }

    private static SerializableConsumer<Success> adaptOnSuccess(
            SerializableRunnable onSuccess) {
        Objects.requireNonNull(onSuccess, "onSuccess must not be null");
        return success -> onSuccess.run();
    }

    private static SerializableConsumer<Error> adaptOnError(
            SerializableConsumer<String> onError) {
        Objects.requireNonNull(onError, "onError must not be null");
        return err -> onError.accept(err.message());
    }

    private static void validate(Action.@Nullable Input<String> text,
            Action.@Nullable Input<String> html) {
        if (text == null && html == null) {
            throw new IllegalArgumentException(
                    "At least one of textInput, htmlInput must be non-null");
        }
    }

    @Override
    protected void appendPromiseExpression(JsBuilder builder,
            StringBuilder out) {
        out.append("navigator.clipboard.write([new ClipboardItem({");
        boolean first = true;
        if (textInput != null) {
            out.append("\"text/plain\":");
            textInput.appendExpression(builder, out);
            first = false;
        }
        if (htmlInput != null) {
            if (!first) {
                out.append(',');
            }
            out.append("\"text/html\":");
            htmlInput.appendExpression(builder, out);
        }
        out.append("})])");
    }
}
