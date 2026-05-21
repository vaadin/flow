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

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * Copies a value to the user's clipboard via
 * {@code navigator.clipboard.writeText} when the bound trigger fires.
 * <p>
 * The Clipboard API requires the call to happen inside a short-lived user
 * gesture (click, key press, …). Bind this action to a {@link Trigger} that
 * fires during such a gesture, typically a {@link ClickTrigger}.
 * <p>
 * Outcome handling is inherited from {@link PromiseAction}: use the no-arg
 * outcome constructor for fire-and-forget, or the overload taking
 * {@code onSuccess}/{@code onError} consumers to react to the {@code writeText}
 * promise on the UI thread.
 *
 * <pre>{@code
 * Action.Input<String> value = new PropertyInput<>(textField, "value",
 *         String.class);
 * CopyTextToClipboardAction copy = new CopyTextToClipboardAction(value,
 *         () -> notification.show("Copied"),
 *         err -> notification.show("Copy failed: " + err));
 * new ClickTrigger(button).triggers(copy);
 * }</pre>
 *
 * For internal use only. May be renamed or removed in a future release.
 */
public class CopyTextToClipboardAction extends PromiseAction {

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
     * server via {@code onSuccess}/{@code onError}. See {@link PromiseAction}
     * for the contract on both consumers.
     *
     * @param textInput
     *            input supplying the text to copy, not {@code null}
     * @param onSuccess
     *            invoked on the UI thread after the client reports
     *            {@code writeText} resolved, not {@code null}
     * @param onError
     *            invoked on the UI thread with the browser's error message
     *            after the client reports {@code writeText} rejected, not
     *            {@code null}
     */
    public CopyTextToClipboardAction(Action.Input<String> textInput,
            SerializableRunnable onSuccess,
            SerializableConsumer<String> onError) {
        super(onSuccess, onError);
        this.textInput = Objects.requireNonNull(textInput,
                "textInput must not be null");
    }

    @Override
    protected void appendPromiseExpression(JsBuilder builder,
            StringBuilder out) {
        out.append("navigator.clipboard.writeText(");
        textInput.appendExpression(builder, out);
        out.append(")");
    }
}
