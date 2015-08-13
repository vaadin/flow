package com.vaadin.hummingbird.kernel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
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

    @Test
    public void setSingleStyle() {
        Element e = new Element("div");
        e.setStyle("foo", "bar");
        assertElementEquals(parse("<div style='foo:bar'>"), e);
    }

    @Test
    public void setMultipleStyles() {
        Element e = new Element("div");
        e.setStyle("foo", "bar");
        e.setStyle("baz", "12px");
        assertElementEquals(parse("<div style='foo:bar;baz:12px'>"), e);
    }

    @Test
    public void removeAllStyles() {
        Element e = new Element("div");
        e.setAttribute("style", "foo:bar;baz:12px;width:100px");
        e.removeStyle("foo");
        e.removeStyle("baz");
        e.removeStyle("width");
        assertElementEquals(parse("<div>"), e);
    }

    @Test
    public void removeFirstStyle() {
        Element e = new Element("div");
        e.setAttribute("style", "foo:bar;baz:12px;width:100px");
        e.removeStyle("foo");
        assertElementEquals(parse("<div style='baz:12px;width:100px'>"), e);
    }

    @Test
    public void removeMiddleStyle() {
        Element e = new Element("div");
        e.setAttribute("style", "foo:bar;baz:12px;width:100px");
        e.removeStyle("baz");
        assertElementEquals(parse("<div style='foo:bar;width:100px'>"), e);
    }

    @Test
    public void removeLastStyle() {
        Element e = new Element("div");
        e.setAttribute("style", "foo:bar;baz:12px;width:100px");
        e.removeStyle("width");
        assertElementEquals(parse("<div style='foo:bar;baz:12px'>"), e);
    }

    @Test
    public void addSingleClass() {
        Element e = new Element("div");
        e.addClass("foo");
        assertElementEquals(parse("<div class='foo'>"), e);
    }

    @Test
    public void addMultipleClasses() {
        Element e = new Element("div");
        e.addClass("foo");
        e.addClass("bar");
        assertElementEquals(parse("<div class='foo bar'>"), e);
    }

    @Test
    public void removeFirstClass() {
        Element e = new Element("div");
        e.setAttribute("class", "foo bar baz");
        e.removeClass("foo");
        assertElementEquals(parse("<div class='bar baz'>"), e);
    }

    @Test
    public void removeMiddleClass() {
        Element e = new Element("div");
        e.setAttribute("class", "foo bar baz");
        e.removeClass("bar");
        assertElementEquals(parse("<div class='foo baz'>"), e);
    }

    @Test
    public void removeLastClass() {
        Element e = new Element("div");
        e.setAttribute("class", "foo bar baz");
        e.removeClass("baz");
        assertElementEquals(parse("<div class='foo bar'>"), e);
    }

    @Test
    public void removeAllClasses() {
        Element e = new Element("div");
        e.setAttribute("class", "foo bar baz");
        e.removeClass("baz");
        e.removeClass("bar");
        e.removeClass("foo");
        assertElementEquals(parse("<div>"), e);
    }

    public static void assertElementEquals(Element expected, Element actual) {
        Assert.assertEquals(expected.getTag(), actual.getTag());
        Assert.assertEquals(expected.getAttributeNames().size(),
                actual.getAttributeNames().size());

        for (String name : expected.getAttributeNames()) {
            Assert.assertTrue("Attribute " + name + " does not exist",
                    actual.hasAttribute(name));
            Assert.assertEquals(expected.getAttribute(name),
                    actual.getAttribute(name));
        }

    }

    public static Element parse(String html) {
        Document d = Jsoup.parse(html);
        org.jsoup.nodes.Element e = d.body().child(0);
        return jsoupToElement(e);
    }

    private static Element jsoupToElement(org.jsoup.nodes.Element model) {
        Element e = new Element(model.tagName());
        Attributes attrs = model.attributes();
        attrs.forEach(c -> {
            e.setAttribute(c.getKey(), c.getValue());
        });
        for (org.jsoup.nodes.Element childModel : model.children()) {
            Element childElement = jsoupToElement(childModel);
            e.appendChild(childElement);
        }
        return e;
    }

}
