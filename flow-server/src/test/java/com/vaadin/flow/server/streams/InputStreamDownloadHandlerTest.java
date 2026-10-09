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
package com.vaadin.flow.server.streams;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.ResponseWriterTest.CapturingServletOutputStream;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InputStreamDownloadHandlerTest {
    private static final int SIMULATED_DOWNLOAD_SIZE = 165000;

    private VaadinRequest request;
    private VaadinResponse response;
    private VaadinSession session;
    private VaadinService service;
    private DownloadEvent downloadEvent;
    private OutputStream outputStream;
    private Element owner;
    private UI ui;

    @BeforeEach
    void setUp() throws IOException {
        request = mock(VaadinRequest.class);
        response = mock(VaadinResponse.class);
        session = mock(VaadinSession.class);
        service = mock(VaadinService.class);

        ui = mock(UI.class);
        // run the command immediately
        doAnswer(invocation -> {
            Command command = invocation.getArgument(0);
            command.execute();
            return null;
        }).when(ui).access(any(Command.class));

        owner = mock(Element.class);
        Component componentOwner = mock(Component.class);
        when(owner.getComponent()).thenReturn(Optional.of(componentOwner));
        when(componentOwner.getUI()).thenReturn(Optional.of(ui));

        downloadEvent = new DownloadEvent(request, response, session, owner);
        outputStream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(outputStream);
        when(response.getService()).thenReturn(service);
        when(service.getMimeType(anyString()))
                .thenReturn("application/octet-stream");
    }

    @Test
    void transferProgressListener_addListener_listenersInvoked()
            throws URISyntaxException, IOException {
        List<String> invocations = new ArrayList<>();
        List<Long> transferredBytesRecords = new ArrayList<>();
        DownloadHandler handler = DownloadHandler.fromInputStream(request -> {
            byte[] data = getBytes();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            return new DownloadResponse(inputStream, "download",
                    "application/octet-stream", data.length);
        }, new TransferProgressListener() {
            @Override
            public void onStart(TransferContext context) {
                assertEquals(SIMULATED_DOWNLOAD_SIZE, context.contentLength());
                assertEquals("download", context.fileName());
                invocations.add("onStart");
            }

            @Override
            public void onProgress(TransferContext context,
                    long transferredBytes, long totalBytes) {
                transferredBytesRecords.add(transferredBytes);
                assertEquals(SIMULATED_DOWNLOAD_SIZE, totalBytes);
                assertEquals("download", context.fileName());
                invocations.add("onProgress");
            }

            @Override
            public void onComplete(TransferContext context,
                    long transferredBytes) {
                assertEquals(SIMULATED_DOWNLOAD_SIZE, context.contentLength());
                assertEquals(SIMULATED_DOWNLOAD_SIZE, transferredBytes);
                assertEquals("download", context.fileName());
                invocations.add("onComplete");
            }

            @Override
            public void onError(TransferContext context, IOException reason) {
                invocations.add("onError");
            }
        });

        handler.handleDownloadRequest(downloadEvent);

        // Two invocations with interval of 65536 bytes for total size 165000
        assertEquals(
                List.of("onStart", "onProgress", "onProgress", "onComplete"),
                invocations);
        assertArrayEquals(new long[] { 65536, 131072 }, transferredBytesRecords
                .stream().mapToLong(Long::longValue).toArray());
        verify(response).setContentType("application/octet-stream");
    }

    @Test
    void transferProgressListener_withFileNameOverride_listenersInvoked()
            throws URISyntaxException, IOException {
        List<String> invocations = new ArrayList<>();
        DownloadHandler handler = DownloadHandler.fromInputStream(request -> {
            byte[] data = getBytes();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            return new DownloadResponse(inputStream, request.getFileName(),
                    "application/octet-stream", data.length);
        }, "downloadOverride", new TransferProgressListener() {
            @Override
            public void onStart(TransferContext context) {
                assertEquals("downloadOverride", context.fileName());
                invocations.add("onStart");
            }

            @Override
            public void onProgress(TransferContext context,
                    long transferredBytes, long totalBytes) {
                assertEquals("downloadOverride", context.fileName());
                invocations.add("onProgress");
            }

            @Override
            public void onComplete(TransferContext context,
                    long transferredBytes) {
                assertEquals("downloadOverride", context.fileName());
                invocations.add("onComplete");
            }

            @Override
            public void onError(TransferContext context, IOException reason) {
                invocations.add("onError");
            }
        });

        handler.handleDownloadRequest(downloadEvent);

        assertEquals(
                List.of("onStart", "onProgress", "onProgress", "onComplete"),
                invocations);
        verify(response).setContentType("application/octet-stream");
    }

    @Test
    void transferProgressListener_addListener_errorOccurred_errorListenerInvoked()
            throws IOException {
        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getSession()).thenReturn(session);
        when(event.getResponse()).thenReturn(response);
        when(event.getOwningElement()).thenReturn(owner);
        when(event.getUI()).thenReturn(ui);
        OutputStream outputStreamMock = mock(OutputStream.class);
        doThrow(new IOException("I/O exception")).when(outputStreamMock)
                .write(any(byte[].class), anyInt(), anyInt());
        when(event.getOutputStream()).thenReturn(outputStreamMock);
        AtomicReference<Boolean> whenCompleteResult = new AtomicReference<>();
        InvocationTrackingTransferProgressListener transferListener = new InvocationTrackingTransferProgressListener();
        DownloadHandler handler = DownloadHandler.fromInputStream(req -> {
            byte[] data = getBytes();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            return new DownloadResponse(inputStream, "download",
                    "application/octet-stream", data.length);
        }, transferListener).whenComplete((context, success) -> {
            whenCompleteResult.set(success);
        });

        try {
            handler.handleDownloadRequest(event);
            fail("Expected an IOException to be thrown");
        } catch (Exception e) {
        }
        assertEquals(List.of("onStart", "onError"),
                transferListener.invocations);
        assertNotNull(whenCompleteResult.get(),
                "Expected whenComplete to be invoked, but was not");
        assertFalse(whenCompleteResult.get(),
                "Expected whenComplete to be invoked with false result, but got true");
        verify(event).setException(any(IOException.class));
    }

    @Test
    void transferProgressListener_addListener_callbackIOExceptionOccurred_errorListenerInvoked() {
        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getSession()).thenReturn(session);
        when(event.getResponse()).thenReturn(response);
        when(event.getOwningElement()).thenReturn(owner);
        when(event.getUI()).thenReturn(ui);
        OutputStream outputStreamMock = mock(OutputStream.class);
        when(event.getOutputStream()).thenReturn(outputStreamMock);

        AtomicReference<Boolean> whenCompleteResult = new AtomicReference<>();

        InvocationTrackingTransferProgressListener transferListener = new InvocationTrackingTransferProgressListener();
        DownloadHandler handler = DownloadHandler.fromInputStream(req -> {
            throw new IOException("I/O exception");
        }, transferListener).whenComplete((context, success) -> {
            whenCompleteResult.set(success);
        });

        try {
            handler.handleDownloadRequest(event);
            fail("Expected an IOException to be thrown");
        } catch (Exception e) {
        }
        assertEquals(List.of("onError"), transferListener.invocations);
        assertNotNull(whenCompleteResult.get(),
                "Expected whenComplete to be invoked, but was not");
        assertFalse(whenCompleteResult.get(),
                "Expected whenComplete to be invoked with false result, but got true");
        verify(event).setException(any(IOException.class));
    }

    @Test
    void transferProgressListener_addListener_callbackUncheckedExceptionOccurred_errorListenerInvoked() {
        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getSession()).thenReturn(session);
        when(event.getResponse()).thenReturn(response);
        when(event.getOwningElement()).thenReturn(owner);
        when(event.getUI()).thenReturn(ui);
        OutputStream outputStreamMock = mock(OutputStream.class);
        when(event.getOutputStream()).thenReturn(outputStreamMock);

        AtomicReference<Boolean> whenCompleteResult = new AtomicReference<>();

        InvocationTrackingTransferProgressListener transferListener = new InvocationTrackingTransferProgressListener();
        DownloadHandler handler = DownloadHandler.fromInputStream(req -> {
            throw new RuntimeException("I/O exception");
        }, transferListener).whenComplete((context, success) -> {
            whenCompleteResult.set(success);
        });

        try {
            handler.handleDownloadRequest(event);
            fail("Expected an exception to be thrown");
        } catch (Exception e) {
        }
        assertEquals(List.of("onError"), transferListener.invocations);
        assertNotNull(whenCompleteResult.get(),
                "Expected whenComplete to be invoked, but was not");
        assertFalse(whenCompleteResult.get(),
                "Expected whenComplete to be invoked with false result, but got true");
        verify(event).setException(any(RuntimeException.class));
    }

    @Test
    void transferProgressListener_addListener_callbackResponseError_errorListenerInvoked()
            throws IOException {
        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getSession()).thenReturn(session);
        when(event.getResponse()).thenReturn(response);
        when(event.getOwningElement()).thenReturn(owner);
        when(event.getUI()).thenReturn(ui);
        OutputStream outputStreamMock = mock(OutputStream.class);
        when(event.getOutputStream()).thenReturn(outputStreamMock);

        AtomicReference<Boolean> whenCompleteResult = new AtomicReference<>();

        InvocationTrackingTransferProgressListener transferListener = new InvocationTrackingTransferProgressListener();
        DownloadHandler handler = DownloadHandler.fromInputStream(
                req -> DownloadResponse.error(500, "I/O exception"),
                transferListener).whenComplete((context, success) -> {
                    whenCompleteResult.set(success);
                });

        handler.handleDownloadRequest(event);
        assertEquals(List.of("onError"), transferListener.invocations);
        assertNotNull(whenCompleteResult.get(),
                "Expected whenComplete to be invoked, but was not");
        assertFalse(whenCompleteResult.get(),
                "Expected whenComplete to be invoked with false result, but got true");
        verify(event).setException(any(IOException.class));
    }

    @Test
    void transferProgressListener_addListener_callbackResponseException_errorListenerInvoked()
            throws IOException {
        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getSession()).thenReturn(session);
        when(event.getResponse()).thenReturn(response);
        when(event.getOwningElement()).thenReturn(owner);
        when(event.getUI()).thenReturn(ui);
        OutputStream outputStreamMock = mock(OutputStream.class);
        when(event.getOutputStream()).thenReturn(outputStreamMock);

        AtomicReference<Boolean> whenCompleteResult = new AtomicReference<>();

        RuntimeException responseException = new RuntimeException(
                "runtime exception");
        InvocationTrackingTransferProgressListener transferListener = new InvocationTrackingTransferProgressListener() {
            @Override
            public void onError(TransferContext context, IOException reason) {
                invocations.add("onError");
                assertEquals("Runtime exception", reason.getMessage());
            }
        };
        DownloadHandler handler = DownloadHandler
                .fromInputStream(req -> DownloadResponse.error(500,
                        "Runtime exception", responseException),
                        transferListener)
                .whenComplete((context, success) -> {
                    whenCompleteResult.set(success);
                });

        handler.handleDownloadRequest(event);
        assertEquals(List.of("onError"), transferListener.invocations);
        assertNotNull(whenCompleteResult.get(),
                "Expected whenComplete to be invoked, but was not");
        assertFalse(whenCompleteResult.get(),
                "Expected whenComplete to be invoked with false result, but got true");
        verify(event).setException(responseException);
    }

    @Test
    void inline_setFileNameInvokedByDefault() throws IOException {
        DownloadEvent event = mock(DownloadEvent.class);
        DownloadHandler handler = DownloadHandler.fromInputStream(request -> {
            verify(event, times(0)).setFileName("my-download.bin");
            verify(event, times(0)).setContentType("application/octet-stream");
            byte[] data = getBytes();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            return new DownloadResponse(inputStream, "my-download.bin",
                    "application/octet-stream", data.length);
        });

        when(event.getSession()).thenReturn(session);
        when(event.getRequest()).thenReturn(request);
        when(event.getResponse()).thenReturn(response);
        when(event.getOwningElement()).thenReturn(owner);
        when(event.getOutputStream()).thenReturn(outputStream);
        when(event.getUI()).thenReturn(ui);
        when(response.getOutputStream()).thenReturn(outputStream);
        when(response.getService()).thenReturn(service);
        when(service.getMimeType(anyString()))
                .thenReturn("application/octet-stream");

        handler.handleDownloadRequest(event);

        verify(event).setFileName("my-download.bin");
        verify(event).setContentType("application/octet-stream");
    }

    @Test
    void attachment_doesNotSetFileNameWhenInlined() throws IOException {
        DownloadHandler handler = DownloadHandler.fromInputStream(request -> {
            byte[] data = getBytes();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            return new DownloadResponse(inputStream, "download",
                    "application/octet-stream", data.length);
        }).inline();

        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getSession()).thenReturn(session);
        when(event.getRequest()).thenReturn(request);
        when(event.getResponse()).thenReturn(response);
        when(event.getOwningElement()).thenReturn(owner);
        when(event.getOutputStream()).thenReturn(outputStream);
        when(event.getUI()).thenReturn(ui);
        when(response.getOutputStream()).thenReturn(outputStream);
        when(response.getService()).thenReturn(service);
        when(service.getMimeType(anyString()))
                .thenReturn("application/octet-stream");

        handler.handleDownloadRequest(event);

        verify(event, times(0)).setFileName("my-download.bin");
        verify(event).setContentType("application/octet-stream");
    }

    @Test
    void inputStreamDownloadCallback_doesNotRequireCatch() {
        new InputStreamDownloadHandler(event -> {
            try (InputStream inputStream = new ByteArrayInputStream(
                    getBytes())) {
                inputStream.readAllBytes();
                return null;
            }
        });
    }

    @Test
    void downloadResponseHasContentType_contentTypeUsed() throws IOException {
        String contentType = "custom";
        InputStreamDownloadHandler handler = new InputStreamDownloadHandler(
                event -> {
                    InputStream stream = mock(InputStream.class);
                    when(stream.read(any(), anyInt(), anyInt())).thenReturn(-1);
                    return new DownloadResponse(stream, "report.pdf",
                            contentType, 0);
                });

        DownloadEvent event = new DownloadEvent(request, response, session,
                new Element("div"));

        handler.handleDownloadRequest(event);

        verify(response).setContentType(contentType);
    }

    @Test
    void downloadResponseNullContentType_fileTypeIsUsed() throws IOException {
        String contentType = "file/pdf";

        when(service.getMimeType("report.pdf")).thenReturn(contentType);

        InputStreamDownloadHandler handler = new InputStreamDownloadHandler(
                event -> {
                    InputStream stream = mock(InputStream.class);
                    when(stream.read(any(), anyInt(), anyInt())).thenReturn(-1);
                    return new DownloadResponse(stream, "report.pdf", null, 0);
                });

        DownloadEvent event = new DownloadEvent(request, response, session,
                new Element("div"));

        handler.handleDownloadRequest(event);

        verify(response).setContentType(contentType);
    }

    @Test
    void handleSetToInline_contentDispositionIsInlineWithFilename()
            throws IOException {
        InputStream stream = mock(InputStream.class);
        when(stream.read(any(), anyInt(), anyInt())).thenReturn(-1);
        DownloadHandler handler = DownloadHandler
                .fromInputStream(event -> new DownloadResponse(stream,
                        "download", "application/octet-stream", 0))
                .inline();

        DownloadEvent event = new DownloadEvent(request, response, session,
                new Element("t"));
        when(response.getOutputStream()).thenReturn(outputStream);
        when(response.getService()).thenReturn(service);
        when(service.getMimeType(anyString()))
                .thenReturn("application/octet-stream");

        handler.handleDownloadRequest(event);

        verify(response).setHeader("Content-Disposition",
                "inline; filename=\"download\"");
    }

    @Test
    void handleSetToInline_contentDispositionIsInlineWithFilenameOverride()
            throws IOException {
        InputStream stream = mock(InputStream.class);
        when(stream.read(any(), anyInt(), anyInt())).thenReturn(-1);
        DownloadHandler handler = DownloadHandler
                .fromInputStream(event -> new DownloadResponse(stream,
                        "download", "application/octet-stream", 0),
                        "download_file")
                .inline();

        DownloadEvent event = new DownloadEvent(request, response, session,
                new Element("t"));
        when(response.getOutputStream()).thenReturn(outputStream);
        when(response.getService()).thenReturn(service);
        when(service.getMimeType(anyString()))
                .thenReturn("application/octet-stream");

        handler.handleDownloadRequest(event);

        verify(response).setHeader("Content-Disposition",
                "inline; filename=\"download_file\"");
    }

    @Test
    void contentLengthProvided_contentLengthHeaderSet() throws IOException {
        long expectedContentLength = 12345L;
        InputStream stream = mock(InputStream.class);
        when(stream.read(any(), anyInt(), anyInt())).thenReturn(-1);

        InputStreamDownloadHandler handler = new InputStreamDownloadHandler(
                event -> new DownloadResponse(stream, "report.pdf",
                        "application/pdf", expectedContentLength));

        DownloadEvent event = new DownloadEvent(request, response, session,
                new Element("div"));

        handler.handleDownloadRequest(event);

        verify(response).setContentLengthLong(expectedContentLength);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "null", value = {
            "\"v1\" | 6  | 206 | cdef", "null   | 6  | 200 | abcdef",
            "\"v1\" | -1 | 200 | abcdef" })
    void rangeRequested_servedOnlyWithETagAndLength(String eTag,
            long contentLength, int status, String body) throws IOException {
        VaadinServletResponse servletResponse = mock(
                VaadinServletResponse.class);
        CapturingServletOutputStream servletOutput = new CapturingServletOutputStream();
        when(servletResponse.getOutputStream()).thenReturn(servletOutput);
        VaadinServletService servletService = mock(VaadinServletService.class);
        when(servletResponse.getService()).thenReturn(servletService);
        when(request.getHeader("Range")).thenReturn("bytes=2-5");
        InputStreamDownloadHandler handler = DownloadHandler
                .fromInputStream(event -> {
                    DownloadResponse download = new DownloadResponse(
                            new ByteArrayInputStream(
                                    "abcdef".getBytes(StandardCharsets.UTF_8)),
                            "content.txt", null, contentLength);
                    download.setETag(eTag);
                    return download;
                });

        handler.handleDownloadRequest(
                new DownloadEvent(request, servletResponse, session, owner));

        assertEquals(body,
                new String(servletOutput.getOutput(), StandardCharsets.UTF_8));
        if (status == 206) {
            verify(servletResponse).setStatus(206);
            verify(servletResponse).setHeader("ETag", eTag);
        } else {
            verify(servletResponse, never()).setStatus(anyInt());
            verify(servletResponse, never()).setHeader(eq("Accept-Ranges"),
                    anyString());
        }
    }

    private static byte[] getBytes() {
        byte[] data = new byte[SIMULATED_DOWNLOAD_SIZE];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }
        return data;
    }

    private static class InvocationTrackingTransferProgressListener
            implements TransferProgressListener {
        final List<String> invocations = new ArrayList<>();

        @Override
        public void onStart(TransferContext context) {
            invocations.add("onStart");
        }

        @Override
        public void onProgress(TransferContext context, long transferredBytes,
                long totalBytes) {
            invocations.add("onProgress");
        }

        @Override
        public void onComplete(TransferContext context, long transferredBytes) {
            invocations.add("onComplete");
        }

        @Override
        public void onError(TransferContext context, IOException reason) {
            invocations.add("onError");
            assertEquals("I/O exception", reason.getMessage());
        }
    }

}
