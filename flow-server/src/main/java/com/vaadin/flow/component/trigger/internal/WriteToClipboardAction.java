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

import com.vaadin.flow.component.trigger.PromiseAction;
import com.vaadin.flow.component.trigger.Trigger;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;

/**
 * Writes a {@code ClipboardItem} to the user's clipboard via
 * {@code navigator.clipboard.write}. Supports up to three concurrent MIME types
 * in one item: {@code text/plain}, {@code text/html} and {@code image/png}
 * (typically produced by an {@link ImageBlobInput}). Any combination is
 * allowed; at least one slot must be set.
 * <p>
 * The Clipboard API requires the {@code write} call to happen inside a
 * short-lived user gesture (click, key press, ...). Bind this action to a
 * trigger that fires during such a gesture.
 * <p>
 * Construction comes in three flavours, each available as fire-and-forget and
 * as a with-outcome variant:
 * <ul>
 * <li>Text/HTML — the typical case for copying a string</li>
 * <li>Image — the typical case for copying an image; the write has no string
 * value, so success is reported via a {@link SerializableRunnable}</li>
 * <li>Multi-format — pass any combination of the {@code text}, {@code html} and
 * {@code image} inputs to combine them in one item</li>
 * </ul>
 * {@code onCopied} receives the exact string that was copied — the
 * {@code text/plain} value if present, otherwise the {@code text/html} value,
 * otherwise {@code null} (image-only case) — useful when the input was a
 * {@link PropertyInput} whose value is only known on the client.
 * {@code onError} receives a {@link PromiseAction.Error} record with the
 * browser's error name and message.
 *
 * @since 25.4
 */
public final class WriteToClipboardAction extends PromiseAction<String> {

    private final @Nullable ActionInput<String> textInput;
    private final @Nullable ActionInput<String> htmlInput;
    private final @Nullable ActionInput<?> imageInput;

    /**
     * Creates a fire-and-forget text/HTML clipboard-copy action.
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
    public WriteToClipboardAction(@Nullable ActionInput<String> textInput,
            @Nullable ActionInput<String> htmlInput) {
        this(textInput, htmlInput, null);
    }

    /**
     * Creates a text/HTML clipboard-copy action whose outcome is reported back
     * to the server.
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
     *            present, otherwise {@code text/html}), or {@code null} if the
     *            JS resolved with {@code undefined}; not {@code null}
     * @param onError
     *            invoked on the UI thread with the browser's error after the
     *            client reports the write rejected, not {@code null}
     * @throws IllegalArgumentException
     *             if both inputs are {@code null}
     */
    public WriteToClipboardAction(@Nullable ActionInput<String> textInput,
            @Nullable ActionInput<String> htmlInput,
            SerializableConsumer<@Nullable String> onCopied,
            SerializableConsumer<Error> onError) {
        this(textInput, htmlInput, null, onCopied, onError);
    }

    /**
     * Creates a fire-and-forget image clipboard-copy action.
     *
     * @param imageInput
     *            input producing the source {@code <img>} for the
     *            {@code image/png} payload (typically an
     *            {@link ImageBlobInput}), not {@code null}
     */
    public WriteToClipboardAction(ActionInput<?> imageInput) {
        this(null, null, Objects.requireNonNull(imageInput,
                "imageInput must not be null"));
    }

    /**
     * Creates an image clipboard-copy action whose outcome is reported back to
     * the server. An image-only write has no string value, so success is
     * reported via a {@link SerializableRunnable} rather than a value callback.
     *
     * @param imageInput
     *            input producing the source {@code <img>} for the
     *            {@code image/png} payload (typically an
     *            {@link ImageBlobInput}), not {@code null}
     * @param onCopied
     *            invoked on the UI thread after the client reports the write
     *            resolved, not {@code null}
     * @param onError
     *            invoked on the UI thread with the browser's error after the
     *            client reports the write rejected, not {@code null}
     */
    public WriteToClipboardAction(ActionInput<?> imageInput,
            SerializableRunnable onCopied,
            SerializableConsumer<Error> onError) {
        this(null, null,
                Objects.requireNonNull(imageInput,
                        "imageInput must not be null"),
                runnableAsConsumer(onCopied), onError);
    }

