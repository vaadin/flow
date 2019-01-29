package com.vaadin.flow.component.webcomponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.WebComponent;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;

public class WebComponentWrapperTest {

    @Test
    public void wrappedMyComponent_syncSetsCorrectValuesToBothFieldAndMethod() {
        MyComponent component = new MyComponent();
        WebComponentWrapper wrapper = new WebComponentWrapper("my-component",
                component);

        wrapper.sync("response", "test value");

        Assert.assertEquals("Response field should have updated with new value",
                "test value", component.response.get());

        wrapper.sync("message", "MyMessage");

        Assert.assertEquals(
                "Message should have updated through 'setMessage' method",
                "MyMessage", component.message);

        wrapper.sync("integerValue", "10");

        Assert.assertEquals(
                "IntegerValue field should contain a matching integer value",
                Integer.valueOf(10), component.integerValue.get());
    }

    @Test
    public void wrappedComponentPropertyListener_listenerFiredWithCorrectValuesOnSync() {
        MyComponent component = new MyComponent();
        WebComponentWrapper wrapper = new WebComponentWrapper("my-component",
                component);

        List<PropertyValueChangeEvent<?>> events = new ArrayList<>();

        component.response.addValueChangeListener(events::add);
        component.integerValue.addValueChangeListener(events::add);

        wrapper.sync("response", "update");
        wrapper.sync("integerValue", "15");

        Assert.assertEquals(
                "Only one event for each sync should have taken place", 2,
                events.size());

        Assert.assertEquals("First event source should be 'response'",
                component.response, events.get(0).getSource());

        Assert.assertEquals("Second event source should be 'integerValue'",
                component.integerValue, events.get(1).getSource());

        Assert.assertEquals("OldValue should match default value", "hello",
                events.get(0).getOldValue());
        Assert.assertEquals("NewValue should match updated value", "update",
                events.get(0).getNewValue());

        Assert.assertNull("OldValue should be null as no default was given",
                events.get(1).getOldValue());
        Assert.assertEquals("New value should be a matching Integer",
                Integer.valueOf(15), events.get(1).getNewValue());

    }

    @Test
    public void extendingWebComponent_inheritedFieldsAreAvailableAndOverridden() {
        MyExtension component = new MyExtension();
        WebComponentWrapper wrapper = new WebComponentWrapper("my-extension",
                component);

        List<PropertyValueChangeEvent<?>> events = new ArrayList<>();

        component.response.addValueChangeListener(events::add);
        component.integerValue.addValueChangeListener(events::add);

        wrapper.sync("response", "update");
        wrapper.sync("integerValue", "15");

        Assert.assertEquals(
                "First event source should be 'response' from the extending class",
                component.response, events.get(0).getSource());

        Assert.assertEquals("Second event source should be 'integerValue'",
                component.integerValue, events.get(1).getSource());

        Assert.assertEquals("OldValue should match default value", "Hi",
                events.get(0).getOldValue());
        Assert.assertEquals("NewValue should match updated value", "update",
                events.get(0).getNewValue());

        Assert.assertNull("OldValue should be null as no default was given",
                events.get(1).getOldValue());
        Assert.assertEquals("New value should be a matching Integer",
                Integer.valueOf(15), events.get(1).getNewValue());
    }

    @Test
    public void extendingWebComponent_inheritedMethodsAreAvailableAndOverridden() {
        MyExtension component = new MyExtension();
        WebComponentWrapper wrapper = new WebComponentWrapper("my-extension",
                component);

        wrapper.sync("message", "MyMessage");

        Assert.assertEquals(
                "Message should have updated through 'setMessage' method",
                "MyMessage!", component.message);
    }

    @Test(expected = IllegalStateException.class)
    public void overlappingFieldAndMethodRegistration_syncFailsWithAnException() {
        Broken component = new Broken();
        WebComponentWrapper wrapper = new WebComponentWrapper("my-extension",
                component);

        wrapper.sync("message", "hello");

        Assert.fail(
                "Synchronisation of property for which both method and field exists should have thrown!");
    }

