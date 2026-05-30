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

import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.server.streams.UploadHandler;

/**
 * Factory for paste-aware {@link UploadHandler}s that group the parallel fetch
 * uploads of a single paste gesture and surface the result to application code.
 * Two flavours are offered, both producing an {@code UploadHandler} suitable
 * for
 * {@link Clipboard#onFilePaste(com.vaadin.flow.component.Component, UploadHandler)
 * Clipboard.onFilePaste}:
 *
 * <ul>
 * <li>{@link #inMemory(SerializableConsumer)} &mdash; a single per-file
 * callback that receives every file as a {@link PasteFile}, with
 * {@link PasteFile#newPaste()} flagging the first file of each paste.</li>
 * <li>{@link #session()} &mdash; a three-step session listener: {@code onStart}
 * fires once per paste with the declared file count, {@code onFile} fires per
 * file, and {@code onComplete} fires once after the last file has been
 * delivered.</li>
 * </ul>
 *
 * Each paste runs in its own session: pastes that overlap in transit (the tail
 * of an older paste's uploads arriving after a newer paste has started) still
 * run through their own {@code onStart} / {@code onFile} / {@code onComplete}
 * lifecycle. Sessions live in memory until they receive all the files the
 * browser declared in {@link Clipboard#PASTE_FILE_COUNT_HEADER}; a paste whose
 * upload fails mid-flight stays open indefinitely (rare in practice, since
 * uploads are small, but worth knowing if an application keeps a process alive
 * for months).
 * <p>
 * Application code that wants "show the latest paste only" semantics tracks the
 * highest paste id seen and filters in its own callbacks &mdash; the handler
 * does not drop any files on the application's behalf, so a slow tail upload
 * from an earlier paste still arrives at the listener.
 */
public final class PasteFileHandler {

    private PasteFileHandler() {
        // factory
    }

    /**
     * Builds an {@link UploadHandler} that reads each upload fully into memory,
     * packages it as a {@link PasteFile}, and delivers it to the supplied
     * consumer on the UI thread.
     *
     * @param consumer
     *            invoked for each accepted file on the UI thread, not
     *            {@code null}
     * @return an upload handler suitable for
     *         {@link Clipboard#onFilePaste(com.vaadin.flow.component.Component, UploadHandler)
     *         Clipboard.onFilePaste}
     */
    public static UploadHandler inMemory(
            SerializableConsumer<PasteFile> consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        return session().onFile(consumer).build();
    }

    /**
     * Starts a session-style handler builder that emits {@code onStart} once
     * per paste before any file, {@code onFile} per file, and
     * {@code onComplete} once the paste's declared file count has been
     * delivered. Any callback may be omitted.
     *
     * @return a fresh, unconfigured session builder
     */
    public static SessionBuilder session() {
        return new SessionBuilder();
    }

    /**
     * Fluent builder for the session-style paste handler. See
     * {@link PasteFileHandler#session()} for the listener semantics. Successive
     * calls to the same {@code onX} method overwrite earlier registrations;
     * omitted steps default to no-ops.
     */
    public static final class SessionBuilder implements Serializable {

        private SerializableConsumer<PasteStart> onStart = start -> {
        };
        private SerializableConsumer<PasteFile> onFile = file -> {
        };
        private SerializableConsumer<PasteComplete> onComplete = end -> {
        };

        private SessionBuilder() {
        }

        /**
         * Registers the {@code onStart} step, fired once per paste before the
         * paste's first {@link PasteFile} is delivered.
         *
         * @param handler
         *            consumer invoked on the UI thread; pass {@code null} to
         *            reset to a no-op
         * @return this builder
         */
        public SessionBuilder onStart(
                SerializableConsumer<PasteStart> handler) {
            this.onStart = handler != null ? handler : start -> {
            };
            return this;
        }

        /**
         * Registers the {@code onFile} step, fired once per accepted file with
         * the file's bytes and metadata.
         *
         * @param handler
         *            consumer invoked on the UI thread; pass {@code null} to
         *            reset to a no-op
         * @return this builder
         */
        public SessionBuilder onFile(SerializableConsumer<PasteFile> handler) {
            this.onFile = handler != null ? handler : file -> {
            };
            return this;
        }

        /**
         * Registers the {@code onComplete} step, fired once after the paste's
         * declared file count has been delivered to
         * {@link #onFile(SerializableConsumer) onFile}.
         *
         * @param handler
         *            consumer invoked on the UI thread; pass {@code null} to
         *            reset to a no-op
         * @return this builder
         */
        public SessionBuilder onComplete(
                SerializableConsumer<PasteComplete> handler) {
            this.onComplete = handler != null ? handler : end -> {
            };
            return this;
        }

        /**
         * Returns an {@link UploadHandler} that orchestrates the configured
         * session steps. The returned handler is independent of this builder;
         * further mutations to the builder do not affect it.
         */
        public UploadHandler build() {
            // Per-handler session map keyed by paste id. All access happens
            // inside event.getUI().access, which runs on the UI thread, so
            // a plain HashMap is enough — no extra synchronisation needed.
            // Each paste id gets its own SessionState that lives until the
            // paste delivers all its declared files (PASTE_FILE_COUNT_HEADER).
            // Pastes do not interfere with each other: a slow tail upload
            // from an earlier paste still resolves through its own session.
            Map<Long, SessionState> sessions = new HashMap<>();
            SerializableConsumer<PasteStart> startHandler = onStart;
            SerializableConsumer<PasteFile> fileHandler = onFile;
            SerializableConsumer<PasteComplete> completeHandler = onComplete;
            return event -> {
                long pasteId = parsePasteId(event.getRequest()
                        .getHeader(Clipboard.PASTE_ID_HEADER));
                int totalFiles = parseFileCount(event.getRequest()
                        .getHeader(Clipboard.PASTE_FILE_COUNT_HEADER));

                byte[] data;
                try (InputStream in = event.getInputStream()) {
                    data = in.readAllBytes();
                }

                event.getUI().access(() -> {
                    SessionState existing = sessions.get(pasteId);
                    SessionState state;
                    boolean newPaste;
                    if (existing != null) {
                        state = existing;
                        newPaste = false;
                    } else {
                        state = new SessionState(totalFiles);
                        sessions.put(pasteId, state);
                        newPaste = true;
                        startHandler
                                .accept(new PasteStart(pasteId, totalFiles));
                    }

                    fileHandler.accept(new PasteFile(pasteId, newPaste,
                            state.expected, event.getFileName(),
                            event.getContentType(), event.getFileSize(), data));
                    state.received++;

                    if (state.expected > 0
                            && state.received >= state.expected) {
                        int received = state.received;
                        sessions.remove(pasteId);
                        completeHandler
                                .accept(new PasteComplete(pasteId, received));
                    }
                });
            };
        }
    }

    private static final class SessionState implements Serializable {

        final int expected;
        int received;

        SessionState(int expected) {
            this.expected = expected;
        }
    }

    private static long parsePasteId(String header) {
        if (header == null || header.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int parseFileCount(String header) {
        if (header == null || header.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(header);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
