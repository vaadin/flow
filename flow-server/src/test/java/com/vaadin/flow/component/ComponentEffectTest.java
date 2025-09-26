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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Node;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.ListSignal;
import com.vaadin.signals.Signal;
import com.vaadin.signals.ValueSignal;
import com.vaadin.tests.util.MockUI;

public class ComponentEffectTest {

    private static MockVaadinServletService service;

    @BeforeClass
    public static void init() {
        runWithFeatureFlagEnabled(() -> {
            service = new MockVaadinServletService();
        });
    }

    @AfterClass
    public static void clean() {
        CurrentInstance.clearAll();
        service.destroy();
    }

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

            if (!latch.await(500, TimeUnit.MILLISECONDS)) {
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

            if (!latch.await(500, TimeUnit.MILLISECONDS)) {
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

            if (!latch.await(500, TimeUnit.MILLISECONDS)) {
                fail("Expected signal effect to be computed asynchronously");
            }

            ErrorEvent event = events.poll(1000, TimeUnit.MILLISECONDS);
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

    @Test
    public void bindChildren_nullArguments_throws() {
        runWithFeatureFlagEnabled(() -> {
            ListSignal<String> taskList = new ListSignal<>(String.class);
            TestLayout parentComponent = new TestLayout();
            new MockUI();

            assertThrows(NullPointerException.class, () -> ComponentEffect
                    .bindChildren(null, taskList, valueSignal -> null));
            assertThrows(NullPointerException.class, () -> ComponentEffect
                    .bindChildren(parentComponent, null, valueSignal -> null));
            assertThrows(NullPointerException.class, () -> ComponentEffect
                    .bindChildren(parentComponent, taskList, null));
        });
    }

    @Test
    public void bindChildren_emptyListSignal_emptyParent() {
        runWithFeatureFlagEnabled(() -> {
            ListSignal<String> taskList = new ListSignal<>(String.class);
            TestLayout parentComponent = new TestLayout();
            new MockUI().add(parentComponent);
            ComponentEffect.bindChildren(parentComponent, taskList,
                    valueSignal -> new TestComponent(valueSignal.value()));
            assertEquals(0, parentComponent.getComponentCount());
        });
    }

    @Test
    public void bindChildren_emptyListSignalWithNotInitiallyEmptyParent_throw() {
        runWithFeatureFlagEnabled(() -> {
            ListSignal<String> taskList = new ListSignal<>(String.class);
            TestLayout parentComponent = new TestLayout();
            var initialComponent = new TestComponent("initial");

            parentComponent.add(initialComponent);

            new MockUI().add(parentComponent);
            assertThrows(IllegalStateException.class, () -> {
                ComponentEffect.bindChildren(parentComponent, taskList,
                        valueSignal -> {
                            fail("Should not call element factory");
                            return null;
                        });
            });
            assertEquals(1, parentComponent.getComponentCount());
        });
    }

    @Test
    public void bindChildren_listSignalWithItem_parentUpdated() {
        runWithFeatureFlagEnabled(() -> {
            ListSignal<String> taskList = new ListSignal<>(String.class);
            taskList.insertFirst("first");

            TestLayout parentComponent = new TestLayout();
            var expectedComponent = new TestComponent();
            new MockUI().add(parentComponent);

            ComponentEffect.bindChildren(parentComponent, taskList,
                    valueSignal -> {
                        expectedComponent.setValue(valueSignal.value());
                        return expectedComponent;
                    });
            assertEquals(1, parentComponent.getComponentCount());
            assertEquals(expectedComponent,
                    parentComponent.getChildren().findFirst().orElse(null));
            assertEquals("first", expectedComponent.getValue());

            assertEquals(1, expectedComponent.attachCounter);
            assertEquals(0, expectedComponent.detachCounter);
        });
    }

    @Test
    public void bindChildren_addItem_parentUpdated() {
        runWithFeatureFlagEnabled(() -> {
            ListSignal<String> taskList = new ListSignal<>(String.class);
            taskList.insertFirst("first");
            TestLayout parentComponent = new TestLayout();
            new MockUI().add(parentComponent);

            ComponentEffect.bindChildren(parentComponent, taskList,
                    valueSignal -> new TestComponent(valueSignal.value()));

            assertEquals("Parent component children count is wrong", 1,
                    parentComponent.getComponentCount());
            assertEquals("first", ((TestComponent) parentComponent.getChildren()
                    .toList().get(0)).getValue());

            taskList.insertLast("last");

            List<TestComponent> children = parentComponent.getChildren()
                    .map(TestComponent.class::cast).toList();

            assertEquals("Parent component children count is wrong", 2,
                    parentComponent.getComponentCount());
            assertEquals("last", children.get(1).getValue());

            assertEquals(1, children.get(0).attachCounter);
            assertEquals(0, children.get(0).detachCounter);
            assertEquals(1, children.get(1).attachCounter);
            assertEquals(0, children.get(1).detachCounter);
        });
    }

    @Test
    public void bindChildren_removeItem_parentUpdated() {
        runWithFeatureFlagEnabled(() -> {
            ListSignal<String> taskList = new ListSignal<>(String.class);
            taskList.insertFirst("first");
            taskList.insertLast("last");
            TestLayout parentComponent = new TestLayout();
            new MockUI().add(parentComponent);

            ComponentEffect.bindChildren(parentComponent, taskList,
                    valueSignal -> new TestComponent(valueSignal.value()));

            assertEquals("Parent component children count is wrong", 2,
                    parentComponent.getComponentCount());

            List<TestComponent> children = parentComponent.getChildren()
                    .map(TestComponent.class::cast).toList();

            taskList.remove(taskList.value().get(0));

            assertEquals("Parent component children count is wrong", 1,
                    parentComponent.getComponentCount());
            assertEquals("last", ((TestComponent) parentComponent.getChildren()
                    .toList().get(0)).getValue());

            assertEquals(1, children.get(0).attachCounter);
            assertEquals(1, children.get(0).detachCounter);
            assertEquals(1, children.get(1).attachCounter);
            assertEquals(0, children.get(1).detachCounter);
        });
    }

    @Test
    public void bindChildren_moveItem_parentUpdated() {
        runWithFeatureFlagEnabled(() -> {
            ListSignal<String> taskList = new ListSignal<>(String.class);
            taskList.insertFirst("first");
            taskList.insertLast("middle");
            taskList.insertLast("last");
            TestLayout parentComponent = new TestLayout();
            new MockUI().add(parentComponent);

            ComponentEffect.bindChildren(parentComponent, taskList,
                    valueSignal -> new TestComponent(valueSignal.value()));

            assertEquals("Parent component children count is wrong", 3,
                    parentComponent.getComponentCount());

            // move last to first
            taskList.moveTo(taskList.value().get(2),
                    ListSignal.ListPosition.first());

            assertEquals("Parent component children count is wrong", 3,
                    parentComponent.getComponentCount());
            assertEquals("last", ((TestComponent) parentComponent.getChildren()
                    .toList().get(0)).getValue());

            // move it back to last
            taskList.moveTo(taskList.value().get(0),
                    ListSignal.ListPosition.last());
            assertEquals("last", ((TestComponent) parentComponent.getChildren()
                    .toList().get(2)).getValue());

            // move last between first and last
            taskList.moveTo(taskList.value().get(2), ListSignal.ListPosition
                    .between(taskList.value().get(0), taskList.value().get(1)));
            assertEquals("last", ((TestComponent) parentComponent.getChildren()
                    .toList().get(1)).getValue());
        });
    }

    @Test
    public void bindChildren_moveLastToFirst_verifyElementAttachDetachCount() {
        runWithFeatureFlagEnabled(() -> {
            ListSignal<String> taskList = new ListSignal<>(String.class);
            taskList.insertFirst("first");
            taskList.insertLast("middle");
            taskList.insertLast("last");

            TestLayout parentComponent = prepareTestLayout(taskList);

            // move last to first
            taskList.moveTo(taskList.value().get(2),
                    ListSignal.ListPosition.first());

            List<TestComponent> children = parentComponent.getChildren()
                    .map(TestComponent.class::cast).toList();

            assertEquals(1, children.get(0).attachCounter);
            assertEquals(1, children.get(0).detachCounter);
            assertEquals(0, children.get(1).attachCounter);
            assertEquals(0, children.get(1).detachCounter);
            assertEquals(0, children.get(2).attachCounter);
            assertEquals(0, children.get(2).detachCounter);
        });
    }

    @Test
    public void bindChildren_moveFirstToLast_verifyElementAttachDetachCount() {
        runWithFeatureFlagEnabled(() -> {
            ListSignal<String> taskList = new ListSignal<>(String.class);
            taskList.insertFirst("first");
            taskList.insertLast("middle");
            taskList.insertLast("last");

            TestLayout parentComponent = prepareTestLayout(taskList);

            // move first to last
            taskList.moveTo(taskList.value().get(0),
                    ListSignal.ListPosition.last());

            List<TestComponent> children = parentComponent.getChildren()
                    .map(TestComponent.class::cast).toList();

            assertEquals(0, children.get(0).attachCounter);
            assertEquals(0, children.get(0).detachCounter);
            assertEquals(0, children.get(1).attachCounter);
            assertEquals(0, children.get(1).detachCounter);
            assertEquals(1, children.get(2).attachCounter);
            assertEquals(1, children.get(2).detachCounter);
        });
    }

    @Test
    public void bindChildren_moveLastBetweenFirstAndSecond_verifyElementAttachDetachCount() {
        runWithFeatureFlagEnabled(() -> {
            ListSignal<String> taskList = new ListSignal<>(String.class);
            taskList.insertFirst("first");
            taskList.insertLast("middle");
            taskList.insertLast("last");

            TestLayout parentComponent = prepareTestLayout(taskList);

            // move last between first and second
            taskList.moveTo(taskList.value().get(2), ListSignal.ListPosition
                    .between(taskList.value().get(0), taskList.value().get(1)));

            List<TestComponent> children = parentComponent.getChildren()
                    .map(TestComponent.class::cast).toList();

            assertEquals(0, children.get(0).attachCounter);
            assertEquals(0, children.get(0).detachCounter);
            assertEquals(0, children.get(1).attachCounter);
            assertEquals(0, children.get(1).detachCounter);
            assertEquals(1, children.get(2).attachCounter);
            assertEquals(1, children.get(2).detachCounter);
        });
    }

    @Test
    public void bindChildren_addToParentComponentAndAddItem_throw() {
        // When adding children directly to parent, exception will be thrown
        // from the effect on next related Signal change.
        runWithFeatureFlagEnabled(() -> {
            LinkedBlockingQueue<ErrorEvent> events = mockSessionWithErrorHandler();
            UI ui = UI.getCurrent();

            ListSignal<String> taskList = new ListSignal<>(String.class);
            taskList.insertFirst("first");
            TestLayout parentComponent = new TestLayout();

            ui.add(parentComponent);

            ComponentEffect.bindChildren(parentComponent, taskList,
                    valueSignal -> new TestComponent(valueSignal.value()));

            var expectedComponent = new TestComponent("added directly");
            // doing wrong
            parentComponent.add(expectedComponent);

            // causes the effect to run and exception being thrown
            taskList.insertLast("last");

            ErrorEvent event = events.poll(1000, TimeUnit.MILLISECONDS);

            assertNotNull(event);
            assertEquals(IllegalStateException.class,
                    event.getThrowable().getClass());
            // no changes in the element
            assertEquals("Parent component children count is wrong", 2,
                    parentComponent.getComponentCount());
            assertEquals("first", ((TestComponent) parentComponent.getChildren()
                    .toList().get(0)).getValue());
            assertEquals("added directly", ((TestComponent) parentComponent
                    .getChildren().toList().get(1)).getValue());
        });
    }

    @Test
    public void bindChildren_directParentComponentChanges_sameChildrenSizeBeforeAfter_throw() {
        // When adding children directly to parent, exception will be thrown
        // from the effect on next related Signal change.
        runWithFeatureFlagEnabled(() -> {
            LinkedBlockingQueue<ErrorEvent> events = mockSessionWithErrorHandler();
            UI ui = UI.getCurrent();

            ListSignal<String> taskList = new ListSignal<>(String.class);
            taskList.insertLast("first");
            taskList.insertLast("middle");
            TestLayout parentComponent = new TestLayout();

            ui.add(parentComponent);

            ComponentEffect.bindChildren(parentComponent, taskList,
                    valueSignal -> new TestComponent(valueSignal.value()));

            var directlyAddedComponent1 = new TestComponent("added directly 1");
            var directlyAddedComponent2 = new TestComponent("added directly 2");

            // doing wrong
            parentComponent.removeAll();
            parentComponent.add(directlyAddedComponent1);
            parentComponent.add(directlyAddedComponent2);
            // notice that size is still same as original

            // causes the effect to run and exception being thrown
            taskList.insertLast("last");

            ErrorEvent event = events.poll(1000, TimeUnit.MILLISECONDS);

            assertNotNull(event);
            assertEquals(IllegalStateException.class,
                    event.getThrowable().getClass());

            List<TestComponent> children = parentComponent.getChildren()
                    .map(TestComponent.class::cast).toList();
            // Changes are still applied as exception is thrown in the end of
            // the effect. Algorithm moves wrongly added elements after signal
            // list.
            assertEquals("Parent component children count is wrong", 5,
                    parentComponent.getComponentCount());
            assertEquals("first", children.get(0).getValue());
            assertEquals("middle", children.get(1).getValue());
            assertEquals("last", children.get(2).getValue());
            assertEquals("added directly 1", children.get(3).getValue());
            assertEquals("added directly 2", children.get(4).getValue());
        });
    }

    @Test
    public void bindChildren_directParentComponentChangeByFactory_throw() {
        // When adding children directly to parent, exception will be thrown
        // from the effect on next related Signal change.
        runWithFeatureFlagEnabled(() -> {
            LinkedBlockingQueue<ErrorEvent> events = mockSessionWithErrorHandler();
            UI ui = UI.getCurrent();

            ListSignal<String> taskList = new ListSignal<>(String.class);
            taskList.insertLast("first");
            taskList.insertLast("middle");
            TestLayout parentComponent = new TestLayout();

            ui.add(parentComponent);

            ComponentEffect.bindChildren(parentComponent, taskList,
                    valueSignal -> {
                        String value = valueSignal.value();
                        var component = new TestComponent(value);
                        if ("middle".equals(value)) {
                            // doing wrong
                            parentComponent
                                    .add(new TestComponent("added directly"));
                        }
                        return component;
                    });

            // causes the effect to run and exception being thrown
            taskList.insertLast("last");

            ErrorEvent event = events.poll(1000, TimeUnit.MILLISECONDS);

            assertNotNull(event);
            assertEquals(IllegalStateException.class,
                    event.getThrowable().getClass());
            assertEquals(
                    "Parent element must have children matching the list signal. Unexpected child count after child factory call: 2, expected: 1",
                    event.getThrowable().getMessage());

            List<TestComponent> children = parentComponent.getChildren()
                    .map(TestComponent.class::cast).toList();
            // Exception is thrown after child factory is called for the
            // 'middle' item
            assertEquals("Parent component children count is wrong", 2,
                    parentComponent.getComponentCount());
            assertEquals("first", children.get(0).getValue());
            assertEquals("added directly", children.get(1).getValue());
        });
    }

    @Test
    public void bindChildren_directParentComponentChangeByCustomAttach_throw() {
        // When adding children directly to parent, exception will be thrown
        // from the effect on next related Signal change.
        runWithFeatureFlagEnabled(() -> {
            LinkedBlockingQueue<ErrorEvent> events = mockSessionWithErrorHandler();
            UI ui = UI.getCurrent();

            ListSignal<String> taskList = new ListSignal<>(String.class);
            taskList.insertLast("first");
            taskList.insertLast("middle");
            TestLayout parentComponent = new TestLayout();

            ui.add(parentComponent);

            ComponentEffect.bindChildren(parentComponent, taskList,
                    valueSignal -> {
                        String value = valueSignal.value();
                        var component = new TestComponent(value);
                        if ("middle".equals(value)) {
                            component.addAttachListener(event -> {
                                // doing wrong
                                parentComponent.add(
                                        new TestComponent("added directly"));
                                event.unregisterListener();
                            });
                        }
                        return component;
                    });

            ErrorEvent event = events.poll(1000, TimeUnit.MILLISECONDS);

            assertNotNull(event);
            assertEquals(IllegalStateException.class,
                    event.getThrowable().getClass());
            assertEquals(
                    "Parent element must have children matching the list signal. Unexpected child count: 3, expected: 2",
                    event.getThrowable().getMessage());

            List<TestComponent> children = parentComponent.getChildren()
                    .map(TestComponent.class::cast).toList();
            // Exception is thrown only in final validation in the end
            assertEquals("Parent component children count is wrong", 3,
                    parentComponent.getComponentCount());
            assertEquals("first", children.get(0).getValue());
            assertEquals("middle", children.get(1).getValue());
            assertEquals("added directly", children.get(2).getValue());
        });
    }

    @Test
    public void bindChildren_directParentComponentChildOrderChanges_throw() {
        // When adding children directly to parent, exception will be thrown
        // from the effect on next related Signal change.
        // Exception is thrown only in final validation in the end when change
        // can't be detected by just checking size.
        runWithFeatureFlagEnabled(() -> {
            LinkedBlockingQueue<ErrorEvent> events = mockSessionWithErrorHandler();
            UI ui = UI.getCurrent();

            ListSignal<String> taskList = new ListSignal<>(String.class);
            taskList.insertLast("first");
            taskList.insertLast("middle");
            taskList.insertLast("last");
            TestLayout parentComponent = new TestLayout();

            ui.add(parentComponent);

            ComponentEffect.bindChildren(parentComponent, taskList,
                    valueSignal -> {
                        String value = valueSignal.value();
                        var component = new TestComponent(value);
                        component.getElement().setText(value);
                        if ("last".equals(value)) {
                            // doing wrong, change order of first two children
                            parentComponent.getElement().insertChild(0,
                                    parentComponent.getElement().getChild(1));
                        }
                        return component;
                    });

            ErrorEvent event = events.poll(1000, TimeUnit.MILLISECONDS);

            assertNotNull(event);
            assertEquals(IllegalStateException.class,
                    event.getThrowable().getClass());
            List<TestComponent> children = parentComponent.getChildren()
                    .map(TestComponent.class::cast).toList();

            assertEquals(
                    "Parent element must have children matching the list signal. Unexpected child: <div>middle</div>, expected: <div>first</div>",
                    event.getThrowable().getMessage());
            assertEquals("Parent component children count is wrong", 3,
                    parentComponent.getComponentCount());
            assertEquals("middle", children.get(0).getValue());
            assertEquals("first", children.get(1).getValue());
            assertEquals("last", children.get(2).getValue());
        });
    }

    @Test
    public void bindChildren_runInTransaction_effectRunOnce() {
        runWithFeatureFlagEnabled(() -> {
            var expectedMockedElements = new ArrayList<Element>();

            ListSignal<String> taskList = new ListSignal<>(String.class);
            TestLayout parentComponent = new TestLayout(expectedMockedElements);
            new MockUI().add(parentComponent);

            ComponentEffect.bindChildren(parentComponent, taskList,
                    valueSignal -> {
                        var component = new TestComponent(valueSignal.value(),
                                parentComponent.getElement(), null);
                        expectedMockedElements.add(component.getElement());
                        return component;
                    });

            Mockito.clearInvocations(expectedMockedElements.toArray());
            Mockito.clearInvocations(parentComponent.getElement());
            Signal.runInTransaction(() -> {
                taskList.insertFirst("first");
                taskList.insertLast("last");

                taskList.insertLast("middle");
                taskList.moveTo(taskList.value().get(2),
                        ListSignal.ListPosition.between(taskList.value().get(0),
                                taskList.value().get(1)));

                taskList.remove(taskList.value().get(0));
            });

            // getChildren() should be called twice per bindChildren effect call
            verify(parentComponent.getElement(), times(2)).getChildren();

            assertEquals("Parent component children count is wrong", 2,
                    parentComponent.getComponentCount());
            assertEquals("middle", ((TestComponent) parentComponent
                    .getChildren().toList().get(0)).getValue());
            assertEquals("last", ((TestComponent) parentComponent.getChildren()
                    .toList().get(1)).getValue());
        });
    }

    @Test
    public void bindChildren_withNullFromChildFactory_throws() {
        runWithFeatureFlagEnabled(() -> {
            LinkedBlockingQueue<ErrorEvent> events = mockSessionWithErrorHandler();
            UI ui = UI.getCurrent();

            ListSignal<String> taskList = new ListSignal<>(String.class);
            taskList.insertFirst("first");
            TestLayout parentComponent = new TestLayout();

            ui.add(parentComponent);

            ComponentEffect.bindChildren(parentComponent, taskList,
                    valueSignal -> null);

            ErrorEvent event = events.poll(1000, TimeUnit.MILLISECONDS);

            assertNotNull(event);
            assertEquals(IllegalStateException.class,
                    event.getThrowable().getClass());
            assertEquals(
                    "ComponentEffect.bindChildren childFactory must not return null",
                    event.getThrowable().getMessage());
        });
    }

    private TestLayout prepareTestLayout(ListSignal<String> listSignal) {
        TestLayout parentComponent = new TestLayout();
        new MockUI().add(parentComponent);

        ComponentEffect.bindChildren(parentComponent, listSignal,
                valueSignal -> new TestComponent(valueSignal.value()));

        parentComponent.getChildren().map(TestComponent.class::cast)
                .forEach(TestComponent::resetCounters);

        return parentComponent;
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
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            CurrentInstance.clearAll();
        }
    }

    private LinkedBlockingQueue<ErrorEvent> mockSessionWithErrorHandler() {
        VaadinService.setCurrent(service);

        var session = new MockVaadinSession(service);
        session.lock();

        var ui = new MockUI(session);
        var events = new LinkedBlockingQueue<ErrorEvent>();
        session.setErrorHandler(events::add);

        return events;
    }

    @Tag("div")
    private static class TestComponent extends Component {
        String value;
        int attachCounter;
        int detachCounter;

        /**
         * Constructor of TestComponent without any mock elements.
         */
        public TestComponent() {
            super(new Element("div"));
            getElement().addAttachListener(event -> attachCounter++);
            getElement().addDetachListener(event -> detachCounter++);
        }

        /**
         * Constructor of TestComponent with mocked element and optional parent
         * element mock and children. Works together with Mockito.spy().
         *
         * @param expectedParentElementMock
         *            Mocked parent element for return value of getParentNode()
         *            when parent is set. <code>null</code> fall back to default
         *            behaviour.
         * @param expectedMockedChildren
         *            List of expected mocked children that replaces elements
         *            created internally by Element API getChild(int).
         *            <code>null</code> fall back to default behaviour.
         */
        public TestComponent(Element expectedParentElementMock,
                List<Element> expectedMockedChildren) {
            super(spy(new Element("div") {
                @Override
                public Node getParentNode() {
                    Node actualParent = super.getParentNode();
                    if (actualParent != null
                            && expectedParentElementMock != null) {
                        return expectedParentElementMock;
                    }
                    return super.getParentNode();
                }

                @Override
                public Element getChild(int index) {
                    if (expectedMockedChildren != null) {
                        return expectedMockedChildren.get(index);
                    }
                    return super.getChild(index);
                }

            }));
        }

        /**
         * Constructor of TestComponent without any mock elements.
         *
         * @param initialValue
         *            initial value
         */
        public TestComponent(String initialValue) {
            this();
            setValue(initialValue);
        }

        /**
         * Constructor of TestComponent with mocked element and optional parent
         * element mock and children. Works together with Mockito.spy().
         *
         * @param initialValue
         *            initial value
         * @param expectedParentElementMock
         *            Mocked parent element for return value of getParentNode()
         *            when parent is set. <code>null</code> fall back to default
         *            behaviour.
         * @param expectedMockedChildren
         *            List of expected mocked children that replaces elements
         *            created internally by Element API getChild(int).
         *            <code>null</code> fall back to default behaviour.
         */
        public TestComponent(String initialValue,
                Element expectedParentElementMock,
                List<Element> expectedMockedChildren) {
            this(expectedParentElementMock, expectedMockedChildren);
            setValue(initialValue);
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        void resetCounters() {
            attachCounter = 0;
            detachCounter = 0;
        }
    }

    /**
     * Test layout component with optional support for Mockito.spy(element).
     */
    @Tag("div")
    private static class TestLayout extends TestComponent
            implements HasOrderedComponents {

        /**
         * Construct test layout component without any mocked elements.
         */
        public TestLayout() {
            super();
        }

        /**
         * Construct test layout component with mocked element with
         * Mockito.spy().
         *
         * @param expectedMockedChildren
         *            List of expected mocked children that replaces elements
         *            created internally by Element API. <code>null</code> fall
         *            back to default behaviour.
         */
        public TestLayout(List<Element> expectedMockedChildren) {
            super(null, expectedMockedChildren);
        }
    }
}
