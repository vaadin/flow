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

    public class CompositeWithComponent extends Composite<Component> {

        @Override
        protected Component initContent() {
            layoutInsideComposite = createTestLayout();
            componentInsideLayoutInsideComposite = createTestComponent();
            layoutInsideComposite
                    .addComponent(componentInsideLayoutInsideComposite);
            return layoutInsideComposite;
        }
    }

    public class CompositeWithGenericType extends Composite<TestComponent> {
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

    public static void assertElementChildren(Element parent,
            Element... expected) {
        Assert.assertEquals(expected.length, parent.getChildCount());
        for (int i = 0; i < parent.getChildCount(); i++) {
            Assert.assertEquals(expected[i], parent.getChild(i));
        }
    }
}
