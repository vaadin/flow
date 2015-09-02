package com.vaadin.hummingbird.component;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.kernel.TemplateBuilder;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Template;

public class ComponentTest {
    @Test
    public void testBasicComponentAttach() {
        AtomicBoolean attached = new AtomicBoolean(false);

        AbstractComponent testComponent = new AbstractComponent() {
            @Override
            public void elementAttached() {
                boolean wasAttached = attached.getAndSet(true);
                Assert.assertFalse(wasAttached);
                super.elementAttached();
            }

            @Override
            public void elementDetached() {
                boolean wasAttached = attached.getAndSet(false);
                Assert.assertTrue(wasAttached);
                super.elementDetached();
            }
        };

        CssLayout container = new CssLayout();

        container.addComponent(testComponent);

        Assert.assertTrue(attached.get());

        container.removeComponent(testComponent);

        Assert.assertFalse(attached.get());
    }

    @Test
    public void testTemplateComponentAttach() {
        AtomicBoolean attached = new AtomicBoolean(false);

        AbstractComponent testComponent = new Template(
                TemplateBuilder.withTag("div").build()) {
            @Override
            public void elementAttached() {
                boolean wasAttached = attached.getAndSet(true);
                Assert.assertFalse(wasAttached);
                super.elementAttached();
            }

            @Override
            public void elementDetached() {
                boolean wasAttached = attached.getAndSet(false);
                Assert.assertTrue(wasAttached);
                super.elementDetached();
            }
        };

        CssLayout container = new CssLayout();

        container.addComponent(testComponent);

        Assert.assertTrue(attached.get());

        container.removeComponent(testComponent);

        Assert.assertFalse(attached.get());
    }
}
