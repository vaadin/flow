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

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * Invokes the browser's native share sheet via {@code navigator.share} when the
 * bound trigger fires. Supports any combination of {@code title}, {@code text},
 * and {@code url} payload slots; at least one slot must be set — the Web Share
 * API rejects a call with an empty payload.
 * <p>
 * The Web Share API requires the call to happen inside a short-lived user
 * gesture (click, key press, ...). Bind this action to a trigger that fires
 * during such a gesture, typically a {@link ClickTrigger}. The share sheet
 * itself acts as the user-facing confirmation; the browser also rejects calls
 * made outside a gesture.
 * <p>
 * Outcome handling extends {@link PromiseAction}: use the no-callbacks
 * constructor for fire-and-forget, or the overload taking
 * {@code onShared}/{@code onError}. {@code onShared} fires after the user
 * dismisses the sheet by sharing; {@code onError} fires for both true failures
 * (no gesture, permissions policy block) and for the {@code AbortError} the
 * browser reports when the user dismisses the sheet without picking a target.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class ShareAction extends PromiseAction<Void> {

    private final Action.@Nullable Input<String> titleInput;
    private final Action.@Nullable Input<String> textInput;
    private final Action.@Nullable Input<String> urlInput;

    /**
     * Creates a fire-and-forget share action.
     *
     * @param titleInput
     *            input producing the {@code title} field, or {@code null} to
     *            omit
     * @param textInput
     *            input producing the {@code text} field, or {@code null} to
     *            omit
     * @param urlInput
     *            input producing the {@code url} field, or {@code null} to omit
     * @throws IllegalArgumentException
     *             if all three inputs are {@code null}
     */
    public ShareAction(Action.@Nullable Input<String> titleInput,
            Action.@Nullable Input<String> textInput,
            Action.@Nullable Input<String> urlInput) {
        super();
        validate(titleInput, textInput, urlInput);
        this.titleInput = titleInput;
        this.textInput = textInput;
        this.urlInput = urlInput;
    }

    /**
     * Creates a share action whose outcome is reported back to the server.
     *
     * @param titleInput
     *            input producing the {@code title} field, or {@code null} to
     *            omit
     * @param textInput
     *            input producing the {@code text} field, or {@code null} to
     *            omit
     * @param urlInput
     *            input producing the {@code url} field, or {@code null} to omit
     * @param onShared
     *            invoked on the UI thread after the client reports the share
     *            resolved, not {@code null}
     * @param onError
     *            invoked on the UI thread with the browser's error after the
     *            client reports the share rejected — typically
     *            {@code AbortError} when the user dismissed the sheet, not
     *            {@code null}
     * @throws IllegalArgumentException
     *             if all three inputs are {@code null}
     */
    public ShareAction(Action.@Nullable Input<String> titleInput,
            Action.@Nullable Input<String> textInput,
            Action.@Nullable Input<String> urlInput,
            SerializableRunnable onShared,
            SerializableConsumer<Error> onError) {
        super(Void.class, runnableAsConsumer(onShared), onError);
        validate(titleInput, textInput, urlInput);
        this.titleInput = titleInput;
        this.textInput = textInput;
        this.urlInput = urlInput;
    }

    private static void validate(Action.@Nullable Input<String> title,
            Action.@Nullable Input<String> text,
            Action.@Nullable Input<String> url) {
        if (title == null && text == null && url == null) {
            throw new IllegalArgumentException(
                    "At least one of titleInput, textInput, urlInput must be non-null");
        }
    }

    private static SerializableConsumer<@Nullable Void> runnableAsConsumer(
            SerializableRunnable onShared) {
        if (onShared == null) {
            throw new NullPointerException("onShared must not be null");
        }
        return ignored -> onShared.run();
    }

    @Override
    protected JsFunction toPromiseJs(Trigger trigger) {
        // navigator.share({title:$0(event), ...}) with only the slots that were
        // set; each slot's value is produced on the client by invoking the
        // input's JsFunction with the trigger event. validate() already ensures
        // at least one slot is present, so the object is never empty (the Web
        // Share API rejects a call with no payload fields).
        StringBuilder expression = new StringBuilder(
                "return navigator.share({");
        List<JsFunction> args = new ArrayList<>();
        appendSlot(expression, args, "title", titleInput, trigger);
        appendSlot(expression, args, "text", textInput, trigger);
        appendSlot(expression, args, "url", urlInput, trigger);
        expression.append("})");
        return JsFunction.of(expression.toString(), args.toArray())
                .withArguments("event");
    }

    private static void appendSlot(StringBuilder expression,
            List<JsFunction> args, String key,
            Action.@Nullable Input<String> input, Trigger trigger) {
        if (input == null) {
            return;
        }
        if (!args.isEmpty()) {
            expression.append(',');
        }
        expression.append(key).append(":$").append(args.size())
                .append("(event)");
        args.add(input.toJs(trigger));
    }
}
