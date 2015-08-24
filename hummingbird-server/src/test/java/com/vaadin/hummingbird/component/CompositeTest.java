package com.vaadin.hummingbird.component;

import java.util.Iterator;

import com.vaadin.hummingbird.component.CompositeInsideCompositeTest.TestButton;
import com.vaadin.hummingbird.component.CompositeInsideCompositeTest.TestComposite;
import com.vaadin.hummingbird.component.CompositeInsideCompositeTest.TestVerticalLayout;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.tests.util.MockUI;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.UI;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CompositeTest {
    TestVerticalLayout layout;
    TestButton compositionRoot;
    TestComposite composite;

    @Before
    public void setup() {
        layout = new TestVerticalLayout();
        compositionRoot = new TestButton("Inside composite");
        composite = new TestComposite(compositionRoot);
        layout.addComponent(composite);
    }

    @Test
    public void compositeElement() {
        Assert.assertEquals(compositionRoot.getElement(),
                composite.getElement());
    }

    @Test
    public void compositeParentElement() {
        Assert.assertEquals(layout.getElement(),
                composite.getElement().getParent());
    }

    @Test
    public void parentChildElements() {
        assertElementChildren(layout.getElement(),
                compositionRoot.getElement());
    }

    @Test
    public void compositeParentComponent() {
        Assert.assertEquals(layout, composite.getParent());
    }

    @Test
    public void compositionRootParentComponent() {
        Assert.assertEquals(composite, compositionRoot.getParent());
    }

    @Test
    public void layoutChildComponent() {
        assertComponentChildren(layout, composite);
    }

    @Test
    public void compositeChildComponent() {
        assertComponentChildren(composite, compositionRoot);
    }

    public static void assertComponentChildren(HasComponents parent,
            Component... expected) {
        Iterator<Component> i = parent.iterator();

        int expectedIndex = 0;
        while (i.hasNext()) {
            Assert.assertEquals(expected[expectedIndex++], i.next());
        }
        Assert.assertEquals("Parent contains fewer components than expected",
                expected.length, expectedIndex);
    }

    public static void assertElementChildren(Element parent,
            Element... expected) {
        Assert.assertEquals(expected.length, parent.getChildCount());
        for (int i = 0; i < parent.getChildCount(); i++) {
            Assert.assertEquals(expected[i], parent.getChild(i));
        }
    }

    @Test
    public void attachForComposite() {
        UI ui = new MockUI();
        ui.setContent(layout);
        Assert.assertEquals(1, layout.tracker.elementAttachCalls);
        Assert.assertEquals(1, layout.tracker.attachCalls);
        Assert.assertEquals(1, composite.tracker.elementAttachCalls);
        Assert.assertEquals(1, composite.tracker.attachCalls);
        Assert.assertEquals(1, compositionRoot.tracker.elementAttachCalls);
        Assert.assertEquals(1, compositionRoot.tracker.attachCalls);

        Assert.assertEquals(0, layout.tracker.elementDetachCalls);
        Assert.assertEquals(0, layout.tracker.detachCalls);
        Assert.assertEquals(0, composite.tracker.elementDetachCalls);
        Assert.assertEquals(0, composite.tracker.detachCalls);
        Assert.assertEquals(0, compositionRoot.tracker.elementDetachCalls);
        Assert.assertEquals(0, compositionRoot.tracker.detachCalls);

    }
}
