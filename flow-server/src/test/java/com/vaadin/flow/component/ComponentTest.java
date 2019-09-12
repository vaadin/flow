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
package com.vaadin.flow.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.internal.DependencyList;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.internal.nodefeature.SynchronizedPropertiesList;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.tests.util.MockUI;
import com.vaadin.tests.util.TestUtil;

import elemental.json.Json;

@NotThreadSafe
public class ComponentTest {

    @After
    public void checkThreadLocal() {
        Assert.assertNull(Component.elementToMapTo.get());
    }

    @com.vaadin.flow.component.DomEvent("foo")
    public static class TestDomEvent extends ComponentEvent<Component> {

        public TestDomEvent(Component source, boolean fromClient) {
            super(source, fromClient);
        }

    }

    @com.vaadin.flow.component.DomEvent(value = "foo", allowUpdates = DisabledUpdateMode.ALWAYS)
    public static class EnabledDomEvent extends ComponentEvent<Component> {
        public EnabledDomEvent(Component source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    @Tag("div")
    public static class TestDiv extends Component {

        @Synchronize(value = "bar", property = "baz", allowUpdates = DisabledUpdateMode.ALWAYS)
        public String getFoo() {
            return null;
        }

        @Synchronize(value = "foo", property = "bar")
        public String getBaz() {
            return null;
        }
    }

    @Tag("div")
    public static class EnabledDiv extends Component implements HasComponents {
    }

    @Tag("div")
    public static class TestComponentWhichHasComponentField extends Component {
        private TestButton button = new TestButton();

        public TestComponentWhichHasComponentField() {
            getElement().appendChild(button.getElement());
        }
    }

    public static class TestComponentWhichUsesElementConstructor
            extends Component {
        public TestComponentWhichUsesElementConstructor() {
            super(new Element("my-element"));
        }
    }

    public static class TestComponentWhichUsesNullElementConstructor
            extends Component {
        public TestComponentWhichUsesNullElementConstructor() {
            super(null);
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
    private Component shadowRootParent;
    private Component shadowChild;
    private UI testUI;
    private MockServletServiceSessionSetup mocks;

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
            return getElement().getText();
        }

        @Override
        public void fireEvent(ComponentEvent<?> componentEvent) {
            super.fireEvent(componentEvent);
        }

        @Override
        public <T extends ComponentEvent<?>> Registration addListener(
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
    public void setup() throws Exception {
        divWithTextComponent = new TestComponent(
                ElementFactory.createDiv("Test component"));
        parentDivComponent = new TestComponent(ElementFactory.createDiv());
        child1SpanComponent = new TestComponent(
                ElementFactory.createSpan("Span"));
        child2InputComponent = new TestComponent(ElementFactory.createInput());
        parentDivComponent.getElement().appendChild(
                child1SpanComponent.getElement(),
                child2InputComponent.getElement());

        mocks = new MockServletServiceSessionSetup();

        VaadinSession session = mocks.getSession();
        UI ui = new UI() {
            @Override
            public VaadinSession getSession() {
                return session;
            }
        };
        ui.getInternals().setSession(session);

        UI.setCurrent(ui);
    }

    @After
    public void tearDown() {
        UI.setCurrent(null);
        mocks.cleanup();
    }

    @Test
    public void getElement() {
        Assert.assertEquals(Tag.DIV,
                divWithTextComponent.getElement().getTag());
        Assert.assertEquals("Test component",
                divWithTextComponent.getElement().getTextRecursively());
    }

    @Test
    public void getParentForAttachedComponent() {
        Assert.assertEquals(parentDivComponent,
                child1SpanComponent.getParent().get());
        Assert.assertEquals(parentDivComponent,
                child2InputComponent.getParent().get());
    }

    @Test
    public void getUIForAttachedComponentInShadowRoot() {
        shadowRootParent = new TestComponent(ElementFactory.createDiv());
        shadowRootParent.getElement().attachShadow();
        shadowChild = new TestComponent(ElementFactory.createSpan());
        shadowRootParent.getElement().getShadowRoot().get()
                .appendChild(shadowChild.getElement());

        testUI = new UI();
        testUI.add(shadowRootParent);

        Assert.assertEquals(testUI, shadowChild.getUI().get());
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
        Assert.assertEquals(c, element.getComponent().get());
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
        Registration attachRegistrationHandle = grandChild.addAttachListener(
                event -> grandChild.getAttachEvents().incrementAndGet());
        Registration detachRegistrationHandle = grandChild.addDetachListener(
                event -> grandChild.getDetachEvents().incrementAndGet());

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
        ui.getInternals()
                .setSession(new VaadinSession(new MockVaadinServletService()));
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

    @Test(expected = IllegalArgumentException.class)
    public void mapToNullComponentType() {
        Component.from(new Element("div"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mapFromNullElement() {
        Component.from(null, TestButton.class);
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

    @Test
    public void mapToComponentWithElementConstructor() {
        Element e = new Element("my-element");
        TestComponentWhichUsesElementConstructor c = Component.from(e,
                TestComponentWhichUsesElementConstructor.class);

        Assert.assertSame(e, c.getElement());
        Assert.assertSame(c, e.getComponent().get());
    }

    @Test
    public void mapToComponentWithNullElementConstructor() {
        Element e = new Element("div");
        TestComponentWhichUsesNullElementConstructor c = Component.from(e,
                TestComponentWhichUsesNullElementConstructor.class);

        Assert.assertSame(e, c.getElement());
        Assert.assertSame(c, e.getComponent().get());
    }

    @Tag("div")
    public static class SynchronizePropertyOnChangeComponent extends Component {
        @Synchronize("change")
        public String getFoo() {
            return "";
        }
    }

    public static class SynchronizePropertyUsingElementConstructor
            extends Component {
        public SynchronizePropertyUsingElementConstructor() {
            super(null);
        }

        @Synchronize("change")
        public String getFoo() {
            return "";
        }

        public void customInit() {
            setElement(this, new Element("Span"));

        }
    }

    @Tag("div")
    public static class SynchronizePropertyOnChangeGivenPropertyComponent
            extends Component {
        @Synchronize(value = "change", property = "bar")
        public String getFoo() {
            return "";
        }
    }

    @Tag("div")
    public static class SynchronizePropertyOnMultipleEventsComponent
            extends Component {
        @Synchronize(value = { "input", "blur" })
        public String getFoo() {
            return "";
        }
    }

    @Tag("div")
    public static class SynchronizeOnNonGetterComponent extends Component {
        @Synchronize("change")
        public String doWork() {
            return "";
        }
    }

    private void assertSynchronizedProperties(Element element,
            String... properties) {
        Set<String> expected = Stream.of(properties)
                .collect(Collectors.toSet());
        Set<String> actual = element.getSynchronizedProperties()
                .collect(Collectors.toSet());
        Assert.assertEquals(expected, actual);

    }

    private void assertSynchronizedPropertiesEvents(Element element,
            String... events) {
        Set<String> expected = Stream.of(events).collect(Collectors.toSet());
        Set<String> actual = element.getSynchronizedPropertyEvents()
                .collect(Collectors.toSet());
        Assert.assertEquals(expected, actual);

    }

    @Test
    public void synchronizePropertyBasedOnGetterName() {
        SynchronizePropertyOnChangeComponent component = new SynchronizePropertyOnChangeComponent();
        Element element = component.getElement();
        assertSynchronizedProperties(element, "foo");
        assertSynchronizedPropertiesEvents(element, "change");
    }

    @Test
    public void synchronizePropertyElementConstructor() {
        SynchronizePropertyUsingElementConstructor component = new SynchronizePropertyUsingElementConstructor();
        component.customInit();
        Element element = component.getElement();
        assertSynchronizedProperties(element, "foo");
        assertSynchronizedPropertiesEvents(element, "change");
    }

    @Test
    public void componentMetaDataCached() {
        ComponentUtil.componentMetaDataCache.clear();
        Assert.assertFalse(
                ComponentUtil.componentMetaDataCache.contains(Text.class));
        new Text("foobar");
        Assert.assertTrue(
                ComponentUtil.componentMetaDataCache.contains(Text.class));
    }

    @Test
    public void synchronizePropertyWithPropertyName() {
        SynchronizePropertyOnChangeGivenPropertyComponent component = new SynchronizePropertyOnChangeGivenPropertyComponent();
        Element element = component.getElement();
        assertSynchronizedProperties(element, "bar");
        assertSynchronizedPropertiesEvents(element, "change");
    }

    @Test
    public void synchronizePropertyWithMultipleEvents() {
        SynchronizePropertyOnMultipleEventsComponent component = new SynchronizePropertyOnMultipleEventsComponent();
        Element element = component.getElement();
        assertSynchronizedProperties(element, "foo");
        assertSynchronizedPropertiesEvents(element, "blur", "input");
    }

    @Test
    public void synchronizePropertyOverride() {
        SynchronizePropertyOnChangeComponent component = new SynchronizePropertyOnChangeComponent();
        component.getElement().removeSynchronizedProperty("foo");
        component.getElement().removeSynchronizedPropertyEvent("change");
        Element element = component.getElement();
        assertSynchronizedProperties(element);
        assertSynchronizedPropertiesEvents(element);
    }

    @Test(expected = IllegalStateException.class)
    public void synchronizeOnNonGetter() {
        new SynchronizeOnNonGetterComponent();
    }

    @Tag("div")
    @HtmlImport("html.html")
    @JavaScript("js.js")
    @StyleSheet("css.css")
    public static class ComponentWithDependencies extends Component {

    }

    @Tag("span")
    @Uses(ComponentWithDependencies.class)
    @JavaScript("uses.js")
    public static class UsesComponentWithDependencies extends Component {

    }

    @Tag("span")
    @Uses(UsesComponentWithDependencies.class)
    @HtmlImport("usesuses.html")
    public static class UsesUsesComponentWithDependencies extends Component {

    }

    @Tag("div")
    @JavaScript("dep1.js")
    @Uses(CircularDependencies2.class)
    public static class CircularDependencies1 extends Component {

    }

    @Tag("div")
    @JavaScript("dep2.js")
    @Uses(CircularDependencies1.class)
    public static class CircularDependencies2 extends Component {

    }

    @Test
    public void usesComponent() {
        UI ui = UI.getCurrent();
        mocks.getDeploymentConfiguration().setCompatibilityMode(true);

        ui.getInternals()
                .addComponentDependencies(UsesComponentWithDependencies.class);

        Map<String, Dependency> pendingDependencies = getDependenciesMap(
                ui.getInternals().getDependencyList().getPendingSendToClient());
        Assert.assertEquals(4, pendingDependencies.size());

        assertDependency(Dependency.Type.HTML_IMPORT, "html.html",
                pendingDependencies);
        assertDependency(Dependency.Type.JAVASCRIPT, "uses.js",
                pendingDependencies);
        assertDependency(Dependency.Type.JAVASCRIPT, "js.js",
                pendingDependencies);
        assertDependency(Dependency.Type.STYLESHEET, "css.css",
                pendingDependencies);
    }

    @Test
    public void usesChain() {
        UIInternals internals = UI.getCurrent().getInternals();
        mocks.getDeploymentConfiguration().setCompatibilityMode(true);

        internals.addComponentDependencies(
                UsesUsesComponentWithDependencies.class);

        Map<String, Dependency> pendingDependencies = getDependenciesMap(
                internals.getDependencyList().getPendingSendToClient());
        Assert.assertEquals(5, pendingDependencies.size());

        assertDependency(Dependency.Type.HTML_IMPORT, "usesuses.html",
                pendingDependencies);
        assertDependency(Dependency.Type.HTML_IMPORT, "html.html",
                pendingDependencies);
        assertDependency(Dependency.Type.JAVASCRIPT, "uses.js",
                pendingDependencies);
        assertDependency(Dependency.Type.JAVASCRIPT, "js.js",
                pendingDependencies);
        assertDependency(Dependency.Type.STYLESHEET, "css.css",
                pendingDependencies);
    }

    @Test
    public void circularDependencies() {
        UIInternals internals = new MockUI().getInternals();
        DependencyList dependencyList = internals.getDependencyList();
        mocks.getDeploymentConfiguration().setCompatibilityMode(true);

        internals.addComponentDependencies(CircularDependencies1.class);
        Map<String, Dependency> pendingDependencies = getDependenciesMap(
                dependencyList.getPendingSendToClient());
        Assert.assertEquals(2, pendingDependencies.size());

        assertDependency(Dependency.Type.JAVASCRIPT, "dep1.js",
                pendingDependencies);
        assertDependency(Dependency.Type.JAVASCRIPT, "dep2.js",
                pendingDependencies);

        internals = new MockUI().getInternals();
        dependencyList = internals.getDependencyList();
        internals.addComponentDependencies(CircularDependencies2.class);
        pendingDependencies = getDependenciesMap(
                dependencyList.getPendingSendToClient());
        Assert.assertEquals(2, pendingDependencies.size());
        assertDependency(Dependency.Type.JAVASCRIPT, "dep2.js",
                pendingDependencies);
        assertDependency(Dependency.Type.JAVASCRIPT, "dep1.js",
                pendingDependencies);

    }

    @Test
    public void inNpmModeNoJsDependenciesAreAdded() {
        mocks.getDeploymentConfiguration().setCompatibilityMode(false);
        UIInternals internals = new MockUI().getInternals();
        DependencyList dependencyList = internals.getDependencyList();

        internals.addComponentDependencies(CircularDependencies1.class);

        Assert.assertTrue(dependencyList.getPendingSendToClient().isEmpty());
    }

    @Test
    public void declarativeSyncProperties_propertiesAreRegisteredWithProperDisabledUpdateMode() {
        TestDiv div = new TestDiv();
        SynchronizedPropertiesList list = div.getElement().getNode()
                .getFeature(SynchronizedPropertiesList.class);

        Set<String> props = list.getSynchronizedProperties();

        Assert.assertTrue(props.contains("bar"));
        Assert.assertTrue(props.contains("baz"));
        Assert.assertEquals(2, props.size());

        Assert.assertEquals(DisabledUpdateMode.ONLY_WHEN_ENABLED,
                list.getDisabledUpdateMode("bar"));
        Assert.assertEquals(DisabledUpdateMode.ALWAYS,
                list.getDisabledUpdateMode("baz"));
    }

    @Test
    public void enabledComponent_fireDomEvent_listenerReceivesEvent() {
        TestDiv div = new TestDiv();

        AtomicInteger count = new AtomicInteger();
        div.addListener(TestDomEvent.class, vent -> count.incrementAndGet());
        div.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(createEvent("foo", div));

        Assert.assertEquals(1, count.get());
    }

    @Test
    public void disabledComponent_fireDomEvent_listenerDoesntReceivesEvent() {
        TestDiv div = new TestDiv();
        div.getElement().setEnabled(false);

        AtomicInteger count = new AtomicInteger();
        div.addListener(TestDomEvent.class, vent -> count.incrementAndGet());
        div.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(createEvent("foo", div));

        Assert.assertEquals(0, count.get());
    }

    @Test
    public void implicityDisabledComponent_fireDomEvent_listenerDoesntReceivesEvent() {
        TestDiv div = new TestDiv();

        UI ui = new UI();
        ui.add(div);
        ui.setEnabled(false);

        AtomicInteger count = new AtomicInteger();
        div.addListener(TestDomEvent.class, event -> count.incrementAndGet());
        div.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(createEvent("foo", div));

        Assert.assertEquals(0, count.get());
    }

    @Test
    public void disabledComponent_fireAlwaysEnabledDomEvent_listenerReceivesEvent() {
        TestDiv div = new TestDiv();

        div.getElement().setEnabled(false);

        AtomicInteger count = new AtomicInteger();
        div.addListener(EnabledDomEvent.class,
                event -> count.incrementAndGet());
        div.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(createEvent("foo", div));

        Assert.assertEquals(1, count.get());
    }

    private DomEvent createEvent(String type, Component component) {
        return new DomEvent(component.getElement(), type, Json.createObject());
    }

    private void assertDependency(Dependency.Type type, String url,
            Map<String, Dependency> pendingDependencies) {
        Dependency dependency = pendingDependencies.get("frontend://" + url);
        Assert.assertNotNull(
                "Could not locate a dependency object for url=" + url,
                dependency);
        Assert.assertEquals(type, dependency.getType());
        Assert.assertEquals("frontend://" + url, dependency.getUrl());
    }

    private Map<String, Dependency> getDependenciesMap(
            Collection<Dependency> dependencies) {
        return dependencies.stream().collect(
                Collectors.toMap(Dependency::getUrl, Function.identity()));
    }

    @Test // 3818
    public void enabledStateChangeOnAttachCalledForParentState() {
        enabledStateChangeOnAttachCalledForParentState(
                (parent, child) -> parent.add(child));
    }

    @Test
    public void enabledStateChangeOnAttachCalledForParentOfVirtualChildState() {
        enabledStateChangeOnAttachCalledForParentState((parent, child) -> {
            Element wrapper = new Element("div");
            parent.getElement().appendVirtualChild(wrapper);
            wrapper.appendChild(child.getElement());
        });
    }

    @Test // 3818
    public void enabledStateChangeOnDetachReturnsOldState() {
        UI ui = new UI();

        EnabledDiv parent = new EnabledDiv();
        parent.setEnabled(false);
        ui.add(parent);

        AtomicReference<Boolean> stateChange = new AtomicReference<>();
        EnabledDiv child = new EnabledDiv() {
            @Override
            public void onEnabledStateChanged(boolean enabled) {
                super.onEnabledStateChanged(enabled);

                stateChange.set(enabled);
            }
        };

        Assert.assertFalse("Parent should be disabled", parent.isEnabled());
        Assert.assertTrue("Child should be enabled.", child.isEnabled());
        Assert.assertNull(child.getElement().getAttribute("disabled"));

        parent.add(child);

        Assert.assertFalse("After attach child should be disabled",
                child.isEnabled());
        Assert.assertFalse("Disabled event should have triggered",
                stateChange.get());
        Assert.assertNotNull(child.getElement().getAttribute("disabled"));

        parent.remove(child);

        Assert.assertTrue("After detach child should be enabled",
                child.isEnabled());
        Assert.assertTrue("Enable event should have triggered",
                stateChange.get());
        Assert.assertNull(child.getElement().getAttribute("disabled"));
    }

    @Test
    public void enabledStateChangeOnParentDetachReturnsOldState() {
        enabledStateChangeOnParentDetachReturnsOldState(
                (parent, child) -> parent.add(child));
    }

    @Test
    public void enabledStateChangeOnParentOfVirtualChildDetachReturnsOldState() {
        enabledStateChangeOnParentDetachReturnsOldState((parent, child) -> {
            Element wrapper = new Element("div");
            parent.getElement().appendVirtualChild(wrapper);
            wrapper.appendChild(child.getElement());
        });
    }

    @Test // 3818
    public void enabledStateChangeOnDetachChildKeepsDisabledState() {
        UI ui = new UI();

        EnabledDiv parent = new EnabledDiv();
        parent.setEnabled(false);
        ui.add(parent);

        AtomicReference<Boolean> stateChange = new AtomicReference<>();
        EnabledDiv child = new EnabledDiv() {
            @Override
            public void onEnabledStateChanged(boolean enabled) {
                super.onEnabledStateChanged(enabled);

                stateChange.set(enabled);
            }
        };
        child.setEnabled(false);
        // Clear state change from setEnabled
        stateChange.set(null);

        Assert.assertFalse("Parent should be disabled", parent.isEnabled());
        Assert.assertFalse("Child should be enabled.", child.isEnabled());
        Assert.assertNotNull(child.getElement().getAttribute("disabled"));

        parent.add(child);

        Assert.assertFalse("After attach child should be disabled",
                child.isEnabled());
        Assert.assertNull("No change event should have fired",
                stateChange.get());
        Assert.assertNotNull(child.getElement().getAttribute("disabled"));

        parent.remove(child);

        Assert.assertFalse("After detach child should still be disabled",
                child.isEnabled());
        Assert.assertNull("No change event should have fired",
                stateChange.get());
        Assert.assertNotNull(child.getElement().getAttribute("disabled"));
    }

    @Test // 3818
    public void enabledStateChangeOnAttachAndDetachChildAndGrandChildrenAreNotified() {
        UI ui = new UI();

        EnabledDiv parent = new EnabledDiv();
        parent.setEnabled(false);
        ui.add(parent);

        AtomicReference<Boolean> stateChange = new AtomicReference<>();
        EnabledDiv child = new EnabledDiv() {
            @Override
            public void onEnabledStateChanged(boolean enabled) {
                super.onEnabledStateChanged(enabled);

                stateChange.set(enabled);
            }
        };
        AtomicReference<Boolean> grandStateChange = new AtomicReference<>();
        EnabledDiv grandChild = new EnabledDiv() {
            @Override
            public void onEnabledStateChanged(boolean enabled) {
                super.onEnabledStateChanged(enabled);

                grandStateChange.set(enabled);
            }
        };
        child.add(grandChild);

        Assert.assertFalse("Parent should be disabled", parent.isEnabled());
        Assert.assertTrue("Child should be enabled.", child.isEnabled());
        Assert.assertNull(child.getElement().getAttribute("disabled"));
        Assert.assertTrue("GrandChild should be enabled.",
                grandChild.isEnabled());
        Assert.assertNull(grandChild.getElement().getAttribute("disabled"));

        parent.add(child);

        Assert.assertFalse("After attach child should be disabled",
                child.isEnabled());
        Assert.assertFalse("Disabled event should have triggered",
                stateChange.get());
        Assert.assertNotNull(child.getElement().getAttribute("disabled"));
        Assert.assertFalse("After attach GrandChild should be disabled",
                grandChild.isEnabled());
        Assert.assertFalse("Disabled event should have triggered",
                grandStateChange.get());
        Assert.assertNotNull(grandChild.getElement().getAttribute("disabled"));

        parent.remove(child);

        Assert.assertTrue("After detach child should be enabled",
                child.isEnabled());
        Assert.assertTrue("Enable event should have triggered",
                stateChange.get());
        Assert.assertNull(child.getElement().getAttribute("disabled"));
        Assert.assertTrue("After detach GrandChild should be enabled",
                grandChild.isEnabled());
        Assert.assertTrue("GrandChild should have gotten true event",
                grandStateChange.get());
        Assert.assertNull(grandChild.getElement().getAttribute("disabled"));
    }

    @Test // 3818
    public void enabledStateChangeOnAttachAndDetachDisabledChildAndGrandChildAreDisabled() {
        UI ui = new UI();

        EnabledDiv parent = new EnabledDiv();
        parent.setEnabled(false);
        ui.add(parent);

        AtomicReference<Boolean> stateChange = new AtomicReference<>();
        EnabledDiv child = new EnabledDiv() {
            @Override
            public void onEnabledStateChanged(boolean enabled) {
                super.onEnabledStateChanged(enabled);

                stateChange.set(enabled);
            }
        };
        child.setEnabled(false);
        // Clear state change from setEnabled
        stateChange.set(null);
        AtomicReference<Boolean> grandStateChange = new AtomicReference<>();
        EnabledDiv grandChild = new EnabledDiv() {
            @Override
            public void onEnabledStateChanged(boolean enabled) {
                super.onEnabledStateChanged(enabled);

                grandStateChange.set(enabled);
            }
        };
        child.add(grandChild);

        Assert.assertFalse("Parent should be disabled", parent.isEnabled());
        Assert.assertFalse("Child should be disabled.", child.isEnabled());
        Assert.assertNotNull(child.getElement().getAttribute("disabled"));
        Assert.assertFalse("GrandChild should be disabled.",
                grandChild.isEnabled());
        // note that add doesn't create an attach event as we are not connected
        // to the UI.
        Assert.assertNull(grandChild.getElement().getAttribute("disabled"));

        parent.add(child);

        Assert.assertFalse("After attach child should be disabled",
                child.isEnabled());
        Assert.assertNull("Disabled event should have triggered",
                stateChange.get());
        Assert.assertNotNull(child.getElement().getAttribute("disabled"));
        Assert.assertFalse("After attach GrandChild should be disabled",
                grandChild.isEnabled());
        Assert.assertFalse("Disabled event should have triggered",
                grandStateChange.get());
        Assert.assertNotNull(grandChild.getElement().getAttribute("disabled"));

        parent.remove(child);

        Assert.assertFalse("After detach child should be disabled",
                child.isEnabled());
        Assert.assertNull("No change event should have been sent",
                stateChange.get());
        Assert.assertFalse("After detach GrandChild should be disabled",
                grandChild.isEnabled());
        Assert.assertFalse("Latest state change should have been disabled",
                grandStateChange.get());
    }

    @Test // 3818
    public void enabledStateChangeOnAttachAndDetachDisabledGrandChildAreDisabled() {
        UI ui = new UI();

        EnabledDiv parent = new EnabledDiv();
        parent.setEnabled(false);
        ui.add(parent);

        AtomicReference<Boolean> stateChange = new AtomicReference<>();
        EnabledDiv child = new EnabledDiv() {
            @Override
            public void onEnabledStateChanged(boolean enabled) {
                super.onEnabledStateChanged(enabled);

                stateChange.set(enabled);
            }
        };
        AtomicReference<Boolean> grandStateChange = new AtomicReference<>();
        EnabledDiv grandChild = new EnabledDiv() {
            @Override
            public void onEnabledStateChanged(boolean enabled) {
                super.onEnabledStateChanged(enabled);

                grandStateChange.set(enabled);
            }
        };
        grandChild.setEnabled(false);
        child.add(grandChild);

        Assert.assertFalse("Parent should be disabled", parent.isEnabled());
        Assert.assertTrue("Child should be enabled.", child.isEnabled());
        Assert.assertNull(child.getElement().getAttribute("disabled"));
        Assert.assertFalse("GrandChild should be disabled.",
                grandChild.isEnabled());
        Assert.assertNotNull(grandChild.getElement().getAttribute("disabled"));

        parent.add(child);

        Assert.assertFalse("After attach child should be disabled",
                child.isEnabled());
        Assert.assertFalse("Disabled event should have triggered",
                stateChange.get());
        Assert.assertNotNull(child.getElement().getAttribute("disabled"));
        Assert.assertFalse("After attach GrandChild should be disabled",
                grandChild.isEnabled());
        Assert.assertFalse("Disabled event should have triggered",
                grandStateChange.get());
        Assert.assertNotNull(grandChild.getElement().getAttribute("disabled"));

        parent.remove(child);

        Assert.assertTrue("After detach child should be enabled",
                child.isEnabled());
        Assert.assertTrue("Enable event should have triggered",
                stateChange.get());
        Assert.assertNull(child.getElement().getAttribute("disabled"));
        Assert.assertFalse("After detach GrandChild should be disabled",
                grandChild.isEnabled());
        Assert.assertFalse("Latest state change should have been disabled",
                grandStateChange.get());
        Assert.assertNotNull(grandChild.getElement().getAttribute("disabled"));
    }

    @Test // 3818
    public void enabledPassesThroughAllChildensChildrenAndAttributeShouldBeSet() {
        UI ui = new UI();

        EnabledDiv parent = new EnabledDiv();
        EnabledDiv child = new EnabledDiv();
        EnabledDiv subChild = new EnabledDiv();
        EnabledDiv subSubChild = new EnabledDiv();

        parent.add(child);
        child.add(subChild);
        subChild.add(subSubChild);

        ui.add(parent);

        Assert.assertTrue("Parent should be enabled.", parent.isEnabled());
        Assert.assertTrue("Child should be enabled.", child.isEnabled());
        Assert.assertTrue("SubChild should be enabled.", subChild.isEnabled());
        Assert.assertTrue("SubsubChild should be enabled.",
                subSubChild.isEnabled());

        Assert.assertNull("No disabled attribute should not exist for parent",
                parent.getElement().getAttribute("disabled"));
        Assert.assertNull("No disabled attribute should not exist for child",
                child.getElement().getAttribute("disabled"));
        Assert.assertNull("No disabled attribute should not exist for subChild",
                subChild.getElement().getAttribute("disabled"));
        Assert.assertNull(
                "No disabled attribute should not exist for subSubChild",
                subSubChild.getElement().getAttribute("disabled"));

        parent.setEnabled(false);

        Assert.assertFalse("Parent should be disabled.", parent.isEnabled());
        Assert.assertFalse("Child should be disabled.", child.isEnabled());
        Assert.assertFalse("SubChild should be disabled.",
                subChild.isEnabled());
        Assert.assertFalse("SubsubChild should be disabled.",
                subSubChild.isEnabled());

        Assert.assertNotNull("Disabled attribute should exist for parent",
                parent.getElement().getAttribute("disabled"));
        Assert.assertNotNull("Disabled attribute should exist for child",
                child.getElement().getAttribute("disabled"));
        Assert.assertNotNull("Disabled attribute should exist for subChild",
                subChild.getElement().getAttribute("disabled"));
        Assert.assertNotNull("Disabled attribute should exist for subSubChild",
                subSubChild.getElement().getAttribute("disabled"));

    }

    @Test(expected = IllegalStateException.class)
    public void add_componentIsAttachedToAnotherUI_throwsIllegalStateException() {
        // given
        TestComponent child = new TestComponent();
        UI ui1 = new UI();
        ui1.add(child);
        UI ui2 = new UI();

        // then
        ui2.add(child);
    }

    private void enabledStateChangeOnAttachCalledForParentState(
            BiConsumer<EnabledDiv, Component> modificationStartegy) {
        UI ui = new UI();

        EnabledDiv parent = new EnabledDiv();
        parent.setEnabled(false);
        ui.add(parent);

        AtomicReference<Boolean> stateChange = new AtomicReference<>();
        EnabledDiv child = new EnabledDiv() {
            @Override
            public void onEnabledStateChanged(boolean enabled) {
                super.onEnabledStateChanged(enabled);
                Assert.assertTrue("Expected empty state for enabled change",
                        stateChange.compareAndSet(null, enabled));
            }
        };

        Assert.assertFalse("Parent should be disabled", parent.isEnabled());
        Assert.assertTrue("Child should be enabled.", child.isEnabled());
        Assert.assertNull(child.getElement().getAttribute("disabled"));

        modificationStartegy.accept(parent, child);

        Assert.assertFalse("After attach child should be disabled",
                child.isEnabled());
        Assert.assertFalse("Disabled event should have triggered",
                stateChange.get());
        Assert.assertNotNull(child.getElement().getAttribute("disabled"));
    }

    private void enabledStateChangeOnParentDetachReturnsOldState(
            BiConsumer<EnabledDiv, Component> modificationStartegy) {
        UI ui = new UI();

        EnabledDiv grandParent = new EnabledDiv();
        grandParent.setEnabled(false);
        ui.add(grandParent);

        EnabledDiv parent = new EnabledDiv();

        AtomicReference<Boolean> stateChange = new AtomicReference<>();
        EnabledDiv child = new EnabledDiv() {
            @Override
            public void onEnabledStateChanged(boolean enabled) {
                super.onEnabledStateChanged(enabled);

                stateChange.set(enabled);
            }
        };

        Assert.assertTrue("Parent should be enabled", parent.isEnabled());
        Assert.assertTrue("Child should be enabled.", child.isEnabled());
        Assert.assertNull(child.getElement().getAttribute("disabled"));

        modificationStartegy.accept(parent, child);

        grandParent.add(parent);

        Assert.assertFalse("After attach child should be disabled",
                child.isEnabled());
        Assert.assertFalse("Disabled event should have triggered",
                stateChange.get());
        Assert.assertNotNull(child.getElement().getAttribute("disabled"));

        grandParent.remove(parent);

        Assert.assertTrue("After detach child should be enabled",
                child.isEnabled());
        Assert.assertTrue("Enable event should have triggered",
                stateChange.get());
        Assert.assertNull(child.getElement().getAttribute("disabled"));
    }
}
