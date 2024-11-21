/*
 * Copyright 2000-2024 Vaadin Ltd.
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
import com.vaadin.flow.component.internal.ComponentTracker.Location;

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

        assertCreateLocation(c1, 70, getClass().getName());
        assertCreateLocation(c2, 72, getClass().getName());
    }

    @Test
    public void attachLocationTracked() {
        Component1 c1 = new Component1();
        Component c2 = new Component1();
        Component c3 = new Component1();

        Layout layout = new Layout(c1);

        assertCreateLocation(c1, 80, getClass().getName());

        layout.add(c2);

        assertAttachLocation(c2, 88, getClass().getName());

        // Last attach is tracked
        layout.add(c3);
        layout.remove(c3);
        layout.add(c3);

        assertAttachLocation(c3, 95, getClass().getName());
    }

    @Test
    public void offsetApplied() {
        Component1 c1 = new Component1();
        Component c2 = new Component1();
        Component c3 = new Component1();

        assertCreateLocation(c1, 102, getClass().getName());

        ComponentTracker.refreshLocation(ComponentTracker.findCreate(c1), 3);

        assertCreateLocation(c2, 103 + 3, getClass().getName());

        ComponentTracker.refreshLocation(ComponentTracker.findCreate(c2), 1);

        assertCreateLocation(c3, 104 + 3 + 1, getClass().getName());
    }

    @Test
    public void memoryIsReleased() throws Exception {
        Field createLocationField = ComponentTracker.class
                .getDeclaredField("createLocation");
        Field createLocationsField = ComponentTracker.class
                .getDeclaredField("createLocations");
        Field attachLocationField = ComponentTracker.class
                .getDeclaredField("attachLocation");
        Field attachLocationsField = ComponentTracker.class
                .getDeclaredField("attachLocations");
        createLocationField.setAccessible(true);
        createLocationsField.setAccessible(true);
        attachLocationField.setAccessible(true);
        attachLocationsField.setAccessible(true);

        Map<?, ?> createMap = (Map<?, ?>) createLocationField.get(null);
        Map<?, ?> attachMap = (Map<?, ?>) attachLocationField.get(null);
        Map<?, ?> createLocationsMap = (Map<?, ?>) createLocationsField
                .get(null);
        Map<?, ?> attachLocationsMap = (Map<?, ?>) attachLocationsField
                .get(null);
        createMap.clear();
        createLocationsMap.clear();
        attachMap.clear();
        attachLocationsMap.clear();

        new Layout(new Component1());

        Assert.assertEquals(2, createMap.size());
        Assert.assertEquals(2, createLocationsMap.size());
        Assert.assertEquals(1, attachMap.size());
        Assert.assertEquals(1, attachLocationsMap.size());

        Assert.assertTrue(isCleared(createMap));
        Assert.assertTrue(isCleared(createLocationsMap));
        Assert.assertTrue(isCleared(attachMap));
        Assert.assertTrue(isCleared(attachLocationsMap));
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

        Location[] locations = ComponentTracker.findCreateLocations(c);
        Assert.assertEquals(lineNumber, locations[1].lineNumber());
        Assert.assertEquals(name, locations[1].className());
    }

    private void assertAttachLocation(Component c, int lineNumber,
            String name) {
        ComponentTracker.Location location = ComponentTracker.findAttach(c);
        Assert.assertEquals(lineNumber, location.lineNumber());
        Assert.assertEquals(name, location.className());

        Location[] locations = ComponentTracker.findAttachLocations(c);
        Assert.assertEquals(lineNumber, locations[0].lineNumber());
        Assert.assertEquals(name, locations[0].className());
    }

}
