package com.vaadin.hummingbird.component;

import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Composite;
import com.vaadin.ui.VerticalLayout;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CompositeInsideCompositeTest {
    VerticalLayout layout;
    Button compositionRoot;
    Composite compositeOuter;
    Composite compositeInner;

    public static class AttachDetachTracker {
        public int elementAttachCalls;
        public int elementDetachCalls;
        public int attachCalls;
        public int detachCalls;

    }

    public static class TestButton extends Button {

        public TestButton(String string) {
            super(string);
        }

        public AttachDetachTracker tracker = new AttachDetachTracker();

        @Override
        public void elementAttached() {
            tracker.elementAttachCalls++;
            super.elementAttached();
        }

        @Override
        public void elementDetached() {
            tracker.elementDetachCalls++;
            super.elementDetached();
        }

        @Override
        public void attach() {
            tracker.attachCalls++;
            super.attach();
        }

        @Override
        public void detach() {
            tracker.detachCalls++;
            super.detach();
        }
    }

    public static class TestVerticalLayout extends VerticalLayout {

        public AttachDetachTracker tracker = new AttachDetachTracker();

        @Override
        public void elementAttached() {
            tracker.elementAttachCalls++;
            super.elementAttached();
        }

        @Override
        public void elementDetached() {
            tracker.elementDetachCalls++;
            super.elementDetached();
        }

        @Override
        public void attach() {
            tracker.attachCalls++;
            super.attach();
        }

        @Override
        public void detach() {
            tracker.detachCalls++;
            super.detach();
        }
    }

    public static class TestComposite extends Composite {
        private Component root;

        public TestComposite(Component root) {
            this.root = root;
        }

        @Override
        protected Component initContent() {
            return root;
        }

        public AttachDetachTracker tracker = new AttachDetachTracker();

        @Override
        public void elementAttached() {
            tracker.elementAttachCalls++;
            super.elementAttached();
        }

        @Override
        public void elementDetached() {
            tracker.elementDetachCalls++;
            super.elementDetached();
        }

        @Override
        public void attach() {
            tracker.attachCalls++;
            super.attach();
        }

        @Override
        public void detach() {
            tracker.detachCalls++;
            super.detach();
        }

    }

    @Before
    public void setup() {
        layout = new TestVerticalLayout();
        compositionRoot = new TestButton("Inside composite");
        compositeInner = new TestComposite(compositionRoot);
        compositeOuter = new TestComposite(compositeInner);
        layout.addComponent(compositeOuter);
    }

    @Test
    public void compositeOuterElement() {
        Assert.assertEquals(compositionRoot.getElement(),
                compositeOuter.getElement());
    }

    @Test
    public void compositeInnerElement() {
        Assert.assertEquals(compositionRoot.getElement(),
                compositeInner.getElement());
    }

    @Test
    public void compositeOuterParentElement() {
        Assert.assertEquals(layout.getElement(),
                compositeOuter.getElement().getParent());
    }

    @Test
    public void compositeInnerParentElement() {
        Assert.assertEquals(layout.getElement(),
                compositeInner.getElement().getParent());
    }

    @Test
    public void layoutChildElements() {
        CompositeTest.assertElementChildren(layout.getElement(),
                compositionRoot.getElement());
    }

    @Test
    public void compositeOuterParentComponent() {
        Assert.assertEquals(layout, compositeOuter.getParent());
    }

    @Test
    public void compositeInnerParentComponent() {
        Assert.assertEquals(compositeOuter, compositeInner.getParent());
    }

    @Test
    public void compositionRootParentComponent() {
        Assert.assertEquals(compositeInner, compositionRoot.getParent());
    }

    @Test
    public void layoutChildComponent() {
        CompositeTest.assertComponentChildren(layout, compositeOuter);
    }

    @Test
    public void compositeOuterChildComponent() {
        CompositeTest.assertComponentChildren(compositeOuter, compositeInner);
    }

    @Test
    public void compositeInnerChildComponent() {
        CompositeTest.assertComponentChildren(compositeInner, compositionRoot);
    }

}
