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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.communication.RpcInvocationEndedEvent;
import com.vaadin.flow.server.communication.RpcInvocationEvent;
import com.vaadin.flow.server.communication.RpcInvocationFailedEvent;
import com.vaadin.flow.server.communication.RpcInvocationListener;
import com.vaadin.flow.server.communication.RpcInvocationStartedEvent;
import com.vaadin.flow.shared.Registration;

/**
 * Tests the listener handling that {@link VaadinService} adds on top of the
 * {@link VaadinService#getEventBus() event bus}: the listener interfaces that
 * predate the bus are served by the new events, and session init and service
 * destroy do something other than logging with a listener that throws.
 */
public class VaadinServiceListenerContractTest {

    private final MockVaadinServletService service = new MockVaadinServletService();

    @Test
    public void rpcInvocationListener_isNotifiedOfTheNewEvents() {
        List<String> calls = new ArrayList<>();
        RuntimeException failure = new RuntimeException("boom");
        service.addRpcInvocationListener(new RpcInvocationListener() {
            @Override
            public void invocationStarted(RpcInvocationEvent event) {
                calls.add("started-" + describe(event));
            }

            @Override
            public void invocationFailed(RpcInvocationEvent event,
                    Throwable error) {
                Assert.assertSame(failure, error);
                calls.add("failed-" + describe(event));
            }

            @Override
            public void invocationEnded(RpcInvocationEvent event) {
                calls.add("ended-" + describe(event));
            }

            private String describe(RpcInvocationEvent event) {
                return event.getType() + ":" + event.getNodeId() + ":"
                        + event.getName();
            }
        });

        UI ui = new UI();
        VaadinServiceEventBus eventBus = service.getEventBus();
        eventBus.fireEvent(
                new RpcInvocationStartedEvent(ui, "event", 7, "click"));
        eventBus.fireEvent(
                new RpcInvocationFailedEvent(ui, "event", 7, "click", failure));
        eventBus.fireEvent(
                new RpcInvocationEndedEvent(ui, "event", 7, "click"));

        Assert.assertEquals(List.of("started-event:7:click",
                "failed-event:7:click", "ended-event:7:click"), calls);
    }

    @Test
    public void sessionLockListener_isNotifiedOfTheNewEvents() {
        List<String> calls = new ArrayList<>();
        service.addSessionLockListener(new SessionLockListener() {
            @Override
            public void lockRequested(SessionLockEvent event) {
                calls.add("requested");
            }

            @Override
            public void lockAcquired(SessionLockEvent event) {
                Assert.assertSame(service, event.getService());
                calls.add("acquired");
            }

            @Override
            public void lockReleased(SessionLockEvent event) {
                calls.add("released");
            }
        });

        VaadinServiceEventBus eventBus = service.getEventBus();
        eventBus.fireEvent(new SessionLockRequestedEvent(service));
        eventBus.fireEvent(new SessionLockAcquiredEvent(service));
        eventBus.fireEventInReverseOrder(new SessionLockReleasedEvent(service));

        Assert.assertEquals(List.of("requested", "acquired", "released"),
                calls);
    }

    @Test
    public void removedRpcInvocationListener_isRemovedFromEveryEventType() {
        VaadinServiceEventBus eventBus = service.getEventBus();
        Registration registration = service
                .addRpcInvocationListener(new RpcInvocationListener() {
                });
        Assert.assertTrue(
                eventBus.hasListener(RpcInvocationStartedEvent.class));
        Assert.assertTrue(eventBus.hasListener(RpcInvocationFailedEvent.class));
        Assert.assertTrue(eventBus.hasListener(RpcInvocationEndedEvent.class));

        registration.remove();

        Assert.assertFalse(
                eventBus.hasListener(RpcInvocationStartedEvent.class));
        Assert.assertFalse(
                eventBus.hasListener(RpcInvocationFailedEvent.class));
        Assert.assertFalse(eventBus.hasListener(RpcInvocationEndedEvent.class));
    }

    @Test
    public void uiInitListener_isNotifiedOfTheUiInitEvent() {
        List<UIInitEvent> events = new ArrayList<>();
        service.addUIInitListener(events::add);

        UI ui = new UI();
        service.getEventBus().fireEvent(new UIInitEvent(ui, service));

        Assert.assertEquals(1, events.size());
        Assert.assertSame(ui, events.get(0).getUI());
        Assert.assertSame(service, events.get(0).getSource());
    }

    @Test
    public void sessionInitListenerThrowsCheckedException_originalGoesToSessionErrorHandler() {
        MockVaadinSession session = new MockVaadinSession(service);
        List<Throwable> errors = new ArrayList<>();
        ServiceException failure = new ServiceException("checked");
        service.addSessionInitListener(event -> {
            throw failure;
        });

        // The session is locked while init listeners are notified
        session.lock();
        try {
            session.setErrorHandler(event -> errors.add(event.getThrowable()));
            service.getEventBus()
                    .fireEvent(new SessionInitEvent(service, session, null));
        } finally {
            session.unlock();
        }

        // The listener exception must reach the handler as-is, not wrapped
        Assert.assertEquals(List.of(failure), errors);
    }

    @Test
    public void serviceDestroyListenersThrow_allNotifiedAndFirstFailureRethrown() {
        List<String> calls = new ArrayList<>();
        RuntimeException first = new RuntimeException("first");
        RuntimeException second = new RuntimeException("second");
        service.addServiceDestroyListener(event -> {
            throw first;
        });
        service.addServiceDestroyListener(event -> {
            throw second;
        });
        service.addServiceDestroyListener(event -> calls.add("third"));

        RuntimeException thrown = Assert.assertThrows(RuntimeException.class,
                service::destroy);

        Assert.assertSame(first, thrown);
        Assert.assertArrayEquals(new Throwable[] { second },
                thrown.getSuppressed());
        Assert.assertEquals(List.of("third"), calls);
    }
}
