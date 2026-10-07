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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.TransferUtil;
import com.vaadin.flow.shared.Registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
            "null          | null | 200 | abcdefghij | null",
            "bytes=2-5     | null | 206 | cdef       | bytes 2-5/10",
            "bytes=7-      | null | 206 | hij        | bytes 7-9/10",
            "bytes=-3      | null | 206 | hij        | bytes 7-9/10",
            "bytes=5-99    | null | 206 | fghij      | bytes 5-9/10",
            "bytes=0-3,2-5 | null | 206 | abcdef     | bytes 0-5/10",
            "bytes=10-     | null | 416 | ''         | bytes */10",
            "bytes=-0      | null | 416 | ''         | bytes */10",
            "bytes=5-2     | null | 416 | ''         | bytes */10",
            "bytes=0-1,4-5 | null | 200 | abcdefghij | null",
            "items=2-5     | null | 200 | abcdefghij | null",
            "bytes=2-5     | x    | 200 | abcdefghij | null" })
    void transferContent_rangeRequested_sendsRequestedBytes(String range,
            String ifRange, int status, String body, String contentRange)
            throws IOException {
        when(request.getHeader("Range")).thenReturn(range);
        when(request.getHeader("If-Range")).thenReturn(ifRange);

        handler.transferContent(downloadEvent,
                new ByteArrayInputStream(
                        "abcdefghij".getBytes(StandardCharsets.UTF_8)),
                outputStream, 10);

        assertEquals(body, outputStream.toString(StandardCharsets.UTF_8));
        verify(response).setHeader("Accept-Ranges", "bytes");
        verify(response).setContentLengthLong(body.length());
        if (status == 200) {
            verify(response, never()).setStatus(anyInt());
        } else {
            verify(response).setStatus(status);
        }
        if (contentRange == null) {
            verify(response, never()).setHeader(eq("Content-Range"),
                    anyString());
        } else {
            verify(response).setHeader("Content-Range", contentRange);
        }
    }

    @Test
    void transferContent_unknownLength_rangeIgnored() throws IOException {
        when(request.getHeader("Range")).thenReturn("bytes=2-5");

        handler.transferContent(downloadEvent,
                new ByteArrayInputStream(
                        "abcdefghij".getBytes(StandardCharsets.UTF_8)),
                outputStream, -1);

        assertEquals("abcdefghij",
                outputStream.toString(StandardCharsets.UTF_8));
        verify(response, never()).setHeader(eq("Accept-Ranges"), anyString());
        verify(response, never()).setStatus(anyInt());
    }
}
