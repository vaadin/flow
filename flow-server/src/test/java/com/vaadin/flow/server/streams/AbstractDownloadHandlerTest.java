/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

public class AbstractDownloadHandlerTest {
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

    @Before
    public void setUp() throws IOException {
        request = Mockito.mock(VaadinRequest.class);
        response = Mockito.mock(VaadinResponse.class);
        session = Mockito.mock(VaadinSession.class);

        ui = Mockito.mock(UI.class);
        // run the command immediately
        Mockito.doAnswer(invocation -> {
            Command command = invocation.getArgument(0);
            command.execute();
            return null;
        }).when(ui).access(Mockito.any(Command.class));

        owner = Mockito.mock(Element.class);
        Component componentOwner = Mockito.mock(Component.class);
        Mockito.when(owner.getComponent())
                .thenReturn(Optional.of(componentOwner));
        Mockito.when(componentOwner.getUI()).thenReturn(Optional.of(ui));

        downloadEvent = new DownloadEvent(request, response, session, owner);

        handler = new AbstractDownloadHandler<>() {
            @Override
            public void handleDownloadRequest(DownloadEvent event) {
            }
        };
        handler.setTransferUI(ui);
        mockContext = Mockito.mock(TransferContext.class);
        Mockito.when(mockContext.contentLength()).thenReturn(TOTAL_BYTES);
        listener = Mockito.mock(TransferProgressListener.class);

        Mockito.when(mockContext.owningElement()).thenReturn(owner);
        Mockito.when(mockContext.getUI()).thenReturn(ui);

        outputStream = new ByteArrayOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);
    }

    @Test
    public void addTransferProgressListener_listenerAdded_listenerInvoked_listenerRemoved_listenerNotInvoked() {
        Registration registration = handler
                .addTransferProgressListener(listener);
        handler.getListeners().forEach(l -> l.onStart(mockContext));
        Mockito.verify(listener).onStart(mockContext);

        Mockito.reset(listener);
        registration.remove();
        handler.getListeners().forEach(l -> l.onStart(mockContext));
        Mockito.verify(listener, Mockito.times(0)).onStart(mockContext);
    }

    @Test
    public void whenStart_onStartCalled() {
        SerializableRunnable startHandler = Mockito
                .mock(SerializableRunnable.class);
        handler.whenStart(startHandler);
        handler.getListeners()
                .forEach(listener -> listener.onStart(mockContext));
        Mockito.verify(startHandler).run();
    }

    @Test
    public void whenProgress_onProgressCalled() {
        SerializableBiConsumer<Long, Long> onProgressHandler = Mockito
                .mock(SerializableBiConsumer.class);
        handler.onProgress(onProgressHandler);
        handler.getListeners().forEach(listener -> listener
                .onProgress(mockContext, TRANSFERRED_BYTES, TOTAL_BYTES));
        Mockito.verify(onProgressHandler).accept(TRANSFERRED_BYTES,
                TOTAL_BYTES);
    }

    @Test
    public void multipleHooks_multipleListenersAdded_InvokedInOrder() {
        List<String> executionOrder = new ArrayList<>();
        handler.whenStart(() -> executionOrder.add("first"));
        handler.whenStart(() -> executionOrder.add("second"));
        handler.getListeners()
                .forEach(listener -> listener.onStart(mockContext));
        List<String> expectedOrder = List.of("first", "second");
        Assert.assertEquals(expectedOrder, executionOrder);
    }

    @Test
    public void whenComplete() {
        SerializableConsumer<Boolean> completeHandler = Mockito
                .mock(SerializableConsumer.class);
        handler.whenComplete(completeHandler);
        handler.getListeners().forEach(listener -> {
            listener.onComplete(mockContext, TRANSFERRED_BYTES);
            listener.onError(mockContext, EXCEPTION);
        });
        Mockito.verify(completeHandler).accept(true);
        Mockito.verify(completeHandler).accept(false);
    }

    @Test
    public void transferProgressListener_transfer_sessionNotLocked()
            throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(
                "Hello".getBytes(StandardCharsets.UTF_8));
        VaadinSession session = Mockito.mock(VaadinSession.class);
        TransferContext context = Mockito.mock(TransferContext.class);
        Mockito.when(context.session()).thenReturn(session);
        OutputStream outputStream = Mockito.mock(OutputStream.class);
        Collection<TransferProgressListener> listeners = new ArrayList<>();
        TransferUtil.transfer(inputStream, outputStream, context, listeners);
        Mockito.verify(session, Mockito.times(0)).lock();
    }

    @Test
    public void customHandlerWithShorthandCompleteListener_noErrorInTransfer_success_errorInTransfer_failure()
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

        customHandler.setTransferUI(ui);
        customHandler.handleDownloadRequest(downloadEvent);

        Assert.assertTrue(successAtomic.get());
        Assert.assertEquals("Hello",
                outputStream.toString(StandardCharsets.UTF_8));
        Assert.assertNull(downloadEvent.getException());

        OutputStream outputStreamError = Mockito.mock(OutputStream.class);
        Mockito.doThrow(new IOException("Test error")).when(outputStreamError)
                .write(Mockito.any(byte[].class), Mockito.anyInt(),
                        Mockito.anyInt());
        Mockito.when(downloadEvent.getOutputStream())
                .thenReturn(outputStreamError);

        customHandler.handleDownloadRequest(downloadEvent);
        Assert.assertFalse(successAtomic.get());
        Assert.assertNull(downloadEvent.getException());
    }

    @Test
    public void doesNotRequireToCatchIOException() {
        DownloadHandler handler = event -> {
            new FileInputStream(new File("foo"));
        };
    }

    @Test
    public void inline_attachmentUsedByDefault() {
        Assert.assertFalse(handler.isInline());
    }

    @Test
    public void inline_inlinedWhenExplicitlyCalled() {
        handler.inline();
        Assert.assertTrue(handler.isInline());
    }

    @Test
    public void getTransferContext_returnsExpectedContextFromEvent() {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);
        Element owner = Mockito.mock(Element.class);
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
        Assert.assertEquals(owner, context.owningElement());
        Assert.assertEquals(session, context.session());
        Assert.assertEquals(request, context.request());
        Assert.assertEquals(response, context.response());
        Assert.assertEquals(1024, context.contentLength());
        Assert.assertEquals("test.txt", context.fileName());
        Assert.assertNull(event.getException());
    }

    @Test
    public void whenStartWithContext_onStartCalled() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        handler.whenStart((context) -> invoked.set(true));
        handler.getListeners()
                .forEach(listener -> listener.onStart(mockContext));
        Assert.assertTrue("Start with context should be invoked",
                invoked.get());
    }

    @Test
    public void whenProgressWithContext_onProgressCalled() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        handler.onProgress((context, current, total) -> invoked.set(true),
                1024);
        handler.getListeners().forEach(listener -> listener
                .onProgress(mockContext, TRANSFERRED_BYTES, TOTAL_BYTES));
        Assert.assertTrue("Progress with context should be invoked",
                invoked.get());
    }

    @Test
    public void whenProgressWithContextNoInterval_onProgressCalled() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        handler.onProgress((context, current, total) -> invoked.set(true));
        handler.getListeners().forEach(listener -> listener
                .onProgress(mockContext, TRANSFERRED_BYTES, TOTAL_BYTES));
        Assert.assertTrue(
                "Progress with context and interval should be invoked",
                invoked.get());
    }

    @Test
    public void whenCompleteWithContext() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        handler.whenComplete((context, success) -> invoked.set(true));
        handler.getListeners().forEach(listener -> {
            listener.onComplete(mockContext, TRANSFERRED_BYTES);
            listener.onError(mockContext, EXCEPTION);
        });
        Assert.assertTrue("Progress with context should be invoked",
                invoked.get());
    }
}