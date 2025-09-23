/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.ValueSignal;
import com.vaadin.tests.util.MockUI;

public class ComponentEffectTest {
    @Test
    public void effect_triggeredWithOwnerUILocked_effectRunSynchronously() {
        runWithFeatureFlagEnabled(() -> {
            MockUI ui = new MockUI();

            AtomicReference<Thread> currentThread = new AtomicReference<>();
            AtomicReference<UI> currentUI = new AtomicReference<>();

            ComponentEffect.effect(ui, () -> {
                currentThread.set(Thread.currentThread());
                currentUI.set(UI.getCurrent());
            });

            assertSame(Thread.currentThread(), currentThread.get());
            assertSame(ui, currentUI.get());
        });
    }

    @Test
    public void effect_triggeredWithNoUILocked_effectRunAsynchronously() {
        runWithFeatureFlagEnabled(() -> {
            var service = new MockVaadinServletService();
            VaadinService.setCurrent(service);

            var session = new MockVaadinSession(service);
            session.lock();
            var ui = new MockUI(session);
            session.unlock();

            UI.setCurrent(null);

            AtomicReference<Thread> currentThread = new AtomicReference<>();
            AtomicReference<UI> currentUI = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            ComponentEffect.effect(ui, () -> {
                currentThread.set(Thread.currentThread());
                currentUI.set(UI.getCurrent());
                latch.countDown();
            });

            if (!latch.await(3, TimeUnit.SECONDS)) {
                fail("Expected signal effect to be computed asynchronously");
            }

            Assert.assertTrue(
                    "Expected effect to be executed in Vaadin Executor thread",
                    currentThread.get().getName()
                            .startsWith("VaadinTaskExecutor-thread-"));
            assertSame(ui, currentUI.get());
        });
    }

    @Test
    public void effect_triggeredWithOtherUILocked_effectRunAsynchronously() {
        runWithFeatureFlagEnabled(() -> {
            var service = new MockVaadinServletService();
            VaadinService.setCurrent(service);

            var session = new MockVaadinSession(service);
            session.lock();
            var ui = new MockUI(session);
            session.unlock();

            VaadinSession.setCurrent(null);
            UI.setCurrent(null);

            MockUI otherUi = new MockUI();
            UI.setCurrent(otherUi);

            AtomicReference<Thread> currentThread = new AtomicReference<>();
            AtomicReference<UI> currentUI = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            ComponentEffect.effect(ui, () -> {
                currentThread.set(Thread.currentThread());
                currentUI.set(UI.getCurrent());
                latch.countDown();
            });

            if (!latch.await(3, TimeUnit.SECONDS)) {
                fail("Expected signal effect to be computed asynchronously");
            }

            Assert.assertTrue(
                    "Expected effect to be executed in Vaadin Executor thread",
                    currentThread.get().getName()
                            .startsWith("VaadinTaskExecutor-thread-"));
            assertSame(ui, currentUI.get());
        });
    }

    @Test
    public void effect_throwExceptionWhenRunningDirectly_delegatedToErrorHandler() {
        runWithFeatureFlagEnabled(() -> {
            var service = new MockVaadinServletService();
            VaadinService.setCurrent(service);

            var session = new MockVaadinSession(service);
            session.lock();
            var ui = new MockUI(session);

            var events = new ArrayList<ErrorEvent>();
            session.setErrorHandler(events::add);

            ComponentEffect.effect(ui, () -> {
                throw new RuntimeException("Expected exception");
            });

            assertEquals(1, events.size());

            Throwable throwable = events.get(0).getThrowable();
            assertEquals(RuntimeException.class, throwable.getClass());
        });
    }

    @Test
    public void effect_throwExceptionWhenRunningAsynchronously_delegatedToErrorHandler() {
        runWithFeatureFlagEnabled(() -> {
            var service = new MockVaadinServletService();
            VaadinService.setCurrent(service);

            var session = new MockVaadinSession(service);
            session.lock();
            var ui = new MockUI(session);

            var events = new LinkedBlockingQueue<ErrorEvent>();
            session.setErrorHandler(events::add);

            UI.setCurrent(null);
            session.unlock();

            CountDownLatch latch = new CountDownLatch(1);
            ComponentEffect.effect(ui, () -> {
                latch.countDown();
                throw new RuntimeException("Expected exception");
            });

            if (!latch.await(3, TimeUnit.SECONDS)) {
                fail("Expected signal effect to be computed asynchronously");
            }

            ErrorEvent event = events.poll(500, TimeUnit.MILLISECONDS);
            assertNotNull(event);

            Throwable throwable = event.getThrowable();
            assertEquals(RuntimeException.class, throwable.getClass());
        });
    }

    @Test
    public void effect_componentAttachedAndDetached_effectEnabledAndDisabled() {
        runWithFeatureFlagEnabled(() -> {
            TestComponent component = new TestComponent();
            ValueSignal<String> signal = new ValueSignal<>("initial");
            AtomicInteger count = new AtomicInteger();
            Registration registration = ComponentEffect.effect(component,
                    () -> {
                        signal.value();
                        count.incrementAndGet();
                    });

            assertEquals("Effect should not be run until component is attached",
                    0, count.get());

            signal.value("test");
            assertEquals(
                    "Effect should not be run until component is attached even after signal value change",
                    0, count.get());

            MockUI ui = new MockUI();
            ui.add(component);

            assertEquals("Effect should be run once component is attached", 1,
                    count.get());

            signal.value("test2");
            assertEquals("Effect should be run when signal value is chaged", 2,
                    count.get());

            ui.remove(component);

            signal.value("test3");
            assertEquals("Effect should not be run after detach", 2,
                    count.get());

            ui.add(component);
            assertEquals("Effect should be run after attach", 3, count.get());

            registration.remove();
            signal.value("test4");
            assertEquals("Effect should not be run after remove", 3,
                    count.get());
        });
    }

    @Test
    public void bind_signalValueChanges_componentUpdated() {
        runWithFeatureFlagEnabled(() -> {
            TestComponent component = new TestComponent();
            ValueSignal<String> signal = new ValueSignal<>("initial");

            MockUI ui = new MockUI();
            ui.add(component);

            Registration registration = ComponentEffect.bind(component, signal,
                    TestComponent::setValue);

            assertEquals("Initial value should be set", "initial",
                    component.getValue());

            // Change signal value
            signal.value("new value");

            assertEquals("Component should be updated with new value",
                    "new value", component.getValue());

            // Change signal value again
            signal.value("another value");

            assertEquals("Component should be updated with another value",
                    "another value", component.getValue());

            registration.remove();

            // Change signal value after registration is removed
            signal.value("final value");

            assertEquals(
                    "Component should not be updated after registration is removed",
                    "another value", component.getValue());
        });
    }

    @FunctionalInterface
    private interface InterruptableRunnable {
        void run() throws InterruptedException;
    }

    private static void runWithFeatureFlagEnabled(InterruptableRunnable test) {
        try (var featureFlagStaticMock = mockStatic(FeatureFlags.class)) {
            FeatureFlags flags = mock(FeatureFlags.class);
            when(flags.isEnabled(FeatureFlags.FLOW_FULLSTACK_SIGNALS.getId()))
                    .thenReturn(true);
            featureFlagStaticMock.when(() -> FeatureFlags.get(any()))
                    .thenReturn(flags);
            test.run();
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        } finally {
            VaadinService.getCurrent().destroy();
            CurrentInstance.clearAll();
        }
    }

    @Tag("div")
    private static class TestComponent extends Component {
        String value;

        public TestComponent() {
            super(new Element("div"));
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
