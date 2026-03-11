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
package com.vaadin.flow.component.page;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;
import com.vaadin.flow.shared.Registration;

/**
 * Provides access to the browser Clipboard API.
 * <p>
 * This interface offers two categories of clipboard operations:
 * <ul>
 * <li><b>Client-side copy (reliable)</b>: Methods like
 * {@link #copyOnClick(Component, String)} set up a click handler that executes
 * the clipboard write directly in the browser event handler, satisfying the
 * user gesture requirement in all browsers.</li>
 * <li><b>Server-initiated operations</b>: Methods like
 * {@link #writeText(String)} go through a server round-trip and may not work in
 * Firefox or Safari due to user gesture timeout.</li>
 * </ul>
 * <p>
 * Usage pattern follows {@link WebStorage} — static methods that use
 * {@link UI#getCurrentOrThrow()} by default.
 */
public interface Clipboard extends Serializable {

    // --- Client-side copy (reliable, no round-trip) ---

    /**
     * Sets up a client-side click handler on the trigger component that copies
     * the given text to the clipboard when clicked.
     * <p>
     * The copy operation executes entirely on the client side within the click
     * event handler, so it satisfies the user gesture requirement in all
     * browsers.
     *
     * @param trigger
     *            the component whose clicks trigger the copy
     * @param text
     *            the text to copy to the clipboard
     * @return a {@link ClipboardCopy} handle for updating the text or removing
     *         the handler
     */
    static ClipboardCopy copyOnClick(Component trigger, String text) {
        Objects.requireNonNull(trigger, "Trigger component must not be null");

        Element element = trigger.getElement();
        element.setProperty(ClipboardCopy.CLIPBOARD_TEXT_PROPERTY,
                text != null ? text : "");

        element.executeJs(
                "window.Vaadin.Flow.clipboard.setupCopyOnClick(this)");

        Registration cleanup = Registration.once(() -> {
            element.executeJs(
                    "window.Vaadin.Flow.clipboard.cleanupCopyOnClick(this)");
        });

        return new ClipboardCopy(element, cleanup);
    }

    /**
     * Sets up a client-side click handler on the trigger component that copies
     * the given text to the clipboard, with success and error callbacks.
     * <p>
     * The copy operation executes on the client side. The callbacks are invoked
     * on the server after the clipboard operation completes or fails.
     *
     * @param trigger
     *            the component whose clicks trigger the copy
     * @param text
     *            the text to copy to the clipboard
     * @param onSuccess
     *            callback invoked on the server when the copy succeeds
     * @param onError
     *            callback invoked on the server when the copy fails
     * @return a {@link ClipboardCopy} handle for updating the text or removing
     *         the handler
     */
    static ClipboardCopy copyOnClick(Component trigger, String text,
            Command onSuccess, Command onError) {
        Objects.requireNonNull(trigger, "Trigger component must not be null");

        Element element = trigger.getElement();
        element.setProperty(ClipboardCopy.CLIPBOARD_TEXT_PROPERTY,
                text != null ? text : "");

        ReturnChannelRegistration successChannel = element.getNode()
                .getFeature(ReturnChannelMap.class).registerChannel(args -> {
                    if (onSuccess != null) {
                        onSuccess.execute();
                    }
                });

        ReturnChannelRegistration errorChannel = element.getNode()
                .getFeature(ReturnChannelMap.class).registerChannel(args -> {
                    if (onError != null) {
                        onError.execute();
                    }
                });

        element.executeJs(
                "window.Vaadin.Flow.clipboard.setupCopyOnClickWithCallbacks(this, $0, $1)",
                successChannel, errorChannel);

        Registration cleanup = Registration.once(() -> {
            successChannel.remove();
            errorChannel.remove();
            element.executeJs(
                    "window.Vaadin.Flow.clipboard.cleanupCopyOnClick(this)");
        });

        return new ClipboardCopy(element, cleanup);
    }

