package com.vaadin.flow.component.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;

public class UIInternalsTest {

    @Mock
    UI ui;
    @Mock
    VaadinService vaadinService;

    UIInternals internals;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        Mockito.when(ui.getUI()).thenReturn(Optional.of(ui));
        Element body = new Element("body");
        Mockito.when(ui.getElement()).thenReturn(body);

        internals = new UIInternals(ui);
        AlwaysLockedVaadinSession session = new AlwaysLockedVaadinSession(
                vaadinService);
        Mockito.when(vaadinService.getInstantiator())
                .thenReturn(new DefaultInstantiator(vaadinService));
        internals.setSession(session);
        Mockito.when(ui.getSession()).thenReturn(session);
    }

    @Test
    public void heartbeatTimestampSet_heartbeatListenersAreCalled() {
        List<Long> heartbeats = new ArrayList<>();
        Registration registration = internals.addHeartbeatListener(
                event -> heartbeats.add(event.getHeartbeatTime()));

        internals.setLastHeartbeatTimestamp(System.currentTimeMillis());

        Assert.assertEquals("Heartbeat listener should have fired", 1,
                heartbeats.size());

        registration.remove();

        internals.setLastHeartbeatTimestamp(System.currentTimeMillis());

        Assert.assertEquals(
                "Heartbeat listener should been removed and no new event recorded",
                1, heartbeats.size());
    }

    @Test
    public void heartbeatListenerRemovedFromHeartbeatEvent_noExplosion() {
        AtomicReference<Registration> reference = new AtomicReference<>();
        AtomicInteger runCount = new AtomicInteger();

        Registration registration = internals.addHeartbeatListener(event -> {
            runCount.incrementAndGet();
            reference.get().remove();
        });
        reference.set(registration);

        internals.setLastHeartbeatTimestamp(System.currentTimeMillis());
        Assert.assertEquals("Listener should have been run once", 1,
                runCount.get());

        internals.setLastHeartbeatTimestamp(System.currentTimeMillis());
        Assert.assertEquals(
                "Listener should not have been run again since it was removed",
                1, runCount.get());
    }

}
