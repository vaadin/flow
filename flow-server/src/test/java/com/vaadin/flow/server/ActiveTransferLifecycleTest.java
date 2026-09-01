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
package com.vaadin.flow.server;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.communication.StreamRequestHandler;
import com.vaadin.flow.server.communication.TransferUtil;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the lifecycle of a UI and its session while an upload or download
 * request for that UI is being served.
 */
@Isolated
class ActiveTransferLifecycleTest {

    @Tag("div")
    private static class TestComponent extends Component {
    }

    private final StreamRequestHandler streamRequestHandler = new StreamRequestHandler();

    private MockVaadinServletService service;
    private MockVaadinSession session;
    private StreamResourceRegistry registry;
    private UI ui;
    private int uiId;
    private TestComponent owner;

    private VaadinServletRequest request;
    private VaadinResponse response;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() throws IOException {
        service = new MockVaadinServletService();

        WrappedSession wrappedSession = Mockito.mock(WrappedSession.class);
        Mockito.when(wrappedSession.getId()).thenReturn("session-id");

        session = new AlwaysLockedVaadinSession(service) {
            @Override
            public StreamResourceRegistry getResourceRegistry() {
                return registry;
            }

            @Override
            public WrappedSession getSession() {
                return wrappedSession;
            }
        };
        registry = new StreamResourceRegistry(session);
        VaadinSession.setCurrent(session);

        request = Mockito.mock(VaadinServletRequest.class);
        response = Mockito.mock(VaadinResponse.class);
        Mockito.when(response.getService()).thenReturn(service);
        responseBody = new ByteArrayOutputStream();
        Mockito.when(response.getOutputStream())
                .thenReturn(new ServletOutputStream() {
                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(WriteListener listener) {
                    }

                    @Override
                    public void write(int b) {
                        responseBody.write(b);
                    }
                });

        ui = new MockUI(session);
        uiId = session.getNextUIid();
        ui.doInit(request, uiId, "app-id");
        session.addUI(ui);
        UI.setCurrent(ui);

        owner = new TestComponent();
        ui.add(owner);
    }

    @AfterEach
    void cleanup() {
        CurrentInstance.clearAll();
    }

    @Test
    void ongoingDownload_uiClosed_uiStaysAttachedUntilDownloadHasCompleted()
            throws IOException {
        byte[] contents = "Downloaded file contents"
                .getBytes(StandardCharsets.UTF_8);
        List<Boolean> attachedWhileDownloading = new ArrayList<>();

        DownloadHandler downloadHandler = event -> {
            // The browser tab is closed while the download is ongoing: the UI
            // is closed and the cleanup for the request that noticed it runs.
            ui.close();
            service.cleanupSession(session);

            attachedWhileDownloading.add(session.getUIById(uiId) != null);

            event.getOutputStream().write(contents);
        };

        handleRequest(downloadHandler);

        assertEquals(List.of(Boolean.TRUE), attachedWhileDownloading,
                "A closed UI should stay attached to the session while a download for it is ongoing");
        assertArrayEquals(contents, responseBody.toByteArray(),
                "The whole download should have been written to the response");

        // The download request has ended, so nothing keeps the UI alive
        service.cleanupSession(session);
        assertNull(session.getUIById(uiId),
                "A closed UI should be detached from the session once its download has completed");
    }

    @Test
    void ongoingUpload_uiClosed_uploadCompletionCallbackIsInvoked()
            throws IOException {
        List<File> uploadedFiles = new ArrayList<>();
        UploadHandler uploadHandler = UploadHandler
                .toTempFile((metadata, file) -> uploadedFiles.add(file));

        String contents = "Uploaded file contents";
        mockUploadRequest(contents, () -> {
            // The browser tab is closed while the upload is ongoing
            ui.close();
            service.cleanupSession(session);
        });

        try {
            handleRequest(uploadHandler);
            // The completion callback is run through UI.access, and the
            // pending access queue is not purged automatically for a session
            // that is permanently locked in this test
            service.runPendingAccessTasks(session);

            assertEquals(1, uploadedFiles.size(),
                    "The upload completion callback should be invoked for a UI that was closed while the upload was ongoing");
            assertEquals(contents,
                    Files.readString(uploadedFiles.get(0).toPath()),
                    "The whole upload should have been written to the file");
        } finally {
            uploadedFiles.forEach(File::delete);
        }
    }

    @Test
    void ongoingDownload_sessionInvalidated_downloadIsTerminated()
            throws IOException {
        // Three buffers worth of data, so that the transfer has to loop
        byte[] contents = new byte[3 * TransferUtil.DEFAULT_BUFFER_SIZE];
        InputStream contentStream = new ByteArrayInputStream(contents) {
            @Override
            public synchronized int read(byte[] b, int off, int len) {
                // The session is invalidated, e.g. due to a password reset,
                // while the download is ongoing
                invalidateSession();
                return super.read(b, off, len);
            }
        };

        DownloadHandler downloadHandler = DownloadHandler.fromInputStream(
                event -> new DownloadResponse(contentStream, "file.bin",
                        "application/octet-stream", contents.length));

        assertThrows(IOException.class, () -> handleRequest(downloadHandler),
                "A download should be terminated when the session is invalidated");
        assertTrue(responseBody.size() < contents.length,
                "A terminated download should not have written all of its contents");
    }

    private void invalidateSession() {
        service.fireSessionDestroy(session);
        // fireSessionDestroy uses VaadinSession.access, and the pending
        // access queue is not purged automatically for a session that is
        // permanently locked in this test
        service.runPendingAccessTasks(session);
    }

    private void handleRequest(
            com.vaadin.flow.server.streams.ElementRequestHandler handler)
            throws IOException {
        StreamRegistration registration = registry.registerResource(handler,
                owner.getElement());
        Mockito.when(request.getPathInfo())
                .thenReturn("/" + registration.getResourceUri().toString());

        streamRequestHandler.handleRequest(session, request, response);
    }

    private void mockUploadRequest(String contents, Runnable onFirstRead)
            throws IOException {
        Mockito.when(request.getMethod()).thenReturn("POST");
        Mockito.when(request.getHeader("X-Filename")).thenReturn("file.txt");
        Mockito.when(request.getContentLengthLong())
                .thenReturn((long) contents.length());
        Mockito.when(request.getInputStream())
                .thenReturn(uploadStream(contents, onFirstRead));
    }

    private static ServletInputStream uploadStream(String contents,
            Runnable onFirstRead) {
        InputStream delegate = new ByteArrayInputStream(
                contents.getBytes(StandardCharsets.UTF_8));
        return new ServletInputStream() {
            private boolean firstRead = true;

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public int read() throws IOException {
                if (firstRead) {
                    firstRead = false;
                    onFirstRead.run();
                }
                return delegate.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (firstRead) {
                    firstRead = false;
                    onFirstRead.run();
                }
                return delegate.read(b, off, len);
            }
        };
    }
}
