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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PasteFileHandlerTest {

    @Test
    void session_singlePaste_emitsStartThenFilesThenComplete()
            throws IOException {
        Fixture fixture = new Fixture();
        List<String> calls = new ArrayList<>();
        UploadHandler handler = PasteFileHandler.session()
                .onStart(s -> calls
                        .add("start:" + s.pasteId() + "/" + s.totalFiles()))
                .onFile(f -> calls.add("file:" + f.pasteId() + "/"
                        + new String(f.bytes(), StandardCharsets.UTF_8)))
                .onComplete(c -> calls.add(
                        "complete:" + c.pasteId() + "/" + c.receivedFiles()))
                .build();

        // Two files of paste id 1, expected count 2 — onStart fires once,
        // onFile twice, onComplete once, in order.
        fixture.fire(handler, 1, 2, "A");
        fixture.fire(handler, 1, 2, "B");

        assertEquals(Arrays.asList("start:1/2", "file:1/A", "file:1/B",
                "complete:1/2"), calls);
    }

    @Test
    void session_overlappingPastes_eachLifecycleStaysOrdered()
            throws IOException {
        // Two pastes (2 files each) interleave on the wire:
        // paste 1 file A, paste 2 file X, paste 1 file B, paste 2 file Y.
        // The framework must keep each paste's own sequence
        // (start → file → file → complete) intact; cross-paste interleaving
        // of the *order between* events is allowed.
        Fixture fixture = new Fixture();
        List<String> calls = new ArrayList<>();
        UploadHandler handler = PasteFileHandler.session()
                .onStart(s -> calls.add("start:" + s.pasteId()))
                .onFile(f -> calls.add("file:" + f.pasteId() + "/"
                        + new String(f.bytes(), StandardCharsets.UTF_8)))
                .onComplete(c -> calls.add("complete:" + c.pasteId())).build();

        fixture.fire(handler, 1, 2, "A");
        fixture.fire(handler, 2, 2, "X");
        fixture.fire(handler, 1, 2, "B");
        fixture.fire(handler, 2, 2, "Y");

        // Per-paste lifecycle is preserved: paste 1 = start, A, B, complete;
        // paste 2 = start, X, Y, complete. Across pastes, paste 2 starts and
        // even completes before paste 1's last file lands, which is exactly
        // the "no cancellation" promise: paste 1 still finishes cleanly.
        assertEquals(
                Arrays.asList("start:1", "file:1/A", "start:2", "file:2/X",
                        "file:1/B", "complete:1", "file:2/Y", "complete:2"),
                calls);
    }

    @Test
    void session_newPasteFlag_trueOnlyForFirstFileOfEachPaste()
            throws IOException {
        // PasteFile.newPaste() is the inMemory variant's session boundary
        // signal — assert it for three files: first of paste 1 (true),
        // second of paste 1 (false), first of paste 2 (true).
        Fixture fixture = new Fixture();
        List<Boolean> flags = new ArrayList<>();
        UploadHandler handler = PasteFileHandler
                .inMemory(file -> flags.add(file.newPaste()));

        fixture.fire(handler, 1, 2, "A");
        fixture.fire(handler, 1, 2, "B");
        fixture.fire(handler, 2, 1, "X");

        assertEquals(Arrays.asList(true, false, true), flags);
    }

    /**
     * Test rig that constructs an {@link UploadEvent} backed by a mocked
     * {@link VaadinRequest} carrying paste-id and file-count headers, then runs
     * {@link UploadHandler#handleUploadRequest(UploadEvent)} on the test
     * thread. The MockUI is sub-classed so {@code access(...)} executes the
     * command synchronously — without that the session callbacks would queue
     * and the assertions would race the runner.
     */
    private static final class Fixture {

        private final UI ui;

        Fixture() {
            this.ui = new MockUI() {
                @Override
                public Future<Void> access(Command command) {
                    command.execute();
                    return CompletableFuture.completedFuture(null);
                }
            };
        }

        void fire(UploadHandler handler, long pasteId, int totalFiles,
                String body) throws IOException {
            VaadinRequest request = Mockito.mock(VaadinRequest.class);
            Mockito.when(request.getHeader(Clipboard.PASTE_ID_HEADER))
                    .thenReturn(Long.toString(pasteId));
            Mockito.when(request.getHeader(Clipboard.PASTE_FILE_COUNT_HEADER))
                    .thenReturn(Integer.toString(totalFiles));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            Mockito.when(request.getInputStream())
                    .thenReturn(new ByteArrayInputStream(bytes));

            UploadEvent event = new UploadEvent(request,
                    Mockito.mock(VaadinResponse.class),
                    ui.getSession() != null ? ui.getSession()
                            : VaadinSession.getCurrent(),
                    "file.txt", bytes.length, "text/plain", ui.getElement(),
                    null);
            handler.handleUploadRequest(event);
        }
    }
}
