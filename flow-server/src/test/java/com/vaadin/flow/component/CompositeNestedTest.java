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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.ComponentTest.TestComponent;
import com.vaadin.flow.component.ComponentTest.TracksAttachDetach;
import com.vaadin.flow.dom.ElementFactory;

public class CompositeNestedTest {
    TestLayout layout;
    TestComponent componentInComposite;
    Composite<?> compositeOuter;
    Composite<?> compositeInner;

    public static class TestComposite extends Composite<Component>
            implements TracksAttachDetach {

        private Component content;

        private AtomicInteger attachEvents = new AtomicInteger();
        private AtomicInteger detachEvents = new AtomicInteger();

        public TestComposite(Component content) {
            this.content = content;
        }

        @Override
        protected Component initContent() {
            return content;
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

    public static class TestLayout extends TestComponent {

        public TestLayout() {
            super(ElementFactory.createDiv());
        }

        public void addComponent(Component... components) {
            for (Component component : components) {
                getElement().appendChild(component.getElement());
            }
        }

    }

    @Before
    public void setup() {
        layout = new TestLayout();
        componentInComposite = new TestComponent(
                ElementFactory.createDiv("Inside composite"));
        compositeInner = new TestComposite(componentInComposite) {
            @Override
            public String toString() {
                return "compositeInner";
            }
        };
        compositeOuter = new TestComposite(compositeInner) {
            @Override
            public String toString() {
                return "compositeOuter";
            }
        };
        layout.addComponent(compositeOuter);
    }

    @Test
    public void compositeOuterElement() {
        Assert.assertEquals(componentInComposite.getElement(),
                compositeOuter.getElement());
    }

    @Test
    public void compositeInnerElement() {
        Assert.assertEquals(componentInComposite.getElement(),
                compositeInner.getElement());
    }

    @Test
    public void getParentElement_compositeOuter() {
        Assert.assertEquals(layout.getElement(),
                compositeOuter.getElement().getParent());
    }

    @Test
    public void getParentElement_compositeInner() {
        Assert.assertEquals(layout.getElement(),
                compositeInner.getElement().getParent());
    }

    @Test
    public void layoutChildElements() {
        CompositeTest.assertElementChildren(layout.getElement(),
                componentInComposite.getElement());
    }

    @Test
    public void getParent_compositeOuter() {
        Assert.assertEquals(layout, compositeOuter.getParent().get());
    }

    @Test
    public void getParent_compositeInner() {
        Assert.assertEquals(compositeOuter, compositeInner.getParent().get());
    }

    @Test
    public void getParent_componentInComposite() {
        Assert.assertEquals(compositeInner,
                componentInComposite.getParent().get());
    }

    @Test
    public void getChildren_layout() {
        ComponentTest.assertChildren(layout, compositeOuter);
    }

    @Test
    public void getChildren_compositeOuter() {
        ComponentTest.assertChildren(compositeOuter, compositeInner);
    }

    @Test
    public void getChildren_compositeInner() {
        ComponentTest.assertChildren(compositeInner, componentInComposite);
    }

}
