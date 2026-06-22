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

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.shared.Registration;

/**
 * Entry point for the browser clipboard API. Bind clipboard actions to a user
 * gesture by chaining off {@link #onClick(Component)}:
 *
 * <pre>{@code
 * Button copyButton = new Button("Copy");
 * Clipboard.onClick(copyButton).writeText(textField);
 *
 * Clipboard.onClick(copyButton)
 *         .write(ClipboardContent.create().text("Hello").html("<b>Hello</b>"));
 * }</pre>
 *
 * The Clipboard API requires a fresh user gesture for each write, so actions
 * only run during the DOM event that fires the underlying trigger.
 * <p>
 * Read-side support is exposed through
 * {@link #onPaste(Component, SerializableConsumer) onPaste}, which forwards the
 * browser's native {@code paste} event to a server-side listener as a
 * {@link PasteEvent}. Unlike the write API, {@code onPaste} does not need a
 * click binding &mdash; it attaches a DOM listener directly to the given
 * component and fires on every paste gesture targeting it (or any of its
 * descendants, since {@code paste} bubbles). Pass the
 * {@link com.vaadin.flow.component.UI UI} as the component for UI-wide scope;
 * the {@link #onPaste(Component, PasteOptions, SerializableConsumer) options
 * overload} lets the application skip pastes whose target is a form field
 * &mdash; useful for page-wide listeners that should only react to pastes
 * intended for the page as a whole.
 * <p>
 * File pastes (screenshots, files dragged in from the OS, anything that
 * surfaces as {@code event.clipboardData.files}) are handled by a dedicated
 * read-side API, {@link #onFilePaste(Component, UploadHandler) onFilePaste}.
 * Each pasted file becomes its own upload &mdash; one fetch POST per file,
 * addressed to the URL generated for the supplied {@link UploadHandler} &mdash;
 * and the handler's own per-file callback (in-memory, file, temp file) is what
 * the application implements. For paste-aware orchestration, pair with
 * {@link PasteFileHandler}:
 * {@link PasteFileHandler#perFile(SerializableConsumer) perFile} for a per-file
 * callback that flags the first file of each paste, or
 * {@link PasteFileHandler#batch() batch()} for a three-step listener
 * ({@code onStart}/{@code onFile}/{@code onComplete}) that knows when the whole
 * paste has finished delivering. Use a raw {@code UploadHandler} when neither
 * shape fits. {@code onFilePaste} is independent of {@code onPaste}; the same
 * paste gesture can deliver text/html via {@code onPaste} and files via
 * {@code onFilePaste} when both are registered.
 *
 * @since 25.2
 */
public final class Clipboard implements Serializable {

    /**
     * Request header set by the client-side paste-upload helper on every fetch
     * POST that originates from a paste gesture. The value is a monotonically
     * increasing sequence number (as decimal text) so server handlers can
     * correlate the parallel uploads of one paste and tell pastes apart from
     * each other. Two distinct pastes in the same browser tab always carry
     * strictly increasing values; pastes from different tabs are not
     * comparable.
     * <p>
     * Raw {@link UploadHandler} implementations may read this header directly
     * via {@code event.getRequest().getHeader(PASTE_ID_HEADER)};
     * {@link PasteFileHandler#perFile(SerializableConsumer)} reads it for the
     * caller and exposes it as {@link PasteFile#pasteId() PasteFile.pasteId()}.
     */
    public static final String PASTE_ID_HEADER = "X-Paste-Id";

    /**
     * Request header set alongside {@link #PASTE_ID_HEADER} carrying the total
     * number of files in the originating paste. The batch handler built by
     * {@link PasteFileHandler#batch()} uses it to fire
     * {@link com.vaadin.flow.function.SerializableConsumer onComplete} once the
     * server has observed all files of a paste &mdash; without it the server
     * has no way to tell mid-paste arrival from "all done".
     */
    public static final String PASTE_FILE_COUNT_HEADER = "X-Paste-File-Count";

    private Clipboard() {
        // utility class
    }

    /**
     * Registers the given component as a clickable trigger for a clipboard
     * action — the common shape for copy-to-clipboard buttons. Equivalent to
     * {@code new ClickTrigger(component)}, without making callers reach for the
     * trigger framework's internal types.
     *
     * @param component
     *            the component to listen for clicks on, not {@code null}
     * @param <T>
     *            the component type, must implement {@link ClickNotifier}
     * @return a new binding that can chain actions to this trigger
     */
    public static <T extends Component & ClickNotifier<?>> ClipboardBinding onClick(
            T component) {
        Objects.requireNonNull(component, "component must not be null");
        return new ClipboardBinding(component);
    }

    /**
     * Registers a listener for browser {@code paste} events on the given
     * component. The listener is invoked on the UI thread once per paste
     * gesture targeting {@code component} (or any descendant, since
     * {@code paste} bubbles) with a {@link PasteEvent} carrying the
     * {@code text/plain} and {@code text/html} representations of the pasted
     * content.
     *
     * <p>
     * The browser only fires {@code paste} when the target element is focused
     * at the moment the user invokes paste. For non-editable elements such as a
     * {@code Div} this means the element must be made focusable, typically via
     * {@code tabindex="0"}. See {@link PasteEvent} for the rest of the browser
     * caveats.
     *
     * <p>
     * Example:
     *
     * <pre>{@code
     * Div pasteTarget = new Div();
     * pasteTarget.getElement().setAttribute("tabindex", "0");
     * add(pasteTarget);
     *
     * Clipboard.onPaste(pasteTarget, event -> {
     *     if (event.hasHtml()) {
     *         renderHtml(event.getHtml());
     *     } else if (event.hasText()) {
     *         renderText(event.getText());
     *     }
     * });
     * }</pre>
     *
     * @param component
     *            the component to listen for paste events on, not {@code null}
     * @param listener
     *            the listener invoked for each paste, not {@code null}
     * @return a {@link Registration} whose {@link Registration#remove() remove}
     *         detaches the paste listener
     */
    public static Registration onPaste(Component component,
            SerializableConsumer<PasteEvent> listener) {
        return onPaste(component, PasteOptions.includingInputFields(),
                listener);
    }

