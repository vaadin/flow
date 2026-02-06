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
package com.vaadin.tests.server.component;

import java.io.OutputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.PropertyChangeDeniedException;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.StreamReceiver;
import com.vaadin.flow.server.StreamVariable;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.testutil.ClassesSerializableTest;
import com.vaadin.signals.local.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FlowClassesSerializableTest extends ClassesSerializableTest {

    /**
     * {@link HtmlComponent} and {@link HtmlContainer} are not covered by
     * generic test because of their constructors
     */
    @Test
    public void htmlComponentAndHtmlContainer() throws Throwable {
        Component[] components = { new HtmlComponent("dummy-tag"),
                new HtmlContainer("dummy-tag") };
        for (Component component : components) {
            Component componentCopy = serializeAndDeserialize(component);
            assertEquals(component.getElement().getTag(),
                    componentCopy.getElement().getTag());
            assertNotSame(component.getElement(), componentCopy.getElement());
        }
    }

    /**
     * Tests a serialization bug (probably located in JVM ) when serialized
     * {@link Command} is deserialized as some internal lambda and produces
     * {@link ClassCastException}
     *
     * see the workaround in ElementAttributeMap#deferRegistration
     */
    @Test
    public void streamResource() throws Throwable {
        UI ui = new UI();
        UI.setCurrent(ui);
        try {
            Element element = new Element("dummy-element");
            StreamReceiver streamReceiver = new StreamReceiver(
                    element.getNode(), "upload", new MyStreamVariable());
            Assert.assertEquals(ui, UI.getCurrent());
            element.setAttribute("target", streamReceiver);
            serializeAndDeserialize(element);
            assertTrue("Basic smoke test with ",
                    element.getAttribute("target").length() > 10);

        } finally {
            UI.setCurrent(null);
        }
    }

    @Test
    public void componentEffectSerializable() {
        CurrentInstance.clearAll();
        var service = new MockVaadinServletService() {
            private final Lock lock = new ReentrantLock();
            {
                lock.lock();
            }

            @Override
            protected Lock getSessionLock(WrappedSession wrappedSession) {
                return lock;
            }

            @Override
            public void init() {
                super.init();

                ApplicationConfiguration configuration = Mockito
                        .mock(ApplicationConfiguration.class);
                Mockito.when(configuration.isProductionMode()).thenReturn(
                        getDeploymentConfiguration().isProductionMode());
                Mockito.when(
                        configuration.isDevModeSessionSerializationEnabled())
                        .thenReturn(true);
                getContext().setAttribute(ApplicationConfiguration.class,
                        configuration);
            }
        };
        VaadinService.setCurrent(service);
        var session = new MockVaadinSession(service);
        session.lock();
        session.refreshTransients(null, service);
        MockUI ui = new MockUI(session);
        ui.doInit(null, 42, "foo");
        session.addUI(ui);

        ValueSignal<String> signal = new ValueSignal<>("initial");
        SerializedComponent component = new SerializedComponent(signal);
        ui.add(component);
        Assert.assertEquals(1, component.effectExecutionCounter);

        // verify that signal works before serialization
        signal.value("changed");
        Assert.assertEquals(2, component.effectExecutionCounter);

        SerializedComponent deserializedComponent;
        VaadinSession deserializedSession = null;
        session.unlock(); // serialization happens for unlocked session
        try {
            deserializedSession = serializeAndDeserialize(session);
            assertNotNull(deserializedSession);
            assertNotSame(deserializedSession, session);
        } catch (Throwable e) {
            fail("ComponentEffect should be serializable: " + e.getClass()
                    + ": " + e.getMessage());
        }
        deserializedSession.refreshTransients(null, service);
        deserializedSession.lock();

        UI deserializedUi = deserializedSession.getUIs().iterator().next();
        deserializedComponent = deserializedUi.getChildren()
                .filter(SerializedComponent.class::isInstance)
                .map(SerializedComponent.class::cast).findFirst()
                .orElseThrow(() -> new AssertionError(
                        "SerializedComponent has not been deserialized"));
        assertNotSame(deserializedComponent, component);

        UI.setCurrent(deserializedUi);
        deserializedComponent.signal.value("changed after deserialization");
        Assert.assertEquals(3, deserializedComponent.effectExecutionCounter);
        deserializedComponent.signal.value("changed");
        Assert.assertEquals(4, deserializedComponent.effectExecutionCounter);

        signal.value("changed in original signal");
        // original signal change should not affect deserialized component
        Assert.assertEquals(4, deserializedComponent.effectExecutionCounter);

        // remove registration and verify that effect is not called anymore
        deserializedComponent.registration.remove();
        deserializedComponent.signal.value("foo");
        Assert.assertEquals(4, deserializedComponent.effectExecutionCounter);

        // verify various bindX methods
        Assert.assertEquals("foo",
                deserializedComponent.getElement().getText());
        Assert.assertEquals("foo",
                deserializedComponent.getElement().getAttribute("attr"));
        Assert.assertEquals("foo",
                deserializedComponent.getElement().getProperty("prop"));
        Assert.assertEquals("foo!!!",
                deserializedComponent.getElement().getProperty("two-way-prop"));
        // verify that two-way-binding works
        emulateClientUpdate(deserializedComponent.getElement(), "two-way-prop",
                "bar!!!");
        Assert.assertEquals("bar!!!",
                deserializedComponent.getElement().getProperty("two-way-prop"));
        Assert.assertEquals("bar", deserializedComponent.signal.peek());

        // verify mapped and computed signals with bindEnabled and bindVisible
        Assert.assertTrue(deserializedComponent.getElement().isEnabled());
        Assert.assertTrue(deserializedComponent.getElement().isVisible());
        deserializedComponent.signal.value(null);
        Assert.assertFalse(deserializedComponent.getElement().isEnabled());
        Assert.assertFalse(deserializedComponent.getElement().isVisible());

        deserializedSession.unlock();
        VaadinService.setCurrent(null);
    }

    private void emulateClientUpdate(Element element, String property,
            String value) {
        ElementPropertyMap childModel = ElementPropertyMap
                .getModel(element.getNode());
        try {
            childModel.deferredUpdateFromClient(property, value);
        } catch (PropertyChangeDeniedException e) {
            Assert.fail(
                    "Failed to update property from client: " + e.getMessage());
        }
    }

    private static class MyStreamVariable implements StreamVariable {
        @Override
        public OutputStream getOutputStream() {
            return null;
        }

        @Override
        public boolean listenProgress() {
            return false;
        }

        @Override
        public void onProgress(StreamingProgressEvent event) {

        }

        @Override
        public void streamingStarted(StreamingStartEvent event) {

        }

        @Override
        public void streamingFinished(StreamingEndEvent event) {

        }

        @Override
        public void streamingFailed(StreamingErrorEvent event) {

        }

        @Override
        public boolean isInterrupted() {
            return false;
        }
    }
}
