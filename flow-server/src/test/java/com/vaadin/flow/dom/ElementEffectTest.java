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
package com.vaadin.flow.dom;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasOrderedComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ElementEffectTest {

    private static TestService service;

    /**
     * Custom executor that queues tasks and executes them synchronously when
     * flushed. This eliminates race conditions in tests by keeping all
     * execution on the main test thread.
     */
    private static class FlushableExecutor implements Executor {
        private final List<Runnable> pendingTasks = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            pendingTasks.add(command);
        }

        /**
         * Executes all pending tasks synchronously on the calling thread.
         * Continues flushing until no more tasks are queued (handles cascading
         * task submissions).
         */
        public void flush() {
            while (!pendingTasks.isEmpty()) {
                List<Runnable> tasks = new ArrayList<>(pendingTasks);
                pendingTasks.clear();
                tasks.forEach(Runnable::run);
            }
        }
    }

    /**
     * Test service that uses FlushableExecutor instead of a background thread
     * pool for deterministic test execution.
     */
    private static class TestService extends MockVaadinServletService {
        private FlushableExecutor executor;

        @Override
        protected Executor createDefaultExecutor() {
            executor = new FlushableExecutor();
            return executor;
        }

        /**
         * Flushes all pending async tasks. This executes tasks queued in the
         * executor and then processes any UI access tasks that were queued as a
         * result.
         */
        public void flushExecutorAndAccessTasks(VaadinSession session) {
            // Flush executor tasks (effect dispatcher)
            executor.flush();

            // Process UI access tasks that were queued by the executor
            session.lock();
            try {
                runPendingAccessTasks(session);
            } finally {
                session.unlock();
            }
        }
    }

    @BeforeAll
    public static void init() {
        service = new TestService();
    }

    @AfterAll
    public static void clean() {
        CurrentInstance.clearAll();
        service.destroy();
    }

    @Test
    public void effect_triggeredWithOwnerUILocked_effectRunSynchronously() {
        CurrentInstance.clearAll();
        MockUI ui = new MockUI();

        AtomicReference<Thread> currentThread = new AtomicReference<>();
        AtomicReference<UI> currentUI = new AtomicReference<>();

        Signal.effect(ui, () -> {
            currentThread.set(Thread.currentThread());
            currentUI.set(UI.getCurrent());
        });

        assertSame(Thread.currentThread(), currentThread.get());
        assertSame(ui, currentUI.get());
    }

    @Test
    public void effect_triggeredWithNoUILocked_effectRunAsynchronously() {
        CurrentInstance.clearAll();
        VaadinService.setCurrent(service);

        var session = new MockVaadinSession(service);
        session.lock();
        var ui = new MockUI(session);
        session.unlock();

        UI.setCurrent(null);

        AtomicReference<UI> currentUI = new AtomicReference<>();

        Signal.effect(ui, () -> {
            currentUI.set(UI.getCurrent());
        });

        // Flush executor and UI access tasks to run pending tasks
        // synchronously
        service.flushExecutorAndAccessTasks(session);

        assertSame(ui, currentUI.get(),
                "Effect should run with correct UI context");
    }

    @Test
    public void effect_triggeredWithOtherUILocked_effectRunAsynchronously() {
        CurrentInstance.clearAll();
        VaadinService.setCurrent(service);

        var session = new MockVaadinSession(service);
        session.lock();
        var ui = new MockUI(session);
        session.unlock();

        VaadinSession.setCurrent(null);
        UI.setCurrent(null);

        // Create otherUi with the same service to avoid creating a second
        // executor
        var otherSession = new MockVaadinSession(service);
        otherSession.lock();
        MockUI otherUi = new MockUI(otherSession);
        otherSession.unlock();
        UI.setCurrent(otherUi);

        AtomicReference<UI> currentUI = new AtomicReference<>();

        Signal.effect(ui, () -> {
            currentUI.set(UI.getCurrent());
        });

        // Flush executor and UI access tasks to run pending tasks
        // synchronously
        service.flushExecutorAndAccessTasks(session);

        assertSame(ui, currentUI.get(),
                "Effect should run with correct UI context");
    }

    @Test
    public void effect_throwExceptionWhenRunningDirectly_delegatedToErrorHandler() {
        CurrentInstance.clearAll();
        VaadinService.setCurrent(service);

        var session = new MockVaadinSession(service);
        session.lock();
        var ui = new MockUI(session);

        var events = new ArrayList<ErrorEvent>();
        session.setErrorHandler(events::add);

        Signal.effect(ui, () -> {
            throw new RuntimeException("Expected exception");
        });

        assertEquals(1, events.size());

        Throwable throwable = events.get(0).getThrowable();
        assertEquals(RuntimeException.class, throwable.getClass());
    }

    @Test
    public void effect_throwExceptionWhenRunningAsynchronously_delegatedToErrorHandler() {
        CurrentInstance.clearAll();
        VaadinService.setCurrent(service);

        var session = new MockVaadinSession(service);
        session.lock();
        var ui = new MockUI(session);

        var events = new ArrayList<ErrorEvent>();
        session.setErrorHandler(events::add);

        UI.setCurrent(null);
        session.unlock();

        Signal.effect(ui, () -> {
            throw new RuntimeException("Expected exception");
        });

        // Flush executor and UI access tasks to run pending tasks
        // synchronously
        service.flushExecutorAndAccessTasks(session);

        assertEquals(1, events.size(), "Error handler should have been called");

        Throwable throwable = events.get(0).getThrowable();
        assertEquals(RuntimeException.class, throwable.getClass());
    }

    @Test
    public void effect_componentAttachedAndDetached_effectEnabledAndDisabled() {
        CurrentInstance.clearAll();
        TestComponent component = new TestComponent();
        ValueSignal<String> signal = new ValueSignal<>("initial");
        AtomicInteger count = new AtomicInteger();
        Registration registration = Signal.effect(component, () -> {
            signal.get();
            count.incrementAndGet();
        });

        assertEquals(0, count.get(),
                "Effect should not be run until component is attached");

        signal.set("test");
        assertEquals(0, count.get(),
                "Effect should not be run until component is attached even after signal value change");

        MockUI ui = new MockUI();
        ui.add(component);

        assertEquals(1, count.get(),
                "Effect should be run once component is attached");

        signal.set("test2");
        assertEquals(2, count.get(),
                "Effect should be run when signal value is chaged");

        ui.remove(component);

        signal.set("test3");
        assertEquals(2, count.get(), "Effect should not be run after detach");

        ui.add(component);
        assertEquals(3, count.get(), "Effect should be run after attach");

        registration.remove();
        signal.set("test4");
        assertEquals(3, count.get(), "Effect should not be run after remove");
    }

    @Test
    public void elementEffect_signalValueChanges_componentUpdated() {
        CurrentInstance.clearAll();
        TestComponent component = new TestComponent();
        ValueSignal<String> signal = new ValueSignal<>("initial");

        MockUI ui = new MockUI();
        ui.add(component);

        Registration registration = new ElementEffect(component.getElement(),
                () -> component.setValue(signal.get()))::close;

        assertEquals("initial", component.getValue(),
                "Initial value should be set");

        // Change signal value
        signal.set("new value");

        assertEquals("new value", component.getValue(),
                "Component should be updated with new value");

        // Change signal value again
        signal.set("another value");

        assertEquals("another value", component.getValue(),
                "Component should be updated with another value");

        registration.remove();

        // Change signal value after registration is removed
        signal.set("final value");

        assertEquals("another value", component.getValue(),
                "Component should not be updated after registration is removed");
    }

    @Test
    public void bindChildren_nullArguments_throws() {
        CurrentInstance.clearAll();
        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        TestLayout parentComponent = new TestLayout();
        new MockUI();

        assertThrows(NullPointerException.class,
                () -> parentComponent.bindChildren(null, valueSignal -> null));
        assertThrows(NullPointerException.class,
                () -> parentComponent.bindChildren(taskList, null));
    }

    @Test
    public void bindChildren_emptySharedListSignal_emptyParent() {
        CurrentInstance.clearAll();
        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        TestLayout parentComponent = new TestLayout();
        new MockUI().add(parentComponent);
        parentComponent.bindChildren(taskList,
                valueSignal -> new TestComponent(valueSignal.get()));
        assertEquals(0, parentComponent.getComponentCount());
    }

    @Test
    public void bindChildren_emptySharedListSignalWithNotInitiallyEmptyParent_throw() {
        CurrentInstance.clearAll();
        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        TestLayout parentComponent = new TestLayout();
        var initialComponent = new TestComponent("initial");

        parentComponent.add(initialComponent);

        new MockUI().add(parentComponent);
        assertThrows(IllegalStateException.class, () -> {
            parentComponent.bindChildren(taskList, valueSignal -> {
                fail("Should not call element factory");
                return null;
            });
        });
        assertEquals(1, parentComponent.getComponentCount());
    }

    @Test
    public void bindChildren_listSignalWithItem_parentUpdated() {
        CurrentInstance.clearAll();
        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        taskList.insertFirst("first");

        TestLayout parentComponent = new TestLayout();
        var expectedComponent = new TestComponent();
        new MockUI().add(parentComponent);

        parentComponent.bindChildren(taskList, valueSignal -> {
            expectedComponent.setValue(valueSignal.get());
            return expectedComponent;
        });
        assertEquals(1, parentComponent.getComponentCount());
        assertEquals(expectedComponent,
                parentComponent.getChildren().findFirst().orElse(null));
        assertEquals("first", expectedComponent.getValue());

        assertEquals(1, expectedComponent.attachCounter);
        assertEquals(0, expectedComponent.detachCounter);
    }

    @Test
    public void bindChildren_addItem_parentUpdated() {
        CurrentInstance.clearAll();
        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        taskList.insertFirst("first");
        TestLayout parentComponent = new TestLayout();
        new MockUI().add(parentComponent);

        parentComponent.bindChildren(taskList,
                valueSignal -> new TestComponent(valueSignal.get()));

        assertEquals(1, parentComponent.getComponentCount(),
                "Parent component children count is wrong");
        assertEquals("first",
                ((TestComponent) parentComponent.getChildren().toList().get(0))
                        .getValue());

        taskList.insertLast("last");

        List<TestComponent> children = parentComponent.getChildren()
                .map(TestComponent.class::cast).toList();

        assertEquals(2, parentComponent.getComponentCount(),
                "Parent component children count is wrong");
        assertEquals("last", children.get(1).getValue());

        assertEquals(1, children.get(0).attachCounter);
        assertEquals(0, children.get(0).detachCounter);
        assertEquals(1, children.get(1).attachCounter);
        assertEquals(0, children.get(1).detachCounter);
    }

    @Test
    public void bindChildren_removeItem_parentUpdated() {
        CurrentInstance.clearAll();
        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        taskList.insertFirst("first");
        taskList.insertLast("middle");
        taskList.insertLast("last");
        TestLayout parentComponent = new TestLayout();
        new MockUI().add(parentComponent);

        parentComponent.bindChildren(taskList,
                valueSignal -> new TestComponent(valueSignal.get()));

        assertEquals(3, parentComponent.getComponentCount(),
                "Parent component children count is wrong");

        List<TestComponent> children = parentComponent.getChildren()
                .map(TestComponent.class::cast).toList();

        taskList.remove(taskList.peek().get(0));

        assertEquals(2, parentComponent.getComponentCount(),
                "Parent component children count is wrong");
        assertEquals("middle",
                ((TestComponent) parentComponent.getChildren().toList().get(0))
                        .getValue());
        assertEquals("last",
                ((TestComponent) parentComponent.getChildren().toList().get(1))
                        .getValue());

        assertEquals(1, children.get(0).attachCounter);
        assertEquals(1, children.get(0).detachCounter);
        assertEquals(1, children.get(1).attachCounter);
        assertEquals(0, children.get(1).detachCounter);
        assertEquals(1, children.get(2).attachCounter);
        assertEquals(0, children.get(2).detachCounter);
    }

    @Test
    public void bindChildren_moveItem_parentUpdated() {
        CurrentInstance.clearAll();
        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        taskList.insertFirst("first");
        taskList.insertLast("middle");
        taskList.insertLast("last");
        TestLayout parentComponent = new TestLayout();
        new MockUI().add(parentComponent);

        parentComponent.bindChildren(taskList,
                valueSignal -> new TestComponent(valueSignal.get()));

        assertEquals(3, parentComponent.getComponentCount(),
                "Parent component children count is wrong");

        // move last to first
        taskList.moveTo(taskList.peek().get(2),
                SharedListSignal.ListPosition.first());

        assertEquals(3, parentComponent.getComponentCount(),
                "Parent component children count is wrong");
        assertEquals("last",
                ((TestComponent) parentComponent.getChildren().toList().get(0))
                        .getValue());

        // move it back to last
        taskList.moveTo(taskList.peek().get(0),
                SharedListSignal.ListPosition.last());
        assertEquals("last",
                ((TestComponent) parentComponent.getChildren().toList().get(2))
                        .getValue());

        // move last between first and last
        taskList.moveTo(taskList.peek().get(2), SharedListSignal.ListPosition
                .between(taskList.peek().get(0), taskList.peek().get(1)));
        assertEquals("last",
                ((TestComponent) parentComponent.getChildren().toList().get(1))
                        .getValue());
    }

    @Test
    public void bindChildren_moveLastToFirst_verifyElementAttachDetachCount() {
        CurrentInstance.clearAll();
        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        taskList.insertFirst("first");
        taskList.insertLast("middle");
        taskList.insertLast("last");

        TestLayout parentComponent = prepareTestLayout(taskList);

        // move last to first
        taskList.moveTo(taskList.peek().get(2),
                SharedListSignal.ListPosition.first());

        List<TestComponent> children = parentComponent.getChildren()
                .map(TestComponent.class::cast).toList();

        assertEquals(1, children.get(0).attachCounter);
        assertEquals(1, children.get(0).detachCounter);
        assertEquals(0, children.get(1).attachCounter);
        assertEquals(0, children.get(1).detachCounter);
        assertEquals(0, children.get(2).attachCounter);
        assertEquals(0, children.get(2).detachCounter);
    }

    @Test
    public void bindChildren_moveFirstToLast_verifyElementAttachDetachCount() {
        CurrentInstance.clearAll();
        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        taskList.insertFirst("first");
        taskList.insertLast("middle");
        taskList.insertLast("last");

        TestLayout parentComponent = prepareTestLayout(taskList);

        // move first to last
        taskList.moveTo(taskList.peek().get(0),
                SharedListSignal.ListPosition.last());

        List<TestComponent> children = parentComponent.getChildren()
                .map(TestComponent.class::cast).toList();

        assertEquals(0, children.get(0).attachCounter);
        assertEquals(0, children.get(0).detachCounter);
        assertEquals(0, children.get(1).attachCounter);
        assertEquals(0, children.get(1).detachCounter);
        assertEquals(1, children.get(2).attachCounter);
        assertEquals(1, children.get(2).detachCounter);
    }

    @Test
    public void bindChildren_moveLastBetweenFirstAndSecond_verifyElementAttachDetachCount() {
        CurrentInstance.clearAll();
        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        taskList.insertFirst("first");
        taskList.insertLast("middle");
        taskList.insertLast("last");

        TestLayout parentComponent = prepareTestLayout(taskList);

        // move last between first and second
        taskList.moveTo(taskList.peek().get(2), SharedListSignal.ListPosition
                .between(taskList.peek().get(0), taskList.peek().get(1)));

        List<TestComponent> children = parentComponent.getChildren()
                .map(TestComponent.class::cast).toList();

        assertEquals(0, children.get(0).attachCounter);
        assertEquals(0, children.get(0).detachCounter);
        assertEquals(0, children.get(1).attachCounter);
        assertEquals(0, children.get(1).detachCounter);
        assertEquals(1, children.get(2).attachCounter);
        assertEquals(1, children.get(2).detachCounter);
    }

    @Test
    public void bindChildren_addToParentComponentAndAddItem_throw() {
        // When adding children directly to parent, exception will be thrown
        // from the effect on next related Signal change.
        CurrentInstance.clearAll();
        LinkedList<ErrorEvent> events = mockLockedSessionWithErrorHandler();
        UI ui = UI.getCurrent();

        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        taskList.insertFirst("first");
        TestLayout parentComponent = new TestLayout();

        ui.add(parentComponent);

        ElementEffect.bindChildren(parentComponent.getElement(), taskList,
                valueSignal -> new TestComponent(valueSignal.get())
                        .getElement());

        var expectedComponent = new TestComponent("added directly");
        // doing wrong
        parentComponent.add(expectedComponent);

        // causes the effect to run and exception being thrown
        taskList.insertLast("last");

        ErrorEvent event = events.pollFirst();

        assertNotNull(event);
        assertEquals(IllegalStateException.class,
                event.getThrowable().getClass());
        // no changes in the element
        assertEquals(2, parentComponent.getComponentCount(),
                "Parent component children count is wrong");
        assertEquals("first",
                ((TestComponent) parentComponent.getChildren().toList().get(0))
                        .getValue());
        assertEquals("added directly",
                ((TestComponent) parentComponent.getChildren().toList().get(1))
                        .getValue());
    }

    @Test
    public void bindChildren_directParentComponentChanges_sameChildrenSizeBeforeAfter_throw() {
        // When adding children directly to parent, exception will be thrown
        // from the effect on next related Signal change.
        CurrentInstance.clearAll();
        LinkedList<ErrorEvent> events = mockLockedSessionWithErrorHandler();
        UI ui = UI.getCurrent();

        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        taskList.insertLast("first");
        taskList.insertLast("middle");
        TestLayout parentComponent = new TestLayout();

        ui.add(parentComponent);

        ElementEffect.bindChildren(parentComponent.getElement(), taskList,
                valueSignal -> new TestComponent(valueSignal.get())
                        .getElement());

        var directlyAddedComponent1 = new TestComponent("added directly 1");
        var directlyAddedComponent2 = new TestComponent("added directly 2");

        // doing wrong
        parentComponent.removeAll();
        parentComponent.add(directlyAddedComponent1);
        parentComponent.add(directlyAddedComponent2);
        // notice that size is still same as original

        // causes the effect to run and exception being thrown
        taskList.insertLast("last");

        ErrorEvent event = events.pollFirst();

        assertNotNull(event);
        assertEquals(IllegalStateException.class,
                event.getThrowable().getClass());

        List<TestComponent> children = parentComponent.getChildren()
                .map(TestComponent.class::cast).toList();
        // Changes are still applied as exception is thrown in the end of
        // the effect. Algorithm moves wrongly added elements after signal
        // list.
        assertEquals(5, parentComponent.getComponentCount(),
                "Parent component children count is wrong");
        assertEquals("first", children.get(0).getValue());
        assertEquals("middle", children.get(1).getValue());
        assertEquals("last", children.get(2).getValue());
        assertEquals("added directly 1", children.get(3).getValue());
        assertEquals("added directly 2", children.get(4).getValue());
    }

    @Test
    public void bindChildren_directParentComponentChangeByFactory_throw() {
        // When adding children directly to parent, exception will be thrown
        // from the effect on next related Signal change.
        CurrentInstance.clearAll();
        LinkedList<ErrorEvent> events = mockLockedSessionWithErrorHandler();
        UI ui = UI.getCurrent();

        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        taskList.insertLast("first");
        taskList.insertLast("middle");
        TestLayout parentComponent = new TestLayout();

        ui.add(parentComponent);

        parentComponent.bindChildren(taskList, valueSignal -> {
            String value = valueSignal.get();
            var component = new TestComponent(value);
            if ("middle".equals(value)) {
                // doing wrong
                parentComponent.add(new TestComponent("added directly"));
            }
            return component;
        });

        // causes the effect to run and exception being thrown
        taskList.insertLast("last");

        ErrorEvent event = events.pollFirst();

        assertNotNull(event);
        assertEquals(IllegalStateException.class,
                event.getThrowable().getClass());
        assertEquals(
                "Parent element must have children matching the list signal. Unexpected child at index 2: <div></div>, expected: none",
                event.getThrowable().getMessage());

        List<TestComponent> children = parentComponent.getChildren()
                .map(TestComponent.class::cast).toList();
        // Exception is thrown only in final validation in the end
        assertEquals(3, parentComponent.getComponentCount(),
                "Parent component children count is wrong");
        assertEquals("first", children.get(0).getValue());
        assertEquals("middle", children.get(1).getValue());
        assertEquals("added directly", children.get(2).getValue());
    }

    @Test
    public void bindChildren_directParentComponentChangeByCustomAttach_throw() {
        // When adding children directly to parent, exception will be thrown
        // from the effect on next related Signal change.
        CurrentInstance.clearAll();
        LinkedList<ErrorEvent> events = mockLockedSessionWithErrorHandler();
        UI ui = UI.getCurrent();

        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        taskList.insertLast("first");
        taskList.insertLast("middle");
        TestLayout parentComponent = new TestLayout();

        ui.add(parentComponent);

        parentComponent.bindChildren(taskList, valueSignal -> {
            String value = valueSignal.get();
            var component = new TestComponent(value);
            if ("middle".equals(value)) {
                component.addAttachListener(event -> {
                    // doing wrong
                    parentComponent.add(new TestComponent("added directly"));
                    event.unregisterListener();
                });
            }
            return component;
        });

        ErrorEvent event = events.pollFirst();

        assertNotNull(event);
        assertEquals(IllegalStateException.class,
                event.getThrowable().getClass());
        assertEquals(
                "Parent element must have children matching the list signal. Unexpected child at index 2: <div></div>, expected: none",
                event.getThrowable().getMessage());

        List<TestComponent> children = parentComponent.getChildren()
                .map(TestComponent.class::cast).toList();
        // Exception is thrown only in final validation in the end
        assertEquals(3, parentComponent.getComponentCount(),
                "Parent component children count is wrong");
        assertEquals("first", children.get(0).getValue());
        assertEquals("middle", children.get(1).getValue());
        assertEquals("added directly", children.get(2).getValue());
    }

    @Test
    public void bindChildren_directParentComponentChildOrderChanges_throw() {
        // When adding children directly to parent, exception will be thrown
        // from the effect on next related Signal change.
        // Exception is thrown only in final validation in the end when change
        // can't be detected by just checking size.
        CurrentInstance.clearAll();
        LinkedList<ErrorEvent> events = mockLockedSessionWithErrorHandler();
        UI ui = UI.getCurrent();

        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        taskList.insertLast("first");
        taskList.insertLast("middle");
        taskList.insertLast("last");
        TestLayout parentComponent = new TestLayout();

        ui.add(parentComponent);

        parentComponent.bindChildren(taskList, valueSignal -> {
            String value = valueSignal.get();
            var component = new TestComponent(value);
            component.getElement().setText(value);
            if ("last".equals(value)) {
                // doing wrong, change order of first two children
                parentComponent.getElement().insertChild(0,
                        parentComponent.getElement().getChild(1));
            }
            return component;
        });

        ErrorEvent event = events.pollFirst();

        assertNotNull(event);
        assertEquals(IllegalStateException.class,
                event.getThrowable().getClass());
        List<TestComponent> children = parentComponent.getChildren()
                .map(TestComponent.class::cast).toList();

        assertEquals(
                "Parent element must have children matching the list signal. Unexpected child at index 0: <div>middle</div>, expected: <div>first</div>",
                event.getThrowable().getMessage());
        assertEquals(3, parentComponent.getComponentCount(),
                "Parent component children count is wrong");
        assertEquals("middle", children.get(0).getValue());
        assertEquals("first", children.get(1).getValue());
        assertEquals("last", children.get(2).getValue());
    }

    @Test
    public void bindChildren_runInTransaction_effectRunOnce() {
        CurrentInstance.clearAll();
        var expectedMockedElements = new ArrayList<Element>();

        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        TestLayout parentComponent = new TestLayout(expectedMockedElements);
        new MockUI().add(parentComponent);

        parentComponent.bindChildren(taskList, valueSignal -> {
            var component = new TestComponent(valueSignal.get(),
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
            taskList.moveTo(taskList.get().get(2), SharedListSignal.ListPosition
                    .between(taskList.get().get(0), taskList.get().get(1)));

            taskList.remove(taskList.get().get(0));
        });

        // getChildren() should be called twice per bindChildren effect call
        verify(parentComponent.getElement(), times(2)).getChildren();

        assertEquals(2, parentComponent.getComponentCount(),
                "Parent component children count is wrong");
        assertEquals("middle",
                ((TestComponent) parentComponent.getChildren().toList().get(0))
                        .getValue());
        assertEquals("last",
                ((TestComponent) parentComponent.getChildren().toList().get(1))
                        .getValue());
    }

    @Test
    public void bindChildren_withNullFromChildFactory_throws() {
        CurrentInstance.clearAll();
        LinkedList<ErrorEvent> events = mockLockedSessionWithErrorHandler();
        UI ui = UI.getCurrent();

        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        taskList.insertFirst("first");
        TestLayout parentComponent = new TestLayout();

        ui.add(parentComponent);

        parentComponent.bindChildren(taskList, valueSignal -> null);

        ErrorEvent event = events.pollFirst();

        assertNotNull(event);
        assertEquals(IllegalStateException.class,
                event.getThrowable().getClass());
        assertEquals(
                "HasComponents.bindChildren childFactory must not return null",
                event.getThrowable().getMessage());
    }

    @Test
    public void bindChildren_registrationRemove_effectRemoved() {
        CurrentInstance.clearAll();
        SharedListSignal<String> taskList = new SharedListSignal<>(
                String.class);
        taskList.insertFirst("first");
        taskList.insertLast("second");

        TestLayout parentComponent = new TestLayout();
        new MockUI().add(parentComponent);

        Registration registration = ElementEffect.bindChildren(
                parentComponent.getElement(), taskList,
                valueSignal -> new TestComponent(valueSignal.get())
                        .getElement());

        assertEquals(2, parentComponent.getComponentCount(),
                "Parent should have initial children");
        assertEquals("first",
                ((TestComponent) parentComponent.getChildren().toList().get(0))
                        .getValue());
        assertEquals("second",
                ((TestComponent) parentComponent.getChildren().toList().get(1))
                        .getValue());

        // Remove the registration
        registration.remove();

        // Modify the list signal after removing registration
        taskList.insertLast("third");

        // Parent should not be updated after registration is removed
        assertEquals(2, parentComponent.getComponentCount(),
                "Parent should still have only 2 children");
        assertEquals("first",
                ((TestComponent) parentComponent.getChildren().toList().get(0))
                        .getValue());
        assertEquals("second",
                ((TestComponent) parentComponent.getChildren().toList().get(1))
                        .getValue());
    }

    @Test
    public void bindChildren_withLocalValueSignalList_parentUpdated() {
        CurrentInstance.clearAll();
        ValueSignal<String> first = new ValueSignal<>("first");
        ValueSignal<String> second = new ValueSignal<>("second");

        ValueSignal<List<ValueSignal<String>>> listSignal = new ValueSignal<>(
                new ArrayList<>(List.of(first)));

        TestLayout parentComponent = new TestLayout();
        new MockUI().add(parentComponent);

        parentComponent.bindChildren(listSignal,
                valueSignal -> new TestComponent(valueSignal.get()));

        assertEquals(1, parentComponent.getComponentCount());
        assertEquals("first",
                ((TestComponent) parentComponent.getChildren().toList().get(0))
                        .getValue());

        // Add second item
        listSignal.set(new ArrayList<>(List.of(first, second)));

        assertEquals(2, parentComponent.getComponentCount());
        assertEquals("first",
                ((TestComponent) parentComponent.getChildren().toList().get(0))
                        .getValue());
        assertEquals("second",
                ((TestComponent) parentComponent.getChildren().toList().get(1))
                        .getValue());

        // Remove first item
        listSignal.set(new ArrayList<>(List.of(second)));

        assertEquals(1, parentComponent.getComponentCount());
        assertEquals("second",
                ((TestComponent) parentComponent.getChildren().toList().get(0))
                        .getValue());

        // Clear list
        listSignal.set(new ArrayList<>());

        assertEquals(0, parentComponent.getComponentCount());
    }

    private TestLayout prepareTestLayout(SharedListSignal<String> listSignal) {
        TestLayout parentComponent = new TestLayout();
        new MockUI().add(parentComponent);

        parentComponent.bindChildren(listSignal,
                valueSignal -> new TestComponent(valueSignal.get()));

        parentComponent.getChildren().map(TestComponent.class::cast)
                .forEach(TestComponent::resetCounters);

        return parentComponent;
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
