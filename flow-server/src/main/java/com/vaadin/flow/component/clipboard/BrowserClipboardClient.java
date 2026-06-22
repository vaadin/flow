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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.PromiseAction;
import com.vaadin.flow.component.trigger.internal.ReadFromClipboardAction;
import com.vaadin.flow.component.trigger.internal.WriteToClipboardAction;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.shared.Registration;

/**
 * {@link ClipboardClient} implementation backed by the real browser Clipboard
 * API. It performs writes/reads by binding a {@code click} trigger that runs
 * the matching {@code window.Vaadin.Flow.clipboard} helper, and handles pastes
 * via native {@code paste} DOM listeners and per-paste file uploads. This is
 * the default implementation used when no {@link ClipboardClientFactory} is
 * registered through {@link com.vaadin.flow.di.Lookup Lookup}.
 * <p>
 * The production wire behavior lives here, unchanged from when it lived
 * directly in {@link Clipboard} and {@link ClipboardBinding}; those classes are
 * now thin facades that resolve this client and delegate to it.
 * <p>
 * <b>Framework internal.</b> Application code does not reference this class
 * directly.
 */
final class BrowserClipboardClient implements ClipboardClient {

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

    @SuppressWarnings("unused")
    private final UI ui;
    private final List<Registration> registrations = new ArrayList<>();
    private boolean closed;

    BrowserClipboardClient(UI ui) {
        this.ui = ui;
    }

    @Override
    public WriteHandle registerWrite(Component trigger, ClipboardWrite write,
            @Nullable SerializableConsumer<@Nullable String> onSuccess,
            @Nullable SerializableConsumer<PromiseAction.Error> onError) {
        ClickTrigger clickTrigger = new ClickTrigger(trigger);
        // The facade passes both callbacks or neither: both present means an
        // observed write, neither means fire-and-forget.
        WriteToClipboardAction action;
        if (onSuccess != null && onError != null) {
            action = new WriteToClipboardAction(write.textInput(),
                    write.htmlInput(), write.imageInput(), onSuccess, onError);
        } else {
            action = new WriteToClipboardAction(write.textInput(),
                    write.htmlInput(), write.imageInput());
        }
        clickTrigger.triggers(action);
        BrowserWriteHandle handle = new BrowserWriteHandle(clickTrigger,
                trigger.getElement(), write, onSuccess != null,
                onError != null);
        registrations.add(handle);
        return handle;
    }

    @Override
    public ReadHandle registerRead(Component trigger, ClipboardReadKind kind,
            SerializableConsumer<@Nullable ClipboardPayload> onSuccess,
            SerializableConsumer<PromiseAction.Error> onError) {
        ClickTrigger clickTrigger = new ClickTrigger(trigger);
        clickTrigger.triggers(new ReadFromClipboardAction(onSuccess, onError));
        BrowserReadHandle handle = new BrowserReadHandle(clickTrigger,
                trigger.getElement(), kind);
        registrations.add(handle);
        return handle;
    }

    @Override
    public Registration registerPaste(Component target, PasteOptions options,
            SerializableConsumer<PasteEvent> listener) {
        Element host = target.getElement();
        DomListenerRegistration registration = host.addEventListener("paste",
                domEvent -> {
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
                    listener.accept(new PasteEvent(target,
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
        Registration result = registration::remove;
        registrations.add(result);
        return result;
    }

    @Override
    public Registration registerFilePaste(Component target,
            UploadHandler uploadHandler) {
        Element element = target.getElement();
        // setAttribute(name, ElementRequestHandler) registers the handler as a
        // stream resource and stores its URL as the attribute value. A fresh
        // UUID scopes the slot per-registration (the same approach as
        // StreamResource) so multiple file-paste listeners on the same element
        // resolve to independent upload URLs.
        String attributeName = "_vaadin-paste-upload-" + UUID.randomUUID();
        element.setAttribute(attributeName, uploadHandler);

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

        Registration result = () -> {
            jsRegistration.remove();
            finishedRegistration.remove();
            element.removeAttribute(attributeName);
        };
        registrations.add(result);
        return result;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        // Copy first: a handle's remove() also removes itself from the list.
        for (Registration registration : new ArrayList<>(registrations)) {
            registration.remove();
        }
        registrations.clear();
    }

    private final class BrowserWriteHandle implements WriteHandle {
        private final ClickTrigger trigger;
        private final Element triggerElement;
        private final ClipboardWrite write;
        private final boolean hasSuccessCallback;
        private final boolean hasErrorCallback;

        BrowserWriteHandle(ClickTrigger trigger, Element triggerElement,
                ClipboardWrite write, boolean hasSuccessCallback,
                boolean hasErrorCallback) {
            this.trigger = trigger;
            this.triggerElement = triggerElement;
            this.write = write;
            this.hasSuccessCallback = hasSuccessCallback;
            this.hasErrorCallback = hasErrorCallback;
        }

        @Override
        public Element trigger() {
            return triggerElement;
        }

        @Override
        public ClipboardWrite write() {
            return write;
        }

        @Override
        public boolean hasSuccessCallback() {
            return hasSuccessCallback;
        }

        @Override
        public boolean hasErrorCallback() {
            return hasErrorCallback;
        }

        @Override
        public void remove() {
            trigger.remove();
            registrations.remove(this);
        }
    }

    private final class BrowserReadHandle implements ReadHandle {
        private final ClickTrigger trigger;
        private final Element triggerElement;
        private final ClipboardReadKind kind;

        BrowserReadHandle(ClickTrigger trigger, Element triggerElement,
                ClipboardReadKind kind) {
            this.trigger = trigger;
            this.triggerElement = triggerElement;
            this.kind = kind;
        }

        @Override
        public Element trigger() {
            return triggerElement;
        }

        @Override
        public ClipboardReadKind kind() {
            return kind;
        }

        @Override
        public void remove() {
            trigger.remove();
            registrations.remove(this);
        }
    }
}
