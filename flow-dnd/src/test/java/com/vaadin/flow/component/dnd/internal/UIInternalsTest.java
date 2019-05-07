package com.vaadin.flow.component.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.UI;
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
        internals.setSession(new AlwaysLockedVaadinSession(vaadinService));
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
}
