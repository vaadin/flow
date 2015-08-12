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

        String html = element.getOuterHTML();
        Assert.assertEquals("<span class=\"foobar\"><strong></strong></span>",
                html);
    }

    @Test
    public void testTextNodeString() {
        Element strong = new Element("strong");
        strong.insertChild(0, Element.createText("world!"));

        Element root = new Element("span");
        root.insertChild(0, Element.createText("Hello "));
        root.insertChild(1, strong);

        Assert.assertEquals("<span>Hello <strong>world!</strong></span>",
                root.getOuterHTML());

    }
}
