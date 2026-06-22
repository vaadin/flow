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
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.trigger.internal.ImageBlobInput;
import com.vaadin.flow.component.trigger.internal.PromiseAction.Error;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * Fluent surface returned from {@link Clipboard#onClick}. Each {@code write*}
 * action binds one clipboard write to the trigger component; each {@code read*}
 * action binds one clipboard read. The binding is a thin facade that resolves
 * the UI's {@link ClipboardClient} on each call and delegates to
 * {@link ClipboardClient#registerWrite registerWrite} /
 * {@link ClipboardClient#registerRead registerRead}.
 * <p>
 * Write actions come in two flavours: fire-and-forget (one argument) and
 * observed (with {@code onCopied}/{@code onError} callbacks). {@code onCopied}
 * receives the string that was copied; {@code onError} receives the browser's
 * error. Both consumers are required in the observed form — pass
 * {@code s -> {}} or {@code err -> {}} to opt out of one.
 * <p>
 * Read actions always take both an {@code onPayload} consumer (receiving the
 * clipboard contents or {@code null} if empty) and an {@code onError} consumer
 * (receiving the browser's error, typically {@code "NotAllowedError"} when the
 * user denied the {@code clipboard-read} permission).
 *
 * <pre>{@code
 * Button copy = new Button("Copy");
 * Clipboard.onClick(copy).writeText(textField);
 *
 * Clipboard.onClick(copy).writeText(textField,
 *         copied -> Notification.show("Copied " + copied),
 *         err -> Notification.show("Failed: " + err.message()));
 *
 * Button paste = new Button("Paste");
 * Clipboard.onClick(paste).readText(
 *         text -> Notification.show("Pasted " + text),
 *         err -> Notification.show("Failed: " + err.message()));
 * }</pre>
 *
 * @since 25.2
 */
public final class ClipboardBinding implements Serializable {

    private final Component trigger;

    ClipboardBinding(Component trigger) {
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
        write(ClipboardWrite.ofText(literal), null, null);
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
        write(ClipboardWrite.ofText(literal), onCopied, onError);
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
        write(ClipboardWrite.ofText(source), null, null);
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
        write(ClipboardWrite.ofText(source), onCopied, onError);
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
        write(ClipboardWrite.ofHtml(literal), null, null);
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
        write(ClipboardWrite.ofHtml(literal), onCopied, onError);
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
     * @throws IllegalArgumentException
     *             if the source's root element is not an {@code <img>}
     */
    public void writeImage(Component source) {
        Objects.requireNonNull(source, "source must not be null");
        write(ClipboardWrite.ofImage(new ImageBlobInput(source)), null, null);
    }

    /**
     * Like {@link #writeImage(Component)} but reports the outcome back to the
     * server. An image-only write has no string value, so success is reported
     * as a plain {@link SerializableRunnable} rather than a value callback.
     *
     * @param source
     *            the component whose root {@code <img>} should be copied, not
     *            {@code null}
     * @param onCopied
     *            UI-thread callback invoked once the write resolves, not
     *            {@code null}
     * @param onError
     *            UI-thread callback receiving the browser's error, not
     *            {@code null}
     * @throws IllegalArgumentException
     *             if the source's root element is not an {@code <img>}
     */
    public void writeImage(Component source, SerializableRunnable onCopied,
            SerializableConsumer<Error> onError) {
        Objects.requireNonNull(source, "source must not be null");
        write(ClipboardWrite.ofImage(new ImageBlobInput(source)),
                runnableAsConsumer(onCopied), onError);
    }

    /**
     * Copies the image served by the given {@link DownloadHandler} to the
     * clipboard as {@code image/png} when the underlying trigger fires.
     * <p>
     * A hidden {@code <img>} bound to the handler is appended to the trigger
     * host, so the browser begins downloading the image as soon as this method
     * is called — well before the user can click. At fire time the image is
     * already decoded (or finishes decoding inside the canvas converter's
     * {@code load} listener), drawn onto a canvas, and exported as PNG.
     * <p>
     * Cross-origin concerns do not apply because the handler is served by the
     * same origin as the application.
     *
     * @param handler
     *            the download handler producing the image bytes, not
     *            {@code null}
     */
    public void writeImage(DownloadHandler handler) {
        Objects.requireNonNull(handler, "handler must not be null");
        write(ClipboardWrite.ofImage(
                new ImageBlobInput(attachHiddenImg(handler))), null, null);
    }

    /**
     * Like {@link #writeImage(DownloadHandler)} but reports the outcome back to
     * the server. An image-only write has no string value, so success is
     * reported as a plain {@link SerializableRunnable} rather than a value
     * callback.
     *
     * @param handler
     *            the download handler producing the image bytes, not
     *            {@code null}
     * @param onCopied
     *            UI-thread callback invoked once the write resolves, not
     *            {@code null}
     * @param onError
     *            UI-thread callback receiving the browser's error, not
     *            {@code null}
     */
    public void writeImage(DownloadHandler handler,
            SerializableRunnable onCopied,
            SerializableConsumer<Error> onError) {
        Objects.requireNonNull(handler, "handler must not be null");
        write(ClipboardWrite
                .ofImage(new ImageBlobInput(attachHiddenImg(handler))),
                runnableAsConsumer(onCopied), onError);
    }

    private Element attachHiddenImg(DownloadHandler handler) {
        Element img = new Element(Tag.IMG);
        img.getStyle().set("display", "none");
        img.setAttribute("src", handler.allowDisabled());
        trigger.getElement().appendChild(img);
        return img;
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
        validateContent(content);
        write(ClipboardWrite.ofContent(content), null, null);
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
        validateContent(content);
        write(ClipboardWrite.ofContent(content), onCopied, onError);
    }

    /**
     * Reads the user's clipboard via {@code navigator.clipboard.read()} when
     * the underlying trigger fires and delivers the contents to
     * {@code onPayload}, or routes any failure to {@code onError}.
     * <p>
     * The Clipboard API requires the call to happen inside a short-lived user
     * gesture AND the user to grant the {@code clipboard-read} permission;
     * binding to a click trigger satisfies the gesture, but the browser may
     * still reject the read with {@code "NotAllowedError"} when the permission
     * is denied.
     *
     * @param onPayload
     *            UI-thread callback receiving the clipboard contents, or
     *            {@code null} if the clipboard was empty; not {@code null}
     * @param onError
     *            UI-thread callback receiving the browser's error, not
     *            {@code null}
     */
    public void read(SerializableConsumer<@Nullable ClipboardPayload> onPayload,
            SerializableConsumer<Error> onError) {
        Objects.requireNonNull(onPayload, "onPayload must not be null");
        Objects.requireNonNull(onError, "onError must not be null");
        Clipboard.client(trigger).registerRead(trigger, ClipboardReadKind.READ,
                onPayload, onError);
    }

    /**
     * Like {@link #read} but delivers only the {@code text/plain} field of the
     * clipboard payload to {@code onText}. {@code onText} receives {@code null}
     * if the clipboard was empty or had no {@code text/plain} representation.
     *
     * @param onText
     *            UI-thread callback receiving the {@code text/plain} value, or
     *            {@code null}; not {@code null}
     * @param onError
     *            UI-thread callback receiving the browser's error, not
     *            {@code null}
     */
    public void readText(SerializableConsumer<@Nullable String> onText,
            SerializableConsumer<Error> onError) {
        Objects.requireNonNull(onText, "onText must not be null");
        Objects.requireNonNull(onError, "onError must not be null");
        Clipboard.client(trigger).registerRead(trigger,
                ClipboardReadKind.READ_TEXT,
                p -> onText.accept(p == null ? null : p.text()), onError);
    }

    /**
     * Like {@link #read} but delivers only the {@code text/html} field of the
     * clipboard payload to {@code onHtml}. {@code onHtml} receives {@code null}
     * if the clipboard was empty or had no {@code text/html} representation.
     *
     * @param onHtml
     *            UI-thread callback receiving the {@code text/html} value, or
     *            {@code null}; not {@code null}
     * @param onError
     *            UI-thread callback receiving the browser's error, not
     *            {@code null}
     */
    public void readHtml(SerializableConsumer<@Nullable String> onHtml,
            SerializableConsumer<Error> onError) {
        Objects.requireNonNull(onHtml, "onHtml must not be null");
        Objects.requireNonNull(onError, "onError must not be null");
        Clipboard.client(trigger).registerRead(trigger,
                ClipboardReadKind.READ_HTML,
                p -> onHtml.accept(p == null ? null : p.html()), onError);
    }

    private void write(ClipboardWrite content,
            @Nullable SerializableConsumer<@Nullable String> onCopied,
            @Nullable SerializableConsumer<Error> onError) {
        Clipboard.client(trigger).registerWrite(trigger, content, onCopied,
                onError);
    }

    private static void validateContent(ClipboardContent content) {
        if (content.getTextInput() == null && content.getHtmlInput() == null
                && content.getImageInput() == null) {
            throw new IllegalArgumentException(
                    "ClipboardContent must have at least one slot set");
        }
    }

    private static SerializableConsumer<@Nullable String> runnableAsConsumer(
            SerializableRunnable onCopied) {
        Objects.requireNonNull(onCopied, "onCopied must not be null");
        return ignored -> onCopied.run();
    }
}
