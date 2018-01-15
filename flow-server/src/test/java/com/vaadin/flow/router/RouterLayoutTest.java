package com.vaadin.flow.router;

import com.vaadin.flow.component.ComponentTest;
import com.vaadin.flow.dom.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RouterLayoutTest {

    private static final String NEW_ID = "newId";
    private TestRouterLayout testRouterLayout;

    @Before
    public void setup() {
        testRouterLayout = new TestRouterLayout();
    }

    @Test
    public void testNotNull() {
        Assert.assertEquals(0, testRouterLayout.getElement().getChildCount());
        testRouterLayout.setRouterLayoutContent(new ComponentTest.TestDiv());
        Assert.assertEquals(1, testRouterLayout.getElement().getChildCount());
        ComponentTest.TestDiv newContent = new ComponentTest.TestDiv();
        newContent.setId(NEW_ID);
        testRouterLayout.setRouterLayoutContent(newContent);
        Assert.assertEquals(1, testRouterLayout.getElement().getChildCount());
        Assert.assertSame(NEW_ID, testRouterLayout.getElement().getChild(0).getAttribute("id"));
    }

    @Test
    public void testNull() {
        Assert.assertEquals(0, testRouterLayout.getElement().getChildCount());
        testRouterLayout.setRouterLayoutContent(new ComponentTest.TestDiv());
        Assert.assertEquals(1, testRouterLayout.getElement().getChildCount());
        testRouterLayout.setRouterLayoutContent(null);
        Assert.assertEquals(0, testRouterLayout.getElement().getChildCount());
    }


    private static class TestRouterLayout implements RouterLayout {
        private final Element element = new Element("span");

        @Override
        public Element getElement() {
            return element;
        }
    }
}
