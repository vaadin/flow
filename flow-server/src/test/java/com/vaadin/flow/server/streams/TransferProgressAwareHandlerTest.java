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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.DownloadRequest;
import com.vaadin.flow.server.TransferProgressListener;
import com.vaadin.flow.shared.Registration;

public class TransferProgressAwareHandlerTest {
    private static final long TOTAL_BYTES = 100L;
    private static final long TRANSFERRED_BYTES = 42L;
    private static final IOException EXCEPTION = new IOException("Test error");

    private TransferProgressAwareHandler handler;
    private TransferContext mockContext;
    private TransferProgressListener listener;

    @Before
    public void setUp() {
        mockContext = Mockito.mock(TransferContext.class);
        handler = new TestTransferProgressAwareHandler(mockContext);
        Mockito.when(mockContext.totalBytes()).thenReturn(TOTAL_BYTES);
        listener = Mockito.mock(TransferProgressListener.class);
    }

    @Test
    public void addTransferProgressListener_listenerAdded_listenerInvoked_listenerRemoved_listenerNotInvoked() {
        Registration registration = handler
                .addTransferProgressListener(listener);
        Collection<TransferProgressListener> listeners = handler.getListeners();
        Assert.assertTrue(listeners.contains(listener));

        handler.handleTransfer(null);
        Mockito.verify(listener).onStart(mockContext);
        Mockito.verify(listener).onProgress(mockContext, TRANSFERRED_BYTES);
        Mockito.verify(listener).onError(mockContext, EXCEPTION);
        Mockito.verify(listener).onComplete(mockContext, TOTAL_BYTES);

        Mockito.reset(listener);
        registration.remove();

        Assert.assertFalse(handler.getListeners().contains(listener));
        handler.handleTransfer(null);
        Mockito.verify(listener, Mockito.times(0)).onStart(mockContext);
        Mockito.verify(listener, Mockito.times(0)).onProgress(mockContext,
                TRANSFERRED_BYTES);
        Mockito.verify(listener, Mockito.times(0)).onError(mockContext,
                EXCEPTION);
        Mockito.verify(listener, Mockito.times(0)).onComplete(mockContext,
                TOTAL_BYTES);
    }

    @Test
    public void addTransferProgressListener_listenerAdded_listenersUnsubscribed() {
        handler.addTransferProgressListener(listener);

        handler.unsubscribe();
        Assert.assertFalse(handler.getListeners().contains(listener));
    }

    @Test
    public void whenStart_onStartCalled() {
        SerializableRunnable startHandler = Mockito
                .mock(SerializableRunnable.class);
        handler.whenStart(startHandler);
        // Passed event is not taken into account in the test mock
        handler.handleTransfer(null);
        Mockito.verify(startHandler).run();
    }

    @Test
    public void whenProgress_onProgressCalled() {
        SerializableBiConsumer<Long, Long> onProgressHandler = Mockito
                .mock(SerializableBiConsumer.class);
        handler.onProgress(onProgressHandler);
        handler.handleTransfer(null);

        Mockito.verify(onProgressHandler).accept(TRANSFERRED_BYTES,
                TOTAL_BYTES);
    }

    @Test
    public void multipleHooks_multipleListenersAdded_InvokedInOrder() {
        List<String> executionOrder = new ArrayList<>();
        handler.whenStart(() -> executionOrder.add("first"));
        handler.whenStart(() -> executionOrder.add("second"));
        handler.handleTransfer(null);
        List<String> expectedOrder = List.of("first", "second");
        Assert.assertEquals(expectedOrder, executionOrder);
    }

    @Test
    public void whenComplete() {
        SerializableConsumer<Boolean> completeHandler = Mockito
                .mock(SerializableConsumer.class);
        handler.whenComplete(completeHandler);
        handler.handleTransfer(null);

        Mockito.verify(completeHandler).accept(true);
        Mockito.verify(completeHandler).accept(false);
    }

    private static class TestTransferProgressAwareHandler extends
            TransferProgressAwareHandler<DownloadRequest, TestTransferProgressAwareHandler> {

        private final TransferContext mockContext;

        public TestTransferProgressAwareHandler(TransferContext mockContext) {
            this.mockContext = mockContext;
        }

        @Override
        protected void handleTransfer(DownloadRequest transferEvent) {
            getListeners().forEach(listener -> {
                listener.onStart(mockContext);
                listener.onProgress(mockContext, TRANSFERRED_BYTES);
                listener.onError(mockContext, EXCEPTION);
                listener.onComplete(mockContext, TOTAL_BYTES);
            });
        }

        @Override
        protected TransferContext getTransferContext(
                DownloadRequest transferEvent) {
            return mockContext;
        }
    }
}