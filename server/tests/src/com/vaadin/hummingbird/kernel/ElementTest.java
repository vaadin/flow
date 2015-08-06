package com.vaadin.hummingbird.kernel;

import org.junit.Assert;
import org.junit.Test;

public class ElementTest {
    @Test
    public void testToString() {
        Element element = new Element("span");
        element.setAttribute("class", "foobar");
        element.setAttribute("nullValued", null);
        element.insertChild(0, new Element("strong"));

        StringBuilder b = new StringBuilder();
        element.toString(b);
        Assert.assertEquals("<span class=\"foobar\"><strong></strong></span>",
                b.toString());
    }

    @Test
    public void testTextNodeString() {
        Element strong = new Element("strong");
        strong.insertChild(0, Element.createText("world!"));

        Element root = new Element("span");
        root.insertChild(0, Element.createText("Hello "));
        root.insertChild(1, strong);

        Assert.assertEquals("<span>Hello <strong>world!</strong></span>",
                root.toString());

    }
}
