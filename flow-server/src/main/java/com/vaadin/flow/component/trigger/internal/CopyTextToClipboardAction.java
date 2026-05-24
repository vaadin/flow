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

import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableConsumer;

/**
 * Copies a value to the user's clipboard via
 * {@code navigator.clipboard.writeText} when the bound trigger fires.
 * <p>
 * The Clipboard API requires the call to happen inside a short-lived user
 * gesture (click, key press, …). Bind this action to a {@link Trigger} that
 * fires during such a gesture, typically a {@link ClickTrigger}.
 * <p>
 * Outcome handling extends {@link PromiseAction}: use the no-arg outcome
 * constructor for fire-and-forget, or the overload taking
 * {@code onCopied}/{@code onError} consumers. {@code onCopied} receives the
 * exact string that was copied — useful when the input was a
 * {@link PropertyInput} whose value is only known on the client (e.g. the
 * current contents of an input field). {@code onError} receives a
 * {@link PromiseAction.Error} record with the browser's error name and message.
 *
 * <pre>{@code
 * Action.Input<String> value = new PropertyInput<>(textField, "value",
 *         String.class);
 * CopyTextToClipboardAction copy = new CopyTextToClipboardAction(value,
 *         copied -> notification.show("Copied: " + copied),
 *         err -> notification.show("Copy failed: " + err.message()));
 * new ClickTrigger(button).triggers(copy);
 * }</pre>
 *
 * For internal use only. May be renamed or removed in a future release.
 */
public class CopyTextToClipboardAction extends PromiseAction<String> {

    private final Action.Input<String> textInput;

    /**
     * Creates a fire-and-forget clipboard-copy action: the rendered JS just
     * calls {@code writeText} and the server never sees the result.
     *
     * @param textInput
     *            input supplying the text to copy, not {@code null}
     */
    public CopyTextToClipboardAction(Action.Input<String> textInput) {
        super();
        this.textInput = Objects.requireNonNull(textInput,
                "textInput must not be null");
    }

    /**
     * Creates a clipboard-copy action whose outcome is reported back to the
     * server.
     *
     * @param textInput
     *            input supplying the text to copy, not {@code null}
     * @param onCopied
     *            invoked on the UI thread with the string that was copied after
     *            the client reports {@code writeText} resolved, not
     *            {@code null}; {@code null} value when the promise resolved
     *            with {@code undefined} (shouldn't happen in normal use)
     * @param onError
     *            invoked on the UI thread with the browser's error after the
     *            client reports {@code writeText} rejected, not {@code null}
     */
    public CopyTextToClipboardAction(Action.Input<String> textInput,
            SerializableConsumer<@Nullable String> onCopied,
            SerializableConsumer<Error> onError) {
        super(String.class, onCopied, onError);
        this.textInput = Objects.requireNonNull(textInput,
                "textInput must not be null");
    }

    @Override
    protected JsFunction renderPromiseExpression(JsBuilder builder) {
        // IIFE: bind the text once on the client, write it, then resolve the
        // promise with the same value so onCopied sees the exact string that
        // reached the clipboard. $0(event) invokes the text input.
        return JsFunction.of(
                "return ((v) => navigator.clipboard.writeText(v).then(() => v))($0(event))",
                textInput.toJs(builder)).withArguments("event");
    }
}