    /**
     * Sets up a client-side click handler on the trigger component that copies
     * the current value of the source component to the clipboard.
     * <p>
     * The source component's value is read client-side (from the DOM element's
     * {@code value} or {@code textContent} property) at click time, so no
     * server round-trip is needed.
     *
     * @param trigger
     *            the component whose clicks trigger the copy
     * @param source
     *            the component whose value to copy
     * @return a {@link ClipboardCopy} handle for removing the handler
     */
    static ClipboardCopy copyOnClick(Component trigger, Component source) {
        Objects.requireNonNull(trigger, "Trigger component must not be null");
        Objects.requireNonNull(source, "Source component must not be null");

        Element element = trigger.getElement();

        element.executeJs(
                "window.Vaadin.Flow.clipboard.setupCopyOnClickFromSource(this, $0)",
                source.getElement());

        Registration cleanup = Registration.once(() -> {
            element.executeJs(
                    "window.Vaadin.Flow.clipboard.cleanupCopyOnClick(this)");
        });

        return new ClipboardCopy(element, cleanup);
    }

    /**
     * Sets up a client-side click handler on the trigger component that copies
     * the image from the given image component to the clipboard.
     * <p>
     * The image source component should have a {@code src} attribute (e.g. an
     * {@code <img>} element). The image is fetched client-side from the
     * element's {@code src} attribute and written to the clipboard as a blob.
     *
     * @param trigger
     *            the component whose clicks trigger the copy
     * @param imageSource
     *            the component with a {@code src} attribute pointing to an
     *            image
     * @return a {@link ClipboardCopy} handle for removing the handler
     */
    static ClipboardCopy copyImageOnClick(Component trigger,
            Component imageSource) {
        Objects.requireNonNull(trigger, "Trigger component must not be null");
        Objects.requireNonNull(imageSource,
                "Image source component must not be null");

        Element element = trigger.getElement();

        element.executeJs(
                "window.Vaadin.Flow.clipboard.setupCopyImageOnClick(this, $0)",
                imageSource.getElement());

        Registration cleanup = Registration.once(() -> {
            element.executeJs(
                    "window.Vaadin.Flow.clipboard.cleanupCopyOnClick(this)");
        });

        return new ClipboardCopy(element, cleanup);
    }

    // --- Server-initiated clipboard operations (round-trip, browser-dependent)
    // ---

    /**
     * Writes the given text to the clipboard.
     * <p>
     * <b>Browser compatibility note:</b> This method involves a server
     * round-trip and may not work in Firefox or Safari, which require clipboard
     * operations to be performed within a user gesture (click handler call
     * stack). Use {@link #copyOnClick(Component, String)} for reliable
     * cross-browser clipboard writes.
     *
     * @param text
     *            the text to write to the clipboard
     * @return a {@link PendingJavaScriptResult} for the clipboard operation
     */
    static PendingJavaScriptResult writeText(String text) {
        return writeText(UI.getCurrentOrThrow(), text);
    }

    /**
     * Writes the given text to the clipboard using the specified UI.
     *
     * @param ui
     *            the UI to use
     * @param text
     *            the text to write to the clipboard
     * @return a {@link PendingJavaScriptResult} for the clipboard operation
     */
    static PendingJavaScriptResult writeText(UI ui, String text) {
        return ui.getPage().executeJs(
                "return window.Vaadin.Flow.clipboard.writeText($0)", text);
    }

    /**
     * Writes the given text to the clipboard with success and error callbacks.
     * <p>
     * <b>Browser compatibility note:</b> This method involves a server
     * round-trip and may not work in Firefox or Safari.
     *
     * @param text
     *            the text to write
     * @param onSuccess
     *            callback invoked when the write succeeds
     * @param onError
     *            callback invoked with an error message when the write fails
     */
    static void writeText(String text, Command onSuccess,
            SerializableConsumer<String> onError) {
        writeText(UI.getCurrentOrThrow(), text, onSuccess, onError);
    }

    /**
     * Writes the given text to the clipboard with success and error callbacks,
     * using the specified UI.
     *
     * @param ui
     *            the UI to use
     * @param text
     *            the text to write
     * @param onSuccess
     *            callback invoked when the write succeeds
     * @param onError
     *            callback invoked with an error message when the write fails
     */
    static void writeText(UI ui, String text, Command onSuccess,
            SerializableConsumer<String> onError) {
        PendingJavaScriptResult result = writeText(ui, text);
        result.then(JsonNode.class, value -> {
            if (onSuccess != null) {
                onSuccess.execute();
            }
        }, error -> {
            if (onError != null) {
                onError.accept(error);
            }
        });
    }

