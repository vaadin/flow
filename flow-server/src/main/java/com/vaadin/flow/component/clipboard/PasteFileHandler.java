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
 * The latest paste wins: pastes are tracked per owning component, and starting
 * a newer paste supersedes any older one that has not finished yet. A late
 * upload belonging to a superseded paste is dropped rather than reopening a
 * finished or abandoned batch, so a paste whose declared file count never
 * arrives (a stalled upload, or a missing/garbage
 * {@link Clipboard#PASTE_FILE_COUNT_HEADER}) cannot accumulate &mdash; the next
 * paste evicts it, and in any case the state lives and dies with the component.
 * The trade-off is that two pastes overlapping in transit no longer complete on
 * independent timelines; in practice pastes are small and serial, so the newer
 * one superseding the older is the desired behaviour.
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
 */
public final class PasteFileHandler implements Serializable {

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
                    ComponentBatches state = componentBatches(owner,
                            batchesKey);

                    // A paste id below the high-water mark is a late upload
                    // from
                    // a paste a newer one already superseded; drop it so it
                    // cannot resurrect a finished batch.
                    if (pasteId < state.maxPasteId) {
                        return;
                    }
                    if (pasteId > state.maxPasteId) {
                        state.maxPasteId = pasteId;
                        // A newer paste drops older, still-unfinished batches
                        // so
                        // a malformed or never-completing one (e.g. a missing
                        // or
                        // bogus X-Paste-File-Count that leaves expected == 0)
                        // cannot pile up.
                        state.batches.keySet().removeIf(id -> id < pasteId);
                    }

                    BatchState batch = state.batches.get(pasteId);
                    boolean newPaste;
                    if (batch != null) {
                        newPaste = false;
                    } else {
                        batch = new BatchState(totalFiles);
                        state.batches.put(pasteId, batch);
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
                        state.batches.remove(pasteId);
                        completeHandler
                                .accept(new PasteComplete(pasteId, received));
                    }
                });
            };
        }
    }

    private static ComponentBatches componentBatches(Component owner,
            String key) {
        if (owner == null) {
            // Unreachable through Clipboard.onFilePaste, which always registers
            // against a component; guard so misuse fails loudly instead of
            // silently leaking state in a handler-owned map.
            throw new IllegalStateException(
                    "PasteFileHandler requires an owning component; "
                            + "use Clipboard.onFilePaste to register it");
        }
        ComponentBatches state = (ComponentBatches) ComponentUtil.getData(owner,
                key);
        if (state == null) {
            state = new ComponentBatches();
            ComponentUtil.setData(owner, key, state);
        }
        return state;
    }

    private static final class ComponentBatches implements Serializable {

        final Map<Long, BatchState> batches = new HashMap<>();
        long maxPasteId;
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
