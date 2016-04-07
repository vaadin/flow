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

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;

public class ComponentTest {

    private Component divWithTextComponent;
    private Component parentDivComponent;
    private Component child1SpanComponent;
    private Component child2InputComponent;

    private static class TestComponent extends Component {

        public TestComponent() {
            this(ElementFactory.createDiv());
        }

        public TestComponent(Element element) {
            super(element);
        }

        @Override
        public String toString() {
            return getElement().getTextContent();
        }

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
        Assert.assertEquals("div", divWithTextComponent.getElement().getTag());
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

    private static void assertChildren(Component parent,
            Component... expectedChildren) {
        List<Component> children = parent.getChildren()
                .collect(Collectors.toList());
        Assert.assertArrayEquals(expectedChildren, children.toArray());

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
}
