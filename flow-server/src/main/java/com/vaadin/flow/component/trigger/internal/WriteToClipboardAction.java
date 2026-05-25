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
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.function.SerializableConsumer;

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
 * Outcome handling extends {@link PromiseAction}: use the no-arg outcome
 * constructor for fire-and-forget, or the overload taking
 * {@code onCopied}/{@code onError} consumers. {@code onCopied} receives the
 * exact string that was copied — the {@code text/plain} value if present,
 * otherwise the {@code text/html} value — useful when the input was a
 * {@link PropertyInput} whose value is only known on the client.
 * {@code onError} receives a {@link PromiseAction.Error} record with the
 * browser's error name and message.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class WriteToClipboardAction extends PromiseAction {

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
    public WriteToClipboardAction(Action.@Nullable Input<String> textInput,
            Action.@Nullable Input<String> htmlInput) {
        super();
        validate(textInput, htmlInput);
        this.textInput = textInput;
        this.htmlInput = htmlInput;
    }

    /**
     * Creates a clipboard-copy action whose outcome is reported back to the
     * server.
     *
     * @param textInput
     *            input producing the {@code text/plain} payload, or
     *            {@code null} to omit
     * @param htmlInput
     *            input producing the {@code text/html} payload, or {@code null}
     *            to omit
     * @param onCopied
     *            invoked on the UI thread with the string that was copied after
     *            the client reports the write resolved ({@code text/plain} if
     *            present, otherwise {@code text/html}), not {@code null}
     * @param onError
     *            invoked on the UI thread with the browser's error after the
     *            client reports the write rejected, not {@code null}
     * @throws IllegalArgumentException
     *             if both inputs are {@code null}
     */
    public WriteToClipboardAction(Action.@Nullable Input<String> textInput,
            Action.@Nullable Input<String> htmlInput,
            SerializableConsumer<String> onCopied,
            SerializableConsumer<Error> onError) {
        super(adaptOnCopied(onCopied), onError);
        validate(textInput, htmlInput);
        this.textInput = textInput;
        this.htmlInput = htmlInput;
    }

    private static SerializableConsumer<Success> adaptOnCopied(
            SerializableConsumer<String> onCopied) {
        Objects.requireNonNull(onCopied, "onCopied must not be null");
        return success -> onCopied.accept(asString(success.value()));
    }

    private static String asString(@Nullable JsonNode value) {
        return value == null || value.isNull() ? "" : value.asString();
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
        // Bind the text and html expressions once on the client, then write
        // them, then resolve the promise with the text (or html if no text)
        // so onCopied sees the exact string that reached the clipboard.
        out.append('(');
        appendParamList(out, "t", "h");
        out.append(" => navigator.clipboard.write([new ClipboardItem({");
        appendItemEntries(out);
        out.append("})]).then(() => ").append(textInput != null ? "t" : "h")
                .append("))(");
        appendArgList(builder, out);
        out.append(')');
    }

    /** Renders the arrow-function parameter list — only the names we use. */
    private void appendParamList(StringBuilder out, String textName,
            String htmlName) {
        out.append('(');
        boolean needComma = false;
        if (textInput != null) {
            out.append(textName);
            needComma = true;
        }
        if (htmlInput != null) {
            if (needComma) {
                out.append(',');
            }
            out.append(htmlName);
        }
        out.append(')');
    }

    /** Renders the ClipboardItem entries against parameter names "t" / "h". */
    private void appendItemEntries(StringBuilder out) {
        boolean needComma = false;
        if (textInput != null) {
            out.append("\"text/plain\":t");
            needComma = true;
        }
        if (htmlInput != null) {
            if (needComma) {
                out.append(',');
            }
            out.append("\"text/html\":h");
        }
    }

    /** Renders the actual input expressions passed into the IIFE. */
    private void appendArgList(JsBuilder builder, StringBuilder out) {
        boolean needComma = false;
        if (textInput != null) {
            textInput.appendExpression(builder, out);
            needComma = true;
        }
        if (htmlInput != null) {
            if (needComma) {
                out.append(',');
            }
            htmlInput.appendExpression(builder, out);
        }
    }
}
