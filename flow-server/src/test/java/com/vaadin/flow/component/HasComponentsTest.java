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
