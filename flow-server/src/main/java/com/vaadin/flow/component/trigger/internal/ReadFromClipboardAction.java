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

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.dom.JsFunction;
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
 * Outcome handling extends {@link PromiseAction}: {@code onPayload} receives a
 * {@link ClipboardPayload} with the {@code text/plain} and {@code text/html}
 * representations of the first clipboard item (or {@code null} if there were no
 * items at all), and {@code onError} receives a {@link PromiseAction.Error}
 * record with the browser's error name and message (typically
 * {@code "NotAllowedError"} when the user denied permission). Both consumers
 * are required and must be non-null; to opt out of either notification, pass
 * {@code payload -> {}} or {@code err -> {}} explicitly.
 *
 * <pre>{@code
 * new ClickTrigger(pasteButton)
 *         .triggers(new ReadFromClipboardAction(payload -> {
 *             if (payload == null) {
 *                 notification.show("Clipboard is empty");
 *             } else {
 *                 editor.setValue(payload.html() != null ? payload.html()
 *                         : payload.text());
 *             }
 *         }, err -> notification
 *                 .show("Clipboard read denied: " + err.message())));
 * }</pre>
 *
 * For internal use only. May be renamed or removed in a future release.
 */
public class ReadFromClipboardAction extends PromiseAction<ClipboardPayload> {

    /**
     * Creates an action that reads the user's clipboard and delivers the
     * contents to {@code onPayload}, or routes any failure to {@code onError}.
     *
     * @param onPayload
     *            invoked on the UI thread with the clipboard contents, or
     *            {@code null} if the clipboard was empty; not {@code null}
     * @param onError
     *            invoked on the UI thread with the browser's error when the
     *            clipboard read failed (permission denied, unsupported, …); not
     *            {@code null}
     */
    public ReadFromClipboardAction(
            SerializableConsumer<@Nullable ClipboardPayload> onPayload,
            SerializableConsumer<Error> onError) {
        super(ClipboardPayload.class, onPayload, onError);
    }

    @Override
    protected JsFunction toPromiseJs(Trigger trigger) {
        // The actual clipboard read + {text, html} extraction lives in
        // Clipboard.ts (window.Vaadin.Flow.clipboard.readPayload); this
        // action just routes the resolved Promise through the PromiseAction
        // framework.
        return JsFunction
                .of("return window.Vaadin.Flow.clipboard.readPayload()");
    }
}
