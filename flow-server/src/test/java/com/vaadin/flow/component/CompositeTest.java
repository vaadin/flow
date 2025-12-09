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
package com.vaadin.flow.component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.ComponentTest.TestComponent;
import com.vaadin.flow.component.ComponentTest.TracksAttachDetach;
import com.vaadin.flow.component.CompositeNestedTest.TestLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.tests.util.TestUtil;

import static org.junit.Assert.assertEquals;

@NotThreadSafe
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

        Assert.assertNull(VaadinService.getCurrent());
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        VaadinService.setCurrent(service);
    }

    @After
    public void tearDown() {
        VaadinService.setCurrent(null);
    }

    @Test
    public void getElement_compositeAndCompositeComponent() {
        assertEquals(layoutInsideComposite.getElement(),
                compositeWithComponent.getElement());
    }

    @Test
    public void getParentElement_compositeInLayout() {
        assertEquals(layoutWithSingleComponentComposite.getElement(),
                compositeWithComponent.getElement().getParent());
    }

    @Test
    public void getElementChildren_layoutWithComponentInComposite() {
        assertElementChildren(layoutWithSingleComponentComposite.getElement(),
                layoutInsideComposite.getElement());
    }

    @Test
    public void getParent_compositeInLayout() {
        assertEquals(layoutWithSingleComponentComposite,
                compositeWithComponent.getParent().get());
    }

    @Test
    public void getParent_componentInComposite() {
        assertEquals(compositeWithComponent,
                layoutInsideComposite.getParent().get());
    }

    @Test
    public void getParent_componentInLayoutInComposite() {
        assertEquals(layoutInsideComposite,
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
        class CompositeWithGenericType extends Composite<TestComponent> {
        }

        CompositeWithGenericType instance = new CompositeWithGenericType();

        assertEquals(TestComponent.class, instance.getContent().getClass());
    }

    @Test(expected = IllegalStateException.class)
    public void compositeContentTypeWithVariableTypeParameter() {
        class CompositeWithVariableType<C extends Component>
                extends Composite<C> {
        }

        CompositeWithVariableType<TestComponent> composite = new CompositeWithVariableType<>();
        composite.getContent();
    }

    public static class CustomComponent<T> extends UI {
    }

    @Test
    public void compositeContentTypeWithSpecifiedType() {
        class CompositeWithCustomComponent
                extends Composite<CustomComponent<List<String>>> {
        }

        CompositeWithCustomComponent composite = new CompositeWithCustomComponent();

        assertEquals(CustomComponent.class, composite.getContent().getClass());
    }

    public static class CompositeWithVariableType<C extends Component>
            extends Composite<C> {
    }

    @Test(expected = IllegalStateException.class)
    public void compositeContentTypeWithTypeVariable() {
        class CompositeWithComposite
                extends Composite<CompositeWithVariableType<TestComponent>> {
        }

        CompositeWithComposite composite = new CompositeWithComposite();
        composite.getContent();
    }

    @Test(expected = IllegalStateException.class)
    public void rawContentType() {
        @SuppressWarnings("rawtypes")
        class CompositeWithRawType extends Composite {
        }

        CompositeWithRawType composite = new CompositeWithRawType();
        composite.getContent();
    }

    @Test(expected = IllegalArgumentException.class)
    public void noDefaultConstructor() {
        class NoDefaultConstructor extends Composite<Text> {
        }

        NoDefaultConstructor composite = new NoDefaultConstructor();
        composite.getContent();
    }

    @Test
    public void compositeHierarchy() {
        class Class1<T extends Component> extends Composite<T> {
        }
        class Class2<T, V extends Component> extends Class1<V> {
        }
        class Class3<E extends Component> extends Class2<String, E> {
        }
        class Class4<A, B extends Component, C> extends Class3<B> {
        }
        class ComplexClass extends Class4<String, UI, Boolean> {
        }

        ComplexClass composite = new ComplexClass();

        assertEquals(UI.class, composite.getContent().getClass());
    }

    // layoutWithSingleComponentComposite (TestLayout)
    // - compositeWithComponent (CompositeWithComponent)
    // --- layoutInsideComposite (TestLayout) content for compositeWithComponent
    // ---- componentInsideLayoutInsideComposite (TestComponent)
    @Test
    public void attachDetachEvents_compositeHierarchy_correctOrder() {
        UI ui = new UI();

        List<Component> attached = new ArrayList<>();
        List<Component> detached = new ArrayList<>();

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
            protected void onAttach(AttachEvent attachEvent) {
                triggered.add(1);
            }

            @Override
            protected void onDetach(DetachEvent detachEvent) {
                triggered.add(-1);
            }
        };
        component.addAttachListener(event -> triggered.add(2));
        component.addDetachListener(event -> triggered.add(-2));

        Composite<Component> compositeInsideComposite = new Composite<Component>() {
            @Override
            protected Component initContent() {
                return component;
            };

            @Override
            protected void onAttach(AttachEvent attachEvent) {
                triggered.add(3);
            }

            @Override
            protected void onDetach(DetachEvent detachEvent) {
                triggered.add(-3);
            }
        };
        compositeInsideComposite.addAttachListener(event -> triggered.add(4));
        compositeInsideComposite.addDetachListener(event -> triggered.add(-4));

        Composite<Component> composite = new Composite<Component>() {
            @Override
            protected Component initContent() {
                return compositeInsideComposite;
            }

            @Override
            protected void onAttach(AttachEvent attachEvent) {
                triggered.add(5);
            }

            @Override
            protected void onDetach(DetachEvent detachEvent) {
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
        assertEquals(expected.length, parent.getChildCount());
        for (int i = 0; i < parent.getChildCount(); i++) {
            assertEquals(expected[i], parent.getChild(i));
        }
    }
}
