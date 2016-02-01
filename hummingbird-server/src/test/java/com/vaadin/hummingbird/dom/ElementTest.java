package com.vaadin.hummingbird.dom;

import org.junit.Assert;
import org.junit.Test;

public class ElementTest {

    @Test
    public void createElementWithTag() {
        Element e = new Element("div");
        Assert.assertEquals("div", e.getTag());
    }

    @Test(expected = AssertionError.class)
    public void createElementWithEmptyTag() {
        new Element("");
    }

    @Test(expected = AssertionError.class)
    public void createElementWithNullTag() {
        new Element(null);
    }

    @Test
    public void createElementWithTagAndIs() {
        Element e = new Element("div", "foo-bar");
        Assert.assertEquals("div", e.getTag());
        // Assert.assertEquals("foo-bar", e.getAttribute("is"));
    }

    @Test(expected = AssertionError.class)
    public void createElementWithTagAndEmptyIs() {
        new Element("div", "");
    }

    @Test(expected = AssertionError.class)
    public void createElementWithTagAndNullIs() {
        new Element("div", null);
    }
}
