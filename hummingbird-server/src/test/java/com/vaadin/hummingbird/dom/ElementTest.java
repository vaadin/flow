package com.vaadin.hummingbird.dom;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class ElementTest {

    @Test
    public void createElementWithTag() {
        Element e = new Element("div");
        Assert.assertEquals("div", e.getTag());
        Assert.assertFalse(e.hasAttribute("is"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createElementWithInvalidTag() {
        new Element("<div>");
    }

    @Test(expected = IllegalArgumentException.class)
    public void createElementWithEmptyTag() {
        new Element("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void createElementWithNullTag() {
        new Element(null);
    }

    @Test
    public void createElementWithTagAndIs() {
        Element e = new Element("div", "foo-bar");
        Assert.assertEquals("div", e.getTag());
        Assert.assertEquals("foo-bar", e.getAttribute("is"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createElementWithTagAndEmptyIs() {
        new Element("div", "");
    }

    public void createElementWithTagAndNullIs() {
        Element e = new Element("div", null);
        Assert.assertFalse(e.hasAttribute("is"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setIsAttribute() {
        Element e = new Element("div");
        // Can't set "is" after creation
        e.setAttribute("is", "bar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setIsAttributeCaseInsensitive() {
        Element e = new Element("div");
        // Can't set "is" after creation
        e.setAttribute("IS", "bar");
    }

    @Test
    public void publicMethodsShouldReturnElement() {
        HashSet<String> ignore = new HashSet<>();
        ignore.add("toString");
        ignore.add("hashCode");
        ignore.add("equals");

        for (Method m : Element.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers())) {
                continue;
            }
            if (m.getName().startsWith("get") || m.getName().startsWith("has")
                    || m.getName().startsWith("is")
                    || ignore.contains(m.getName())) {
                // Ignore
            } else {
                // Setters and such
                Class<?> returnType = m.getReturnType();
                Assert.assertEquals(
                        "Method " + m.getName() + " has invalid return type",
                        Element.class, returnType);
            }
        }

    }

    @Test
    public void stringAttribute() {
        Element e = new Element("div");
        e.setAttribute("foo", "bar");
        Assert.assertEquals("bar", e.getAttribute("foo"));
    }

    @Test
    public void setEmptyAttribute() {
        Element e = new Element("div");
        e.setAttribute("foo", "");
        Assert.assertEquals("", e.getAttribute("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setNullAttribute() {
        Element e = new Element("div");
        e.setAttribute("foo", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setInvalidAttribute() {
        Element e = new Element("div");
        e.setAttribute("\"foo\"", "bar");
    }

    @Test
    public void hasDefinedAttribute() {
        Element e = new Element("div");
        e.setAttribute("foo", "bar");
        Assert.assertTrue(e.hasAttribute("foo"));
    }

    @Test
    public void doesNotHaveUndefinedAttribute() {
        Element e = new Element("div");
        Assert.assertFalse(e.hasAttribute("foo"));
    }

    @Test
    public void doesNotHaveRemovedAttribute() {
        Element e = new Element("div");
        e.setAttribute("foo", "bar");
        e.removeAttribute("foo");
        Assert.assertFalse(e.hasAttribute("foo"));
    }

    @Test
    public void removeNonExistingAttributeIsNoOp() {
        Element e = new Element("div");
        Assert.assertFalse(e.hasAttribute("foo"));
        e.removeAttribute("foo");
        Assert.assertFalse(e.hasAttribute("foo"));
    }

    @Test
    public void attributesWhenNoneDefined() {
        Element e = new Element("div");
        Assert.assertTrue(e.getAttributeNames().isEmpty());
    }

    @Test
    public void attributesNames() {
        Element e = new Element("div");
        e.setAttribute("foo", "bar");
        Assert.assertArrayEquals(new String[] { "foo" },
                e.getAttributeNames().toArray());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void attributesNamesModificationsNotReflected() {
        Element e = new Element("div");
        e.setAttribute("foo", "bar");
        e.getAttributeNames().remove("foo");
        Assert.assertTrue(e.hasAttribute("foo"));
    }

    @Test
    public void attributesNamesShouldNotBeDynamic() {
        Element e = new Element("div");
        e.setAttribute("foo", "bar");
        Set<String> names = e.getAttributeNames();
        Assert.assertEquals(1, names.size());
        e.setAttribute("baz", "zoo");

        // NOTE: This only tests the current implementation. This is not
        // guaranteed behavior
        Assert.assertEquals(2, names.size());
        Assert.assertTrue(e.hasAttribute("baz"));
    }

    @Test
    public void attributesNamesAfterRemoved() {
        Element e = new Element("div");
        e.setAttribute("foo", "bar");
        e.setAttribute("bar", "baz");
        e.removeAttribute("foo");
        Assert.assertArrayEquals(new String[] { "bar" },
                e.getAttributeNames().toArray());
    }

    @Test
    public void setGetAttributeValueCaseSensitive() {
        Element e = new Element("span");
        e.setAttribute("foo", "bAr");
        Assert.assertEquals("bAr", e.getAttribute("foo"));
        e.setAttribute("foo", "BAR");
        Assert.assertEquals("BAR", e.getAttribute("foo"));
    }

    @Test
    public void setGetAttributeNameCaseInsensitive() {
        Element e = new Element("span");
        e.setAttribute("foo", "bar");
        e.setAttribute("FOO", "baz");

        Assert.assertEquals("baz", e.getAttribute("foo"));
        Assert.assertEquals("baz", e.getAttribute("FOO"));
    }

    @Test
    public void hasAttributeNamesCaseInsensitive() {
        Element e = new Element("span");
        e.setAttribute("fooo", "bar");
        Assert.assertTrue(e.hasAttribute("fOoO"));
    }

    @Test
    public void getAttributeNamesLowerCase() {
        Element e = new Element("span");
        e.setAttribute("FOO", "bar");
        e.setAttribute("Baz", "bar");

        Assert.assertTrue(e.getAttributeNames().contains("foo"));
        Assert.assertFalse(e.getAttributeNames().contains("FOO"));
        Assert.assertTrue(e.getAttributeNames().contains("baz"));
        Assert.assertFalse(e.getAttributeNames().contains("Baz"));
    }

    @Test
    public void appendChild() {
        Element parent = new Element("div");
        Element child = new Element("child");
        parent.appendChild(child);

        assertChildren(parent, child);
    }

    @Test
    public void appendChildren() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1, child2);

        assertChildren(parent, child1, child2);
    }

    private void assertChildren(Element parent, Element... children) {

        Assert.assertEquals(children.length, parent.getChildCount());
        for (int i = 0; i < children.length; i++) {
            Assert.assertEquals(parent, children[i].getParent());
            Assert.assertEquals(children[i], parent.getChild(i));
        }
    }

    @Test
    public void insertChildFirst() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1);
        parent.insertChild(0, child2);

        assertChildren(parent, child2, child1);
    }

    @Test
    public void insertChildMiddle() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2);
        parent.insertChild(1, child3);

        assertChildren(parent, child1, child3, child2);
    }

    @Test
    public void insertChildLast() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2);
        parent.insertChild(2, child3);

        assertChildren(parent, child1, child2, child3);
    }

    @Test
    public void removeChildFirst() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(child1);

        assertChildren(parent, child2, child3);
    }

    @Test
    public void removeChildFirstIndex() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(0);

        assertChildren(parent, child2, child3);
    }

    @Test
    public void removeChildrenFirst() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(child1, child2);

        assertChildren(parent, child3);
    }

    @Test
    public void removeChildMiddle() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(child2);

        assertChildren(parent, child1, child3);
    }

    @Test
    public void removeChildMiddleIndex() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(1);

        assertChildren(parent, child1, child3);
    }

    @Test
    public void removeChildrenMiddle() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        Element child4 = new Element("child4");
        parent.appendChild(child1, child2, child3, child4);
        parent.removeChild(child2, child3);

        assertChildren(parent, child1, child4);
    }

    @Test
    public void removeChildLast() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(child3);

        assertChildren(parent, child1, child2);
    }

    @Test
    public void removeChildLastIndex() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(2);

        assertChildren(parent, child1, child2);
    }

    @Test
    public void removeChildrenLast() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        Element child4 = new Element("child4");
        parent.appendChild(child1, child2, child3, child4);
        parent.removeChild(child3, child4);

        assertChildren(parent, child1, child2);
    }

    @Test
    public void removeAllChildren() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        Element child4 = new Element("child4");
        parent.appendChild(child1, child2, child3, child4);
        parent.removeAllChildren();

        assertChildren(parent);
    }

    @Test
    public void removeAllChildrenEmpty() {
        Element parent = new Element("div");
        parent.removeAllChildren();

        assertChildren(parent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeNonChild() {
        Element parent = new Element("div");
        Element otherElement = new Element("other");
        parent.removeChild(otherElement);
    }

    @Test
    public void getChild() {
        Element parent = new Element("div");
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        Element child4 = new Element("child4");
        parent.appendChild(child1, child2, child3, child4);
        Assert.assertEquals(child1, parent.getChild(0));
        Assert.assertEquals(child2, parent.getChild(1));
        Assert.assertEquals(child3, parent.getChild(2));
        Assert.assertEquals(child4, parent.getChild(3));
    }

    @Test
    public void getDetachedParent() {
        Element otherElement = new Element("other");
        Assert.assertNull(otherElement.getParent());
    }

}
