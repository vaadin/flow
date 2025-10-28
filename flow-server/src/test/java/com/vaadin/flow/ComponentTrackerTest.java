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
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.flow.component.internal.ComponentTracker.Location;

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
    public void offsetApplied() {
        Component1 c1 = new Component1();
        Component c2 = new Component1();
        Component c3 = new Component1();

        int c1Line = 106;
        assertCreateLocation(c1, c1Line, getClass().getName());

        ComponentTracker.refreshLocation(ComponentTracker.findCreate(c1), 3);

        assertCreateLocation(c2, c1Line + 1 + 3, getClass().getName());

        ComponentTracker.refreshLocation(ComponentTracker.findCreate(c2), 1);

        assertCreateLocation(c3, c1Line + 2 + 3 + 1, getClass().getName());
    }

    @Test
    public void memoryIsReleased() throws Exception {
        Field createThrowableField = ComponentTracker.class
                .getDeclaredField("createThrowable");
        Field attachThrowableField = ComponentTracker.class
                .getDeclaredField("attachThrowable");
        createThrowableField.setAccessible(true);
        attachThrowableField.setAccessible(true);

        Map<?, ?> createMap = (Map<?, ?>) createThrowableField.get(null);
        Map<?, ?> attachMap = (Map<?, ?>) attachThrowableField.get(null);
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

        Location locationFromArray = getLocationFromArray(
                ComponentTracker.findCreateLocations(c));
        Assert.assertEquals(lineNumber, locationFromArray.lineNumber());
        Assert.assertEquals(name, locationFromArray.className());
    }

    private void assertAttachLocation(Component c, int lineNumber,
            String name) {
        ComponentTracker.Location location = ComponentTracker.findAttach(c);
        Assert.assertEquals(lineNumber, location.lineNumber());
        Assert.assertEquals(name, location.className());

        Location locationFromArray = getLocationFromArray(
                ComponentTracker.findAttachLocations(c));

        Assert.assertEquals(lineNumber, locationFromArray.lineNumber());
        Assert.assertEquals(name, locationFromArray.className());
    }

    private Location getLocationFromArray(Location[] locations) {
        return Stream.of(locations).filter(
                l -> l.className().equals(ComponentTrackerTest.class.getName()))
                .findFirst().orElseThrow();

    }
}
