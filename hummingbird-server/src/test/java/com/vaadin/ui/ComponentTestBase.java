package com.vaadin.ui;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.junit.Assert;

import com.vaadin.hummingbird.kernel.Element;

public class ComponentTestBase {

    protected void assertElementEquals(Element expected, Element actual) {
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

    protected static <T extends Component> T createComponent(Class<T> cls,
            String html) {
        try {
            T c = cls.newInstance();
            setComponentElement(c, html);
            return c;
        } catch (InstantiationException | IllegalAccessException e1) {
            throw new RuntimeException(e1);
        }
    }

    protected static void setComponentElement(Component c, String html) {
        Element e = parse(html);
        ((AbstractComponent) c).setElement(e);

    }

    protected static Element parse(String html) {
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
