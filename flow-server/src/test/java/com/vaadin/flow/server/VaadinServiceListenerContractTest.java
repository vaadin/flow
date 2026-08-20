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
import com.vaadin.flow.server.communication.RpcInvocationEvent;
import com.vaadin.flow.server.communication.RpcInvocationListener;
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

    private static RpcInvocationEvent rpcEvent() {
        return new RpcInvocationEvent(new UI(), "event", 1, "click");
    }

    @Test
    public void rpcInvocationListeners_notifiedInRegistrationOrder() {
        List<String> calls = new ArrayList<>();
        service.addRpcInvocationListener(recordingRpcListener(calls, "first"));
        service.addRpcInvocationListener(recordingRpcListener(calls, "second"));

        RpcInvocationEvent event = rpcEvent();
        service.fireRpcInvocationStarted(event);
        service.fireRpcInvocationFailed(event, new RuntimeException("boom"));
        service.fireRpcInvocationEnded(event);

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

        RpcInvocationEvent event = rpcEvent();
        service.fireRpcInvocationStarted(event);
        service.fireRpcInvocationFailed(event, new RuntimeException("boom"));
        service.fireRpcInvocationEnded(event);

        Assert.assertEquals(
                List.of("second-started", "second-failed", "second-ended"),
                calls);
    }

    @Test
    public void hasRpcInvocationListeners_tracksRegistrations() {
        Assert.assertFalse(service.hasRpcInvocationListeners());

        Registration registration = service
                .addRpcInvocationListener(new RpcInvocationListener() {
                });
        Assert.assertTrue(service.hasRpcInvocationListeners());

        registration.remove();
        Assert.assertFalse(service.hasRpcInvocationListeners());
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
}
