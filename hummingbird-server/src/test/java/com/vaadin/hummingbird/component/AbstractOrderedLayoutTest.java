package com.vaadin.hummingbird.component;

import javax.servlet.http.HttpSession;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.hummingbird.kernel.ElementTest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedHttpSession;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;

public class AbstractOrderedLayoutTest
        extends AbstractSimpleDOMComponentContainerTest {

    UI ui;
    VaadinSession session;
    private VaadinServletService service;
    private VaadinServletRequest vaadinRequest;
    private WrappedHttpSession wrappedSession;
    private HttpSession httpSession;

    @Override
    protected AbstractOrderedLayout getLayout() {
        return (AbstractOrderedLayout) super.getLayout();
    }

    @Override
    protected AbstractOrderedLayout createLayout() {
        return new AbstractOrderedLayout() {
        };
    }

    @Test
    public void initialState() {
        ElementTest.assertElementEquals(
                ElementTest.parse("<div class='layout flex-children' />"),
                getLayout().getElement());
    }

    @Test
    public void initialFromDOM() {
        AbstractOrderedLayout aol2 = createLayout();
        setComponentElement(aol2, "<div class='layout flex-children' />");
        ElementTest.assertElementEquals(getLayout().getElement(),
                aol2.getElement());
    }

    @Test
    public void setExpand() {
        Button button = new Button();
        getLayout().addComponent(button);

        getLayout().setExpandRatio(button, 1);
        Assert.assertEquals(1, getLayout().getExpandRatio(button));
        assertHasClass(button.getElement(), "flex-1");
    }

    @Test
    public void changeExpand() {
        Button button = new Button();
        getLayout().addComponent(button);
        getLayout().setExpandRatio(button, 1);
        Element domBefore = ElementTest
                .parse(button.getElement().getOuterHTML());
        getLayout().setExpandRatio(button, 2);
        domBefore.removeClass("flex-1").addClass("flex-2");
        ElementTest.assertElementEquals(domBefore, button.getElement());

        Assert.assertEquals(2, getLayout().getExpandRatio(button));
        assertNotHasClass(button.getElement(), "flex-1");
        assertHasClass(button.getElement(), "flex-2");
    }

    @Test
    public void removeExpand() {
        Button button = new Button();
        getLayout().addComponent(button);
        getLayout().setExpandRatio(button, 1);
        getLayout().setExpandRatio(button, 0);

        Assert.assertEquals(0, getLayout().getExpandRatio(button));
        assertNotHasClass(button.getElement(), "flex-1");
        assertNotHasClass(button.getElement(), "flex-0");
        assertHasClass(getLayout().getElement(), "flex-children");
    }

    @Test
    public void allExpandInitially() {
        Button b1 = new Button();
        Button b2 = new Button();
        Button b3 = new Button();
        getLayout().addComponents(b1, b2, b3);

        Assert.assertEquals(0, getLayout().getExpandRatio(b1));
        Assert.assertEquals(0, getLayout().getExpandRatio(b2));
        Assert.assertEquals(0, getLayout().getExpandRatio(b3));
        assertHasClass(getLayout().getElement(),
                AbstractOrderedLayout.CLASS_FLEX_CHILDREN);
    }

    @Test
    public void setAlignment() {
        Button button = new Button();
        getLayout().addComponent(button);

        getLayout().setComponentAlignment(button, Alignment.BOTTOM_RIGHT);
        Assert.assertEquals(Alignment.BOTTOM_RIGHT,
                getLayout().getComponentAlignment(button));
        assertHasClass(button.getElement(), "bottom-right");
    }

    @Test
    public void changeAlignment() {
        Button button = new Button();
        getLayout().addComponent(button);
        getLayout().setComponentAlignment(button, Alignment.BOTTOM_CENTER);
        Element domBefore = ElementTest
                .parse(button.getElement().getOuterHTML());
        getLayout().setComponentAlignment(button, Alignment.MIDDLE_LEFT);
        domBefore.removeClass(Alignment.BOTTOM_CENTER.getClassName())
                .addClass(Alignment.MIDDLE_LEFT.getClassName());
        ElementTest.assertElementEquals(domBefore, button.getElement());

        Assert.assertEquals(Alignment.MIDDLE_LEFT,
                getLayout().getComponentAlignment(button));
        assertNotHasClass(button.getElement(), "bottom-center");
        assertHasClass(button.getElement(), "middle-left");
    }

    @Test
    public void removeAlignment() {
        Button button = new Button();
        getLayout().addComponent(button);
        getLayout().setComponentAlignment(button, Alignment.BOTTOM_CENTER);
        getLayout().setComponentAlignment(button,
                AbstractOrderedLayout.ALIGNMENT_DEFAULT);

        Assert.assertEquals(AbstractOrderedLayout.ALIGNMENT_DEFAULT,
                getLayout().getComponentAlignment(button));
        assertNotHasClass(button.getElement(), "bottom-center");
        assertNotHasClass(button.getElement(), "top-left");
    }

    @Test
    public void replaceComponentRetainsExpand() {
        Button b1 = new Button("First");
        Button b2 = new Button("Second");

        getLayout().addComponent(b1);
        getLayout().setExpandRatio(b1, 5);

        getLayout().replaceComponent(b1, b2);

        Assert.assertEquals(5, getLayout().getExpandRatio(b2));
    }

    @Override
    @Test(expected = IllegalArgumentException.class)
    public void replaceWithComponentAlreadyInLayout() {
        Button b1 = new Button("First");
        Button b2 = new Button("Second");
        getLayout().addComponents(b1, b2);
        getLayout().replaceComponent(b1, b2);
    }

    @Test
    public void setExpandWhenChildHasClass() {
        Button button = new Button();
        button.addStyleName("buttonclass");
        getLayout().addComponent(button);
        getLayout().setExpandRatio(button, 1);

        assertHasClass(button.getElement(), "buttonclass");
        assertHasClass(button.getElement(), "flex-1");
    }

}
