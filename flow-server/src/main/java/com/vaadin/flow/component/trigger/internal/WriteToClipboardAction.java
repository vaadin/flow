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

import com.vaadin.flow.component.clipboard.ClipboardContent;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.function.SerializableConsumer;

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
 * as a with-outcome variant taking {@code onCopied}/{@code onError} consumers:
 * <ul>
 * <li>Text/HTML — the typical case for copying a string</li>
 * <li>Image — the typical case for copying an image</li>
 * <li>Multi-format via {@link ClipboardContent} — combine any of the three
 * slots in one item</li>
 * </ul>
 * {@code onCopied} receives the exact string that was copied — the
 * {@code text/plain} value if present, otherwise the {@code text/html} value,
 * otherwise {@code null} (image-only case) — useful when the input was a
 * {@link PropertyInput} whose value is only known on the client.
 * {@code onError} receives a {@link PromiseAction.Error} record with the
 * browser's error name and message.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class WriteToClipboardAction extends PromiseAction<String> {

    /**
     * Stand-in input that yields a JS {@code null} for a missing MIME slot, so
     * the rendered call always reaches the TS helper with three arguments
     * regardless of which slots were set on the server.
     */
    private static final JsFunction NULL_INPUT_FN = JsFunction
            .of("return null");

    private final Action.@Nullable Input<String> textInput;
    private final Action.@Nullable Input<String> htmlInput;
    private final Action.@Nullable Input<?> imageInput;

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
    public WriteToClipboardAction(Action.@Nullable Input<String> textInput,
            Action.@Nullable Input<String> htmlInput) {
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
    public WriteToClipboardAction(Action.@Nullable Input<String> textInput,
            Action.@Nullable Input<String> htmlInput,
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
    public WriteToClipboardAction(Action.Input<?> imageInput) {
        this(null, null, Objects.requireNonNull(imageInput,
                "imageInput must not be null"));
    }

    /**
     * Creates an image clipboard-copy action whose outcome is reported back to
     * the server. {@code onCopied} receives {@code null} — the image-only write
     * has no meaningful string value.
     *
     * @param imageInput
     *            input producing the source {@code <img>} for the
     *            {@code image/png} payload (typically an
     *            {@link ImageBlobInput}), not {@code null}
     * @param onCopied
     *            invoked on the UI thread with {@code null} after the client
     *            reports the write resolved, not {@code null}
     * @param onError
     *            invoked on the UI thread with the browser's error after the
     *            client reports the write rejected, not {@code null}
     */
    public WriteToClipboardAction(Action.Input<?> imageInput,
            SerializableConsumer<@Nullable String> onCopied,
            SerializableConsumer<Error> onError) {
        this(null, null, Objects.requireNonNull(imageInput,
                "imageInput must not be null"), onCopied, onError);
    }

    /**
     * Creates a fire-and-forget multi-format clipboard-copy action from a
     * {@link ClipboardContent} describing the payload. Use
     * {@code Clipboard.onClick(...).write(content)} as the typical entry point.
     *
     * @param content
     *            the clipboard payload, not {@code null}; must have at least
     *            one slot set
     * @throws IllegalArgumentException
     *             if {@code content} has no slots set
     */
    public WriteToClipboardAction(ClipboardContent content) {
        this(Objects.requireNonNull(content, "content must not be null")
                .getTextInput(), content.getHtmlInput(),
                content.getImageInput());
    }

    /**
     * Creates a multi-format clipboard-copy action from a
     * {@link ClipboardContent} whose outcome is reported back to the server.
     *
     * @param content
     *            the clipboard payload, not {@code null}; must have at least
     *            one slot set
     * @param onCopied
     *            invoked on the UI thread with the string that was copied after
     *            the client reports the write resolved ({@code text/plain} if
     *            present, otherwise {@code text/html}, otherwise {@code null}
     *            in the image-only case), or {@code null} if the JS resolved
     *            with {@code undefined}; not {@code null}
     * @param onError
     *            invoked on the UI thread with the browser's error after the
     *            client reports the write rejected, not {@code null}
     * @throws IllegalArgumentException
     *             if {@code content} has no slots set
     */
    public WriteToClipboardAction(ClipboardContent content,
            SerializableConsumer<@Nullable String> onCopied,
            SerializableConsumer<Error> onError) {
        this(Objects.requireNonNull(content, "content must not be null")
                .getTextInput(), content.getHtmlInput(),
                content.getImageInput(), onCopied, onError);
    }

    private WriteToClipboardAction(Action.@Nullable Input<String> textInput,
            Action.@Nullable Input<String> htmlInput,
            Action.@Nullable Input<?> imageInput) {
        super();
        validate(textInput, htmlInput, imageInput);
        this.textInput = textInput;
        this.htmlInput = htmlInput;
        this.imageInput = imageInput;
    }

    private WriteToClipboardAction(Action.@Nullable Input<String> textInput,
            Action.@Nullable Input<String> htmlInput,
            Action.@Nullable Input<?> imageInput,
            SerializableConsumer<@Nullable String> onCopied,
            SerializableConsumer<Error> onError) {
        super(String.class, onCopied, onError);
        validate(textInput, htmlInput, imageInput);
        this.textInput = textInput;
        this.htmlInput = htmlInput;
        this.imageInput = imageInput;
    }

    private static void validate(Action.@Nullable Input<String> text,
            Action.@Nullable Input<String> html,
            Action.@Nullable Input<?> image) {
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
        JsFunction text = textInput != null ? textInput.toJs(trigger)
                : NULL_INPUT_FN;
        JsFunction html = htmlInput != null ? htmlInput.toJs(trigger)
                : NULL_INPUT_FN;
        JsFunction image = imageInput != null ? imageInput.toJs(trigger)
                : NULL_INPUT_FN;
        return JsFunction.of(
                "return window.Vaadin.Flow.clipboard.writePayload($0(event), $1(event), $2(event))",
                text, html, image).withArguments("event");
    }
}
