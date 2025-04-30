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
import java.util.List;
import java.util.Optional;

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
import com.vaadin.flow.server.DownloadRequest;
import com.vaadin.flow.server.TransferProgressListener;
import com.vaadin.flow.shared.Registration;

public class AbstractDownloadHandlerTest {
    private static final long TOTAL_BYTES = 100L;
    private static final long TRANSFERRED_BYTES = 42L;
    private static final IOException EXCEPTION = new IOException("Test error");

    private AbstractDownloadHandler handler;
    private TransferContext mockContext;
    private TransferProgressListener listener;

    @Before
    public void setUp() {
        handler = new AbstractDownloadHandler() {
            @Override
            public void handleDownloadRequest(DownloadRequest event) {
            }
        };
        mockContext = Mockito.mock(TransferContext.class);
        Mockito.when(mockContext.totalBytes()).thenReturn(TOTAL_BYTES);
        listener = Mockito.mock(TransferProgressListener.class);

        UI ui = Mockito.mock(UI.class);
        // run the command immediately
        Mockito.doAnswer(invocation -> {
            Command command = invocation.getArgument(0);
            command.execute();
            return null;
        }).when(ui).access(Mockito.any(Command.class));

        Element owner = Mockito.mock(Element.class);
        Component componentOwner = Mockito.mock(Component.class);
        Mockito.when(owner.getComponent())
                .thenReturn(Optional.of(componentOwner));
        Mockito.when(componentOwner.getUI()).thenReturn(Optional.of(ui));
        Mockito.when(mockContext.owningElement()).thenReturn(owner);
        Mockito.when(mockContext.getUI()).thenReturn(ui);
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
    public void addTransferProgressListener_listenerAdded_listenersUnsubscribed() {
        handler.addTransferProgressListener(listener);
        handler.unsubscribeFromTransferProgress();
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
                .onProgress(mockContext, TRANSFERRED_BYTES));
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
}