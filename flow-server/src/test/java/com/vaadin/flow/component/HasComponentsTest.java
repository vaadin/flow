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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.nodefeature.SignalBindingFeature;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.signals.BindingActiveException;
import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class HasComponentsTest {

    private static MockVaadinServletService service;

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

    @BeforeAll
    public static void init() {
        service = new MockVaadinServletService();
    }

    @AfterAll
    public static void clean() {
        CurrentInstance.clearAll();
        service.destroy();
    }

    @Test
    public void addStringToComponent() {
        String text = "Add text";
        TestComponent component = new TestComponent();
        component.add(text);

        assertEquals(text, component.getElement().getText());
    }

    @Test
    public void insertComponentAtFirst() {
        TestComponent component = createTestStructure();
        TestComponent innerComponent = new TestComponent();
        innerComponent.setId("insert-component-first");
        component.addComponentAsFirst(innerComponent);
        checkChildren(4, component);
        assertEquals(innerComponent.getId(),
                component.getChildren().findFirst().get().getId());
    }

    @Test
    public void insertComponentAtIndex() {
        TestComponent component = createTestStructure();
        TestComponent innerComponent = new TestComponent();
        innerComponent.setId("insert-component-index");
        component.addComponentAtIndex(2, innerComponent);
        checkChildren(4, component);
        assertEquals(innerComponent.getId(), component.getElement().getChild(2)
                .getComponent().get().getId());
    }

    @Test
    public void insertComponentIndexLessThanZero() {
        TestComponent component = createTestStructure();
        TestComponent innerComponent = new TestComponent();
        innerComponent.setId("insert-component-index-less");
        assertThrows(IllegalArgumentException.class,
                () -> component.addComponentAtIndex(-5, innerComponent));
    }

    @Test
    public void insertComponentIndexGreaterThanChildrenNumber() {
        TestComponent component = createTestStructure();
        TestComponent innerComponent = new TestComponent();
        innerComponent.setId("insert-component-index-greater");
        assertThrows(IllegalArgumentException.class,
                () -> component.addComponentAtIndex(100, innerComponent));
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
            fail();
        } catch (IllegalArgumentException exception) {
            assertEquals(component, child.getParent().get());
        }
    }

    @Test
    public void remove_removeSeveralComponents_oneHasNoParent_childIsRemoved() {
        TestComponent component = createTestStructure();

        TestComponent child = new TestComponent();
        component.add(child);

        TestComponent notAChild = new TestComponent();

        component.remove(notAChild, child);
        assertFalse(child.getParent().isPresent());
        assertFalse(component.getChildren().filter(comp -> comp.equals(child))
                .findAny().isPresent());
    }

    @Test
    public void remove_removeComponentWithCorrectParent() {
        TestComponent component = createTestStructure();
        TestComponent innerComponent = new TestComponent();

        long size = component.getChildren().count();

        component.add(innerComponent);

        component.remove(innerComponent);

        assertEquals(size, component.getChildren().count());
    }

    @Test
    public void remove_removeComponentWithDifferentParent() {
        TestComponent component = createTestStructure();

        TestComponent another = createTestStructure();
        TestComponent innerComponent = new TestComponent();

        another.add(innerComponent);

        assertThrows(IllegalArgumentException.class,
                () -> component.remove(innerComponent));
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
        assertEquals(number, component.getChildren().count());
    }

    @Test
    public void bindChildren_addsChildrenFromListSignal() {
        CurrentInstance.clearAll();
        TestComponent container = new TestComponent();
        new MockUI().add(container);

        SharedListSignal<String> items = new SharedListSignal<>(String.class);
        items.insertFirst("first");
        items.insertLast("second");
        items.insertLast("third");

        container.bindChildren(items, item -> new TestComponent(item.get()));

        assertEquals(3, container.getChildren().count());
        assertEquals("first",
                container.getChildren().toList().get(0).getId().orElse(null));
        assertEquals("second",
                container.getChildren().toList().get(1).getId().orElse(null));
        assertEquals("third",
                container.getChildren().toList().get(2).getId().orElse(null));
    }

    @Test
    public void bindChildren_updatesChildrenOnListChange() {
        CurrentInstance.clearAll();
        TestComponent container = new TestComponent();
        new MockUI().add(container);

        SharedListSignal<String> items = new SharedListSignal<>(String.class);
        items.insertFirst("first");

        container.bindChildren(items, item -> new TestComponent(item.get()));

        assertEquals(1, container.getChildren().count());

        items.insertLast("second");
        assertEquals(2, container.getChildren().count());
        assertEquals("second",
                container.getChildren().toList().get(1).getId().orElse(null));

        items.remove(items.peek().get(0));
        assertEquals(1, container.getChildren().count());
        assertEquals("second",
                container.getChildren().toList().get(0).getId().orElse(null));
    }

    @Test
    public void bindChildren_removeBindingViaFeature_stopsUpdatesAndAllowsManualAddRemove() {
        CurrentInstance.clearAll();
        TestComponent container = new TestComponent();
        new MockUI().add(container);

        SharedListSignal<String> items = new SharedListSignal<>(String.class);
        items.insertFirst("first");

        container.bindChildren(items, item -> new TestComponent(item.get()));

        assertEquals(1, container.getChildren().count());

        // Remove binding via the node's SignalBindingFeature
        SignalBindingFeature feature = container.getElement().getNode()
                .getFeature(SignalBindingFeature.class);
        feature.removeBinding(SignalBindingFeature.CHILDREN);

        // Signal changes should no longer affect children
        items.insertLast("second");
        assertEquals(1, container.getChildren().count());

        // Manual add and remove should work without throwing
        TestComponent newChild = new TestComponent("manual");
        container.add(newChild);
        assertEquals(2, container.getChildren().count());

        Component firstChild = container.getChildren().toList().get(0);
        container.remove(firstChild);
        assertEquals(1, container.getChildren().count());
    }

    @Test
    public void bindChildren_throwsIfContainerNotEmpty() {
        CurrentInstance.clearAll();
        TestComponent container = new TestComponent();
        new MockUI().add(container);
        container.add(new TestComponent("existing"));

        SharedListSignal<String> items = new SharedListSignal<>(String.class);

        assertThrows(IllegalStateException.class, () -> container
                .bindChildren(items, item -> new TestComponent(item.get())));
    }

    @Test
    public void bindChildren_throwsIfBindingAlreadyExists() {
        CurrentInstance.clearAll();
        TestComponent container = new TestComponent();
        new MockUI().add(container);

        SharedListSignal<String> items = new SharedListSignal<>(String.class);

        container.bindChildren(items, item -> new TestComponent(item.get()));

        SharedListSignal<String> otherItems = new SharedListSignal<>(
                String.class);
        assertThrows(BindingActiveException.class,
                () -> container.bindChildren(otherItems,
                        item -> new TestComponent(item.get())));
    }

    @Test
    public void bindChildren_addThrowsWhileBindingActive() {
        CurrentInstance.clearAll();
        TestComponent container = new TestComponent();
        new MockUI().add(container);

        SharedListSignal<String> items = new SharedListSignal<>(String.class);
        items.insertFirst("first");

        container.bindChildren(items, item -> new TestComponent(item.get()));

        assertThrows(BindingActiveException.class,
                () -> container.add(new TestComponent("manual")),
                "add should throw while binding is active");
    }

    @Test
    public void bindChildren_removeThrowsWhileBindingActive() {
        CurrentInstance.clearAll();
        TestComponent container = new TestComponent();
        new MockUI().add(container);

        SharedListSignal<String> items = new SharedListSignal<>(String.class);
        items.insertFirst("first");

        container.bindChildren(items, item -> new TestComponent(item.get()));

        Component child = container.getChildren().toList().get(0);

        assertThrows(BindingActiveException.class,
                () -> container.remove(child),
                "remove should throw while binding is active");
    }

}
