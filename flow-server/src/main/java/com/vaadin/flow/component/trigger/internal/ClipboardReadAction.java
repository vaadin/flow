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

import com.vaadin.flow.function.SerializableConsumer;

/**
 * Reads the user's clipboard via {@code navigator.clipboard.read()} when the
 * bound trigger fires and delivers the textual contents to the handler on the
 * UI thread.
 * <p>
 * The Clipboard API requires the call to happen inside a short-lived user
 * gesture (click, key press, …) AND the user to grant the
 * {@code clipboard-read} permission — without both, the browser rejects the
 * read. Bind this action to a {@link Trigger} that fires during such a gesture,
 * typically a {@link ClickTrigger}.
 * <p>
 * The handler receives a {@link ClipboardPayload} with the {@code text/plain}
 * and {@code text/html} representations of the first clipboard item, or
 * {@code null} if the read fails for any reason (permission denied, no item,
 * unsupported browser, …). Error detail is intentionally not exposed — see
 * {@link PromiseAction}'s subclasses for the success/error-split shape if that
 * distinction matters.
 *
 * <pre>{@code
 * new ClickTrigger(pasteButton).triggers(new ClipboardReadAction(payload -> {
 *     if (payload == null) {
 *         notification.show("Clipboard read denied");
 *     } else {
 *         editor.setValue(
 *                 payload.html() != null ? payload.html() : payload.text());
 *     }
 * }));
 * }</pre>
 *
 * For internal use only. May be renamed or removed in a future release.
 */
public class ClipboardReadAction extends Action {

    private final SerializableConsumer<@Nullable ClipboardPayload> handler;

    /**
     * Creates an action that reads the user's clipboard and delivers the
     * contents to {@code handler}.
     *
     * @param handler
     *            invoked on the UI thread with the clipboard contents, or
     *            {@code null} if the read failed; not {@code null}
     */
    public ClipboardReadAction(
            SerializableConsumer<@Nullable ClipboardPayload> handler) {
        this.handler = Objects.requireNonNull(handler,
                "handler must not be null");
    }

    @Override
    protected void appendStatement(JsBuilder builder, StringBuilder out) {
        String cb = builder.callback(ClipboardPayload.class, handler);
        // The actual clipboard read + {text, html} extraction lives in
        // Clipboard.ts (window.Vaadin.Flow.clipboard.readPayload); this
        // Action just routes its resolved value (or null on any failure)
        // to the typed server callback.
        out.append("window.Vaadin.Flow.clipboard.readPayload()")
                .append(".then(p=>").append(cb).append("(p))")
                .append(".catch(()=>").append(cb).append("(null))");
    }
}
