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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.shared.SharedListSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class HasComponentsTest {

    @Tag("div")
    private static class TestComponent extends Component
            implements HasComponents {
        public TestComponent() {
            super();
        }

        public TestComponent(String id) {
            super();
            setId(id);
        }
    }

    @BeforeClass
    public static void init() {
        runWithFeatureFlagEnabled(() -> {
        });
    }

    @AfterClass
    public static void clean() {
        CurrentInstance.clearAll();
    }

    @Test
    public void addStringToComponent() {
        String text = "Add text";
        TestComponent component = new TestComponent();
        component.add(text);

        Assert.assertEquals(text, component.getElement().getText());
    }

    @Test
    public void insertComponentAtFirst() {
        TestComponent component = createTestStructure();
        TestComponent innerComponent = new TestComponent();
        innerComponent.setId("insert-component-first");
        component.addComponentAsFirst(innerComponent);
        checkChildren(4, component);
        Assert.assertEquals(innerComponent.getId(),
                component.getChildren().findFirst().get().getId());
    }

    @Test
    public void insertComponentAtIndex() {
        TestComponent component = createTestStructure();
        TestComponent innerComponent = new TestComponent();
        innerComponent.setId("insert-component-index");
        component.addComponentAtIndex(2, innerComponent);
        checkChildren(4, component);
        Assert.assertEquals(innerComponent.getId(), component.getElement()
                .getChild(2).getComponent().get().getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertComponentIndexLessThanZero() {
        TestComponent component = createTestStructure();
        TestComponent innerComponent = new TestComponent();
        innerComponent.setId("insert-component-index-less");
        component.addComponentAtIndex(-5, innerComponent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertComponentIndexGreaterThanChildrenNumber() {
        TestComponent component = createTestStructure();
        TestComponent innerComponent = new TestComponent();
        innerComponent.setId("insert-component-index-greater");
        component.addComponentAtIndex(100, innerComponent);
    }

    @Test
    public void remove_removeComponentWithNoParent() {
        TestComponent component = createTestStructure();
        TestComponent innerComponent = new TestComponent();

        // No any exception is thrown
        component.remove(innerComponent);
    }

    @Test
    public void remove_removeSeveralComponents_oneHasParent_nothingRemovedAndThrows() {
        TestComponent component = createTestStructure();

        TestComponent child = new TestComponent();
        component.add(child);

        TestComponent another = createTestStructure();
        TestComponent innerComponent = new TestComponent();
        another.add(innerComponent);

        try {
            component.remove(child, innerComponent);
            Assert.fail();
        } catch (IllegalArgumentException exception) {
            Assert.assertEquals(component, child.getParent().get());
        }
    }

    @Test
    public void remove_removeSeveralComponents_oneHasNoParent_childIsRemoved() {
        TestComponent component = createTestStructure();

        TestComponent child = new TestComponent();
        component.add(child);

        TestComponent notAChild = new TestComponent();

        component.remove(notAChild, child);
        Assert.assertFalse(child.getParent().isPresent());
        Assert.assertFalse(component.getChildren()
                .filter(comp -> comp.equals(child)).findAny().isPresent());
    }

    @Test
    public void remove_removeComponentWithCorrectParent() {
        TestComponent component = createTestStructure();
        TestComponent innerComponent = new TestComponent();

        long size = component.getChildren().count();

        component.add(innerComponent);

        component.remove(innerComponent);

        Assert.assertEquals(size, component.getChildren().count());
    }

    @Test(expected = IllegalArgumentException.class)
    public void remove_removeComponentWithDifferentParent() {
        TestComponent component = createTestStructure();

        TestComponent another = createTestStructure();
        TestComponent innerComponent = new TestComponent();

        another.add(innerComponent);

        component.remove(innerComponent);
    }

    private TestComponent createTestStructure() {
        TestComponent component = new TestComponent();
        checkChildren(0, component);
        component.add(new TestComponent(), new TestComponent(),
                new TestComponent());
        checkChildren(3, component);
        return component;
    }

    private void checkChildren(int number, TestComponent component) {
        Assert.assertEquals(number, component.getChildren().count());
    }

    @Test
    public void bindChildren_addsChildrenFromListSignal() {
        runWithFeatureFlagEnabled(() -> {
            TestComponent container = new TestComponent();
            new MockUI().add(container);

            SharedListSignal<String> items = new SharedListSignal<>(
                    String.class);
            items.insertFirst("first");
            items.insertLast("second");
            items.insertLast("third");

            container.bindChildren(items,
                    item -> new TestComponent(item.value()));

            assertEquals(3, container.getChildren().count());
            assertEquals("first", container.getChildren().toList().get(0)
                    .getId().orElse(null));
            assertEquals("second", container.getChildren().toList().get(1)
                    .getId().orElse(null));
            assertEquals("third", container.getChildren().toList().get(2)
                    .getId().orElse(null));
        });
    }

    @Test
    public void bindChildren_updatesChildrenOnListChange() {
        runWithFeatureFlagEnabled(() -> {
            TestComponent container = new TestComponent();
            new MockUI().add(container);

            SharedListSignal<String> items = new SharedListSignal<>(
                    String.class);
            items.insertFirst("first");

            container.bindChildren(items,
                    item -> new TestComponent(item.value()));

            assertEquals(1, container.getChildren().count());

            items.insertLast("second");
            assertEquals(2, container.getChildren().count());
            assertEquals("second", container.getChildren().toList().get(1)
                    .getId().orElse(null));

            items.remove(items.value().get(0));
            assertEquals(1, container.getChildren().count());
            assertEquals("second", container.getChildren().toList().get(0)
                    .getId().orElse(null));
        });
    }

    @Test
    public void bindChildren_registrationRemove_stopsUpdating() {
        runWithFeatureFlagEnabled(() -> {
            TestComponent container = new TestComponent();
            new MockUI().add(container);

            SharedListSignal<String> items = new SharedListSignal<>(
                    String.class);
            items.insertFirst("first");

            Registration registration = container.bindChildren(items,
                    item -> new TestComponent(item.value()));

            assertEquals(1, container.getChildren().count());

            registration.remove();

            items.insertLast("second");
            assertEquals(
                    "After removing registration, children should not be updated",
                    1, container.getChildren().count());
        });
    }

    @Test
    public void bindChildren_throwsIfContainerNotEmpty() {
        runWithFeatureFlagEnabled(() -> {
            TestComponent container = new TestComponent();
            new MockUI().add(container);
            container.add(new TestComponent("existing"));

            SharedListSignal<String> items = new SharedListSignal<>(
                    String.class);

            assertThrows(IllegalStateException.class,
                    () -> container.bindChildren(items,
                            item -> new TestComponent(item.value())));
        });
    }

    @Test
    public void bindChildren_throwsIfBindingAlreadyExists() {
        runWithFeatureFlagEnabled(() -> {
            TestComponent container = new TestComponent();
            new MockUI().add(container);

            SharedListSignal<String> items = new SharedListSignal<>(
                    String.class);

            container.bindChildren(items,
                    item -> new TestComponent(item.value()));

            SharedListSignal<String> otherItems = new SharedListSignal<>(
                    String.class);
            assertThrows(BindingActiveException.class,
                    () -> container.bindChildren(otherItems,
                            item -> new TestComponent(item.value())));
        });
    }

    @Test
    public void bindChildren_addThrowsWhileBindingActive() {
        runWithFeatureFlagEnabled(() -> {
            TestComponent container = new TestComponent();
            new MockUI().add(container);

            SharedListSignal<String> items = new SharedListSignal<>(
                    String.class);
            items.insertFirst("first");

            container.bindChildren(items,
                    item -> new TestComponent(item.value()));

            assertThrows("add should throw while binding is active",
                    BindingActiveException.class,
                    () -> container.add(new TestComponent("manual")));
        });
    }

    @Test
    public void bindChildren_removeThrowsWhileBindingActive() {
        runWithFeatureFlagEnabled(() -> {
            TestComponent container = new TestComponent();
            new MockUI().add(container);

            SharedListSignal<String> items = new SharedListSignal<>(
                    String.class);
            items.insertFirst("first");

            container.bindChildren(items,
                    item -> new TestComponent(item.value()));

            Component child = container.getChildren().toList().get(0);

            assertThrows("remove should throw while binding is active",
                    BindingActiveException.class,
                    () -> container.remove(child));
        });
    }

    @Test
    public void bindChildren_addAndRemoveWorkAfterRegistrationRemoved() {
        runWithFeatureFlagEnabled(() -> {
            TestComponent container = new TestComponent();
            new MockUI().add(container);

            SharedListSignal<String> items = new SharedListSignal<>(
                    String.class);
            items.insertFirst("first");

            Registration registration = container.bindChildren(items,
                    item -> new TestComponent(item.value()));

            assertEquals(1, container.getChildren().count());

            registration.remove();

            // Now add and remove should work normally
            TestComponent newChild = new TestComponent("manual");
            container.add(newChild);
            assertEquals(2, container.getChildren().count());

            Component firstChild = container.getChildren().toList().get(0);
            container.remove(firstChild);
            assertEquals(1, container.getChildren().count());
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
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            CurrentInstance.clearAll();
        }
    }

}
