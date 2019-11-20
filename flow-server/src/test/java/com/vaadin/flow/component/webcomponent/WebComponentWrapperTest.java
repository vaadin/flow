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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
import com.vaadin.flow.server.webcomponent.WebComponentBinding;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;

import elemental.json.Json;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebComponentWrapperTest {

    private static final String MSG_PROPERTY = "message";
    private static final String INT_PROPERTY = "integer-value";
    private static final String BOOLEAN_PROPERTY = "boolean-value";

    private Element element;
    private MyComponent component;
    private WebComponentBinding<MyComponent> binding;
    private WebComponentExporter<MyComponent> exporter;
    private WebComponentWrapper wrapper;

    @Before
    public void init() {
        element = new Element("tag");
        exporter = new MyComponentExporter();

        // make component available and bind properties to it
        binding = (WebComponentBinding<MyComponent>) new WebComponentExporter
                .WebComponentConfigurationFactory().create(exporter)
                .createWebComponentBinding(new MockInstantiator(), element, Json.createObject());
        wrapper = new WebComponentWrapper(element, binding);
        component = binding.getComponent();
    }

    @Test
    public void wrappedMyComponent_syncSetsCorrectValuesToFields() {
        wrapper.sync(MSG_PROPERTY, Json.create("MyMessage"));

        Assert.assertEquals("Message field should have updated with new value",
                "MyMessage", component.message);

        wrapper.sync(INT_PROPERTY, Json.create(10));

        Assert.assertEquals(
                "IntegerValue field should contain a matching integer value",
                10, component.integerValue);
    }

    @Test
    public void wrappedComponentPropertyListener_listenerFiredWithCorrectValuesOnSync() {
        wrapper.sync(MSG_PROPERTY, Json.create("one"));
        wrapper.sync(INT_PROPERTY, Json.create(2));
        wrapper.sync(MSG_PROPERTY, Json.create("three"));
        wrapper.sync(INT_PROPERTY, Json.create(4));

        // 3, since creation sets the initial value
        Assert.assertEquals("Three string messages should have come through", 3,
                component.oldMessages.size());

        // 3, since creation sets the initial value
        Assert.assertEquals("Three integer messages should have come through", 3,
                component.oldIntegers.size());

        Assert.assertEquals("String messages arrived in correct order",
                Arrays.asList("", "one", "three"), component.oldMessages);

        Assert.assertEquals("Integer messages arrived in correct order",
                Arrays.asList(0, 2, 4), component.oldIntegers);

    }

    @Test
    public void exportingExtendedComponent_inheritedFieldsAreAvailableAndOverridden() {
        WebComponentBinding<MyExtension> binding =
                constructWrapperAndGetBinding(new MyExtensionExporter(), null, null);

        MyExtension component = binding.getComponent();

        wrapper.sync(MSG_PROPERTY, Json.create("one"));
        wrapper.sync(INT_PROPERTY, Json.create(2));
        wrapper.sync(MSG_PROPERTY, Json.create("three"));
        wrapper.sync(INT_PROPERTY, Json.create(4));

        // 3, since creation sets the initial value
        Assert.assertEquals("Three string messages should have come through", 3,
                component.oldMessages.size());

        // 3, since creation sets the initial value
        Assert.assertEquals("Three integer messages should have come through", 3,
                component.oldIntegers.size());

        Assert.assertEquals("String messages arrived in correct order",
                Arrays.asList("Extended ", "Extended one", "Extended three"),
                component.oldMessages);

        Assert.assertEquals("Integer messages arrived in correct order",
                Arrays.asList(0, 2, 4), component.oldIntegers);
    }

    @Test
    public void extendedExporter_propertiesAreOverwrittenAndAvailable() {
        WebComponentBinding<MyComponent> binding =
                constructWrapperAndGetBinding(new ExtendedExporter(),
                null, null);

        MyComponent component = binding.getComponent();

        wrapper.sync(MSG_PROPERTY, Json.create("one"));
        wrapper.sync(INT_PROPERTY, Json.create(2));
        wrapper.sync(MSG_PROPERTY, Json.create("three"));
        wrapper.sync(INT_PROPERTY, Json.create(4));
        wrapper.sync(BOOLEAN_PROPERTY, Json.create(true));

        // 3, since creation sets the initial value
        Assert.assertEquals("Three string messages should have come through", 3,
                component.oldMessages.size());

        // 3, since creation sets the initial value
        Assert.assertEquals("Three integer messages should have come through", 3,
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
    public void disconnectReconnect_componentIsNotCleaned() {
        Element element = new Element("tag");
        WebComponentUI ui = constructWebComponentUI(element);
        constructWrapperAndGetBinding(new MyComponentExporter(), element, ui);
        UIInternals internals = ui.getInternals();

        wrapper.disconnected();

        internals.setLastHeartbeatTimestamp(System.currentTimeMillis());

        Assert.assertTrue("Wrapper should still be connected on the server",
                wrapper.getParent().isPresent());

        wrapper.reconnect();

        internals.setLastHeartbeatTimestamp(System.currentTimeMillis() + 1200);

        Assert.assertTrue("Wrapper should stay connected on the server",
                wrapper.getParent().isPresent());
    }

    @Test
    public void disconnectOnClient_componentIsCleaned() {
        Element element = new Element("tag");
        WebComponentUI ui = constructWebComponentUI(element);
        constructWrapperAndGetBinding(new MyComponentExporter(), element, ui);
        UIInternals internals = ui.getInternals();

        wrapper.disconnected();

        internals.setLastHeartbeatTimestamp(System.currentTimeMillis());

        Assert.assertTrue("Wrapper should still be connected on the server",
                wrapper.getParent().isPresent());

        internals.setLastHeartbeatTimestamp(System.currentTimeMillis() + 1200);

        Assert.assertFalse(
                "Wrapper should have been disconnected also on the server",
                wrapper.getParent().isPresent());
    }

    /**
     * @param exporter
     *         exporter of the correct type, defines C
     * @param element
     *         nullable root element
     * @param ui
     *         nullable WebComponentUI
     * @param <C>
     *         type of the exported component
     * @return web component wrapper
     */
    private <C extends Component> WebComponentBinding<C> constructWrapperAndGetBinding(
            WebComponentExporter<C> exporter, Element element,
            WebComponentUI ui) {
        if (element == null) {
            element = new Element("tag");
        }
        WebComponentBinding<C> binding = (WebComponentBinding<C>)
                new WebComponentExporter.WebComponentConfigurationFactory().create(exporter)
                        .createWebComponentBinding(new MockInstantiator(), element, Json.createObject());
        wrapper = new WebComponentWrapper(element, binding) {
            @Override
            public Optional<UI> getUI() {
                return Optional.of(ui);
            }
        };
        return binding;
    }

    private static WebComponentUI constructWebComponentUI(
            Element wrapperElement) {
        WebComponentUI ui = mock(WebComponentUI.class);
        when(ui.getUI()).thenReturn(Optional.of(ui));
        Element body = new Element("body");
        when(ui.getElement()).thenReturn(body);

        UIInternals internals = new UIInternals(ui);
        internals.setSession(
                new AlwaysLockedVaadinSession(mock(VaadinService.class)));
        when(ui.getInternals()).thenReturn(internals);

        Component parent = new Parent();
        parent.getElement().appendVirtualChild(wrapperElement);

        VaadinSession session = mock(VaadinSession.class);
        DeploymentConfiguration configuration = mock(
                DeploymentConfiguration.class);

        when(ui.getSession()).thenReturn(session);
        when(session.getConfiguration()).thenReturn(configuration);
        when(configuration.getWebComponentDisconnect()).thenReturn(1);

        return ui;
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

    public static class MyComponentExporter
            extends WebComponentExporter<MyComponent> {

        public MyComponentExporter() {
            this("my-component");
        }

        // extension point
        protected MyComponentExporter(String tag) {
            super(tag);
            addProperty(MSG_PROPERTY, "")
                    .onChange(MyComponent::setMessage);
            addProperty(INT_PROPERTY, 0)
                    .onChange(MyComponent::setIntegerValue);
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                                      MyComponent component) {
        }
    }

    public static class MyExtensionExporter
            extends WebComponentExporter<MyExtension> {

        public MyExtensionExporter() {
            super("extended-component");
            addProperty(MSG_PROPERTY, "")
                    .onChange(MyExtension::setMessage);
            addProperty(INT_PROPERTY, 0)
                    .onChange(MyExtension::setIntegerValue);
        }

        @Override
        public void configureInstance(WebComponent<MyExtension> webComponent,
                                      MyExtension component) {
        }
    }

    public static class ExtendedExporter extends MyComponentExporter {
        public ExtendedExporter() {
            super("my-component-extended");

            addProperty(MSG_PROPERTY, "Default")
                    .onChange(MyComponent::setMessage);

            addProperty(BOOLEAN_PROPERTY, false)
                    .onChange(MyComponent::setBooleanValue);
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent, MyComponent component) {
            super.configureInstance(webComponent, component);
        }
    }
}
