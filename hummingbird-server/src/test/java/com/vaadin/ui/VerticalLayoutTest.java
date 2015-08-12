package com.vaadin.ui;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class VerticalLayoutTest extends ComponentTestBase {

    VerticalLayout vl;

    @Before
    public void setup() {
        vl = new VerticalLayout();
    }

    @Test
    public void initialClass() {
        assertElementEquals(parse("<div class='layout vertical'>"),
                vl.getElement());
    }

    @Test
    public void testAddToEmpty() {
        VerticalLayout vl = new VerticalLayout();
        Button button = new Button();
        vl.addComponent(button);

        Assert.assertEquals(vl, button.getParent());
        Assert.assertEquals(1, vl.getComponentCount());
        Assert.assertEquals(button, vl.getComponent(0));

        // assertElement("<div class=\"layout vertical\"></div>",
        // vl.getElement());
    }

    @Test
    public void testAddAtEnd() {
        VerticalLayout vl = new VerticalLayout();
        vl.addComponent(new Button());
        Label label = new Label();
        vl.addComponent(label);

        Assert.assertEquals(vl, label.getParent());
        Assert.assertEquals(1, vl.getComponentCount());
        Assert.assertEquals(label, vl.getComponent(0));
        // assertElement("<div class=\"layout vertical\"><button /></div>",
        // vl.getElement());
    }
}
