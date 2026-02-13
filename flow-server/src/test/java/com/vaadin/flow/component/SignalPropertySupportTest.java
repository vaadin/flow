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
package com.vaadin.flow.component;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class SignalPropertySupportTest {

    private static MockVaadinServletService service;

    private LinkedList<ErrorEvent> events;

    private AtomicInteger callCount;
    private AtomicReference<Object> lastValue;

    @BeforeClass
    public static void init() {
        service = new MockVaadinServletService();
    }

    @AfterClass
    public static void clean() {
        CurrentInstance.clearAll();
        service.destroy();
    }

    @Before
    public void before() {
        events = mockLockedSessionWithErrorHandler();
        callCount = new AtomicInteger(0);
        lastValue = new AtomicReference<>();
    }

    @After
    public void after() {
        Assert.assertTrue(events.isEmpty());
        CurrentInstance.clearAll();
        events = null;
    }

    private LinkedList<ErrorEvent> mockLockedSessionWithErrorHandler() {
        VaadinService.setCurrent(service);

        var session = new MockVaadinSession(service);
        session.lock();

        var ui = new MockUI(session);
        var events = new LinkedList<ErrorEvent>();
        session.setErrorHandler(events::add);

        return events;
    }

    @Test
    public void create_nullArguments_throwException() {
        assertThrows(NullPointerException.class,
                () -> SignalPropertySupport.create(null, value -> {
                }));
        assertThrows(NullPointerException.class,
                () -> SignalPropertySupport.create(new TestComponent(), null));
    }

    @Test
    public void get_notBound_nullValue() {
        var component = new TestComponent();
        SignalPropertySupport<String> signalPropertySupport = SignalPropertySupport
                .create(component, value -> {
                });
        assertNull(signalPropertySupport.get());
    }

    @Test
    public void get_boundButNotAttached_valueNotSetInitially() {
        var component = new TestComponent();

        ValueSignal<String> signal = new ValueSignal<>("foo");
        SignalPropertySupport<String> signalPropertySupport = SignalPropertySupport
                .create(component, value -> {
                    callCount.incrementAndGet();
                });
        signalPropertySupport.bind(signal);

        assertNull(signalPropertySupport.get());
        assertEquals(0, callCount.get());
    }

    @Test
    public void get_boundAndAttached_valueSet() {
        var component = new TestComponent();
        UI.getCurrent().add(component);

        ValueSignal<String> signal = new ValueSignal<>("foo");
        SignalPropertySupport<String> signalPropertySupport = SignalPropertySupport
                .create(component, value -> {
                    callCount.incrementAndGet();
                    lastValue.set(value);
                });
        signalPropertySupport.bind(signal);

        assertEquals("foo", signalPropertySupport.get());
        assertEquals("foo", lastValue.get());

        signal.set("bar");

        assertEquals("bar", signalPropertySupport.get());
        assertEquals("bar", lastValue.get());
        assertEquals(2, callCount.get());

    }

    @Test
    public void set_notBound_valueSet() {
        var component = new TestComponent();

        SignalPropertySupport<String> signalPropertySupport = SignalPropertySupport
                .create(component, value -> {
                    callCount.incrementAndGet();
                    lastValue.set(value);
                });
        signalPropertySupport.set("foo");

        assertEquals("foo", signalPropertySupport.get());
        assertEquals("foo", lastValue.get());

        signalPropertySupport.set("bar");

        assertEquals("bar", signalPropertySupport.get());
        assertEquals("bar", lastValue.get());
        assertEquals(2, callCount.get());
    }

    @Test
    public void set_alreadyBound_throwException() {
        var component = new TestComponent();
        ValueSignal<String> signal = new ValueSignal<>("foo");
        SignalPropertySupport<String> signalPropertySupport = SignalPropertySupport
                .create(component, value -> {
                    callCount.incrementAndGet();
                });
        signalPropertySupport.bind(signal);

        assertThrows(BindingActiveException.class,
                () -> signalPropertySupport.set("bar"));
        assertEquals(0, callCount.get());
    }

    @Test
    public void set_computedSignal_valueSet() {
        var component = new TestComponent();
        UI.getCurrent().add(component);

        SignalPropertySupport<String> signalPropertySupport = SignalPropertySupport
                .create(component, lastValue::set);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        signalPropertySupport
                .bind(Signal.computed(() -> "computed-" + signal.get()));
        assertEquals("computed-foo", signalPropertySupport.get());
        assertEquals("computed-foo", lastValue.get());
    }

    @Test
    public void set_mappedSignal_valueSet() {
        var component = new TestComponent();
        UI.getCurrent().add(component);

        SignalPropertySupport<String> signalPropertySupport = SignalPropertySupport
                .create(component, lastValue::set);
        ValueSignal<String> signal = new ValueSignal<>("foo");
        signalPropertySupport.bind(signal.map(value -> "mapped-" + value));
        assertEquals("mapped-foo", signalPropertySupport.get());
        assertEquals("mapped-foo", lastValue.get());
    }

    @Test
    public void bind_nullSignal_throwsNPE() {
        var component = new TestComponent();
        UI.getCurrent().add(component);

        SignalPropertySupport<String> signalPropertySupport = SignalPropertySupport
                .create(component, value -> {
                });

        assertThrows(NullPointerException.class,
                () -> signalPropertySupport.bind(null));
    }

    @Test
    public void bind_alreadyBound_throw() {
        var component = new TestComponent();
        UI.getCurrent().add(component);

        SignalPropertySupport<String> signalPropertySupport = SignalPropertySupport
                .create(component, value -> {
                });
        signalPropertySupport.bind(new ValueSignal<>("foo"));

        assertThrows(BindingActiveException.class,
                () -> signalPropertySupport.bind(new ValueSignal<>("foo")));
    }

    @Test
    public void bind_boundDetachedAttached_bindingRemovedAndAddedBack() {
        var component = new TestComponent();
        UI.getCurrent().add(component);

        SignalPropertySupport<String> signalPropertySupport = SignalPropertySupport
                .create(component, value -> {
                });
        ValueSignal<String> signal = new ValueSignal<>("foo");
        signalPropertySupport.bind(signal);
        assertEquals("foo", signalPropertySupport.get());
        component.removeFromParent();
        signal.set("bar");
        assertEquals("foo", signalPropertySupport.get());
        UI.getCurrent().add(component);
        assertEquals("bar", signalPropertySupport.get());
    }

    @Tag("div")
    private static class TestComponent extends Component {

    }
}
