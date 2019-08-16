package com.vaadin.flow.component.internal;

import java.lang.reflect.Field;
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
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.AbstractTheme;
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

    public static class MyTheme implements AbstractTheme {

        @Override
        public String getBaseUrl() {
            return "base";
        }

        @Override
        public String getThemeUrl() {
            return "theme";
        }

    }

    @Test
    public void setThemeNull() throws Exception {
        Assert.assertNull(getTheme(internals));
        internals.setTheme(MyTheme.class);
        internals.setTheme((Class) null);
        Assert.assertNull(getTheme(internals));

    }

    @Test
    public void setTheme() throws Exception {
        Assert.assertNull(getTheme(internals));
        internals.setTheme(MyTheme.class);
        Assert.assertTrue(getTheme(internals) instanceof MyTheme);
    }

    @Test
    public void setThemeAgain() throws Exception {
        Assert.assertNull(getTheme(internals));
        internals.setTheme(MyTheme.class);
        internals.setTheme(MyTheme.class);
        Assert.assertTrue(getTheme(internals) instanceof MyTheme);
    }

    private AbstractTheme getTheme(UIInternals internals)
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        Field t = UIInternals.class.getDeclaredField("theme");
        t.setAccessible(true);
        return (AbstractTheme) t.get(internals);
    }
}
