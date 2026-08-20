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
 * Pins down the semantics that {@link VaadinService} listener families have
 * today, so that they can be compared against, and preserved by, a possible
 * migration to a shared service-level event bus.
 * <p>
 * The point of these tests is that the families do <em>not</em> behave alike:
 * they differ in notification order and, most importantly, in what happens when
 * a listener throws. Any generic {@code fireEvent} needs a per-event-type
 * policy to keep these contracts intact.
 */
public class VaadinServiceListenerContractTest {

    private final MockVaadinServletService service = new MockVaadinServletService();

    private void fireRpcInvocationPhases() {
        UI ui = new UI();
        VaadinServiceEventBus eventBus = service.getEventBus();
        eventBus.fireEvent(
                new RpcInvocationStartedEvent(ui, "event", 1, "click"),
                VaadinServiceEventBus.logErrors());
        eventBus.fireEvent(
                new RpcInvocationFailedEvent(ui, "event", 1, "click",
                        new RuntimeException("boom")),
                VaadinServiceEventBus.logErrors());
        eventBus.fireEvent(new RpcInvocationEndedEvent(ui, "event", 1, "click"),
                VaadinServiceEventBus.logErrors());
    }

    @Test
    public void rpcInvocationListeners_notifiedInRegistrationOrder() {
        List<String> calls = new ArrayList<>();
        service.addRpcInvocationListener(recordingRpcListener(calls, "first"));
        service.addRpcInvocationListener(recordingRpcListener(calls, "second"));

        fireRpcInvocationPhases();

        Assert.assertEquals(List.of("first-started", "second-started",
                "first-failed", "second-failed", "first-ended", "second-ended"),
                calls);
    }

    @Test
    public void rpcInvocationListenerThrows_othersStillNotified() {
        List<String> calls = new ArrayList<>();
        service.addRpcInvocationListener(new RpcInvocationListener() {
            @Override
            public void invocationStarted(RpcInvocationEvent event) {
                throw new RuntimeException("started");
            }

            @Override
            public void invocationFailed(RpcInvocationEvent event,
                    Throwable error) {
                throw new RuntimeException("failed");
            }

            @Override
            public void invocationEnded(RpcInvocationEvent event) {
                throw new RuntimeException("ended");
            }
        });
        service.addRpcInvocationListener(recordingRpcListener(calls, "second"));

        fireRpcInvocationPhases();

        Assert.assertEquals(
                List.of("second-started", "second-failed", "second-ended"),
                calls);
    }

    @Test
    public void rpcInvocationListenerRegistration_tracksAllPhases() {
        VaadinServiceEventBus eventBus = service.getEventBus();
        Assert.assertFalse(
                eventBus.hasListener(RpcInvocationStartedEvent.class));

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
    }

    @Test
    public void listenerFamiliesAreIndependent_rpcListenerNotNotifiedOfOtherEvents() {
        List<String> calls = new ArrayList<>();
        service.addRpcInvocationListener(recordingRpcListener(calls, "rpc"));

        service.fireSessionLockRequested();
        service.fireSessionLockAcquired();
        service.fireSessionLockReleased();
        service.fireUIInitListeners(new UI());

        Assert.assertTrue(calls.isEmpty());
    }

    @Test
    public void sessionLockListenerThrows_othersStillNotified() {
        List<String> calls = new ArrayList<>();
        service.addSessionLockListener(new SessionLockListener() {
            @Override
            public void lockAcquired(SessionLockEvent event) {
                throw new RuntimeException("acquired");
            }
        });
        service.addSessionLockListener(new SessionLockListener() {
            @Override
            public void lockAcquired(SessionLockEvent event) {
                calls.add("second-acquired");
            }
        });

        service.fireSessionLockAcquired();

        Assert.assertEquals(List.of("second-acquired"), calls);
    }

    @Test
    public void uiInitListenerThrows_exceptionPropagatesAndStopsLaterListeners() {
        List<String> calls = new ArrayList<>();
        service.addUIInitListener(event -> {
            throw new RuntimeException("uiInit");
        });
        service.addUIInitListener(event -> calls.add("second"));

        RuntimeException thrown = Assert.assertThrows(RuntimeException.class,
                () -> service.fireUIInitListeners(new UI()));

        Assert.assertEquals("uiInit", thrown.getMessage());
        Assert.assertTrue(calls.isEmpty());
    }

    @Test
    public void serviceDestroyListenerThrows_othersNotifiedAndFirstFailureRethrown() {
        List<String> calls = new ArrayList<>();
        service.addServiceDestroyListener(event -> {
            throw new RuntimeException("serviceDestroy");
        });
        service.addServiceDestroyListener(event -> calls.add("second"));

        RuntimeException thrown = Assert.assertThrows(RuntimeException.class,
                service::destroy);

        Assert.assertEquals("serviceDestroy", thrown.getMessage());
        Assert.assertEquals(List.of("second"), calls);
    }

    @Test
    public void uiInitListener_receivesEventWithUiAndService() {
        List<UIInitEvent> events = new ArrayList<>();
        service.addUIInitListener(events::add);

        UI ui = new UI();
        service.fireUIInitListeners(ui);

        Assert.assertEquals(1, events.size());
        Assert.assertSame(ui, events.get(0).getUI());
        Assert.assertSame(service, events.get(0).getSource());
    }

