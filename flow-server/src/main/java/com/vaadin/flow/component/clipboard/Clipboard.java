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
import java.util.UUID;

import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
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

    // The same string is used both as the JS expression evaluated client-side
    // and as the lookup key in DomEvent#getEventData() server-side. Any drift
    // between the two would silently produce a null value, so the expressions
    // are kept in a single constant each. `?.` guards against synthetic events
    // without a DataTransfer; `|| null` collapses the empty string (the
    // browser's value for an absent MIME type) and the optional-chain
    // short-circuit into JSON null.
    private static final String PASTE_TEXT_EXPR = "event.clipboardData?.getData('text/plain') || null";
    private static final String PASTE_HTML_EXPR = "event.clipboardData?.getData('text/html') || null";

    // Walks event.composedPath() so the check sees through open shadow DOMs
    // (e.g. a Vaadin web component's internal <input>). Matches input,
    // textarea, or anything with isContentEditable; the filter passes only
    // when none of those are in the path.
    private static final String PASTE_FILTER_SKIP_EDITABLE = "!event.composedPath().some(function(e){"
            + "return e.tagName&&(e.tagName==='INPUT'||e.tagName==='TEXTAREA'||e.isContentEditable===true);})";

    // DOM event the client paste-upload helper dispatches once every upload of
    // a paste has settled. A server-side listener for it (registered in
    // registerFilePaste) does nothing but force a Flow round trip, which
    // flushes the UI changes the per-file UploadHandler queued via UI.access —
    // so onFilePaste updates reach the client without @Push. Must stay in sync
    // with the literal dispatched in flow-client/Clipboard.ts.
    private static final String FILE_PASTE_FINISHED_EVENT = "vaadin-paste-upload-finished";

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
        return new ClipboardBinding(new ClickTrigger(component));
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
        return register(component, options, listener);
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
        return registerFilePaste(component, handler);
    }

    private static Registration register(Component host, PasteOptions options,
            SerializableConsumer<PasteEvent> listener) {
        DomListenerRegistration registration = host.getElement()
                .addEventListener("paste", domEvent -> {
                    JsonNode data = domEvent.getEventData();
                    // mapEventTargetElement() does the DOM ancestor walk in
                    // the browser to find the closest Flow-tracked element to
                    // event.target. That gives DOM-truth (not state-tree
                    // order, which can diverge from DOM for virtual children,
                    // slotted content, etc.).
                    Element targetElement = domEvent.getEventTarget()
                            .orElse(null);
                    // The JS `|| null` already collapses "" and missing MIME
                    // types to JSON null; asStringOpt() then yields empty for
                    // those, so callers see null (not "").
                    listener.accept(new PasteEvent(host,
                            data.optional(PASTE_TEXT_EXPR)
                                    .flatMap(JsonNode::asStringOpt)
                                    .orElse(null),
                            data.optional(PASTE_HTML_EXPR)
                                    .flatMap(JsonNode::asStringOpt)
                                    .orElse(null),
                            targetElement));
                });
        registration.addEventData(PASTE_TEXT_EXPR);
        registration.addEventData(PASTE_HTML_EXPR);
        registration.mapEventTargetElement();
        if (!options.includeInputFieldPastes()) {
            registration.setFilter(PASTE_FILTER_SKIP_EDITABLE);
        }
        return registration::remove;
    }

    private static Registration registerFilePaste(Component host,
            UploadHandler handler) {
        Element element = host.getElement();
        // setAttribute(name, ElementRequestHandler) registers the handler as a
        // stream resource and stores its URL as the attribute value. A fresh
        // UUID scopes the slot per-registration (the same approach as
        // StreamResource) so multiple file-paste listeners on the same element
        // resolve to independent upload URLs.
        String attributeName = "_vaadin-paste-upload-" + UUID.randomUUID();
        element.setAttribute(attributeName, handler);

        // Attach a native paste listener in the browser via the TS helper
        // window.Vaadin.Flow.clipboard.uploadPastedFiles (defined in
        // flow-client/Clipboard.ts, registered at bootstrap). On each paste the
        // helper reads the upload URL from the per-registration attribute and
        // POSTs one upload per file. addJsInitializer re-runs the install on a
        // real re-attach and invokes the returned cleanup (removeEventListener)
        // when the registration is removed or the client DOM node is discarded.
        Registration jsRegistration = element.addJsInitializer("""
                const upload = (event) => window.Vaadin.Flow.clipboard
                        .uploadPastedFiles(event, this, $0);
                this.addEventListener('paste', upload);
                return () => this.removeEventListener('paste', upload);""",
                attributeName);

        // Once the helper has finished POSTing a paste's files it dispatches
        // FILE_PASTE_FINISHED_EVENT on the element. This listener does nothing
        // on its own — its sole purpose is to make the browser perform a Flow
        // round trip, which flushes the UI changes the UploadHandler queued via
        // UI.access while handling each upload. Without it those changes would
        // sit on the server until the next round trip, which is why the API
        // would otherwise need @Push.
        DomListenerRegistration finishedRegistration = element
                .addEventListener(FILE_PASTE_FINISHED_EVENT, domEvent -> {
                    // intentionally empty; see comment above
                });

        return () -> {
            jsRegistration.remove();
            finishedRegistration.remove();
            element.removeAttribute(attributeName);
        };
    }
}
