package com.vaadin.hummingbird.kernel;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.UI;

public class BeforeClientResponseTest {
    private static class TestComponent extends AbstractComponent {
        public void runBeforeNextClientResponse(Runnable runnable) {
            getElement().runBeforeNextClientResponse(runnable);
        }
    }

    private TestComponent c = new TestComponent();

    private UI ui = new UI() {
        @Override
        protected void init(VaadinRequest request) {
            // Silly method...
        }

        {
            addComponent(c);
        }
    };

    private AtomicInteger count = new AtomicInteger();

    private int commitAndGetCount() {
        ui.getRootNode().commit();
        return count.get();
    }

    @Test
    public void testBasicBeforeClientResponse() {
        c.runBeforeNextClientResponse(count::incrementAndGet);

        Assert.assertEquals("Callback should not be run yet", 0, count.get());

        Assert.assertEquals("Callback should run after committing", 1,
                commitAndGetCount());

        c.getElement().addClass("markAsDirty");

        Assert.assertEquals("Callback should only run once", 1,
                commitAndGetCount());
    }

    @Test
    public void testMultipleBeforeClientResponse() {
        c.runBeforeNextClientResponse(count::incrementAndGet);
        // Make sure we have different runnables
        c.runBeforeNextClientResponse(() -> count.set(count.get() + 2));

        Assert.assertEquals("Both callbacks should be run", 0 + 1 + 2,
                commitAndGetCount());
    }

    @Test
    public void testSameBeforeClientResponseMultipleTimes() {
        Runnable r = count::incrementAndGet;
        // Add the same callback twice
        c.runBeforeNextClientResponse(r);
        c.runBeforeNextClientResponse(r);

        Assert.assertEquals("Callback should only be run once", 1,
                commitAndGetCount());
    }

    @Test
    public void testMultipleBeforeClientResponseOrder() {
        List<Integer> result = new ArrayList<>();
        c.runBeforeNextClientResponse(() -> {
            result.add(1);
        });
        c.runBeforeNextClientResponse(() -> {
            result.add(2);
        });
        c.runBeforeNextClientResponse(() -> {
            result.add(3);
        });

        ui.getRootNode().commit();
        Assert.assertArrayEquals(new Integer[] { 1, 2, 3 }, result.toArray());
    }
}
