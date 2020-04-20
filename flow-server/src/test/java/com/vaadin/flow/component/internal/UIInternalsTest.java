package com.vaadin.flow.component.internal;

import java.util.ArrayList;
import java.util.Collections;
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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;

public class UIInternalsTest {

    @Mock
    UI ui;
    @Mock
    VaadinService vaadinService;

    UIInternals internals;

    @Route
    @Push
    @Tag(Tag.DIV)
    public static class RouteTarget extends Component implements RouterLayout {

    }

    @Route(value = "foo", layout = RouteTarget.class)
    @Tag(Tag.DIV)
    public static class RouteTarget1 extends Component {

    }

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

    @Test
    public void showRouteTarget_usePushConfigFromComponent() {
        PushConfiguration pushConfig = setUpInitialPush();
        internals.showRouteTarget(Mockito.mock(Location.class),
                new RouteTarget(), Collections.emptyList());

        Mockito.verify(pushConfig).setPushMode(PushMode.AUTOMATIC);
        Mockito.verify(pushConfig).setTransport(Transport.WEBSOCKET_XHR);
    }

    @Test
    public void showRouteTarget_usePushConfigFromParentLayout() {
        PushConfiguration pushConfig = setUpInitialPush();
        internals.showRouteTarget(Mockito.mock(Location.class),
                new RouteTarget1(),
                Collections.singletonList(new RouteTarget()));

        Mockito.verify(pushConfig).setPushMode(PushMode.AUTOMATIC);
        Mockito.verify(pushConfig).setTransport(Transport.WEBSOCKET_XHR);
    }

    @Test
    public void showRouteTarget_componentHasNoPush_pushIsDisabled() {
        PushConfiguration pushConfig = setUpInitialPush();
        DeploymentConfiguration deploymentConfiguration = vaadinService
                .getDeploymentConfiguration();
        Mockito.when(deploymentConfiguration.getPushMode())
                .thenReturn(PushMode.AUTOMATIC);

        internals.showRouteTarget(Mockito.mock(Location.class),
                new Text(""), Collections.emptyList());

        Mockito.verify(pushConfig).setPushMode(PushMode.AUTOMATIC);
        Mockito.verify(pushConfig, Mockito.times(0))
                .setTransport(Mockito.any());
    }

    private PushConfiguration setUpInitialPush() {
        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(vaadinService.getDeploymentConfiguration())
                .thenReturn(config);

        PushConfiguration pushConfig = Mockito.mock(PushConfiguration.class);
        Mockito.when(ui.getPushConfiguration()).thenReturn(pushConfig);

        Mockito.when(config.getPushMode()).thenReturn(PushMode.DISABLED);
        return pushConfig;
    }

}
