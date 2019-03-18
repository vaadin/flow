/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.component.webcomponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.MockInstantiator;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationImpl;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;

import elemental.json.Json;

public class WebComponentWrapperTest {

    private static final String MSG_PROPERTY = "message";
    private static final String INT_PROPERTY = "integerValue";
    private static final String BOOLEAN_PROPERTY = "booleanValue";

    private MyComponent component;
    private WebComponentBinding<MyComponent> binding;
    private WebComponentConfiguration<MyComponent> configuration;

    @Before
    public void init() {
        configuration = new WebComponentConfigurationImpl<>("my-component",
                new MyComponentExporter());
        // make component available and bind properties to it
        binding = configuration.createBinding(new MockInstantiator());
        component = binding.getComponent();
    }


    @Test
    public void wrappedMyComponent_syncSetsCorrectValuesToFields() {
        WebComponentWrapper wrapper = new WebComponentWrapper("my-component",
                binding);

        wrapper.sync(MSG_PROPERTY, Json.create("MyMessage"));

        Assert.assertEquals(
                "Message field should have updated with new value",
                "MyMessage", component.message);

        wrapper.sync(INT_PROPERTY, Json.create(10));

        Assert.assertEquals(
                "IntegerValue field should contain a matching integer value",
                10, component.integerValue);
    }

    @Test
    public void wrappedComponentPropertyListener_listenerFiredWithCorrectValuesOnSync() {
        WebComponentWrapper wrapper = new WebComponentWrapper("my-component",
                binding);

        wrapper.sync(MSG_PROPERTY, Json.create("one"));
        wrapper.sync(INT_PROPERTY, Json.create(2));
        wrapper.sync(MSG_PROPERTY, Json.create("three"));
        wrapper.sync(INT_PROPERTY, Json.create(4));

        // 3, since creation sets the initial value
        Assert.assertEquals(
                "Two string messages should have come through", 3,
                component.oldMessages.size());

        // 3, since creation sets the initial value
        Assert.assertEquals(
                "Two integer messages should have come through", 3,
                component.oldIntegers.size());

        Assert.assertEquals("String messages arrived in correct order",
                Arrays.asList("", "one", "three"), component.oldMessages);

        Assert.assertEquals("Integer messages arrived in correct order",
                Arrays.asList(0, 2,4), component.oldIntegers);

    }

    @Test
    public void exportingExtendedComponent_inheritedFieldsAreAvailableAndOverridden() {
        WebComponentConfiguration<MyExtension> configuration =
                new WebComponentConfigurationImpl<>("extended-component",
                        new MyExtensionExporter());
        WebComponentBinding<MyExtension> binding =
                configuration.createBinding(new MockInstantiator());
        MyExtension component = binding.getComponent();

        WebComponentWrapper wrapper = new WebComponentWrapper("extension" +
                "-component", binding);

        wrapper.sync(MSG_PROPERTY, Json.create("one"));
        wrapper.sync(INT_PROPERTY, Json.create(2));
        wrapper.sync(MSG_PROPERTY, Json.create("three"));
        wrapper.sync(INT_PROPERTY, Json.create(4));

        // 3, since creation sets the initial value
        Assert.assertEquals(
                "Two string messages should have come through", 3,
                component.oldMessages.size());

        // 3, since creation sets the initial value
        Assert.assertEquals(
                "Two integer messages should have come through", 3,
                component.oldIntegers.size());

        Assert.assertEquals("String messages arrived in correct order",
                Arrays.asList("Extended ", "Extended one", "Extended three"),
                component.oldMessages);

        Assert.assertEquals("Integer messages arrived in correct order",
                Arrays.asList(0, 2,4), component.oldIntegers);
    }