    /**
     * Creates a fire-and-forget multi-format clipboard-copy action. Pass any
     * combination of inputs; at least one must be non-{@code null}.
     *
     * @param textInput
     *            input producing the {@code text/plain} payload, or
     *            {@code null} to omit
     * @param htmlInput
     *            input producing the {@code text/html} payload, or {@code null}
     *            to omit
     * @param imageInput
     *            input producing the source {@code <img>} for the
     *            {@code image/png} payload, or {@code null} to omit
     * @throws IllegalArgumentException
     *             if all inputs are {@code null}
     */
    public WriteToClipboardAction(@Nullable ActionInput<String> textInput,
            @Nullable ActionInput<String> htmlInput,
            @Nullable ActionInput<?> imageInput) {
        super();
        validate(textInput, htmlInput, imageInput);
        this.textInput = textInput;
        this.htmlInput = htmlInput;
        this.imageInput = imageInput;
    }

    /**
     * Creates a multi-format clipboard-copy action whose outcome is reported
     * back to the server. Pass any combination of inputs; at least one must be
     * non-{@code null}.
     *
     * @param textInput
     *            input producing the {@code text/plain} payload, or
     *            {@code null} to omit
     * @param htmlInput
     *            input producing the {@code text/html} payload, or {@code null}
     *            to omit
     * @param imageInput
     *            input producing the source {@code <img>} for the
     *            {@code image/png} payload, or {@code null} to omit
     * @param onCopied
     *            invoked on the UI thread with the string that was copied after
     *            the client reports the write resolved ({@code text/plain} if
     *            present, otherwise {@code text/html}, otherwise {@code null}
     *            in the image-only case); not {@code null}
     * @param onError
     *            invoked on the UI thread with the browser's error after the
     *            client reports the write rejected, not {@code null}
     * @throws IllegalArgumentException
     *             if all inputs are {@code null}
     */
    public WriteToClipboardAction(@Nullable ActionInput<String> textInput,
            @Nullable ActionInput<String> htmlInput,
            @Nullable ActionInput<?> imageInput,
            SerializableConsumer<@Nullable String> onCopied,
            SerializableConsumer<Error> onError) {
        super(String.class, onCopied, onError);
        validate(textInput, htmlInput, imageInput);
        this.textInput = textInput;
        this.htmlInput = htmlInput;
        this.imageInput = imageInput;
    }

    private static SerializableConsumer<@Nullable String> runnableAsConsumer(
            SerializableRunnable onCopied) {
        Objects.requireNonNull(onCopied, "onCopied must not be null");
        return ignored -> onCopied.run();
    }

    private static void validate(@Nullable ActionInput<String> text,
            @Nullable ActionInput<String> html,
            @Nullable ActionInput<?> image) {
        if (text == null && html == null && image == null) {
            throw new IllegalArgumentException(
                    "At least one of textInput, htmlInput, imageInput must be non-null");
        }
    }

    @Override
    protected JsFunction toPromiseJs(Trigger trigger) {
        // All three slots are always present in the call; absent slots become
        // a no-op input that returns null, so the TS helper sees null and
        // skips that MIME type. Keeping the call shape uniform across all
        // combinations means no per-action JS assembly.
        // Created per call rather than held in a constant, so that the csp
        // mode reporting of JsFunction.of runs for a call, not a class load
        JsFunction nullInput = JsFunction.of("return null");
        JsFunction text = textInput != null ? textInput.toJs(trigger)
                : nullInput;
        JsFunction html = htmlInput != null ? htmlInput.toJs(trigger)
                : nullInput;
        JsFunction image = imageInput != null ? imageInput.toJs(trigger)
                : nullInput;
        return JsFunction.of(
                "return window.Vaadin.Flow.clipboard.writePayload($0(event), $1(event), $2(event))",
                text, html, image).withArguments("event");
    }
}
