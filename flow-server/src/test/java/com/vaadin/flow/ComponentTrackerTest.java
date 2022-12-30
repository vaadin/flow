package com.vaadin.flow;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.tests.util.TestUtil;

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

    @Before
    public void setup() throws Exception {
        Field prodMode = ComponentTracker.class
                .getDeclaredField("productionMode");
        prodMode.setAccessible(true);
        prodMode.set(null, false);
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

        Assert.assertEquals(0, createMap.size());
        Assert.assertEquals(0, attachMap.size());

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

    @Test
    public void createLocationTracked() {
        Component1 c1 = new Component1();
        Component c2;
        c2 = new Component1();

        StackTraceElement c1Location = ComponentTracker.findCreate(c1);
        Assert.assertEquals(44, c1Location.getLineNumber());
        Assert.assertEquals(getClass().getName(), c1Location.getClassName());

        StackTraceElement c2Location = ComponentTracker.findCreate(c2);
        Assert.assertEquals(46, c2Location.getLineNumber());
        Assert.assertEquals(getClass().getName(), c2Location.getClassName());
    }

    @Test
    public void attachLocationTracked() {
        Component1 c1 = new Component1();
        Component c2 = new Component1();
        Component c3 = new Component1();

        Layout layout = new Layout(c1);

        StackTraceElement c1Location = ComponentTracker.findAttach(c1);
        Assert.assertEquals(63, c1Location.getLineNumber());
        Assert.assertEquals(getClass().getName(), c1Location.getClassName());

        layout.add(c2);

        StackTraceElement c2Location = ComponentTracker.findAttach(c2);
        Assert.assertEquals(69, c2Location.getLineNumber());
        Assert.assertEquals(getClass().getName(), c2Location.getClassName());

        // Last attach is tracked
        layout.add(c3);
        layout.remove(c3);
        layout.add(c3);

        StackTraceElement c3Location = ComponentTracker.findAttach(c3);
        Assert.assertEquals(78, c3Location.getLineNumber());
        Assert.assertEquals(getClass().getName(), c3Location.getClassName());
    }

}
