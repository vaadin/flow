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
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.internal.ResponseWriterTest.CapturingServletOutputStream;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.TransferUtil;
import com.vaadin.flow.shared.Registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractDownloadHandlerTest {
    private static final long TOTAL_BYTES = 100L;
    private static final long TRANSFERRED_BYTES = 42L;
    private static final IOException EXCEPTION = new IOException("Test error");

    private AbstractDownloadHandler<?> handler;
    private TransferContext mockContext;
    private TransferProgressListener listener;

    private VaadinRequest request;
    private VaadinResponse response;
    private VaadinSession session;
    private DownloadEvent downloadEvent;
    private ByteArrayOutputStream outputStream;
    private Element owner;
    private UI ui;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        request = mock(VaadinRequest.class);
        response = mock(VaadinResponse.class);
        session = mock(VaadinSession.class);

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

        handler = new AbstractDownloadHandler<>() {
            @Override
            public void handleDownloadRequest(DownloadEvent event) {
            }
        };

        mockContext = mock(TransferContext.class);
        when(mockContext.contentLength()).thenReturn(TOTAL_BYTES);
        listener = mock(TransferProgressListener.class);

        when(mockContext.owningElement()).thenReturn(owner);
        when(mockContext.getUI()).thenReturn(ui);

        outputStream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(outputStream);
    }

    @Test
    void addTransferProgressListener_listenerAdded_listenerInvoked_listenerRemoved_listenerNotInvoked() {
        Registration registration = handler
                .addTransferProgressListener(listener);
        handler.getListeners().forEach(l -> l.onStart(mockContext));
        verify(listener).onStart(mockContext);

        reset(listener);
        registration.remove();
        handler.getListeners().forEach(l -> l.onStart(mockContext));
        verify(listener, times(0)).onStart(mockContext);
    }

    @Test
    void whenStart_onStartCalled() {
        SerializableRunnable startHandler = mock(SerializableRunnable.class);
        handler.whenStart(startHandler);
        handler.getListeners()
                .forEach(listener -> listener.onStart(mockContext));
        verify(startHandler).run();
    }

    @Test
    void whenProgress_onProgressCalled() {
        SerializableBiConsumer<Long, Long> onProgressHandler = mock(
                SerializableBiConsumer.class);
        handler.onProgress(onProgressHandler);
        handler.getListeners().forEach(listener -> listener
                .onProgress(mockContext, TRANSFERRED_BYTES, TOTAL_BYTES));
        verify(onProgressHandler).accept(TRANSFERRED_BYTES, TOTAL_BYTES);
    }

    @Test
    void multipleHooks_multipleListenersAdded_InvokedInOrder() {
        List<String> executionOrder = new ArrayList<>();
        handler.whenStart(() -> executionOrder.add("first"));
        handler.whenStart(() -> executionOrder.add("second"));
        handler.getListeners()
                .forEach(listener -> listener.onStart(mockContext));
        List<String> expectedOrder = List.of("first", "second");
        assertEquals(expectedOrder, executionOrder);
    }

    @Test
    void whenComplete() {
        SerializableConsumer<Boolean> completeHandler = mock(
                SerializableConsumer.class);
        handler.whenComplete(completeHandler);
        handler.getListeners().forEach(listener -> {
            listener.onComplete(mockContext, TRANSFERRED_BYTES);
            listener.onError(mockContext, EXCEPTION);
        });
        verify(completeHandler).accept(true);
        verify(completeHandler).accept(false);
    }

    @Test
    void transferProgressListener_transfer_sessionNotLocked()
            throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(
                "Hello".getBytes(StandardCharsets.UTF_8));
        VaadinSession session = mock(VaadinSession.class);
        TransferContext context = mock(TransferContext.class);
        when(context.session()).thenReturn(session);
        OutputStream outputStream = mock(OutputStream.class);
        Collection<TransferProgressListener> listeners = new ArrayList<>();
        TransferUtil.transfer(inputStream, outputStream, context, listeners);
        verify(session, times(0)).lock();
    }

    @Test
    void customHandlerWithShorthandCompleteListener_noErrorInTransfer_success_errorInTransfer_failure()
            throws IOException {
        AtomicBoolean successAtomic = new AtomicBoolean(false);
        AbstractDownloadHandler customHandler = new AbstractDownloadHandler<>() {
            @Override
            public void handleDownloadRequest(DownloadEvent event) {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(
                        "Hello".getBytes(StandardCharsets.UTF_8));
                TransferContext context = getTransferContext(event);
                try {
                    TransferUtil.transfer(inputStream, event.getOutputStream(),
                            context, getListeners());
                } catch (IOException e) {
                    getListeners()
                            .forEach(listener -> listener.onError(context, e));
                }
            }
        }.whenComplete(success -> {
            successAtomic.set(success);
        });

        customHandler.handleDownloadRequest(downloadEvent);

        assertTrue(successAtomic.get());
        assertEquals("Hello", outputStream.toString(StandardCharsets.UTF_8));
        assertNull(downloadEvent.getException());

        OutputStream outputStreamError = mock(OutputStream.class);
        doThrow(new IOException("Test error")).when(outputStreamError)
                .write(any(byte[].class), anyInt(), anyInt());
        when(downloadEvent.getOutputStream()).thenReturn(outputStreamError);

        customHandler.handleDownloadRequest(downloadEvent);
        assertFalse(successAtomic.get());
        assertNull(downloadEvent.getException());
    }

    @Test
    void doesNotRequireToCatchIOException() {
        DownloadHandler handler = event -> {
            new FileInputStream(new File("foo"));
        };
    }

    @Test
    void inline_attachmentUsedByDefault() {
        assertFalse(handler.isInline());
    }

    @Test
    void inline_inlinedWhenExplicitlyCalled() {
        handler.inline();
        assertTrue(handler.isInline());
    }

    @Test
    void getTransferContext_returnsExpectedContextFromEvent() {
        VaadinRequest request = mock(VaadinRequest.class);
        VaadinResponse response = mock(VaadinResponse.class);
        VaadinSession session = mock(VaadinSession.class);
        Element owner = mock(Element.class);
        DownloadEvent event = new DownloadEvent(request, response, session,
                owner);
        event.setContentLength(1024);
        event.setFileName("test.txt");
        AbstractDownloadHandler<AbstractDownloadHandler> handler = new AbstractDownloadHandler<>() {
            @Override
            public void handleDownloadRequest(DownloadEvent event)
                    throws IOException {
            }
        };
        TransferContext context = handler.getTransferContext(event);
        assertEquals(owner, context.owningElement());
        assertEquals(session, context.session());
        assertEquals(request, context.request());
        assertEquals(response, context.response());
        assertEquals(1024, context.contentLength());
        assertEquals("test.txt", context.fileName());
        assertNull(event.getException());
    }

    @Test
    void whenStartWithContext_onStartCalled() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        handler.whenStart((context) -> invoked.set(true));
        handler.getListeners()
                .forEach(listener -> listener.onStart(mockContext));
        assertTrue(invoked.get(), "Start with context should be invoked");
    }

    @Test
    void whenProgressWithContext_onProgressCalled() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        handler.onProgress((context, current, total) -> invoked.set(true),
                1024);
        handler.getListeners().forEach(listener -> listener
                .onProgress(mockContext, TRANSFERRED_BYTES, TOTAL_BYTES));
        assertTrue(invoked.get(), "Progress with context should be invoked");
    }

    @Test
    void whenProgressWithContextNoInterval_onProgressCalled() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        handler.onProgress((context, current, total) -> invoked.set(true));
        handler.getListeners().forEach(listener -> listener
                .onProgress(mockContext, TRANSFERRED_BYTES, TOTAL_BYTES));
        assertTrue(invoked.get(),
                "Progress with context and interval should be invoked");
    }

    @Test
    void whenCompleteWithContext() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        handler.whenComplete((context, success) -> invoked.set(true));
        handler.getListeners().forEach(listener -> {
            listener.onComplete(mockContext, TRANSFERRED_BYTES);
            listener.onError(mockContext, EXCEPTION);
        });
        assertTrue(invoked.get(), "Progress with context should be invoked");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "null", value = {
            "null      | null | 200 | abcdefghij | null         | true",
            "bytes=2-5 | null | 206 | cdef       | bytes 2-5/10 | false",
            "bytes=0-  | null | 206 | abcdefghij | bytes 0-9/10 | true",
            "bytes=abc | null | 200 | abcdefghij | null         | true",
            "bytes=2-5 | x    | 200 | abcdefghij | null         | true",
            "bytes=2-5 | etag | 206 | cdef       | bytes 2-5/10 | false" })
    void transferContent_file_rangeAnswered(String range, String ifRange,
            int status, String body, String contentRange,
            boolean listenersNotified) throws IOException {
        Path file = Files.writeString(tempDir.resolve("content.txt"),
                "abcdefghij");
        Files.setLastModifiedTime(file, FileTime.fromMillis(0x1234567L));
        String etag = "\"1234567-a\"";
        VaadinServletResponse servletResponse = mock(
                VaadinServletResponse.class);
        CapturingServletOutputStream servletOutput = new CapturingServletOutputStream();
        when(servletResponse.getOutputStream()).thenReturn(servletOutput);
        when(request.getHeader("Range")).thenReturn(range);
        when(request.getHeader("If-Range"))
                .thenReturn("etag".equals(ifRange) ? etag : ifRange);
        handler.addTransferProgressListener(listener);

        try (InputStream inputStream = Files.newInputStream(file)) {
            handler.transferContent(
                    new DownloadEvent(request, servletResponse, session, owner),
                    inputStream, servletOutput, 10, file.toFile());
        }

        assertEquals(body,
                new String(servletOutput.getOutput(), StandardCharsets.UTF_8));
        verify(servletResponse, atLeastOnce()).setHeader("Accept-Ranges",
                "bytes");
        verify(servletResponse).setHeader("ETag", etag);
        if (status == 200) {
            verify(servletResponse, never()).setStatus(anyInt());
            verify(servletResponse, never()).setHeader(eq("Content-Range"),
                    anyString());
        } else {
            verify(servletResponse).setStatus(status);
            verify(servletResponse).setHeader("Content-Range", contentRange);
        }
        if (listenersNotified) {
            verify(listener).onStart(any());
            verify(listener).onComplete(any(), eq(10L));
        } else {
            verify(listener, never()).onStart(any());
            verify(listener, never()).onComplete(any(), anyLong());
        }
    }

    @ParameterizedTest
    @CsvSource({ "bytes=15-19, 10, 20, false", "bytes=5-14, 10, 20, false",
            "bytes=1-49999, 40000, 50000, true" })
    void transferContent_fileShorterThanLength_failsWithoutRangeHeaders(
            String range, int actualLength, long declaredLength,
            boolean failsMidBody) throws IOException {
        Path file = Files.write(tempDir.resolve("content.bin"),
                new byte[actualLength]);
        VaadinServletResponse servletResponse = mock(
                VaadinServletResponse.class);
        when(servletResponse.getOutputStream())
                .thenReturn(new CapturingServletOutputStream());
        when(request.getHeader("Range")).thenReturn(range);

        try (InputStream inputStream = new FileInputStream(file.toFile())) {
            IOException failure = assertThrows(IOException.class,
                    () -> handler.transferContent(
                            new DownloadEvent(request, servletResponse, session,
                                    owner),
                            inputStream, outputStream, declaredLength,
                            file.toFile()));
            assertTrue(failure.getCause() instanceof EOFException);
        }
        // only a failure after the first chunk has set the 206 status
        verify(servletResponse, failsMidBody ? times(1) : never())
                .setStatus(206);
        // set before the handler closes the stream, keeping other headers
        verify(servletResponse).setStatus(500);
        verify(servletResponse).setHeader("Content-Range", null);
        verify(servletResponse).setContentLengthLong(0);
        verify(servletResponse, never()).reset();
    }

    @ParameterizedTest
    @CsvSource({ "bytes=10-99999, 50000", "bytes=10-19, 0" })
    void transferContent_readFailsAfterCommit_propagated(String range,
            long failAfter) throws IOException {
        Path file = Files.write(tempDir.resolve("content.bin"),
                new byte[100000]);
        VaadinServletResponse servletResponse = mock(
                VaadinServletResponse.class);
        when(servletResponse.getOutputStream())
                .thenReturn(new CapturingServletOutputStream());
        when(servletResponse.isCommitted()).thenReturn(true);
        when(request.getHeader("Range")).thenReturn(range);
        handler.addTransferProgressListener(listener);

        try (InputStream inputStream = new FileInputStream(file.toFile()) {
            private long read;

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (read >= failAfter) {
                    throw new IOException("Disk read failed");
                }
                int count = super.read(b, off, len);
                read += count;
                return count;
            }
        }) {
            AbstractDownloadHandler.RangeRequestException failure = assertThrows(
                    AbstractDownloadHandler.RangeRequestException.class,
                    () -> handler.transferContent(
                            new DownloadEvent(request, servletResponse, session,
                                    owner),
                            inputStream, outputStream, 100000, file.toFile()));
            // propagated as a failure, not mistaken for a cancel
            assertFalse(failure.isCancelled());
        }
        verify(servletResponse, never()).setStatus(500);
        verify(listener, never()).onComplete(any(), anyLong());
    }

    @ParameterizedTest
    @CsvSource({ "bytes=10-99999", "bytes=10-19", "bytes=0-" })
    void transferContent_rangeCancelledByClient_notAnErrorOfTheContent(
            String range) throws IOException {
        Path file = Files.write(tempDir.resolve("content.bin"),
                new byte[100000]);
        VaadinServletResponse servletResponse = mock(
                VaadinServletResponse.class);
        CapturingServletOutputStream servletOutput = new CapturingServletOutputStream() {
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                if (len > 10) {
                    throw new IOException("Connection reset by peer");
                }
                super.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                // a small range only fails when the buffered body is sent
                throw new IOException("Connection reset by peer");
            }
        };
        when(servletResponse.getOutputStream()).thenReturn(servletOutput);
        when(servletResponse.isCommitted()).thenReturn(true);
        when(request.getHeader("Range")).thenReturn(range);
        handler.addTransferProgressListener(listener);

        try (InputStream inputStream = new FileInputStream(file.toFile())) {
            assertThrows(AbstractDownloadHandler.RangeRequestException.class,
                    () -> handler.transferContent(
                            new DownloadEvent(request, servletResponse, session,
                                    owner),
                            inputStream, servletOutput, 100000, file.toFile()));
        }

        verify(listener, never()).onComplete(any(), anyLong());
        if ("bytes=0-".equals(range)) {
            // reported as started, so it ends like a cancelled download
            verify(listener).onStart(any());
            verify(listener).onError(any(), any());
        } else {
            verify(listener, never()).onStart(any());
            verify(listener, never()).onError(any(), any());
        }
        verify(servletResponse, never()).setStatus(500);
    }

    @Test
    void transferContent_notAFile_rangeIgnored() throws IOException {
        when(request.getHeader("Range")).thenReturn("bytes=2-5");

        handler.transferContent(downloadEvent,
                new ByteArrayInputStream(
                        "abcdefghij".getBytes(StandardCharsets.UTF_8)),
                outputStream, 10, null);

        assertEquals("abcdefghij",
                outputStream.toString(StandardCharsets.UTF_8));
        verify(response, never()).setHeader(eq("Accept-Ranges"), anyString());
        verify(response, never()).setStatus(anyInt());
    }
}
