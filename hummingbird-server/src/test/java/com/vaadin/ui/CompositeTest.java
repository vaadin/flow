package com.vaadin.ui;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.ui.ComponentTest.TestComponent;
import com.vaadin.ui.ComponentTest.TracksAttachDetach;
import com.vaadin.ui.CompositeNestedTest.TestLayout;

public class CompositeTest {

    TestLayout layoutWithSingleComponentComposite;
    CompositeWithComponent compositeWithComponent;
    TestLayout layoutInsideComposite;
    TestComponent componentInsideLayoutInsideComposite;

    protected TestComponent createTestComponent() {
        return new TestComponent(
                ElementFactory.createDiv("Component in composite"));

    }

    protected TestLayout createTestLayout() {
        return new TestLayout() {
            @Override
            public String toString() {
                return "layoutInsideComposite";
            }
        };
    }

    public class CompositeWithComponent extends Composite
            implements TracksAttachDetach {

        private AtomicInteger attachEvents = new AtomicInteger();
        private AtomicInteger detachEvents = new AtomicInteger();

        @Override
        protected Component initContent() {
            layoutInsideComposite = createTestLayout();
            componentInsideLayoutInsideComposite = createTestComponent();
            layoutInsideComposite
                    .addComponent(componentInsideLayoutInsideComposite);
            return layoutInsideComposite;
        }

        @Override
        public AtomicInteger getAttachEvents() {
            return attachEvents;
        }

        @Override
        public AtomicInteger getDetachEvents() {
            return detachEvents;
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

        componentInsideLayoutInsideComposite.track();
        compositeWithComponent.track();
        layoutInsideComposite.track();
        layoutWithSingleComponentComposite.track();
    }

    @Test
    public void getElement_compositeAndCompositeComponent() {
        Assert.assertEquals(layoutInsideComposite.getElement(),
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
                layoutInsideComposite.getElement());
    }

    @Test
    public void getParent_compositeInLayout() {
        Assert.assertEquals(layoutWithSingleComponentComposite,
                compositeWithComponent.getParent().get());
    }

    @Test
    public void getParent_componentInComposite() {
        Assert.assertEquals(compositeWithComponent,
                layoutInsideComposite.getParent().get());
    }

    @Test
    public void getParent_componentInLayoutInComposite() {
        Assert.assertEquals(layoutInsideComposite,
                componentInsideLayoutInsideComposite.getParent().get());
    }

    @Test
    public void getChildren_layoutWithComposite() {
        ComponentTest.assertChildren(layoutWithSingleComponentComposite,
                compositeWithComponent);
    }

    @Test
    public void getChildren_compositeWithComponent() {
        ComponentTest.assertChildren(compositeWithComponent,
                layoutInsideComposite);
    }

    @Test
    public void getChildren_layoutInComposite() {
        ComponentTest.assertChildren(layoutInsideComposite,
                componentInsideLayoutInsideComposite);
    }

    @Test
    public void attachDetachEvents_compositeHierarchy() {
        UI ui = new UI();

        layoutInsideComposite.assertAttachEvents(0);
        layoutWithSingleComponentComposite.assertAttachEvents(0);
        compositeWithComponent.assertAttachEvents(0);
        componentInsideLayoutInsideComposite.assertAttachEvents(0);

        ui.add(layoutWithSingleComponentComposite);

        layoutWithSingleComponentComposite.assertAttachEvents(1);
        layoutInsideComposite.assertAttachEvents(1);
        compositeWithComponent.assertAttachEvents(1);
        componentInsideLayoutInsideComposite.assertAttachEvents(1);

        layoutWithSingleComponentComposite.assertDetachEvents(0);
        layoutInsideComposite.assertDetachEvents(0);
        compositeWithComponent.assertDetachEvents(0);
        componentInsideLayoutInsideComposite.assertDetachEvents(0);

        ui.remove(layoutWithSingleComponentComposite);

        layoutWithSingleComponentComposite.assertDetachEvents(1);
        layoutInsideComposite.assertDetachEvents(1);
        compositeWithComponent.assertDetachEvents(1);
        componentInsideLayoutInsideComposite.assertDetachEvents(1);
    }

    public static void assertElementChildren(Element parent,
            Element... expected) {
        Assert.assertEquals(expected.length, parent.getChildCount());
        for (int i = 0; i < parent.getChildCount(); i++) {
            Assert.assertEquals(expected[i], parent.getChild(i));
        }
    }
}
