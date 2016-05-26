/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.hummingbird.dom.ElementUtil;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.event.ComponentEventBus;
import com.vaadin.hummingbird.event.ComponentEventListener;
import com.vaadin.server.VaadinSession;
import com.vaadin.tests.util.TestUtil;

public class ComponentTest {

    @Tag("div")
    public static class TestDiv extends Component {
    }

    @Tag("div")
    public static class TestComponentWhichHasComponentField extends Component {
        private TestButton button = new TestButton();

        public TestComponentWhichHasComponentField() {
            getElement().appendChild(button.getElement());
        }
    }

    @Tag("div")
    public static class TestComponentWhichCreatesComponentInConstructor
            extends Component {

        public TestComponentWhichCreatesComponentInConstructor() {
            getElement().appendChild(new TestButton().getElement());
        }
    }

    @Tag("button")
    public static class TestButton extends Component {
    }

    @Tag("button")
    public static class TestOtherButton extends Component {
    }

    private Component divWithTextComponent;
    private Component parentDivComponent;
    private Component child1SpanComponent;
    private Component child2InputComponent;

    public interface TracksAttachDetach {
        default void track() {
            if (this instanceof Component) {
                ((Component) this).addAttachListener(
                        event -> getAttachEvents().incrementAndGet());
                ((Component) this).addDetachListener(
                        event -> getDetachEvents().incrementAndGet());
            } else {
                throw new IllegalStateException("Cannot track a non-component");
            }
        }

        AtomicInteger getAttachEvents();

        AtomicInteger getDetachEvents();

        default void assertAttachEvents(int attachEvents) {
            Assert.assertEquals(attachEvents, getAttachEvents().get());
        }

        default void assertDetachEvents(int detachEvents) {
            Assert.assertEquals(detachEvents, getDetachEvents().get());
        }
    }

    public static abstract class TracksAttachDetachComponent extends Component
            implements TracksAttachDetach {

        private AtomicInteger attachEvents = new AtomicInteger();
        private AtomicInteger detachEvents = new AtomicInteger();

        public TracksAttachDetachComponent() {
        }

        public TracksAttachDetachComponent(Element element) {
            super(element);
        }

        @Override
        public AtomicInteger getAttachEvents() {
            return attachEvents;
        }

        @Override
        public AtomicInteger getDetachEvents() {
            return detachEvents;
        }

    }

    public static class TestComponent extends TracksAttachDetachComponent {

        public TestComponent() {
            this(ElementFactory.createDiv());
        }

        public TestComponent(Element element) {
            super(element);
        }

        @Override
        public String toString() {
            return getElement().getOwnTextContent();
        }

        @Override
        public void fireEvent(ComponentEvent<?> componentEvent) {
            super.fireEvent(componentEvent);
        }

        @Override
        public <T extends ComponentEvent<?>> EventRegistrationHandle addListener(
                Class<T> eventType, ComponentEventListener<T> listener) {
            return super.addListener(eventType, listener);
        }

        @Override
        public ComponentEventBus getEventBus() {
            return super.getEventBus();
        }

    }

    @Tag(Tag.DIV)
    private static class TestComponentWithTag extends Component {

    }

    private static class TestComponentWithInheritedTag
            extends TestComponentWithTag {

    }

    @Tag("")
    private static class TestComponentWithEmptyTag extends Component {

    }

    private static class TestComponentWithoutTag extends Component {

    }

    private static class BrokenComponent extends Component {

        public BrokenComponent() {
            super(null);
        }

    }

    private static class TestComponentContainer extends TestComponent {

        public TestComponentContainer() {
        }

        public void add(Component c) {
            getElement().appendChild(c.getElement());
        }

        public void remove(Component c) {
            getElement().removeChild(c.getElement());
        }
    }

