/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.tests.event;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.event.EventRouter;
import com.vaadin.event.UIEvents.PollEvent;
import com.vaadin.event.UIEvents.PollListener;
import com.vaadin.server.ErrorEvent;
import com.vaadin.server.ErrorHandler;
import com.vaadin.ui.UI;

/**
 * Test EventRouter and related error handling.
 */
public class EventRouterTest {

    private EventRouter router;
    private UI ui;
    private ErrorHandler errorHandler;
    private PollListener listener;

    @Before
    public void createMocks() {
        router = new EventRouter();
        ui = EasyMock.createNiceMock(UI.class);
        errorHandler = EasyMock.createMock(ErrorHandler.class);
        listener = EasyMock.createMock(PollListener.class);
        router.addListener(PollEvent.class, listener, PollListener.POLL_METHOD);
    }

    @Test
    public void fireEvent_noException_eventReceived() {
        listener.poll(EasyMock.<PollEvent> anyObject());

        EasyMock.replay(ui, listener, errorHandler);
        router.fireEvent(new PollEvent(ui), errorHandler);
        EasyMock.verify(listener, errorHandler);
    }

    @Test
    public void fireEvent_exceptionFromListenerAndNoHandler_exceptionPropagated() {
        listener.poll(EasyMock.<PollEvent> anyObject());
        EasyMock.expectLastCall()
                .andThrow(new RuntimeException("listener failed"));

        EasyMock.replay(ui, listener);
        try {
            router.fireEvent(new PollEvent(ui));
            Assert.fail("Did not receive expected exception from listener");
        } catch (RuntimeException e) {
            // e is a ListenerMethod@MethodException
            Assert.assertEquals("listener failed", e.getCause().getMessage());
        }
        EasyMock.verify(listener);
    }

    @Test
    public void fireEvent_exceptionFromListener_errorHandlerCalled() {
        listener.poll(EasyMock.<PollEvent> anyObject());
        EasyMock.expectLastCall()
                .andThrow(new RuntimeException("listener failed"));
        errorHandler.error(EasyMock.<ErrorEvent> anyObject());

        EasyMock.replay(ui, listener, errorHandler);
        router.fireEvent(new PollEvent(ui), errorHandler);
        EasyMock.verify(listener, errorHandler);
    }

    @Test
    public void fireEvent_multipleListenersAndException_errorHandlerCalled() {
        PollListener listener2 = EasyMock.createMock(PollListener.class);
        router.addListener(PollEvent.class, listener2,
                PollListener.POLL_METHOD);

        listener.poll(EasyMock.<PollEvent> anyObject());
        EasyMock.expectLastCall()
                .andThrow(new RuntimeException("listener failed"));
        errorHandler.error(EasyMock.<ErrorEvent> anyObject());
        // second listener should be called despite an error in the first
        listener2.poll(EasyMock.<PollEvent> anyObject());

        EasyMock.replay(ui, listener, listener2, errorHandler);
        router.fireEvent(new PollEvent(ui), errorHandler);
        EasyMock.verify(listener, listener2, errorHandler);
    }

}