    /**
     * Registers a listener for browser {@code paste} events on the given
     * component with the given {@link PasteOptions}. The listener is invoked on
     * the UI thread for each paste gesture targeting {@code component} (or any
     * descendant, since {@code paste} bubbles) whose target matches the
     * options. For UI-wide scope, pass the {@link com.vaadin.flow.component.UI
     * UI} as the component; the UI's root element is {@code <body>} so it
     * receives every paste event that bubbles up from anywhere on the page.
     * <p>
     * The component does not need to be attached at registration time — the
     * underlying DOM listener is bound to the component's element and is
     * applied when the element is attached to a UI.
     * <p>
     * Pass {@link PasteOptions#defaults()} to skip pastes whose target is an
     * input, textarea, or {@code contenteditable} element (typically what a
     * page-wide listener wants). Pass
     * {@link PasteOptions#includingInputFields()} to observe every paste
     * regardless of focus.
     *
     * @param component
     *            the component to listen for paste events on, not {@code null}
     * @param options
     *            paste filtering options, not {@code null}
     * @param listener
     *            the listener invoked for each matching paste, not {@code null}
     * @return a {@link Registration} whose {@link Registration#remove() remove}
     *         detaches the paste listener
     */
    public static Registration onPaste(Component component,
            PasteOptions options, SerializableConsumer<PasteEvent> listener) {
        Objects.requireNonNull(component, "component must not be null");
        Objects.requireNonNull(options, "options must not be null");
        Objects.requireNonNull(listener, "listener must not be null");
        return client(component).registerPaste(component, options, listener);
    }

    /**
     * Registers a file-upload listener for browser {@code paste} events on the
     * given component. When a paste delivers files (a screenshot from the OS
     * clipboard, files copied from a file manager, anything that arrives on
     * {@code event.clipboardData.files}) each file is uploaded to the URL
     * generated for {@code handler}; the handler's own per-file callback then
     * runs server-side, exactly as for a regular upload. Pastes without files
     * are ignored.
     *
     * <p>
     * One XHR is sent per file, matching the wire format used by
     * {@code vaadin-upload}: the file is the request body, the file name
     * travels in the {@code X-Filename} header (percent-encoded), and the
     * file's MIME type goes in {@code Content-Type}. The supplied
     * {@link UploadHandler}'s configured per-file size limit therefore applies
     * unchanged.
     *
     * <p>
     * Unlike {@link #onPaste(Component, SerializableConsumer) onPaste}, the
     * file variant does not offer an "ignore pastes into form fields" option:
     * browsers never paste files into an {@code <input>} or {@code <textarea>},
     * so there is no native paste behaviour to compete with, and a paste
     * containing a file in a focused text field is still a "the user dropped a
     * file on the page" event from the application's point of view.
     *
     * <p>
     * This API is independent of
     * {@link #onPaste(Component, SerializableConsumer) onPaste}. A paste that
     * carries both files and text fires both listeners when both are
     * registered.
     *
     * <p>
     * Example:
     *
     * <pre>{@code
     * Clipboard.onFilePaste(this, UploadHandler.inMemory((meta, bytes) -> {
     *     store(meta.fileName(), bytes);
     * }));
     * }</pre>
     *
     * @param component
     *            the component to listen for paste events on, not {@code null}
     * @param handler
     *            the upload handler invoked per pasted file, not {@code null}
     * @return a {@link Registration} whose {@link Registration#remove() remove}
     *         detaches the paste listener and discards the per-registration
     *         upload URL
     */
    public static Registration onFilePaste(Component component,
            UploadHandler handler) {
        Objects.requireNonNull(component, "component must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        return client(component).registerFilePaste(component, handler);
    }

    /**
     * Resolves the {@link ClipboardClient} for the UI the given component
     * belongs to, installing the default browser-backed client (or a
     * {@link ClipboardClientFactory}-produced one) on first use. The UI is
     * taken from the component, falling back to {@link UI#getCurrent()} for
     * components that are not yet attached.
     */
    static ClipboardClient client(Component component) {
        UI ui = component.getUI().orElseGet(UI::getCurrent);
        if (ui == null) {
            throw new IllegalStateException(
                    "Cannot resolve a UI for the clipboard operation: the "
                            + "component is not attached and there is no current "
                            + "UI. Call this from the UI thread, or attach the "
                            + "component first.");
        }
        return client(ui);
    }

    static ClipboardClient client(UI ui) {
        ClipboardClient existing = ui.getInternals().getClipboardClient();
        if (existing != null) {
            return existing;
        }
        ClipboardClientFactory factory = lookupFactory(ui);
        ClipboardClient client = factory != null ? factory.create(ui)
                : new BrowserClipboardClient(ui);
        ui.getInternals().setClipboardClient(client);
        return client;
    }

    private static @Nullable ClipboardClientFactory lookupFactory(UI ui) {
        VaadinService service = VaadinService.getCurrent();
        if (service == null && ui.getSession() != null) {
            service = ui.getSession().getService();
        }
        if (service == null) {
            return null;
        }
        VaadinContext context = service.getContext();
        if (context == null) {
            return null;
        }
        Lookup lookup = context.getAttribute(Lookup.class);
        if (lookup == null) {
            return null;
        }
        return lookup.lookup(ClipboardClientFactory.class);
    }
}
