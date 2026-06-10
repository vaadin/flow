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

import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;

/**
 * Internal bridge between the {@link Clipboard} facade and the paste helpers
 * that {@code Clipboard.ts} registers on {@code window.Vaadin.Flow.clipboard}.
 * The facade owns the public API and its documentation; this class owns the
 * JS-facing details: which TS helpers the browser evaluates for event data and
 * filtering, and how the evaluated results map to a {@link PasteEvent}.
 */
final class PasteEventDispatcher {

    // The same string is used both as the JS expression evaluated client-side
    // and as the lookup key in DomEvent#getEventData() server-side. Any drift
    // between the two would silently produce a null value, so the expressions
    // are kept in a single constant each. The extraction logic itself (null
    // collapsing, DataTransfer guards) lives in the Clipboard.ts helpers,
    // which Flow.ts always loads.
    static final String PASTE_TEXT_EXPR = "window.Vaadin.Flow.clipboard.pasteEventText(event)";
    static final String PASTE_HTML_EXPR = "window.Vaadin.Flow.clipboard.pasteEventHtml(event)";

    // The TS helper walks event.composedPath() and reports whether the paste
    // targets an input, textarea, or contenteditable element; the filter
    // passes only when it does not.
    static final String PASTE_FILTER_SKIP_EDITABLE = "!window.Vaadin.Flow.clipboard.pasteEventTargetsEditable(event)";

    private PasteEventDispatcher() {
        // static-only bridge
    }

    static Registration register(Component host, PasteOptions options,
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
                    // The TS helpers already collapse "" and missing MIME
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
}
