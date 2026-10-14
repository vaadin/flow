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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
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
 * <li>{@link #perFile(SerializableConsumer)} &mdash; a single per-file callback
 * that receives every file as a {@link PasteFile}, with
 * {@link PasteFile#newPaste()} flagging the first file of each paste.</li>
 * <li>{@link #batch()} &mdash; a three-step batch listener: {@code onStart}
 * fires once per paste with the declared file count, {@code onFile} fires per
 * file, and {@code onComplete} fires once after the last file has been
 * delivered.</li>
 * </ul>
 *
 * Each paste runs as its own batch and completes on its own timeline: pastes
 * that overlap in transit (the tail of an older paste's uploads arriving after
 * a newer paste has started) still run through their own {@code onStart} /
 * {@code onFile} / {@code onComplete} lifecycle, and no file is dropped on the
 * application's behalf. Batch state is tracked per owning component, so it is
 * released when the component is detached or the session ends rather than
 * living in the handler &mdash; a handler instance shared across components or
 * UIs cannot accumulate state. As a further bound, only a limited number of
 * unfinished pastes are tracked per component at once: if a paste's declared
 * {@link Clipboard#PASTE_FILE_COUNT_HEADER} count never fully arrives (a
 * stalled upload, or a missing/garbage header), the oldest such batch is
 * dropped once that many are open. The limit is far above any real overlap, so
 * it only ever trims abandoned or malformed batches.
 * <p>
 * Application code that wants "show the latest paste only" semantics tracks the
 * highest paste id seen and filters in its own callbacks &mdash; the handler
 * does not drop any files of a live paste on the application's behalf.
 *
 * <h2>Resource notes</h2>
 *
 * <ul>
 * <li>{@link #perFile(SerializableConsumer)} and the {@code onFile} step of
 * {@link #batch()} read each uploaded file <em>fully into memory</em> as a
 * {@code byte[]} before invoking the callback. A very large paste is therefore
 * held entirely in heap; cap it with the request/file size limits on a custom
 * {@link UploadHandler}, or read the stream yourself, when that matters.</li>
 * <li>{@link PasteFile#fileName()} is the browser-supplied name and is not
 * sanitized. Treat it as untrusted &mdash; never use it directly as a
 * filesystem path without sanitizing it first.</li>
 * </ul>
 *
 * @since 25.2
 */
public final class PasteFileHandler implements Serializable {

    // Upper bound on the number of unfinished pastes tracked per component at
    // once. Real overlap is a handful of pastes, so this only bounds memory
    // against abandoned or malformed batches; once this many are open, starting
    // another drops the oldest still-unfinished one.
    private static final int MAX_TRACKED_BATCHES = 16;

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
    public static UploadHandler perFile(
            SerializableConsumer<PasteFile> consumer) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        return batch().onFile(consumer).build();
    }

    /**
     * Starts a batch handler builder that emits {@code onStart} once per paste
     * before any file, {@code onFile} per file, and {@code onComplete} once the
     * paste's declared file count has been delivered. Any callback may be
     * omitted.
     *
     * @return a fresh, unconfigured batch builder
     */
    public static BatchBuilder batch() {
        return new BatchBuilder();
    }

    /**
     * Fluent builder for the batch paste handler. See
     * {@link PasteFileHandler#batch()} for the listener semantics. Successive
     * calls to the same {@code onX} method overwrite earlier registrations;
     * omitted steps default to no-ops.
     */
    public static final class BatchBuilder implements Serializable {

        private SerializableConsumer<PasteStart> onStart = start -> {
        };
        private SerializableConsumer<PasteFile> onFile = file -> {
        };
        private SerializableConsumer<PasteComplete> onComplete = end -> {
        };

        private BatchBuilder() {
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
        public BatchBuilder onStart(SerializableConsumer<PasteStart> handler) {
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
        public BatchBuilder onFile(SerializableConsumer<PasteFile> handler) {
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
        public BatchBuilder onComplete(
                SerializableConsumer<PasteComplete> handler) {
            this.onComplete = handler != null ? handler : end -> {
            };
            return this;
        }

        /**
         * Returns an {@link UploadHandler} that orchestrates the configured
         * batch steps. The returned handler is independent of this builder;
         * further mutations to the builder do not affect it.
         */
        public UploadHandler build() {
            // The batch state is stored on the owning component (Clipboard
            // #onFilePaste always registers against one) rather than in this
            // handler, so a handler instance shared across components or UIs
            // cannot accumulate state: the batches live and die with the
            // component. The key is unique per build() so several handlers on
            // the same component stay independent.
            String batchesKey = PasteFileHandler.class.getName() + "$batches$"
                    + UUID.randomUUID();
            SerializableConsumer<PasteStart> startHandler = onStart;
            SerializableConsumer<PasteFile> fileHandler = onFile;
            SerializableConsumer<PasteComplete> completeHandler = onComplete;
            return event -> {
                long pasteId = parsePasteId(event.getRequest()
                        .getHeader(Clipboard.PASTE_ID_HEADER));
                int totalFiles = parseFileCount(event.getRequest()
                        .getHeader(Clipboard.PASTE_FILE_COUNT_HEADER));
                Component owner = event.getOwningComponent();

                byte[] data;
                try (InputStream in = event.getInputStream()) {
                    data = in.readAllBytes();
                }

                event.getUI().access(() -> {
                    // All access runs on the UI thread holding the session
                    // lock, and the state is scoped to this component, so a
                    // plain map needs no extra synchronisation.
                    Map<Long, BatchState> batches = componentBatches(owner,
                            batchesKey);

                    BatchState batch = batches.get(pasteId);
                    boolean newPaste;
                    if (batch != null) {
                        newPaste = false;
                    } else {
                        batch = new BatchState(totalFiles);
                        // Bound the number of unfinished pastes tracked at once
                        // so batches whose declared X-Paste-File-Count never
                        // fully arrives (a stalled upload, or a missing/garbage
                        // header that leaves expected == 0) cannot pile up.
                        // Genuine overlap is a handful of pastes, well under
                        // the
                        // cap, so this only ever trims abandoned batches and
                        // does not affect pastes completing on their own
                        // timelines.
                        if (batches.size() >= MAX_TRACKED_BATCHES) {
                            Iterator<Long> oldest = batches.keySet().iterator();
                            oldest.next();
                            oldest.remove();
                        }
                        batches.put(pasteId, batch);
                        newPaste = true;
                        startHandler
                                .accept(new PasteStart(pasteId, totalFiles));
                    }

                    fileHandler.accept(new PasteFile(pasteId, newPaste,
                            batch.expected, event.getFileName(),
                            event.getContentType(), event.getFileSize(), data));
                    batch.received++;

                    if (batch.expected > 0
                            && batch.received >= batch.expected) {
                        int received = batch.received;
                        batches.remove(pasteId);
                        completeHandler
                                .accept(new PasteComplete(pasteId, received));
                    }
                });
            };
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, BatchState> componentBatches(Component owner,
            String key) {
        if (owner == null) {
            // Unreachable through Clipboard.onFilePaste, which always registers
            // against a component; guard so misuse fails loudly instead of
            // silently leaking state in a handler-owned map.
            throw new IllegalStateException(
                    "PasteFileHandler requires an owning component; "
                            + "use Clipboard.onFilePaste to register it");
        }
        Map<Long, BatchState> batches = (Map<Long, BatchState>) ComponentUtil
                .getData(owner, key);
        if (batches == null) {
            // Insertion-ordered so the cap can evict the oldest open batch.
            batches = new LinkedHashMap<>();
            ComponentUtil.setData(owner, key, batches);
        }
        return batches;
    }

    private static final class BatchState implements Serializable {

        final int expected;
        int received;

        BatchState(int expected) {
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
