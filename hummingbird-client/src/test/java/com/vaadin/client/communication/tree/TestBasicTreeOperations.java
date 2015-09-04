package com.vaadin.client.communication.tree;

import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.vaadin.shared.communication.MethodInvocation;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

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

    public void testEventHandling() {
        applyChanges(
                Changes.listInsert(containerElementId, "LISTENERS", 0, "click"),
                Changes.putNode(containerElementId, "EVENT_DATA", 3),
                Changes.listInsert(3, "click", 0, "clientX"));

        NativeEvent event = Document.get().createClickEvent(0, 1, 2, 3, 4,
                false, false, false, false);
        updater.getRootElement().dispatchEvent(event);

        List<MethodInvocation> enqueuedInvocations = updater
                .getEnqueuedInvocations();
        assertEquals(1, enqueuedInvocations.size());

        MethodInvocation invocation = enqueuedInvocations.get(0);
        assertEquals("com.vaadin.ui.JavaScript$JavaScriptCallbackRpc",
                invocation.getInterfaceName());
        assertEquals("call", invocation.getMethodName());
        assertEquals("vEvent", invocation.getJavaScriptCallbackRpcName());

        JsonArray parameters = invocation.getParameters();
        assertEquals(3, parameters.length());

        // node ID
        assertEquals(containerElementId, (int) parameters.getNumber(0));
        // Event name
        assertEquals(event.getType(), parameters.getString(1));
        // Event data
        JsonObject eventData = parameters.getObject(2);
        assertEquals(1, eventData.keys().length);
        assertEquals(event.getClientX(), (int) eventData.getNumber("clientX"));

        enqueuedInvocations.clear();

        // Remove the listener
        applyChanges(Changes.listRemove(containerElementId, "LISTENERS", 0));

        // Fire a new event
        event = Document.get().createClickEvent(0, 1, 2, 3, 4, false, false,
                false, false);
        updater.getRootElement().dispatchEvent(event);

        assertEquals(0, enqueuedInvocations.size());
    }
}
