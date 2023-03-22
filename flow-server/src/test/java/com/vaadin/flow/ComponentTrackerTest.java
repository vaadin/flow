/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.internal.ComponentTracker;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

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

    private Object previousProdMode;
    private Field prodModeField;

    @Before
    public void setup() throws Exception {
        prodModeField = ComponentTracker.class
                .getDeclaredField("productionMode");
        prodModeField.setAccessible(true);
        previousProdMode = prodModeField.get(null);
        prodModeField.set(null, false);
    }

    @After
    public void teardown() throws Exception {
        prodModeField.set(null, previousProdMode);
    }

    @Test
    public void createLocationTracked() {
        Component1 c1 = new Component1();
        Component c2;
        c2 = new Component1();

        ComponentTracker.Location c1Location = ComponentTracker.findCreate(c1);
        Assert.assertEquals(69, c1Location.lineNumber());
        Assert.assertEquals(getClass().getName(), c1Location.className());

        ComponentTracker.Location c2Location = ComponentTracker.findCreate(c2);
        Assert.assertEquals(71, c2Location.lineNumber());
        Assert.assertEquals(getClass().getName(), c2Location.className());
    }

    @Test
    public void attachLocationTracked() {
        Component1 c1 = new Component1();
        Component c2 = new Component1();
        Component c3 = new Component1();

        Layout layout = new Layout(c1);

        ComponentTracker.Location c1Location = ComponentTracker.findAttach(c1);
        Assert.assertEquals(88, c1Location.lineNumber());
        Assert.assertEquals(getClass().getName(), c1Location.className());

        layout.add(c2);

        ComponentTracker.Location c2Location = ComponentTracker.findAttach(c2);
        Assert.assertEquals(94, c2Location.lineNumber());
        Assert.assertEquals(getClass().getName(), c2Location.className());

        // Last attach is tracked
        layout.add(c3);
        layout.remove(c3);
        layout.add(c3);

        ComponentTracker.Location c3Location = ComponentTracker.findAttach(c3);
        Assert.assertEquals(103, c3Location.lineNumber());
        Assert.assertEquals(getClass().getName(), c3Location.className());
    }

    @Test
    public void offsetApplied() {
        Component1 c1 = new Component1();
        Component c2 = new Component1();
        Component c3 = new Component1();

        ComponentTracker.Location c1Location = ComponentTracker.findCreate(c1);
        Assert.assertEquals(112, c1Location.lineNumber());
        Assert.assertEquals(getClass().getName(), c1Location.className());

        ComponentTracker.refreshLocation(c1Location, 3);

        ComponentTracker.Location c2Location = ComponentTracker.findCreate(c2);
        Assert.assertEquals(113 + 3, c2Location.lineNumber());
        Assert.assertEquals(getClass().getName(), c2Location.className());

        ComponentTracker.refreshLocation(c2Location, 1);

        ComponentTracker.Location c3Location = ComponentTracker.findCreate(c3);
        Assert.assertEquals(114 + 3 + 1, c3Location.lineNumber());
        Assert.assertEquals(getClass().getName(), c3Location.className());
    }

    @Test
    public void memoryIsReleased() throws Exception {
        Field createLocationField = ComponentTracker.class
                .getDeclaredField("createLocation");
        Field attachLocationField = ComponentTracker.class
                .getDeclaredField("attachLocation");
        createLocationField.setAccessible(true);
        attachLocationField.setAccessible(true);
        Map<Component, StackTraceElement> createMap = (Map<Component, StackTraceElement>) createLocationField
                .get(null);
        Map<Component, StackTraceElement> attachMap = (Map<Component, StackTraceElement>) attachLocationField
                .get(null);
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
            if (map.size() == 0) {
                return true;
            }
            Thread.sleep(1);
        }
        return false;
    }

}
