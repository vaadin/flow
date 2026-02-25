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

import java.io.NotSerializableException;
import java.io.OutputStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Test;
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
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.signals.shared.SharedValueSignal;
import com.vaadin.flow.testutil.ClassesSerializableTest;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class FlowClassesSerializableTest extends ClassesSerializableTest {

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
            assertEquals(ui, UI.getCurrent());
            element.setAttribute("target", streamReceiver);
            serializeAndDeserialize(element);
            assertTrue(element.getAttribute("target").length() > 10,
                    "Basic smoke test with ");

        } finally {
            UI.setCurrent(null);
        }
    }

    private MockVaadinServletService createTestService() {
        return new MockVaadinServletService() {
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
    }

    private MockUI createSessionAndUI(MockVaadinServletService service) {
        var session = new MockVaadinSession(service);
        session.lock();
        session.refreshTransients(null, service);
        MockUI ui = new MockUI(session);
        ui.doInit(null, 42, "foo");
        session.addUI(ui);
        return ui;
    }

    private MockUI setupForSignalSerializationTest() {
        CurrentInstance.clearAll();
        MockVaadinServletService service = createTestService();
        VaadinService.setCurrent(service);
        return createSessionAndUI(service);
    }

    @Test
    public void localSignalSerializable() {
        MockUI ui = setupForSignalSerializationTest();
        VaadinSession session = ui.getSession();
        VaadinService service = session.getService();

        ValueSignal<String> signal = new ValueSignal<>("initial");
        SerializedLocalSignalComponent component = new SerializedLocalSignalComponent(
                signal);
        ui.add(component);
        assertEquals(1, component.effectExecutionCounter);

        // verify that signal works before serialization
        signal.set("changed");
        assertEquals(2, component.effectExecutionCounter);

        SerializedLocalSignalComponent deserializedComponent;
        VaadinSession deserializedSession = null;
        session.unlock(); // serialization happens for unlocked session
        try {
            deserializedSession = serializeAndDeserialize(session);
            assertNotNull(deserializedSession);
            assertNotSame(deserializedSession, session);
        } catch (Throwable e) {
            CurrentInstance.clearAll();
            fail("SerializedLocalSignalComponent should be serializable: "
                    + e.getClass() + ": " + e.getMessage());
        }
        deserializedSession.refreshTransients(null, service);
        deserializedSession.lock();

        UI deserializedUi = deserializedSession.getUIs().iterator().next();
        deserializedComponent = deserializedUi.getChildren()
                .filter(SerializedLocalSignalComponent.class::isInstance)
                .map(SerializedLocalSignalComponent.class::cast).findFirst()
                .orElseThrow(() -> new AssertionError(
                        "SerializedLocalSignalComponent has not been deserialized"));
        assertNotSame(deserializedComponent, component);

        UI.setCurrent(deserializedUi);
        deserializedComponent.signal.set("changed after deserialization");
        assertEquals(3, deserializedComponent.effectExecutionCounter);
        deserializedComponent.signal.set("changed");
        assertEquals(4, deserializedComponent.effectExecutionCounter);

        signal.set("changed in original signal");
        // original signal change should not affect deserialized component
        assertEquals(4, deserializedComponent.effectExecutionCounter);

        // remove registration and verify that effect is not called anymore
        deserializedComponent.registration.remove();
        deserializedComponent.signal.set("foo");
        assertEquals(4, deserializedComponent.effectExecutionCounter);

        // verify various bindX methods
        assertEquals("foo", deserializedComponent.getElement().getText());
        assertEquals("foo",
                deserializedComponent.getElement().getAttribute("attr"));
        assertEquals("foo",
                deserializedComponent.getElement().getProperty("prop"));
        assertEquals("foo!!!",
                deserializedComponent.getElement().getProperty("two-way-prop"));
        // verify that two-way-binding works
        emulateClientUpdate(deserializedComponent.getElement(), "two-way-prop",
                "bar!!!");
        assertEquals("bar!!!",
                deserializedComponent.getElement().getProperty("two-way-prop"));
        assertEquals("bar", deserializedComponent.signal.peek());

        // verify mapped and computed signals with bindEnabled and bindVisible
        assertTrue(deserializedComponent.getElement().isEnabled());
        assertTrue(deserializedComponent.getElement().isVisible());
        deserializedComponent.signal.set(null);
        assertFalse(deserializedComponent.getElement().isEnabled());
        assertFalse(deserializedComponent.getElement().isVisible());

        deserializedSession.unlock();
        CurrentInstance.clearAll();
    }

    @Test
    public void sharedSignalNotSerializable() {
        MockUI ui = setupForSignalSerializationTest();
        VaadinSession session = ui.getSession();
        VaadinService service = session.getService();

        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");
        SerializedSharedSignalComponent component = new SerializedSharedSignalComponent(
                signal);
        ui.add(component);
        assertEquals(1, component.effectExecutionCounter);

        // verify that signal works before serialization
        signal.set("changed");
        assertEquals(2, component.effectExecutionCounter);

        session.unlock(); // serialization happens for unlocked session
        try {
            serializeAndDeserialize(session);
            CurrentInstance.clearAll();
            fail("Serialization should have failed because of shared signal");
        } catch (NotSerializableException e) {
            // OK, expected because shared signals are not serializable
            CurrentInstance.clearAll();
        } catch (Throwable e) {
            CurrentInstance.clearAll();
            fail("Expected NotSerializableException. Session serialization throws this instead: "
                    + e.getClass() + ": " + e.getMessage());
        }
    }

    @Test
    public void sharedSignalNotSerializable_twoSessionsSharingSignal_onlyOneSessionAttemptedToSerialize() {
        // Create both services before touching any CurrentInstance state so
        // neither service's construction clobbers the other's ThreadLocals.
        CurrentInstance.clearAll();
        MockVaadinServletService serviceA = createTestService();
        MockVaadinServletService serviceB = createTestService();

        // Build session A (the one to serialize) and session B independently,
        // each backed by their own service. No CurrentInstance.clearAll() is
        // called between them so both remain valid.
        VaadinService.setCurrent(serviceA);
        MockUI uiA = createSessionAndUI(serviceA);
        VaadinSession sessionA = uiA.getSession();

        VaadinService.setCurrent(serviceB);
        MockUI uiB = createSessionAndUI(serviceB);
        VaadinSession sessionB = uiB.getSession();

        // Point CurrentInstance at serviceA for the rest of the test
        VaadinService.setCurrent(serviceA);

        // Both sessions share the exact same SharedValueSignal instance
        // (same SignalTree), mimicking a signal stored in e.g. a static field
        SharedValueSignal<String> signal = new SharedValueSignal<>("initial");

        UI.setCurrent(uiA);
        SerializedSharedSignalComponent componentA = new SerializedSharedSignalComponent(
                signal);
        uiA.add(componentA);
        assertEquals(1, componentA.effectExecutionCounter);

        VaadinService.setCurrent(serviceB);
        UI.setCurrent(uiB);
        SerializedSharedSignalComponent componentB = new SerializedSharedSignalComponent(
                signal);
        uiB.add(componentB);
        assertEquals(1, componentB.effectExecutionCounter);

        VaadinService.setCurrent(serviceA);
        UI.setCurrent(uiA);

        assertEquals(0,
                ((MockVaadinSession) uiA.getSession()).writeObjectCallCount);
        assertEquals(0,
                ((MockVaadinSession) uiB.getSession()).writeObjectCallCount);

        // Serialize only session A; session B must not be pulled in
        sessionA.unlock();
        sessionB.unlock();
        try {
            serializeAndDeserialize(sessionA);
            CurrentInstance.clearAll();
            fail("Serialization should have failed because of shared signal");
        } catch (NotSerializableException e) {
            // OK, expected because shared signals are not serializable

            /*
             * Object tree sample during session A serialization showing where
             * session B would be leaked in this test if the shared signal was
             * not properly handled. Alternative paths are via
             * ElementEffect#effect, SignalTree#subscribers,
             * AsynchronousSignalTree#unconfirmedCommands:
             *
             * SessionA → ComponentA → SharedValueSignal →
             * AsynchronousSignalTree (inherited SignalTree) → observers map →
             * TransientListener (session B's effect lambda) → closes over uiB →
             * VaadinSession B ← LEAK
             */
            assertEquals(0, ((MockVaadinSession) uiA
                    .getSession()).writeObjectCallCount);
            assertEquals(0, ((MockVaadinSession) uiB
                    .getSession()).writeObjectCallCount);
            CurrentInstance.clearAll();
        } catch (Throwable e) {
            CurrentInstance.clearAll();
            fail("Expected NotSerializableException. Session serialization throws this instead: "
                    + e.getClass() + ": " + e.getMessage());
        }
    }

    private void emulateClientUpdate(Element element, String property,
            String value) {
        ElementPropertyMap childModel = ElementPropertyMap
                .getModel(element.getNode());
        try {
            childModel.deferredUpdateFromClient(property, value);
        } catch (PropertyChangeDeniedException e) {
            fail("Failed to update property from client: " + e.getMessage());
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
