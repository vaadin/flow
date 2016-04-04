package com.vaadin.ui;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.ui.ComponentTest.TestComponent;
import com.vaadin.ui.CompositeNestedTest.TestLayout;

public class CompositeTest {

    TestLayout layoutWithSingleComponentComposite;
    CompositeWithComponent compositeWithComponent;
    Component componentInsideComposite;

    protected Component createTestComponent() {
        return new TestComponent(
                ElementFactory.createDiv("Component in composite"));

    }

    public class CompositeWithComponent extends Composite {

        @Override
        protected Component initContent() {
            componentInsideComposite = createTestComponent();
            return componentInsideComposite;
        }
    }

    @Before
    public void setup() {
        compositeWithComponent = new CompositeWithComponent() {
            @Override
            public String toString() {
                return "Composite";
            }
        };

        layoutWithSingleComponentComposite = new TestLayout() {
            @Override
            public String toString() {
                return "Layout";
            }
        };
        layoutWithSingleComponentComposite.addComponent(compositeWithComponent);
    }

    @Test
    public void getElement_compositeAndCompositeComponent() {
        Assert.assertEquals(componentInsideComposite.getElement(),
                compositeWithComponent.getElement());
    }

    @Test
    public void getParentElement_compositeInLayout() {
        Assert.assertEquals(layoutWithSingleComponentComposite.getElement(),
                compositeWithComponent.getElement().getParent());
    }

    @Test
    public void getElementChildren_layoutWithComponentInComposite() {
        assertElementChildren(layoutWithSingleComponentComposite.getElement(),
                componentInsideComposite.getElement());
    }

    @Test
    public void getParent_compositeInLayout() {
        Assert.assertEquals(layoutWithSingleComponentComposite,
                compositeWithComponent.getParent().get());
    }

    @Test
    public void getParent_componentInComposite() {
        Assert.assertEquals(compositeWithComponent,
                componentInsideComposite.getParent().get());
    }

    @Test
    public void getChildren_layoutWithComposite() {
        ComponentTest.assertChildren(layoutWithSingleComponentComposite,
                compositeWithComponent);
    }

    @Test
    public void getChildren_compositeWithComponent() {
        ComponentTest.assertChildren(compositeWithComponent,
                componentInsideComposite);
    }

    public static void assertElementChildren(Element parent,
            Element... expected) {
        Assert.assertEquals(expected.length, parent.getChildCount());
        for (int i = 0; i < parent.getChildCount(); i++) {
            Assert.assertEquals(expected[i], parent.getChild(i));
        }
    }

    // @Test
    // public void attachForComposite() {
    // UI ui = new MockUI();
    // ui.setContent(layout);
    // Assert.assertEquals(1, layout.tracker.elementAttachCalls);
    // Assert.assertEquals(1, layout.tracker.attachCalls);
    // Assert.assertEquals(1, composite.tracker.elementAttachCalls);
    // Assert.assertEquals(1, composite.tracker.attachCalls);
    // Assert.assertEquals(1,
    // componentInsideComposite.tracker.elementAttachCalls);
    // Assert.assertEquals(1, componentInsideComposite.tracker.attachCalls);
    //
    // Assert.assertEquals(0, layout.tracker.elementDetachCalls);
    // Assert.assertEquals(0, layout.tracker.detachCalls);
    // Assert.assertEquals(0, composite.tracker.elementDetachCalls);
    // Assert.assertEquals(0, composite.tracker.detachCalls);
    // Assert.assertEquals(0,
    // componentInsideComposite.tracker.elementDetachCalls);
    // Assert.assertEquals(0, componentInsideComposite.tracker.detachCalls);
    //
    // }
}