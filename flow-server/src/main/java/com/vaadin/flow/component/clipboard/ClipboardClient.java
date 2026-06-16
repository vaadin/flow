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

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.PromiseAction;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.shared.Registration;

/**
 * Framework-internal port between the {@link Clipboard} /
 * {@link ClipboardBinding} static API and whatever actually performs the
 * clipboard operations — the browser in production, an in-memory driver in
 * browserless tests. Application code does not interact with this interface; it
 * is exposed so external test drivers can replace the production client via
 * {@link com.vaadin.flow.component.internal.UIInternals#setClipboardClient(ClipboardClient)}.
 * <p>
 * Every write/read is bound to a click trigger on a component, mirroring the
 * Clipboard API's requirement that the underlying browser call happen inside a
 * user gesture. Paste registration attaches a listener (or upload handler) to a
 * target element directly. One instance per UI.
 * <p>
 * <b>Threading:</b> every callback passed to this interface (write
 * {@code onSuccess}/{@code onError}, read {@code onSuccess}/{@code onError},
 * paste {@code listener}) is invoked on the UI thread.
 *
 * @since 25.2
 */
public interface ClipboardClient extends Serializable {

    /**
     * Binds a clipboard write to a click trigger on {@code trigger}. The write
     * runs in the browser when the trigger fires. Pass {@code null} for both
     * {@code onSuccess} and {@code onError} for a fire-and-forget write, or
     * both non-{@code null} to be notified of the outcome.
     *
     * @param trigger
     *            the component whose click fires the write, not {@code null}
     * @param write
     *            the content descriptor to write, not {@code null}
     * @param onSuccess
     *            UI-thread callback receiving the copied string (the
     *            {@code text/plain} value if present, otherwise
     *            {@code text/html}, otherwise {@code null} for an image-only
     *            write), or {@code null} for fire-and-forget
     * @param onError
     *            UI-thread callback receiving the browser's error, or
     *            {@code null} for fire-and-forget
     * @return a handle that can inspect and remove the binding
     */
    WriteHandle registerWrite(Component trigger, ClipboardWrite write,
            @Nullable SerializableConsumer<@Nullable String> onSuccess,
            @Nullable SerializableConsumer<PromiseAction.Error> onError);

    /**
     * Binds a clipboard read to a click trigger on {@code trigger}. The read
     * runs in the browser when the trigger fires and delivers the full
     * {@link ClipboardPayload} to {@code onSuccess}; {@code kind} records which
     * slice of the payload the application asked for (the binding extracts the
     * requested field before the application sees it).
     *
     * @param trigger
     *            the component whose click fires the read, not {@code null}
     * @param kind
     *            which slice of the clipboard the application requested, not
     *            {@code null}
     * @param onSuccess
     *            UI-thread callback receiving the clipboard contents, or
     *            {@code null} if the clipboard was empty, not {@code null}
     * @param onError
     *            UI-thread callback receiving the browser's error, not
     *            {@code null}
     * @return a handle that can inspect and remove the binding
     */
    ReadHandle registerRead(Component trigger, ClipboardReadKind kind,
            SerializableConsumer<@Nullable ClipboardPayload> onSuccess,
            SerializableConsumer<PromiseAction.Error> onError);

    /**
     * Registers a listener for browser {@code paste} events on {@code target}.
     *
     * @param target
     *            the component to listen on, not {@code null}
     * @param options
     *            paste filtering options, not {@code null}
     * @param listener
     *            UI-thread callback invoked for each matching paste, not
     *            {@code null}
     * @return a registration that detaches the listener
     */
    Registration registerPaste(Component target, PasteOptions options,
            SerializableConsumer<PasteEvent> listener);

    /**
     * Registers a file-upload handler for browser {@code paste} events on
     * {@code target}. Each pasted file is fed through {@code uploadHandler}.
     *
     * @param target
     *            the component to listen on, not {@code null}
     * @param uploadHandler
     *            the handler invoked per pasted file, not {@code null}
     * @return a registration that detaches the listener and discards the upload
     *         URL
     */
    Registration registerFilePaste(Component target,
            UploadHandler uploadHandler);

    /**
     * Releases any resources held by this client. Called when one client is
     * being replaced by another (e.g. when a test driver is installed).
     * Idempotent: calling more than once is a no-op.
     */
    void close();

    /**
     * Inspect/resolve handle for a bound write action. {@link #remove()}
     * detaches the binding.
     */
    interface WriteHandle extends Registration {

        /**
         * The element whose click fires the write.
         *
         * @return the trigger element, never {@code null}
         */
        Element trigger();

        /**
         * The content descriptor that will be written.
         *
         * @return the write descriptor, never {@code null}
         */
        ClipboardWrite write();

        /**
         * Whether the write reports success back to the server.
         *
         * @return {@code true} if an {@code onSuccess} callback was supplied
         */
        boolean hasSuccessCallback();

        /**
         * Whether the write reports failure back to the server.
         *
         * @return {@code true} if an {@code onError} callback was supplied
         */
        boolean hasErrorCallback();
    }

    /**
     * Inspect/resolve handle for a bound read action. {@link #remove()}
     * detaches the binding.
     */
    interface ReadHandle extends Registration {

        /**
         * The element whose click fires the read.
         *
         * @return the trigger element, never {@code null}
         */
        Element trigger();

        /**
         * Which slice of the clipboard the application requested.
         *
         * @return the read kind, never {@code null}
         */
        ClipboardReadKind kind();
    }
}