    @Test
    public void extendedExporter_propertiesAreOverwrittenAndAvailable() {
        WebComponentConfiguration<MyComponent> configuration =
                new WebComponentConfigurationImpl<>("my-component-extended",
                        new ExtendedExporter());
        WebComponentBinding<MyComponent> binding =
                configuration.createBinding(new MockInstantiator());
        MyComponent component = binding.getComponent();

        WebComponentWrapper wrapper = new WebComponentWrapper("extension" +
                "-component", binding);

        wrapper.sync(MSG_PROPERTY, Json.create("one"));
        wrapper.sync(INT_PROPERTY, Json.create(2));
        wrapper.sync(MSG_PROPERTY, Json.create("three"));
        wrapper.sync(INT_PROPERTY, Json.create(4));
        wrapper.sync(BOOLEAN_PROPERTY, Json.create(true));

        // 3, since creation sets the initial value
        Assert.assertEquals(
                "Two string messages should have come through", 3,
                component.oldMessages.size());

        // 3, since creation sets the initial value
        Assert.assertEquals(
                "Two integer messages should have come through", 3,
                component.oldIntegers.size());

        Assert.assertEquals("String messages arrived in correct order",
                Arrays.asList("Default", "one", "three"),
                component.oldMessages);

        Assert.assertEquals("Integer messages arrived in correct order",
                Arrays.asList(0, 2, 4), component.oldIntegers);

        Assert.assertTrue("Boolean property should have been set to true",
                component.booleanValue);
    }

    @Test
    public void disconnectReconnect_componentIsNotCleaned()
            throws InterruptedException {
        WebComponentUI ui = Mockito.mock(WebComponentUI.class);
        Mockito.when(ui.getUI()).thenReturn(Optional.of(ui));
        Element body = new Element("body");
        Mockito.when(ui.getElement()).thenReturn(body);

        UIInternals internals = new UIInternals(ui);
        internals.setSession(new AlwaysLockedVaadinSession(
                Mockito.mock(VaadinService.class)));
        Mockito.when(ui.getInternals()).thenReturn(internals);

        WebComponentWrapper wrapper = new WebComponentWrapper("my-component",
                binding) {
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

        WebComponentWrapper wrapper = new WebComponentWrapper("my-component",
                binding) {
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

    @Tag("my-component")
    public static class MyComponent extends Component {
        ArrayList<String> oldMessages = new ArrayList<>();
        ArrayList<Integer> oldIntegers = new ArrayList<>();
        protected String message;
        int integerValue;
        boolean booleanValue;

        public MyComponent() {
            super(new Element("div"));
        }

        public void setMessage(String message) {
            oldMessages.add(message);
            this.message = message;
        }

        public void setIntegerValue(int integerValue) {
            this.oldIntegers.add(integerValue);
            this.integerValue = integerValue;
        }

        public void setBooleanValue(boolean value) {
            booleanValue = value;
        }
    }

    public static class MyExtension extends MyComponent {
        @Override
        public void setMessage(String message) {
            super.setMessage("Extended " + message);
        }
    }

    @Tag("div")
    public static class Parent extends Component {
    }

    @Tag("my-component")
    public static class MyComponentExporter implements WebComponentExporter<MyComponent> {
        @Override
        public void define(WebComponentDefinition<MyComponent> definition) {
            definition.addProperty(MSG_PROPERTY, "")
                    .onChange(MyComponent::setMessage);
            definition.addProperty(INT_PROPERTY, 0)
                    .onChange(MyComponent::setIntegerValue);
        }
    }

    @Tag("extended-component")
    public static class MyExtensionExporter implements WebComponentExporter<MyExtension> {
        @Override
        public void define(WebComponentDefinition<MyExtension> definition) {
            definition.addProperty(MSG_PROPERTY, "")
                    .onChange(MyExtension::setMessage);
            definition.addProperty(INT_PROPERTY, 0)
                    .onChange(MyExtension::setIntegerValue);
        }
    }

    @Tag("my-component-extended")
    public static class ExtendedExporter extends MyComponentExporter {
        @Override
        public void define(WebComponentDefinition<MyComponent> definition) {
            super.define(definition);

            definition.addProperty(MSG_PROPERTY, "Default")
                    .onChange(MyComponent::setMessage);

            definition.addProperty(BOOLEAN_PROPERTY, false)
                    .onChange(MyComponent::setBooleanValue);
        }
    }
}
