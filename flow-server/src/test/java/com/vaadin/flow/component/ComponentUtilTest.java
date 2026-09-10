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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.ComponentTest.TestComponent;
import com.vaadin.flow.component.ComponentTest.TestDiv;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.shared.Registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentUtilTest {
    private Component component = new TestDiv();

    @Test
    void setData_byString() {
        assertNull(ComponentUtil.getData(component, "name"),
                "There should initially not be any value");

        ComponentUtil.setData(component, "name", "value");
        assertEquals("value", ComponentUtil.getData(component, "name"),
                "The stored value should be returned");

        ComponentUtil.setData(component, "name", "value2");
        assertEquals("value2", ComponentUtil.getData(component, "name"),
                "The replaced value should be returned");

        ComponentUtil.setData(component, "name", null);
        assertNull(ComponentUtil.getData(component, "name"),
                "The value should be removed");
        assertNull(component.attributes,
                "Storage should be cleared after removing the last attribute");
    }

    @Test
    void setData_byClass() {
        Integer instance1 = new Integer(1);
        Integer instance2 = new Integer(2);

        assertNull(ComponentUtil.getData(component, Integer.class),
                "There should initially not be any value");

        ComponentUtil.setData(component, Integer.class, instance1);
        assertSame(instance1, ComponentUtil.getData(component, Integer.class),
                "The stored value should be returned");

        assertNull(ComponentUtil.getData(component, Number.class),
                "Attribute should not be available based on super type");

        ComponentUtil.setData(component, Integer.class, instance2);
        assertSame(instance2, ComponentUtil.getData(component, Integer.class),
                "The replaced value should be returned");

        ComponentUtil.setData(component, Integer.class, null);
        assertNull(ComponentUtil.getData(component, Integer.class),
                "The value should be removed");
        assertNull(component.attributes,
                "Storage should be cleared after removing the last attribute");
    }

    @Test
    void addListenerToComponent_hasListener_returnsTrue() {
        assertFalse(ComponentUtil.hasEventListener(component, PollEvent.class));

        Registration listener = ComponentUtil.addListener(component,
                PollEvent.class, event -> {
                });
        assertTrue(ComponentUtil.hasEventListener(component, PollEvent.class));

        listener.remove();
        assertFalse(ComponentUtil.hasEventListener(component, PollEvent.class));
    }

    @Test
    void addListenerToComponent_getListeners_returnsCollection() {
        assertFalse(ComponentUtil.hasEventListener(component, PollEvent.class));

        Registration listener = ComponentUtil.addListener(component,
                PollEvent.class, event -> {
                });
        Collection<?> listeners = ComponentUtil.getListeners(component,
                PollEvent.class);
        assertEquals(1, listeners.size());

        listener.remove();
        assertTrue(ComponentUtil.getListeners(component, PollEvent.class)
                .isEmpty());
    }

    @Test
    void registerComponentClass_and_getComponentsByTag_shouldReturnCorrectComponent() {
        Class<? extends Component> testComponentClass = TestDiv.class;
        String testTag = "test-div";

        ComponentUtil.registerComponentClass(testTag, testComponentClass);

        Set<Class<? extends Component>> retrievedClasses = ComponentUtil
                .getComponentsByTag(testTag);

        assertTrue(retrievedClasses.contains(testComponentClass),
                "The retrieved classes should contain the registered component class");

        ComponentUtil.getComponentsByTag(testTag).clear();
    }

    @Test
    void getComponentsByTag_withUnregisteredTag_shouldReturnEmptySet() {
        String unregisteredTag = "unregistered-tag";

        Set<Class<? extends Component>> retrievedClasses = ComponentUtil
                .getComponentsByTag(unregisteredTag);

        assertTrue(retrievedClasses.isEmpty(),
                "The retrieved classes should be empty for an unregistered tag");
    }

    @Test
    void getChildrenOfType_returnsMatchingChildrenInOrder() {
        // parent
        // ├── div1 (TestDiv)
        // ├── span (TestComponent)
        // └── div2 (TestDiv)
        TestComponent parent = new TestComponent(ElementFactory.createDiv());
        TestDiv div1 = new TestDiv();
        TestComponent span = new TestComponent(ElementFactory.createSpan());
        TestDiv div2 = new TestDiv();
        parent.getElement().appendChild(div1.getElement(), span.getElement(),
                div2.getElement());

        assertEquals(List.of(div1, div2),
                ComponentUtil.getChildrenOfType(parent, TestDiv.class).toList(),
                "Only the children of the given type must be returned, in child order");
        assertEquals(Optional.of(div1),
                ComponentUtil.getFirstChildOfType(parent, TestDiv.class));
    }

    @Test
    void getFirstChildOfType_withNoMatchingChild_returnsEmpty() {
        TestComponent parent = new TestComponent(ElementFactory.createDiv());
        parent.getElement().appendChild(
                new TestComponent(ElementFactory.createSpan()).getElement());

        assertEquals(Optional.empty(),
                ComponentUtil.getFirstChildOfType(parent, TestDiv.class));
    }

    @Test
    void getAllChildren_includesVirtualChildren() {
        // parent
        // ├── regular (direct DOM child)
        // └── virtual (appendVirtualChild)
        TestComponent parent = new TestComponent(ElementFactory.createDiv());
        TestComponent regular = new TestComponent(ElementFactory.createSpan());
        TestComponent virtual = new TestComponent(ElementFactory.createDiv());

        parent.getElement().appendChild(regular.getElement());
        parent.getElement().appendVirtualChild(virtual.getElement());

        assertEquals(List.of(regular), parent.getChildren().toList(),
                "getChildren must keep ignoring virtual children");
        assertEquals(List.of(regular, virtual),
                ComponentUtil.getAllChildren(parent).toList(),
                "getAllChildren must return regular children first, virtual children last");
    }

    @Test
    void getAllChildren_doesNotFailOnTextNodeChild() {
        // parent
        // └── (plain element, no component)
        // └── text node (no VirtualChildrenList feature)
        TestComponent parent = new TestComponent(ElementFactory.createDiv());
        Element wrapper = ElementFactory.createDiv();
        wrapper.appendChild(Element.createText("hello"));
        parent.getElement().appendChild(wrapper);

        assertTrue(ComponentUtil.getAllChildren(parent).toList().isEmpty(),
                "Walking past a text node must not throw");
    }

    @Test
    void getAllChildren_skipsNonComponentWrapperElement() {
        // parent
        // └── (plain element, no component)
        // └── virtual (appendVirtualChild on the wrapper)
        TestComponent parent = new TestComponent(ElementFactory.createDiv());
        Element wrapper = ElementFactory.createDiv();
        TestComponent virtual = new TestComponent(ElementFactory.createSpan());

        parent.getElement().appendChild(wrapper);
        wrapper.appendVirtualChild(virtual.getElement());

        assertEquals(List.of(virtual),
                ComponentUtil.getAllChildren(parent).toList(),
                "Walker must descend through plain elements and find virtual children on them");
    }

    @Test
    void streamDescendants_preOrderIncludingVirtual() {
        // parent
        // ├── child1
        // │ ├── grandchild1 (regular)
        // │ └── grandchild2 (virtual)
        // └── child2 (virtual)
        TestComponent parent = new TestComponent(ElementFactory.createDiv());
        TestComponent child1 = new TestComponent(ElementFactory.createDiv());
        TestComponent grandchild1 = new TestComponent(
                ElementFactory.createSpan());
        TestComponent grandchild2 = new TestComponent(
                ElementFactory.createSpan());
        TestComponent child2 = new TestComponent(ElementFactory.createDiv());

        parent.getElement().appendChild(child1.getElement());
        parent.getElement().appendVirtualChild(child2.getElement());
        child1.getElement().appendChild(grandchild1.getElement());
        child1.getElement().appendVirtualChild(grandchild2.getElement());

        assertEquals(List.of(child1, grandchild1, grandchild2, child2),
                ComponentUtil.streamDescendants(parent).toList(),
                "streamDescendants must walk pre-order and include virtual children");
    }

    @Test
    void getAllChildren_compositeReturnsContent() {
        TestComponent content = new TestComponent(ElementFactory.createDiv());
        Composite<TestComponent> composite = new Composite<TestComponent>() {
            @Override
            protected TestComponent initContent() {
                return content;
            }
        };

        assertEquals(List.of(content),
                ComponentUtil.getAllChildren(composite).toList(),
                "getAllChildren on a Composite must return its content");
    }

    @Test
    void streamDescendants_recursesIntoCompositeContent() {
        TestComponent grandchild = new TestComponent(
                ElementFactory.createSpan());
        TestComponent contentWithChild = new TestComponent(
                ElementFactory.createDiv());
        contentWithChild.getElement().appendChild(grandchild.getElement());

        Composite<TestComponent> composite = new Composite<TestComponent>() {
            @Override
            protected TestComponent initContent() {
                return contentWithChild;
            }
        };

        assertEquals(List.of(contentWithChild, grandchild),
                ComponentUtil.streamDescendants(composite).toList(),
                "streamDescendants must recurse through Composite content");
    }

    @Test
    void resolveOrGenerateIdLater_existingId_valueSetImmediately() {
        UI ui = new UI();
        TestDiv source = new TestDiv();
        TestDiv target = new TestDiv();
        target.setId("the-target");
        ui.add(source, target);

        Element sourceElement = source.getElement();
        ComponentUtil.resolveOrGenerateIdLater(sourceElement, target, "prefix-",
                () -> Optional
                        .ofNullable(sourceElement.getAttribute("data-target")),
                id -> sourceElement.setAttribute("data-target", id));

        assertEquals("the-target", sourceElement.getAttribute("data-target"),
                "The value should be available before the resolution runs");
    }

    @Test
    void resolveOrGenerateIdLater_explicitValueSetLater_resolutionSuperseded() {
        UI ui = new UI();
        TestDiv source = new TestDiv();
        TestDiv target = new TestDiv();
        ui.add(source, target);

        Element sourceElement = source.getElement();
        ComponentUtil.resolveOrGenerateIdLater(sourceElement, target, "prefix-",
                () -> Optional
                        .ofNullable(sourceElement.getAttribute("data-target")),
                id -> sourceElement.setAttribute("data-target", id));
        sourceElement.setAttribute("data-target", "explicit");

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertEquals("explicit", sourceElement.getAttribute("data-target"));
        assertFalse(target.getId().isPresent(),
                "No id should be generated for a superseded target");
    }

    @Test
    void resolveOrGenerateIdLater_calledTwice_firstResolutionSuperseded() {
        UI ui = new UI();
        TestDiv source = new TestDiv();
        TestDiv firstTarget = new TestDiv();
        TestDiv secondTarget = new TestDiv();
        ui.add(source, firstTarget, secondTarget);

        Element sourceElement = source.getElement();
        SerializableSupplier<Optional<String>> valueGetter = () -> Optional
                .ofNullable(sourceElement.getAttribute("data-target"));
        SerializableConsumer<String> valueSetter = id -> sourceElement
                .setAttribute("data-target", id);
        ComponentUtil.resolveOrGenerateIdLater(sourceElement, firstTarget,
                "prefix-", valueGetter, valueSetter);
        ComponentUtil.resolveOrGenerateIdLater(sourceElement, secondTarget,
                "prefix-", valueGetter, valueSetter);

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertFalse(firstTarget.getId().isPresent(),
                "No id should be generated for a superseded target");
        assertEquals(secondTarget.getId().orElse(null),
                sourceElement.getAttribute("data-target"));
    }

    @Test
    void resolveOrGenerateIdLater_attached_pendingResolutionIsSerializable()
            throws Exception {
        UI ui = new UI();
        TestDiv source = new TestDiv();
        TestDiv target = new TestDiv();
        target.setId("the-target");
        ui.add(source, target);

        Element sourceElement = source.getElement();
        ComponentUtil.resolveOrGenerateIdLater(sourceElement, target, "prefix-",
                () -> Optional
                        .ofNullable(sourceElement.getAttribute("data-target")),
                id -> sourceElement.setAttribute("data-target", id));

        UI uiCopy = serializeAndDeserialize(ui);
        uiCopy.getInternals().getStateTree()
                .runExecutionsBeforeClientResponse();

        assertEquals("the-target", uiCopy.getChildren().findFirst()
                .orElseThrow().getElement().getAttribute("data-target"));
    }

    @Test
    void resolveOrGenerateIdLater_detached_pendingResolutionIsSerializable()
            throws Exception {
        TestDiv source = new TestDiv();
        TestDiv target = new TestDiv();
        target.setId("the-target");

        // not attached yet, so the resolution is kept as an attach listener
        Element sourceElement = source.getElement();
        ComponentUtil.resolveOrGenerateIdLater(sourceElement, target, "prefix-",
                () -> Optional
                        .ofNullable(sourceElement.getAttribute("data-target")),
                id -> sourceElement.setAttribute("data-target", id));

        TestDiv sourceCopy = serializeAndDeserialize(source);
        UI ui = new UI();
        ui.add(sourceCopy);
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        assertEquals("the-target",
                sourceCopy.getElement().getAttribute("data-target"));
    }

    @SuppressWarnings("unchecked")
    private static <T> T serializeAndDeserialize(T instance) throws Exception {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bs)) {
            out.writeObject(instance);
        }
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bs.toByteArray()))) {
            return (T) in.readObject();
        }
    }

}
