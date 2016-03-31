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
    private Component parentDiv;
    private Component childSpan;
    private Component childInput;

    private static class TestComponent implements Component {

        private Element element;

        public TestComponent(Element element) {
            this.element = element;
            this.element.attachComponent(this);
        }

        @Override
        public Element getElement() {
            return element;
        }

    }

    @Before
    public void setup() {
        divWithTextComponent = new TestComponent(
                ElementFactory.createDiv("Test component"));
        parentDiv = new TestComponent(ElementFactory.createDiv());
        childSpan = new TestComponent(ElementFactory.createSpan("Span"));
        childInput = new TestComponent(ElementFactory.createInput());
        parentDiv.getElement().appendChild(childSpan.getElement(),
                childInput.getElement());
    }

    @Test
    public void getElement() {
        Assert.assertEquals("div", divWithTextComponent.getElement().getTag());
        Assert.assertEquals("Test component",
                divWithTextComponent.getElement().getTextContent());
    }

    @Test
    public void getParentForAttachedComponent() {
        Assert.assertEquals(parentDiv, childSpan.getParent().get());
        Assert.assertEquals(parentDiv, childInput.getParent().get());
    }

    @Test
    public void getParentForDetachedComponent() {
        Assert.assertFalse(parentDiv.getParent().isPresent());
    }

    @Test
    public void defaultGetChildrenDirectlyAttached() {
        List<Component> children = parentDiv.getChildren()
                .collect(Collectors.toList());
        Assert.assertArrayEquals(new Component[] { childSpan, childInput },
                children.toArray());
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
        List<Component> children = parentDiv.getChildren()
                .collect(Collectors.toList());
        Assert.assertArrayEquals(new Component[] { childSpan, childInput },
                children.toArray());

    }
}
