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

import org.junit.Assert;
import org.junit.Test;

public class HasComponentsTest {

    @Tag("div")
    private static class TestComponent extends Component
            implements HasComponents {

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

}
