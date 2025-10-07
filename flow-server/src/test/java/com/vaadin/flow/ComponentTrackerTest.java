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
package com.vaadin.flow;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentTest;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.ComponentTracker;

/**
 * Note that this is intentionally in the "wrong" package as internal packages
 * are excluded in the tracker.
 *
 * Note that if you reformat this file, the tests will need to be adjusted as
 * they track line numbers.
 */
public class ComponentTrackerTest {

    @Tag("component1")
    public static class Component1 extends Component {
    }

    @Tag("layout")
    public static class Layout extends Component implements HasComponents {
        public Layout(Component... components) {
            add(components);
        }
    }

    private Object previousDisabled;
    private Field disabledField;

    @Before
    public void setup() throws Exception {
        disabledField = ComponentTracker.class.getDeclaredField("disabled");
        disabledField.setAccessible(true);
        previousDisabled = disabledField.get(null);
        disabledField.set(null, false);
    }

    @After
    public void teardown() throws Exception {
        disabledField.set(null, previousDisabled);
    }

    @Test
    public void createLocationTracked() {
        Component1 c1 = new Component1();
        Component c2;
        c2 = new Component1();
        int c1Line = 71;

        assertCreateLocation(c1, c1Line, getClass().getName());
        assertCreateLocation(c2, c1Line + 2, getClass().getName());
    }

    @Test
    public void attachLocationTracked() {
        Component1 c1 = new Component1();
        Component c2 = new Component1();
        Component c3 = new Component1();

        Layout layout = new Layout(c1);

        int c1Line = 82;

        assertCreateLocation(c1, c1Line, getClass().getName());

        layout.add(c2);

        assertAttachLocation(c2, c1Line + 10, getClass().getName());

        // Last attach is tracked
        layout.add(c3);
        layout.remove(c3);
        layout.add(c3);

        assertAttachLocation(c3, c1Line + 17, getClass().getName());
    }

    @Test
    public void memoryIsReleased() throws Exception {
        Field createLocationField = ComponentTracker.class
                .getDeclaredField("createLocation");
        Field attachLocationField = ComponentTracker.class
                .getDeclaredField("attachLocation");
        createLocationField.setAccessible(true);
        attachLocationField.setAccessible(true);

        Map<?, ?> createMap = (Map<?, ?>) createLocationField.get(null);
        Map<?, ?> attachMap = (Map<?, ?>) attachLocationField.get(null);
        createMap.clear();
        attachMap.clear();

        new Layout(new Component1());

        Assert.assertEquals(2, createMap.size());
        Assert.assertEquals(1, attachMap.size());

        Assert.assertTrue(isCleared(createMap));
        Assert.assertTrue(isCleared(attachMap));
    }

    private boolean isCleared(Map<?, ?> map) throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            System.gc();
            if (map.isEmpty()) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }

    private void assertCreateLocation(Component c, int lineNumber,
            String name) {
        ComponentTracker.Location location = ComponentTracker.findCreate(c);
        Assert.assertEquals(lineNumber, location.lineNumber());
        Assert.assertEquals(name, location.className());
    }

    private void assertAttachLocation(Component c, int lineNumber,
            String name) {
        ComponentTracker.Location location = ComponentTracker.findAttach(c);
        Assert.assertEquals(lineNumber, location.lineNumber());
        Assert.assertEquals(name, location.className());
    }

    @Test
    public void cannotMoveComponentsToOtherUI() {
        // tests https://github.com/vaadin/flow/issues/22282
        final UI firstUI = new UI();
        final UI secondUI = new UI();
        final ComponentTest.TestButton button = new ComponentTest.TestButton();
        firstUI.add(button);

        IllegalStateException ex = Assert.assertThrows(
                IllegalStateException.class, () -> secondUI.add(button));
        Assert.assertTrue(ex.getMessage(), ex.getMessage().startsWith(
                "Can't move a node from one state tree to another. If this is "
                        + "intentional, first remove the node from its current "
                        + "state tree by calling removeFromTree. This usually "
                        + "happens when a component is moved from one UI to another, "
                        + "which is not recommended. This may be caused by "
                        + "assigning components to static members or spring "
                        + "singleton scoped beans and referencing them from "
                        + "multiple UIs. Offending component: "
                        + "com.vaadin.flow.component.ComponentTest$TestButton"));
    }
}
