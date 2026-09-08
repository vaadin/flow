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
package com.vaadin.flow.server.communication;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.ElementRequestHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;
import com.vaadin.tests.util.TestServletStreams;

import static com.vaadin.flow.server.communication.StreamRequestHandler.DYN_RES_PREFIX;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Isolated
class StreamRequestHandlerTest {

    private StreamRequestHandler handler = new StreamRequestHandler();
    private MockVaadinServletService service;
    private MockVaadinSession session;
    private VaadinServletRequest request;
    private VaadinResponse response;
    private StreamResourceRegistry streamResourceRegistry;
    private UI ui;

    /*
     * Used by the tests that cover the lifecycle of a UI and its session while
     * a transfer is being served. Those need a UI that is added to the session
     * and servlet based request and response instances, since a transfer is
     * terminated through the streams that those hand out.
     */
    private int uiId;
    private HttpServletRequest httpRequest;
    private VaadinServletRequest servletRequest;
    private VaadinServletResponse servletResponse;
    private ByteArrayOutputStream responseBody;

    @BeforeEach
    void setUp() throws ServletException, ServiceException {
        service = new MockVaadinServletService();

        WrappedSession wrappedSession = Mockito.mock(WrappedSession.class);
        Mockito.when(wrappedSession.getId()).thenReturn("session-id");

        session = new AlwaysLockedVaadinSession(service) {
            @Override
            public StreamResourceRegistry getResourceRegistry() {
                return streamResourceRegistry;
            }

            @Override
            public WrappedSession getSession() {
                return wrappedSession;
            }
        };
        streamResourceRegistry = new StreamResourceRegistry(session);
        request = Mockito.mock(VaadinServletRequest.class);
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getMimeType(Mockito.anyString()))
                .thenReturn(null);
        Mockito.when(request.getServletContext()).thenReturn(servletContext);
        response = Mockito.mock(VaadinResponse.class);
        ui = new MockUI(session);
        UI.setCurrent(ui);
    }

    @AfterEach
    void cleanup() {
        CurrentInstance.clearAll();
    }

    @Test
    void streamResourceNameEndsWithPluses_streamFactory_resourceIsStreamed()
            throws IOException {
        testStreamResourceInputStreamFactory("end with multiple pluses",
                "readme++.md");
    }

    @Test
    void streamResourceNameEndsWithPluses_resourceWriter_resourceIsStreamed()
            throws IOException {
        testStreamResourceStreamResourceWriter("end with multiple pluses",
                "readme++.md");
    }

    @Test
    void streamResourceNameContainsSpaceEndsWithPluses_streamFactory_resourceIsStreamed()
            throws IOException {
        testStreamResourceInputStreamFactory(
                "end with space and multiple pluses", "readme ++.md");
    }

    @Test
    void streamResourceNameContainsSpaceEndsWithPluses_resourceWriter_resourceIsStreamed()
            throws IOException {
        testStreamResourceStreamResourceWriter(
                "end with space and multiple pluses", "readme ++.md");
    }

    @Test
    void streamResourceNameEndsInPlus_streamFactory_resourceIsStreamed()
            throws IOException {
        testStreamResourceInputStreamFactory("end in plus", "readme+.md");
    }

    @Test
    void streamResourceNameEndsInPlus_resourceWriter_resourceIsStreamed()
            throws IOException {
        testStreamResourceStreamResourceWriter("end in plus", "readme+.md");
    }

    @Test
    void streamResourceNameContainsPlus_streamFactory_resourceIsStreamed()
            throws IOException {
        testStreamResourceInputStreamFactory("plus in middle",
                "readme+mine.md");
    }

    @Test
    void streamResourceNameContainsPlus_resourceWriter_resourceIsStreamed()
            throws IOException {
        testStreamResourceStreamResourceWriter("plus in middle",
                "readme+mine.md");
    }

    @Test
    void streamResourceNameContainsPlusAndSpaces_streamFactory_resourceIsStreamed()
            throws IOException {
        testStreamResourceInputStreamFactory("plus surrounded by spaces",
                "readme + mine.md");
    }

    @Test
    void streamResourceNameContainsPlusAndSpaces_resourceWriter_resourceIsStreamed()
            throws IOException {
        testStreamResourceStreamResourceWriter("plus surrounded by spaces",
                "readme + mine.md");
    }

    @Test
    public void streamResourceNameContainsPercent_streamFactory_resourceIsStreamed()
            throws IOException {
        testStreamResourceInputStreamFactory("percent in name", "file%.txt");
    }

    @Test
    public void streamResourceNameContainsPercent_resourceWriter_resourceIsStreamed()
            throws IOException {
        testStreamResourceStreamResourceWriter("percent in name", "file%.txt");
    }

    @Test
    void stateNodeStates_handlerMustNotReplyWhenNodeDisabled()
            throws IOException {
        stateNodeStatesTestInternal(false, true);
        Mockito.verify(response).sendError(403, "Resource not available");
    }

    @Test
    void nodeDisabled_shouldReplyForDisabledUpdateModeAlways()
            throws IOException {
        TestElementHandlerBuilder builder = new TestElementHandlerBuilder()
                .withDisabledUpdateMode(DisabledUpdateMode.ALWAYS);
        stateNodeStatesTestInternal(builder);
        Mockito.verify(response, Mockito.never()).sendError(Mockito.anyInt(),
                Mockito.anyString());
    }

    @Test
    void nodeInert_shouldRespondWithResourceNotAvailable() throws IOException {
        TestElementHandlerBuilder builder = new TestElementHandlerBuilder()
                .withInert(true);
        stateNodeStatesTestInternal(builder);
        Mockito.verify(response).sendError(403, "Resource not available");
    }

    @Test
    void nodeInert_handlerShouldReplyForAllowInert() throws IOException {
        TestElementHandlerBuilder builder = new TestElementHandlerBuilder()
                .withInert(true).withAllowInert(true);
        stateNodeStatesTestInternal(builder);
        Mockito.verify(response, Mockito.never()).sendError(Mockito.anyInt(),
                Mockito.anyString());
    }

    @Test
    void nodeHidden_shouldRespondWithResourceNotAvailable() throws IOException {
        TestElementHandlerBuilder builder = new TestElementHandlerBuilder()
                .withVisible(false);
        stateNodeStatesTestInternal(builder);
        Mockito.verify(response).sendError(403, "Resource not available");
    }

    @Test
    void stateNodeStates_handlerMustNotReplyWhenNodeDetached()
            throws IOException {
        stateNodeStatesTestInternal(true, false);
        Mockito.verify(response).sendError(403, "Resource not available");
    }

    @Test
    void stateNodeStates_handlerMustReplyWhenNodeAttachedAndEnabled()
            throws IOException {
        stateNodeStatesTestInternal(true, true);
        Mockito.verify(response, Mockito.never()).sendError(Mockito.anyInt(),
                Mockito.anyString());
    }

    private VaadinResponse stateNodeStatesTestInternal(boolean enabled,
            boolean attached) throws IOException {
        TestElementHandlerBuilder builder = new TestElementHandlerBuilder()
                .withEnalbed(enabled).withAttached(attached);
        return stateNodeStatesTestInternal(builder);
    }

    private VaadinResponse stateNodeStatesTestInternal(
            TestElementHandlerBuilder builder) throws IOException {
        ElementRequestHandler stateHandler = builder
                .buildElementRequestHandler();
        TestStateNodeProperties testStateNodeProperties = builder
                .buildStateNodeProperties();

        Element owner = Mockito.mock(Element.class);
        StateNode stateNode = Mockito.mock(StateNode.class);
        Mockito.when(owner.getNode()).thenReturn(stateNode);

        Mockito.when(stateNode.isEnabled())
                .thenReturn(testStateNodeProperties.enabled);
        Mockito.when(stateNode.isAttached())
                .thenReturn(testStateNodeProperties.attached);
        Mockito.when(stateNode.isVisible())
                .thenReturn(testStateNodeProperties.visible);
        Mockito.when(stateNode.isInert())
                .thenReturn(testStateNodeProperties.inert);

        StreamResourceRegistry.ElementStreamResource res = new StreamResourceRegistry.ElementStreamResource(
                stateHandler, owner);

        streamResourceRegistry.registerResource(res);

        ServletOutputStream outputStream = Mockito
                .mock(ServletOutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);
        Mockito.when(request.getPathInfo()).thenReturn(String.format(
                "/%s%s/%s/%s", DYN_RES_PREFIX, ui.getId().orElse("-1"),
                res.getId(),
                StreamRequestHandler.sanitizeNameForUrl(res.getName())));

        handler.handleRequest(session, request, response);

        return response;
    }

    private void testStreamResourceInputStreamFactory(String testString,
            String fileName) throws IOException {

        final byte[] testBytes = testString.getBytes();
        StreamResource res = new StreamResource(fileName,
                () -> new ByteArrayInputStream(testBytes));

        streamResourceRegistry.registerResource(res);

        ServletOutputStream outputStream = Mockito
                .mock(ServletOutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);
        Mockito.when(request.getPathInfo()).thenReturn(String.format(
                "/%s%s/%s/%s", DYN_RES_PREFIX, ui.getId().orElse("-1"),
                res.getId(),
                StreamRequestHandler.sanitizeNameForUrl(res.getName())));

        handler.handleRequest(session, request, response);

        Mockito.verify(response).getOutputStream();

        ArgumentCaptor<byte[]> argument = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(outputStream).write(argument.capture(), Mockito.anyInt(),
                Mockito.anyInt());

        byte[] buf = new byte[1024];
        for (int i = 0; i < testBytes.length; i++) {
            buf[i] = testBytes[i];
        }
        assertArrayEquals(buf, argument.getValue(),
                "Output differed from expected");
        Mockito.verify(response).setCacheTime(Mockito.anyLong());
        Mockito.verify(response).setContentType("application/octet-stream");
    }

    private void testStreamResourceStreamResourceWriter(String testString,
            String fileName) throws IOException {

        final byte[] testBytes = testString.getBytes();
        StreamResource res = new StreamResource(fileName,
                (stream, session) -> stream.write(testBytes));

        streamResourceRegistry.registerResource(res);

        ServletOutputStream outputStream = Mockito
                .mock(ServletOutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);
        Mockito.when(request.getPathInfo()).thenReturn(String.format(
                "/%s%s/%s/%s", DYN_RES_PREFIX, ui.getId().orElse("-1"),
                res.getId(),
                StreamRequestHandler.sanitizeNameForUrl(res.getName())));

        handler.handleRequest(session, request, response);

        Mockito.verify(response).getOutputStream();

        ArgumentCaptor<byte[]> argument = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(outputStream).write(argument.capture());

        assertArrayEquals(testBytes, argument.getValue(),
                "Output differed from expected");
        Mockito.verify(response).setCacheTime(Mockito.anyLong());
        Mockito.verify(response).setContentType("application/octet-stream");
    }

    @Test
    public void sanitizeNameForUrl_percentIsReplaced() {
        assertEquals("file_.txt",
                StreamRequestHandler.sanitizeNameForUrl("file%.txt"));
    }

    @Test
    public void sanitizeNameForUrl_nullReturnsNull() {
        assertNull(StreamRequestHandler.sanitizeNameForUrl(null));
    }

    @Test
    public void sanitizeNameForUrl_emptyReturnsEmpty() {
        assertEquals("", StreamRequestHandler.sanitizeNameForUrl(""));
    }

    private static final class TestElementHandlerBuilder {
        private final TestElementHandlerProperties elementHandlerProperties;
        private final TestStateNodeProperties stateNodeProperties;

        public TestElementHandlerBuilder() {
            this.elementHandlerProperties = new TestElementHandlerProperties(
                    DisabledUpdateMode.ONLY_WHEN_ENABLED, false);
            this.stateNodeProperties = new TestStateNodeProperties(true, true,
                    false, true);
        }

        public TestElementHandlerBuilder withEnalbed(boolean enabled) {
            stateNodeProperties.enabled = enabled;
            return this;
        }

        public TestElementHandlerBuilder withAttached(boolean attached) {
            stateNodeProperties.attached = attached;
            return this;
        }

        public TestElementHandlerBuilder withInert(boolean inert) {
            stateNodeProperties.inert = inert;
            return this;
        }

        public TestElementHandlerBuilder withVisible(boolean visible) {
            stateNodeProperties.visible = visible;
            return this;
        }

        public TestElementHandlerBuilder withDisabledUpdateMode(
                DisabledUpdateMode disabledUpdateMode) {
            elementHandlerProperties.disabledUpdateMode = disabledUpdateMode;
            return this;
        }

        public TestElementHandlerBuilder withAllowInert(boolean allowInert) {
            elementHandlerProperties.allowInert = allowInert;
            return this;
        }

        public TestStateNodeProperties buildStateNodeProperties() {
            return new TestStateNodeProperties(stateNodeProperties.enabled,
                    stateNodeProperties.attached, stateNodeProperties.inert,
                    stateNodeProperties.visible);
        }

        public ElementRequestHandler buildElementRequestHandler() {
            return new ElementRequestHandler() {
                @Override
                public void handleRequest(VaadinRequest request,
                        VaadinResponse response, VaadinSession session,
                        Element owner) {
                    // Handle the request
                }

                @Override
                public boolean isAllowInert() {
                    return elementHandlerProperties.allowInert;
                }

                @Override
                public DisabledUpdateMode getDisabledUpdateMode() {
                    return elementHandlerProperties.disabledUpdateMode;
                }
            };
        }
    }

    private static final class TestStateNodeProperties {
        private boolean enabled;
        private boolean attached;
        private boolean inert;
        private boolean visible;

        private TestStateNodeProperties(boolean enabled, boolean attached,
                boolean inert, boolean visible) {
            this.enabled = enabled;
            this.attached = attached;
            this.inert = inert;
            this.visible = visible;
        }
    }

    private static final class TestElementHandlerProperties {
        private DisabledUpdateMode disabledUpdateMode;
        private boolean allowInert;

        private TestElementHandlerProperties(
                DisabledUpdateMode disabledUpdateMode, boolean allowInert) {
            this.disabledUpdateMode = disabledUpdateMode;
            this.allowInert = allowInert;
        }
    }

    @Test
    void ongoingDownload_uiClosed_uiStaysAttachedUntilDownloadHasCompleted()
            throws IOException {
        Element owner = initTransferFixture();
        byte[] contents = "Downloaded file contents"
                .getBytes(StandardCharsets.UTF_8);
        List<Boolean> attachedWhileDownloading = new ArrayList<>();

        DownloadHandler downloadHandler = event -> {
            // The browser tab is closed while the download is ongoing: the UI
            // is closed and the cleanup for the request that noticed it runs.
            ui.close();
            service.runSessionCleanup(session);

            attachedWhileDownloading.add(session.getUIById(uiId) != null);

            event.getOutputStream().write(contents);
        };

        handleTransferRequest(downloadHandler, owner);

        assertEquals(List.of(Boolean.TRUE), attachedWhileDownloading,
                "A closed UI should stay attached to the session while a download for it is ongoing");
        assertArrayEquals(contents, responseBody.toByteArray(),
                "The whole download should have been written to the response");

        // The download request has ended, so nothing keeps the UI alive
        service.runSessionCleanup(session);
        assertNull(session.getUIById(uiId),
                "A closed UI should be detached from the session once its download has completed");
    }

    @Test
    void ongoingUpload_uiClosed_uploadCompletionCallbackIsInvoked()
            throws IOException {
        Element owner = initTransferFixture();
        List<File> uploadedFiles = new ArrayList<>();
        UploadHandler uploadHandler = UploadHandler
                .toTempFile((metadata, file) -> uploadedFiles.add(file));

        String contents = "Uploaded file contents";
        mockUploadRequest(contents, () -> {
            // The browser tab is closed while the upload is ongoing
            ui.close();
            service.runSessionCleanup(session);
        });

        try {
            handleTransferRequest(uploadHandler, owner);
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
        Element owner = initTransferFixture();
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

        // The progress listener is notified through the UI that the download
        // was started with, which is detached along with the session. That must
        // not hide the reason the download failed.
        DownloadHandler downloadHandler = DownloadHandler
                .fromInputStream(
                        event -> new DownloadResponse(contentStream, "file.bin",
                                "application/octet-stream", contents.length))
                .onProgress((transferred, total) -> {
                });

        assertThrows(IOException.class,
                () -> handleTransferRequest(downloadHandler, owner),
                "A download should be terminated when the session is invalidated");
        assertTrue(responseBody.size() < contents.length,
                "A terminated download should not have written all of its contents");
    }

    @Test
    void ongoingDownload_customHandlerWritingToResponse_sessionInvalidatedTerminatesIt()
            throws IOException {
        Element transferOwner = initTransferFixture();
        byte[] chunk = new byte[1024];
        int chunkCount = 10;

        // A handler that implements the interface directly and writes to the
        // response instead of using TransferUtil
        ElementRequestHandler rawHandler = (request, response, session,
                owner) -> {
            OutputStream outputStream = response.getOutputStream();
            outputStream.write(chunk);

            invalidateSession();

            for (int i = 1; i < chunkCount; i++) {
                outputStream.write(chunk);
            }
        };

        assertThrows(IOException.class,
                () -> handleTransferRequest(rawHandler, transferOwner),
                "A download writing to the response should be terminated when the session is invalidated");
        assertTrue(responseBody.size() < chunkCount * chunk.length,
                "A terminated download should not have written all of its contents");
    }

    /**
     * Adds an initialized UI with an owner component to the session and creates
     * the servlet based request and response that a transfer for that owner is
     * served with.
     *
     * @return the owner element to scope a request handler to
     */
    private Element initTransferFixture() throws IOException {
        httpRequest = mock(HttpServletRequest.class);
        servletRequest = new VaadinServletRequest(httpRequest, service);

        responseBody = new ByteArrayOutputStream();
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        when(httpResponse.getOutputStream())
                .thenReturn(TestServletStreams.outputStream(responseBody));
        servletResponse = new VaadinServletResponse(httpResponse, service);

        uiId = session.getNextUIid();
        ui.doInit(servletRequest, uiId, "app-id");
        session.addUI(ui);

        TransferOwnerComponent owner = new TransferOwnerComponent();
        ui.add(owner);
        return owner.getElement();
    }

    private void handleTransferRequest(ElementRequestHandler requestHandler,
            Element owner) throws IOException {
        StreamRegistration registration = streamResourceRegistry
                .registerResource(requestHandler, owner);
        when(httpRequest.getPathInfo())
                .thenReturn("/" + registration.getResourceUri().toString());

        handler.handleRequest(session, servletRequest, servletResponse);
    }

    private void invalidateSession() {
        service.fireSessionDestroy(session);
        // fireSessionDestroy uses VaadinSession.access, and the pending access
        // queue is not purged automatically for a session that is permanently
        // locked in this test
        service.runPendingAccessTasks(session);
    }

    private void mockUploadRequest(String contents, Runnable onFirstRead)
            throws IOException {
        when(httpRequest.getMethod()).thenReturn("POST");
        when(httpRequest.getHeader("X-Filename")).thenReturn("file.txt");
        when(httpRequest.getContentLengthLong())
                .thenReturn((long) contents.length());
        when(httpRequest.getInputStream()).thenReturn(TestServletStreams
                .inputStream(contents.getBytes(StandardCharsets.UTF_8),
                        onFirstRead));
    }

    @Tag("div")
    private static class TransferOwnerComponent extends Component {
    }

}