    /**
     * Reads text from the clipboard.
     * <p>
     * <b>Browser compatibility note:</b> This method involves a server
     * round-trip. The browser may prompt the user for permission to read the
     * clipboard.
     *
     * @param callback
     *            callback invoked with the clipboard text
     */
    static void readText(SerializableConsumer<String> callback) {
        readText(UI.getCurrentOrThrow(), callback);
    }

    /**
     * Reads text from the clipboard using the specified UI.
     *
     * @param ui
     *            the UI to use
     * @param callback
     *            callback invoked with the clipboard text
     */
    static void readText(UI ui, SerializableConsumer<String> callback) {
        Objects.requireNonNull(callback, "Callback must not be null");
        ui.getPage().executeJs("return window.Vaadin.Flow.clipboard.readText()")
                .then(String.class, callback);
    }

    /**
     * Writes an image from the given URL to the clipboard.
     * <p>
     * The browser fetches the image from the URL client-side and writes it to
     * the clipboard as a PNG blob.
     * <p>
     * <b>Browser compatibility note:</b> This method involves a server
     * round-trip for delivering the JavaScript command.
     *
     * @param imageUrl
     *            the URL of the image to copy
     * @return a {@link PendingJavaScriptResult} for the clipboard operation
     */
    static PendingJavaScriptResult writeImage(String imageUrl) {
        return writeImage(UI.getCurrentOrThrow(), imageUrl);
    }

    /**
     * Writes an image from the given URL to the clipboard using the specified
     * UI.
     *
     * @param ui
     *            the UI to use
     * @param imageUrl
     *            the URL of the image to copy
     * @return a {@link PendingJavaScriptResult} for the clipboard operation
     */
    static PendingJavaScriptResult writeImage(UI ui, String imageUrl) {
        return ui.getPage().executeJs(
                "return window.Vaadin.Flow.clipboard.writeImage($0)", imageUrl);
    }

    /**
     * Writes an image from the given {@link StreamResource} to the clipboard.
     * <p>
     * The stream resource is registered in the session and the browser fetches
     * it client-side.
     *
     * @param resource
     *            the stream resource providing the image data
     * @return a {@link PendingJavaScriptResult} for the clipboard operation
     */
    static PendingJavaScriptResult writeImage(StreamResource resource) {
        return writeImage(UI.getCurrentOrThrow(), resource);
    }

    /**
     * Writes an image from the given {@link StreamResource} to the clipboard
     * using the specified UI.
     *
     * @param ui
     *            the UI to use
     * @param resource
     *            the stream resource providing the image data
     * @return a {@link PendingJavaScriptResult} for the clipboard operation
     */
    static PendingJavaScriptResult writeImage(UI ui, StreamResource resource) {
        Objects.requireNonNull(resource, "Resource must not be null");

        StreamRegistration registration = ui.getSession().getResourceRegistry()
                .registerResource(resource);

        String url = registration.getResourceUri().toString();
        return writeImage(ui, url);
    }

    // --- Event listeners ---