    @Before
    public void setup() {
        divWithTextComponent = new TestComponent(
                ElementFactory.createDiv("Test component"));
        parentDivComponent = new TestComponent(ElementFactory.createDiv());
        child1SpanComponent = new TestComponent(
                ElementFactory.createSpan("Span"));
        child2InputComponent = new TestComponent(ElementFactory.createInput());
        parentDivComponent.getElement().appendChild(
                child1SpanComponent.getElement(),
                child2InputComponent.getElement());
    }

    @Test
    public void getElement() {
        Assert.assertEquals(Tag.DIV,
                divWithTextComponent.getElement().getTag());
        Assert.assertEquals("Test component",
                divWithTextComponent.getElement().getTextContent());
    }

    @Test
    public void getParentForAttachedComponent() {
        Assert.assertEquals(parentDivComponent,
                child1SpanComponent.getParent().get());
        Assert.assertEquals(parentDivComponent,
                child2InputComponent.getParent().get());
    }

    @Test
    public void getParentForDetachedComponent() {
        Assert.assertFalse(parentDivComponent.getParent().isPresent());
    }

    @Test
    public void defaultGetChildrenDirectlyAttached() {
        assertChildren(parentDivComponent, child1SpanComponent,
                child2InputComponent);
    }

    public static void assertChildren(Component parent,
            Component... expectedChildren) {
        List<Component> children = parent.getChildren()
                .collect(Collectors.toList());
        Assert.assertArrayEquals(expectedChildren, children.toArray());
        for (Component c : children) {
            Assert.assertEquals(c.getParent().get(), parent);
        }
    }

    @Test
    public void defaultGetChildrenMultiple() {
        // parent
        // * level1
        // ** child1
        // ** child2

        Element level1 = ElementFactory.createDiv("Level1");

        parentDivComponent.getElement().appendChild(level1);
        level1.appendChild(child1SpanComponent.getElement());
        level1.appendChild(child2InputComponent.getElement());

        assertChildren(parentDivComponent, child1SpanComponent,
                child2InputComponent);

    }

    @Test
    public void defaultGetChildrenDirectlyDeepElementHierarchy() {
        // parent
        // * level1
        // ** level2
        // *** child1
        // * child2
        // * level1b
        // ** child3

        TestComponent parent = new TestComponent(ElementFactory.createDiv());
        TestComponent child1 = new TestComponent(
                ElementFactory.createDiv("Child1"));
        TestComponent child2 = new TestComponent(
                ElementFactory.createDiv("Child2"));
        TestComponent child3 = new TestComponent(
                ElementFactory.createDiv("Child2"));

        Element parentElement = parent.getElement();
        parentElement.appendChild(
                new Element("level1").appendChild(
                        new Element("level2").appendChild(child1.getElement())),
                child2.getElement(),
                new Element("level1b").appendChild(child3.getElement()));

        List<Component> children = parent.getChildren()
                .collect(Collectors.toList());
        Assert.assertArrayEquals(new Component[] { child1, child2, child3 },
                children.toArray());

    }

    @Test
    public void defaultGetChildrenNoChildren() {
        List<Component> children = parentDivComponent.getChildren()
                .collect(Collectors.toList());
        Assert.assertArrayEquals(
                new Component[] { child1SpanComponent, child2InputComponent },
                children.toArray());

    }

    @Test(expected = AssertionError.class)
    public void attachBrokenComponent() {
        BrokenComponent c = new BrokenComponent();
        TestComponentContainer tc = new TestComponentContainer();
        tc.add(c);
    }