    @Test
    public void disconnectReconnect_componentIsNotCleaned()
            throws ExecutionException, InterruptedException {
        WebComponentUI ui = Mockito.mock(WebComponentUI.class);
        Mockito.when(ui.getUI()).thenReturn(Optional.of(ui));
        Element body = new Element("body");
        Mockito.when(ui.getElement()).thenReturn(body);

        UIInternals internals = new UIInternals(ui);
        internals.setSession(new AlwaysLockedVaadinSession(
                Mockito.mock(VaadinService.class)));
        Mockito.when(ui.getInternals()).thenReturn(internals);

        MyComponent component = new MyComponent();
        WebComponentWrapper wrapper = new WebComponentWrapper("my-component",
                component) {
            @Override
            public Optional<UI> getUI() {
                return Optional.of(ui);
            }
        };

        Component parent = new Parent();
        parent.getElement().appendChild(wrapper.getElement());

        VaadinSession session = Mockito.mock(VaadinSession.class);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);

        Mockito.when(ui.getSession()).thenReturn(session);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);
        Mockito.when(configuration.getWebComponentDisconnect()).thenReturn(1);

        wrapper.disconnected();

        internals.setLastHeartbeatTimestamp(System.currentTimeMillis());

        Assert.assertTrue("Wrapper should still be connected on the server",
                wrapper.getParent().isPresent());

        wrapper.reconnect();

        internals.setLastHeartbeatTimestamp(System.currentTimeMillis());

        Thread.sleep(1200);

        internals.setLastHeartbeatTimestamp(System.currentTimeMillis());

        Assert.assertTrue("Wrapper should stay connected on the server",
                wrapper.getParent().isPresent());
    }

    private void awaitTerminationAfterShutdown(
            ScheduledExecutorService threadPool) {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(2, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
            Assert.fail("Executor was interrupted");
        }
    }

    @Test
    public void disconnectOnClient_componentIsCleaned()
            throws ExecutionException, InterruptedException {
        WebComponentUI ui = Mockito.mock(WebComponentUI.class);
        Mockito.when(ui.getUI()).thenReturn(Optional.of(ui));
        Element body = new Element("body");
        Mockito.when(ui.getElement()).thenReturn(body);

        UIInternals internals = new UIInternals(ui);
        internals.setSession(new AlwaysLockedVaadinSession(
                Mockito.mock(VaadinService.class)));
        Mockito.when(ui.getInternals()).thenReturn(internals);

        MyComponent component = new MyComponent();
        WebComponentWrapper wrapper = new WebComponentWrapper("my-component",
                component) {
            @Override
            public Optional<UI> getUI() {
                return Optional.of(ui);
            }
        };

        Component parent = new Parent();
        parent.getElement().appendChild(wrapper.getElement());

        VaadinSession session = Mockito.mock(VaadinSession.class);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);

        Mockito.when(ui.getSession()).thenReturn(session);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);
        Mockito.when(configuration.getWebComponentDisconnect()).thenReturn(1);

        wrapper.disconnected();

        internals.setLastHeartbeatTimestamp(System.currentTimeMillis());

        Assert.assertTrue("Wrapper should still be connected on the server",
                wrapper.getParent().isPresent());

        Thread.sleep(1200);

        internals.setLastHeartbeatTimestamp(System.currentTimeMillis());

        Assert.assertFalse(
                "Wrapper should have been disconnected also on the server",
                wrapper.getParent().isPresent());
    }

    @WebComponent("my-component")
    public static class MyComponent extends Component {

        protected String message;
        protected WebComponentProperty<String> response = new WebComponentProperty<>(
                "hello", String.class);
        protected WebComponentProperty<Integer> integerValue = new WebComponentProperty<>(
                Integer.class);

        public MyComponent() {
            super(new Element("div"));
        }

        @WebComponentMethod("message")
        public void setMessage(String message) {
            this.message = message;
        }

    }

    @WebComponent("my-extension")
    public static class MyExtension extends MyComponent {

        protected WebComponentProperty<String> response = new WebComponentProperty<>(
                "Hi", String.class);

        @WebComponentMethod("message")
        public void setMyFancyMessage(String extendedMessage) {
            message = extendedMessage + "!";
        }
    }

    @WebComponent("broken-component")
    public static class Broken extends Component {
        protected WebComponentProperty<String> message = new WebComponentProperty<>(
                "", String.class);

        public Broken() {
            super(new Element("div"));
        }

        @WebComponentMethod("message")
        public void setMessage(String message) {
        }
    }

    @Tag("div")
    public static class Parent extends Component {
    }
}