    /**
     * Adds a listener for paste events on the given component.
     * <p>
     * The listener receives text, HTML, and file data from the paste event.
     * Pasted files are transferred to the server via the upload mechanism.
     *
     * @param target
     *            the component to listen for paste events on
     * @param listener
     *            the paste event listener
     * @return a registration for removing the listener
     */
    static Registration addPasteListener(Component target,
            SerializableConsumer<ClipboardEvent> listener) {
        Objects.requireNonNull(target, "Target component must not be null");
        Objects.requireNonNull(listener, "Listener must not be null");

        Element element = target.getElement();

        // Shared state for coordinating upload callbacks and the return channel
        PasteState pasteState = new PasteState(listener);

        // Register an upload handler for receiving pasted files
        element.setAttribute("__clipboard-paste-upload", UploadHandler
                .inMemory((UploadMetadata metadata, byte[] data) -> {
                    pasteState.addFile(new ClipboardFile(metadata.fileName(),
                            metadata.contentType(), metadata.contentLength(),
                            data));
                }));

        // Register a return channel for receiving text/html/file count
        ReturnChannelRegistration channel = element.getNode()
                .getFeature(ReturnChannelMap.class).registerChannel(args -> {
                    String text = args.get(0).isNull() ? null
                            : args.get(0).asText();
                    String html = args.get(1).isNull() ? null
                            : args.get(1).asText();
                    int fileCount = args.get(2).asInt();
                    pasteState.setTextData(text, html, fileCount);
                });

        // Install the paste event handler
        element.executeJs(
                "window.Vaadin.Flow.clipboard.setupPasteListener(this, $0)",
                channel);

        return Registration.once(() -> {
            channel.remove();
            element.removeAttribute("__clipboard-paste-upload");
            element.executeJs(
                    "window.Vaadin.Flow.clipboard.cleanupPasteListener(this)");
        });
    }

    /**
     * Adds a listener for copy events on the given component.
     *
     * @param target
     *            the component to listen for copy events on
     * @param listener
     *            the copy event listener
     * @return a registration for removing the listener
     */
    static Registration addCopyListener(Component target,
            SerializableConsumer<ClipboardEvent> listener) {
        return addClipboardEventListener(target, "copy", listener);
    }

    /**
     * Adds a listener for cut events on the given component.
     *
     * @param target
     *            the component to listen for cut events on
     * @param listener
     *            the cut event listener
     * @return a registration for removing the listener
     */
    static Registration addCutListener(Component target,
            SerializableConsumer<ClipboardEvent> listener) {
        return addClipboardEventListener(target, "cut", listener);
    }

    // --- Internal helpers ---

    /**
     * Adds a listener for copy or cut events using the standard
     * DomListenerRegistration pattern.
     */
    private static Registration addClipboardEventListener(Component target,
            String eventType, SerializableConsumer<ClipboardEvent> listener) {
        Objects.requireNonNull(target, "Target component must not be null");
        Objects.requireNonNull(listener, "Listener must not be null");

        Element element = target.getElement();

        DomListenerRegistration reg = element.addEventListener(eventType,
                domEvent -> {
                    JsonNode data = domEvent.getEventData();
                    String textKey = "event.clipboardData.getData('text/plain')";
                    String htmlKey = "event.clipboardData.getData('text/html')";

                    String text = data.has(textKey)
                            && !data.get(textKey).isNull()
                                    ? data.get(textKey).asText()
                                    : null;
                    String html = data.has(htmlKey)
                            && !data.get(htmlKey).isNull()
                                    ? data.get(htmlKey).asText()
                                    : null;

                    listener.accept(new ClipboardEvent(eventType, text, html,
                            List.of()));
                });
        reg.addEventData("event.clipboardData.getData('text/plain')");
        reg.addEventData("event.clipboardData.getData('text/html')");

        return reg;
    }

    /**
     * Coordinates the asynchronous arrival of text/HTML metadata and uploaded
     * file data for a paste event.
     */
    class PasteState implements Serializable {
        private final SerializableConsumer<ClipboardEvent> listener;
        private String text;
        private String html;
        private int expectedFileCount = -1;
        private final List<ClipboardFile> files = new ArrayList<>();
        private boolean dispatched = false;

        PasteState(SerializableConsumer<ClipboardEvent> listener) {
            this.listener = listener;
        }

        synchronized void setTextData(String text, String html, int fileCount) {
            this.text = text;
            this.html = html;
            this.expectedFileCount = fileCount;
            this.dispatched = false;
            this.files.clear();
            tryDispatch();
        }

        synchronized void addFile(ClipboardFile file) {
            files.add(file);
            tryDispatch();
        }

        private void tryDispatch() {
            if (dispatched || expectedFileCount < 0) {
                return;
            }
            if (files.size() >= expectedFileCount) {
                dispatched = true;
                listener.accept(new ClipboardEvent("paste", text, html,
                        new ArrayList<>(files)));
            }
        }
    }
}