    @Test
    public void setElement() {
        Component c = new Component(null) {

        };
        Element element = ElementFactory.createDiv();
        Component.setElement(c, element);
        Assert.assertEquals(c, ElementUtil.getComponent(element).get());
        Assert.assertEquals(element, c.getElement());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setElementNull() {
        Component c = new Component(null) {
        };
        Component.setElement(c, null);
    }

    @Test(expected = IllegalStateException.class)
    public void setElementTwice() {
        Component c = new Component(null) {
        };
        Element element = ElementFactory.createDiv();
        Component.setElement(c, element);
        Component.setElement(c, element);

    }

    @Test
    public void createComponentWithTag() {
        Component component = new TestComponentWithTag();

        Assert.assertEquals(Tag.DIV, component.getElement().getTag());
    }

    @Test
    public void createComponentWithInheritedTag() {
        Component component = new TestComponentWithInheritedTag();

        Assert.assertEquals(Tag.DIV, component.getElement().getTag());
    }

    @Test(expected = IllegalStateException.class)
    public void createComponentWithEmptyTag() {
        new TestComponentWithEmptyTag();
    }

    @Test(expected = IllegalStateException.class)
    public void createComponentWithoutTag() {
        new TestComponentWithoutTag();
    }

    @Test
    public void getUI_noParent() {
        TestComponent c = new TestComponent();
        assertEmpty(c.getUI());
    }

    @Test
    public void getUI_detachedParent() {
        TestComponentContainer parent = new TestComponentContainer();
        TestComponent child = new TestComponent();
        parent.add(child);
        assertEmpty(child.getUI());
    }

    @Test
    public void getUI_attachedToUI() {
        TestComponent child = new TestComponent();
        UI ui = new UI();
        ui.add(child);
        Assert.assertEquals(ui, child.getUI().get());
    }

    @Test
    public void getUI_attachedThroughParent() {
        TestComponentContainer parent = new TestComponentContainer();
        TestComponent child = new TestComponent();
        parent.add(child);
        UI ui = new UI();
        ui.add(parent);
        Assert.assertEquals(ui, child.getUI().get());
    }

    private void assertEmpty(Optional<?> optional) {
        Assert.assertEquals("Optional should be empty but is " + optional,
                Optional.empty(), optional);
    }

    @Test
    public void testAttachDetachListeners_parentAttachDetach_childListenersTriggered() {
        UI ui = new UI();
        TestComponentContainer parent = new TestComponentContainer();
        TestComponentContainer child = new TestComponentContainer();
        TestComponent grandChild = new TestComponent();
        child.track();
        EventRegistrationHandle attachRegistrationHandle = grandChild
                .addAttachListener(event -> grandChild.getAttachEvents()
                        .incrementAndGet());
        EventRegistrationHandle detachRegistrationHandle = grandChild
                .addDetachListener(event -> grandChild.getDetachEvents()
                        .incrementAndGet());

        parent.add(child);
        child.add(grandChild);

        child.assertAttachEvents(0);
        grandChild.assertAttachEvents(0);

        ui.add(parent);

        child.assertAttachEvents(1);
        grandChild.assertAttachEvents(1);
        child.assertDetachEvents(0);
        grandChild.assertDetachEvents(0);

        ui.remove(parent);
        parent.remove(child);

        child.assertAttachEvents(1);
        grandChild.assertAttachEvents(1);
        child.assertDetachEvents(1);
        grandChild.assertDetachEvents(1);

        ui.add(parent);
        parent.add(child);

        child.assertAttachEvents(2);
        grandChild.assertAttachEvents(2);
        child.assertDetachEvents(1);
        grandChild.assertDetachEvents(1);

        ui.remove(parent);

        child.assertAttachEvents(2);
        grandChild.assertAttachEvents(2);
        child.assertDetachEvents(2);
        grandChild.assertDetachEvents(2);

        attachRegistrationHandle.remove();
        detachRegistrationHandle.remove();

        ui.add(child);

        child.assertAttachEvents(3);
        grandChild.assertAttachEvents(2);

        ui.remove(child);

        child.assertDetachEvents(3);
        grandChild.assertDetachEvents(2);
    }

    @Test
    public void testAttachListener_eventOrder_childFirst() {
        UI ui = new UI();
        TestComponentContainer parent = new TestComponentContainer();
        TestComponent child = new TestComponent();
        child.track();
        parent.track();

        child.addAttachListener(event -> {
            Assert.assertEquals(0, parent.getAttachEvents().get());
        });
        parent.addAttachListener(event -> {
            Assert.assertEquals(1, child.getAttachEvents().get());
        });

        parent.add(child);

        child.assertAttachEvents(0);
        parent.assertAttachEvents(0);

        ui.add(parent);

        child.assertAttachEvents(1);
        parent.assertAttachEvents(1);
    }

    @Test
    public void testDetachListener_eventOrder_childFirst() {
        UI ui = new UI();
        TestComponentContainer parent = new TestComponentContainer();
        TestComponent child = new TestComponent();
        child.track();
        parent.track();

        child.addDetachListener(event -> {
            Assert.assertEquals(0, parent.getDetachEvents().get());
        });
        parent.addDetachListener(event -> {
            Assert.assertEquals(1, child.getDetachEvents().get());
        });

        parent.add(child);
        ui.add(parent);

        child.assertDetachEvents(0);
        parent.assertDetachEvents(0);

        ui.remove(parent);

        child.assertDetachEvents(1);
        parent.assertDetachEvents(1);
    }

    @Test
    public void testAttachDetach_elementMoved_bothEventsTriggered() {
        UI ui = new UI();
        TestComponentContainer parent = new TestComponentContainer();
        TestComponent child = new TestComponent();

        parent.add(child);
        ui.add(parent);

        child.track();

        child.addAttachListener(event -> {
            Assert.assertEquals(1, child.getDetachEvents().get());
        });
        child.addDetachListener(event -> {
            Assert.assertEquals(0, child.getAttachEvents().get());
        });

        ui.add(child);

        child.assertAttachEvents(1);
        child.assertDetachEvents(1);
    }

    @Test
    public void testAttachDetachEvent_uiCanBeFound() {
        UI ui = new UI();
        TestComponent testComponent = new TestComponent();
        testComponent.track();

        testComponent.addAttachListener(event -> Assert.assertEquals(ui,
                event.getSource().getUI().get()));

        testComponent.addDetachListener(event -> Assert.assertEquals(ui,
                event.getSource().getUI().get()));

        testComponent.assertAttachEvents(0);

        ui.add(testComponent);

        testComponent.assertAttachEvents(1);

        testComponent.assertDetachEvents(0);

        ui.remove(testComponent);

        testComponent.assertDetachEvents(1);
    }

    @Test
    public void testOnAttachOnDetachAndEventsOrder() {
        List<String> triggered = new ArrayList<>();
        Component customComponent = new Component(new Element("div")) {
            @Override
            protected void onAttach(AttachEvent attachEvent) {
                triggered.add("onAttach");
            }

            @Override
            protected void onDetach(DetachEvent detachEvent) {
                triggered.add("onDetach");
            }
        };
        customComponent
                .addAttachListener(event -> triggered.add("attachEvent"));
        customComponent
                .addDetachListener(event -> triggered.add("detachEvent"));

        UI ui = new UI();
        ui.add(customComponent);

        TestUtil.assertArrays(triggered.toArray(),
                new String[] { "onAttach", "attachEvent" });

        triggered.clear();
        ui.remove(customComponent);

        TestUtil.assertArrays(triggered.toArray(),
                new String[] { "onDetach", "detachEvent" });

        TestComponentContainer container = new TestComponentContainer();

        ui.add(customComponent, container);
        triggered.clear();

        container.add(customComponent);

        TestUtil.assertArrays(triggered.toArray(), new String[] { "onDetach",
                "detachEvent", "onAttach", "attachEvent" });
    }

    @Test
    public void testUIInitialAttach() {
        AtomicBoolean initialAttach = new AtomicBoolean(false);
        UI ui = new UI();
        ui.addAttachListener(e -> {
            initialAttach.set(e.isInitialAttach());
        });
        ui.getInternals().setSession(new VaadinSession(null));
        Assert.assertTrue(initialAttach.get());
        // UI is never detached and reattached
    }

    @Test
    public void testInitialAttach() {
        AtomicBoolean initialAttach = new AtomicBoolean(false);
        TestComponent c = new TestComponent();
        c.addAttachListener(e -> {
            initialAttach.set(e.isInitialAttach());
        });
        UI ui = new UI();
        ui.add(c);
        Assert.assertTrue(initialAttach.get());
    }

    @Test
    public void testSecondAttach() {
        AtomicBoolean initialAttach = new AtomicBoolean(false);
        TestComponent c = new TestComponent();
        c.addAttachListener(e -> {
            initialAttach.set(e.isInitialAttach());
        });
        UI ui = new UI();
        ui.add(c);
        ui.remove(c);
        ui.add(c);
        Assert.assertFalse(initialAttach.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrapNullComponentType() {
        new Element("div").as(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrapWrongTag() {
        Element foo = new Element("foo");
        foo.as(TestDiv.class);
    }

    @Test(expected = IllegalStateException.class)
    public void wrappedComponentGetParent() {
        Element div = new Element("div");
        Element button = new Element("button");
        div.appendChild(button);

        div.as(TestDiv.class);
        button.as(TestButton.class).getParent();
    }

    @Test(expected = IllegalStateException.class)
    public void wrappedComponentGetChildren() {
        Element div = new Element("div");
        Element button = new Element("button");
        div.appendChild(button);

        button.as(TestButton.class);
        div.as(TestDiv.class).getChildren();
    }

    @Test
    public void componentFromHierarchy() {
        Element div = new Element("div");
        Element button = new Element("button");
        div.appendChild(button);

        TestDiv testDiv = Component.from(div, TestDiv.class);
        TestButton testButton = Component.from(button, TestButton.class);
        Assert.assertEquals(testButton.getParent().get(), testDiv);
        Assert.assertTrue(testDiv.getChildren().anyMatch(c -> c == testButton));
    }

    @Test
    public void wrappedComponentUsesElement() {
        Element div = new Element("div");
        div.setAttribute("id", "foo");
        Assert.assertEquals(Optional.of("foo"), div.as(TestDiv.class).getId());

    }

    @Test
    public void wrappedComponentModifyElement() {
        Element div = new Element("div");
        div.as(TestDiv.class).setId("foo");
        Assert.assertEquals("foo", div.getAttribute("id"));
    }

    @Test
    public void wrapToExistingComponent() {
        TestButton button = new TestButton();
        TestButton button2 = button.getElement().as(TestButton.class);
        button.setId("id1");
        Assert.assertEquals(Optional.of("id1"), button2.getId());
        Assert.assertEquals(Optional.of("id1"), button.getId());
    }

    @Test
    public void wrapDifferentTypeToExistingComponent() {
        TestButton button = new TestButton();
        TestOtherButton button2 = button.getElement().as(TestOtherButton.class);
        button.setId("id1");
        Assert.assertEquals(Optional.of("id1"), button2.getId());
        Assert.assertEquals(Optional.of("id1"), button.getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapToExistingComponent() {
        TestButton button = new TestButton();
        Component.from(button.getElement(), TestButton.class);
    }

    @Test
    public void mapToComponentWhichCreatesComponentInConstructor() {
        Element e = new Element("div");
        TestComponentWhichCreatesComponentInConstructor c = Component.from(e,
                TestComponentWhichCreatesComponentInConstructor.class);
        Element buttonElement = c.getElement().getChild(0);

        Assert.assertEquals(e, c.getElement());
        Assert.assertNotEquals(e, buttonElement);
        Assert.assertEquals("button", buttonElement.getTag());
    }

    @Test
    public void mapToComponentWhichHasComponentField() {
        Element e = new Element("div");
        TestComponentWhichHasComponentField c = Component.from(e,
                TestComponentWhichHasComponentField.class);
        Element buttonElement = c.getElement().getChild(0);

        Assert.assertEquals(e, c.getElement());
        Assert.assertNotEquals(e, buttonElement);
        Assert.assertEquals("button", buttonElement.getTag());
    }

}