    @Test
    public void multipleThrowingServiceDestroyListeners_firstFailureRethrownWithOthersSuppressed() {
        RuntimeException first = new RuntimeException("first");
        RuntimeException second = new RuntimeException("second");
        RuntimeException third = new RuntimeException("third");
        service.addServiceDestroyListener(event -> {
            throw first;
        });
        service.addServiceDestroyListener(event -> {
            throw second;
        });
        service.addServiceDestroyListener(event -> {
            throw third;
        });

        RuntimeException thrown = Assert.assertThrows(RuntimeException.class,
                service::destroy);

        Assert.assertSame(first, thrown);
        Assert.assertArrayEquals(new Throwable[] { second, third },
                thrown.getSuppressed());
    }

    @Test
    public void listenersRegisteredWithTheTypedApis_areNotifiedByTheEventBus() {
        List<String> calls = new ArrayList<>();
        service.addRpcInvocationListener(recordingRpcListener(calls, "rpc"));
        service.addSessionLockListener(new SessionLockListener() {
            @Override
            public void lockAcquired(SessionLockEvent event) {
                calls.add("lock-acquired");
            }
        });
        service.addUIInitListener(event -> calls.add("ui-init"));

        UI ui = new UI();
        VaadinServiceEventBus eventBus = service.getEventBus();
        eventBus.fireEvent(new RpcInvocationStartedEvent(ui, "event", 1, "x"),
                VaadinServiceEventBus.logErrors());
        eventBus.fireEvent(
                new RpcInvocationFailedEvent(ui, "event", 1, "x",
                        new RuntimeException("boom")),
                VaadinServiceEventBus.logErrors());
        eventBus.fireEvent(new RpcInvocationEndedEvent(ui, "event", 1, "x"),
                VaadinServiceEventBus.logErrors());
        eventBus.fireEvent(new SessionLockAcquiredEvent(service),
                VaadinServiceEventBus.logErrors());
        eventBus.fireEvent(new UIInitEvent(ui, service),
                VaadinServiceEventBus.logErrors());

        Assert.assertEquals(List.of("rpc-started", "rpc-failed", "rpc-ended",
                "lock-acquired", "ui-init"), calls);
    }

    @Test
    public void removedTypedListener_isRemovedFromAllItsEventTypes() {
        List<String> calls = new ArrayList<>();
        service.addRpcInvocationListener(recordingRpcListener(calls, "rpc"))
                .remove();

        VaadinServiceEventBus eventBus = service.getEventBus();
        Assert.assertFalse(
                eventBus.hasListener(RpcInvocationStartedEvent.class));
        Assert.assertFalse(
                eventBus.hasListener(RpcInvocationFailedEvent.class));
        Assert.assertFalse(eventBus.hasListener(RpcInvocationEndedEvent.class));

        fireRpcInvocationPhases();
        Assert.assertTrue(calls.isEmpty());
    }

    private static RpcInvocationListener recordingRpcListener(
            List<String> calls, String name) {
        return new RpcInvocationListener() {
            @Override
            public void invocationStarted(RpcInvocationEvent event) {
                calls.add(name + "-started");
            }

            @Override
            public void invocationFailed(RpcInvocationEvent event,
                    Throwable error) {
                calls.add(name + "-failed");
            }

            @Override
            public void invocationEnded(RpcInvocationEvent event) {
                calls.add(name + "-ended");
            }
        };
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
            service.getEventBus().fireEvent(
                    new SessionInitEvent(service, session, null),
                    VaadinServiceEventBus.logErrors());
        } finally {
            session.unlock();
        }

        // The listener exception must reach the handler as-is, not wrapped
        Assert.assertEquals(1, errors.size());
        Assert.assertSame(failure, errors.get(0));
    }

    @Test
    public void rpcInvocationPhases_areDistinctEventsDescribingTheSameInvocation() {
        List<RpcInvocationEvent> events = new ArrayList<>();
        service.addRpcInvocationListener(new RpcInvocationListener() {
            @Override
            public void invocationStarted(RpcInvocationEvent event) {
                events.add(event);
            }

            @Override
            public void invocationEnded(RpcInvocationEvent event) {
                events.add(event);
            }
        });

        UI ui = new UI();
        VaadinServiceEventBus eventBus = service.getEventBus();
        eventBus.fireEvent(
                new RpcInvocationStartedEvent(ui, "event", 7, "click"),
                VaadinServiceEventBus.logErrors());
        eventBus.fireEvent(new RpcInvocationEndedEvent(ui, "event", 7, "click"),
                VaadinServiceEventBus.logErrors());

        Assert.assertEquals(2, events.size());
        // Phases are correlated by thread, not by event identity
        Assert.assertNotSame(events.get(0), events.get(1));
        for (RpcInvocationEvent event : events) {
            Assert.assertSame(ui, event.getUI());
            Assert.assertEquals("event", event.getType());
            Assert.assertEquals(7, event.getNodeId());
            Assert.assertEquals("click", event.getName());
        }
    }
}
