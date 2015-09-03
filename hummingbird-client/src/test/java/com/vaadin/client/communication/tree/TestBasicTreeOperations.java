package com.vaadin.client.communication.tree;

import com.google.gwt.dom.client.Element;

public class TestBasicTreeOperations extends AbstractTreeUpdaterTest {
    public void testAddRemoveElements() {
        applyChanges(
                Changes.listInsertNode(containerElementId, "CHILDREN", 0, 3),
                Changes.put(3, "TAG", "div"));

        Element root = updater.getRootElement();
        assertEquals(1, root.getChildCount());

        Element divChild = root.getFirstChildElement();
        assertEquals("DIV", divChild.getTagName());
        assertFalse(divChild.hasAttribute("tag"));

        applyChanges(
                Changes.listInsertNode(containerElementId, "CHILDREN", 1, 4),
                Changes.put(4, "TAG", "span"));
        assertEquals(2, root.getChildCount());

        Element spanChild = divChild.getNextSiblingElement();
        assertEquals("SPAN", spanChild.getTagName());

        applyChanges(
                Changes.listInsertNode(containerElementId, "CHILDREN", 2, 5),
                Changes.put(5, "TAG", "img"));
        assertEquals(3, root.getChildCount());

        Element imgChild = Element.as(root.getChild(2));
        assertEquals("IMG", imgChild.getTagName());

        // Remove middle child
        applyChanges(Changes.listRemove(containerElementId, "CHILDREN", 1));
        assertEquals(2, root.getChildCount());
        assertNull(spanChild.getParentElement());

        // Remove first child even though it's no longer in the DOM
        spanChild.removeFromParent();
        applyChanges(Changes.listRemove(containerElementId, "CHILDREN", 0));
        assertEquals(1, root.getChildCount());
        assertEquals(root, imgChild.getParentElement());
    }

    public void testAttributesAndProperties() {
        Element element = updater.getRootElement();

        // Should only set property, not attribute
        applyChanges(Changes.put(containerElementId, "foo", "bar"));
        assertFalse(element.hasAttribute("foo"));
        assertEquals("bar", element.getPropertyString("foo"));

        applyChanges(Changes.remove(containerElementId, "foo"));
        assertFalse(element.hasAttribute("foo"));
        assertNull(element.getPropertyString("foo"));

        // Class should be set as attribute, not property
        applyChanges(Changes.put(containerElementId, "class", "foo bar"));
        assertEquals("foo bar", element.getClassName());
        assertNull(element.getPropertyString("class"));

        applyChanges(Changes.remove(containerElementId, "class"));
        assertEquals("", element.getClassName());
        assertNull(element.getPropertyString("class"));

        // Style should be set as attribute, not property
        applyChanges(Changes.put(containerElementId, "style", "height: 100%"));
        assertEquals("100%", element.getStyle().getHeight());

        applyChanges(Changes.remove(containerElementId, "style"));
        assertEquals("", element.getStyle().getHeight());
    }
}
