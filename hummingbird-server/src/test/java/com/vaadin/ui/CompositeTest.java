package com.vaadin.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.hummingbird.event.ComponentEventListener;
import com.vaadin.tests.util.TestUtil;
import com.vaadin.ui.ComponentTest.TestComponent;
import com.vaadin.ui.ComponentTest.TracksAttachDetach;
import com.vaadin.ui.CompositeNestedTest.TestLayout;

public class CompositeTest {

    // layoutWithSingleComponentComposite (TestLayout)
    // - compositeWithComponent (CompositeWithComponent)
    // --- layoutInsideComposite (TestLayout) content for compositeWithComponent
    // ---- componentInsideLayoutInsideComposite (TestComponent)

    TestLayout layoutWithSingleComponentComposite;
    CompositeWithComponent compositeWithComponent;
    TestLayout layoutInsideComposite;
    Component componentInsideLayoutInsideComposite;

    protected Component createTestComponent() {
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

    public class CompositeWithComponent extends Composite<Component>
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

    public class CompositeWithGenericType extends Composite<TestComponent> {
        // That's all
    }

    public class CompositeWithComposite
            extends Composite<CompositeWithVariableType<TestComponent>> {
        // That's all
    }

    public class CompositeWithVariableType<C extends Component>
            extends Composite<C> {
        // That's all
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

        ((TracksAttachDetach) componentInsideLayoutInsideComposite).track();
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
    public void automaticCompositeContentType() {
        CompositeWithGenericType instance = new CompositeWithGenericType();

        Assert.assertEquals(TestComponent.class,
                instance.getContent().getClass());
    }

    @Test(expected = IllegalStateException.class)
    public void compositeContentTypeWithVariableTypeParameter() {
        CompositeWithVariableType<TestComponent> composite = new CompositeWithVariableType<>();
        composite.getContent();
    }

    @Test(expected = IllegalStateException.class)
    public void genericCompositeContentType() {
        CompositeWithComposite composite = new CompositeWithComposite();
        composite.getContent();
    }

    // layoutWithSingleComponentComposite (TestLayout)
    // - compositeWithComponent (CompositeWithComponent)
    // --- layoutInsideComposite (TestLayout) content for compositeWithComponent
    // ---- componentInsideLayoutInsideComposite (TestComponent)
    @Test
    public void attachDetachEvents_compositeHierarchy_correctOrder() {
        UI ui = new UI();

        ArrayList<Component> attached = new ArrayList<Component>();
        ArrayList<Component> detached = new ArrayList<Component>();

        ComponentEventListener<AttachEvent> attachListener = event -> attached
                .add(event.getSource());
        ComponentEventListener<DetachEvent> detachListener = event -> detached
                .add(event.getSource());

        layoutInsideComposite.addAttachListener(attachListener);
        layoutWithSingleComponentComposite.addAttachListener(attachListener);
        compositeWithComponent.addAttachListener(attachListener);
        componentInsideLayoutInsideComposite.addAttachListener(attachListener);

        layoutInsideComposite.addDetachListener(detachListener);
        layoutWithSingleComponentComposite.addDetachListener(detachListener);
        compositeWithComponent.addDetachListener(detachListener);
        componentInsideLayoutInsideComposite.addDetachListener(detachListener);

        layoutInsideComposite.assertAttachEvents(0);
        layoutWithSingleComponentComposite.assertAttachEvents(0);
        compositeWithComponent.assertAttachEvents(0);
        ((TracksAttachDetach) componentInsideLayoutInsideComposite)
                .assertAttachEvents(0);

        ui.add(layoutWithSingleComponentComposite);

        layoutInsideComposite.assertAttachEvents(1);
        layoutWithSingleComponentComposite.assertAttachEvents(1);
        compositeWithComponent.assertAttachEvents(1);
        ((TracksAttachDetach) componentInsideLayoutInsideComposite)
                .assertAttachEvents(1);

        TestUtil.assertArrays(attached.toArray(),
                new Component[] { componentInsideLayoutInsideComposite,
                        layoutInsideComposite, compositeWithComponent,
                        layoutWithSingleComponentComposite });

        layoutInsideComposite.assertDetachEvents(0);
        layoutWithSingleComponentComposite.assertDetachEvents(0);
        compositeWithComponent.assertDetachEvents(0);
        ((TracksAttachDetach) componentInsideLayoutInsideComposite)
                .assertDetachEvents(0);

        ui.removeAll();

        layoutInsideComposite.assertDetachEvents(1);
        layoutWithSingleComponentComposite.assertDetachEvents(1);
        compositeWithComponent.assertDetachEvents(1);
        ((TracksAttachDetach) componentInsideLayoutInsideComposite)
                .assertDetachEvents(1);

        TestUtil.assertArrays(detached.toArray(),
                new Component[] { componentInsideLayoutInsideComposite,
                        layoutInsideComposite, compositeWithComponent,
                        layoutWithSingleComponentComposite });
    }

    @Test
    public void testOnAttachOnDetachAndEventsOrder() {
        List<Integer> triggered = new ArrayList<>();

        Component component = new Component(new Element("div")) {
            @Override
            protected void onAttach() {
                triggered.add(1);
            }

            @Override
            protected void onDetach() {
                triggered.add(-1);
            }
        };
        component.addAttachListener(event -> triggered.add(2));
        component.addDetachListener(event -> triggered.add(-2));

        Composite compositeInsideComposite = new Composite() {
            @Override
            protected Component initContent() {
                return component;
            };

            @Override
            protected void onAttach() {
                triggered.add(3);
            }

            @Override
            protected void onDetach() {
                triggered.add(-3);
            }
        };
        compositeInsideComposite.addAttachListener(event -> triggered.add(4));
        compositeInsideComposite.addDetachListener(event -> triggered.add(-4));

        Composite composite = new Composite() {
            @Override
            protected Component initContent() {
                return compositeInsideComposite;
            }

            @Override
            protected void onAttach() {
                triggered.add(5);
            }

            @Override
            protected void onDetach() {
                triggered.add(-5);
            }
        };
        composite.addAttachListener(event -> triggered.add(6));
        composite.addDetachListener(event -> triggered.add(-6));

        UI ui = new UI();
        ui.add(composite);

        TestUtil.assertArrays(triggered.toArray(),
                new Integer[] { 1, 2, 3, 4, 5, 6 });
        triggered.clear();

        ui.remove(composite);

        TestUtil.assertArrays(triggered.toArray(),
                new Integer[] { -1, -2, -3, -4, -5, -6 });

        TestLayout container = createTestLayout();
        ui.add(container, composite);

        triggered.clear();

        container.addComponent(composite);

        TestUtil.assertArrays(triggered.toArray(),
                new Integer[] { -1, -2, -3, -4, -5, -6, 1, 2, 3, 4, 5, 6 });
    }

    public static void assertElementChildren(Element parent,
            Element... expected) {
        Assert.assertEquals(expected.length, parent.getChildCount());
        for (int i = 0; i < parent.getChildCount(); i++) {
            Assert.assertEquals(expected[i], parent.getChild(i));
        }
    }
}
