package com.vaadin.client.communication.tree;

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.vaadin.client.ChangeUtil;
import com.vaadin.shared.communication.MethodInvocation;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;

public class TestBasicTreeOperations extends AbstractTreeUpdaterTest {
    public void testAddRemoveElements() {
        int containerChildrenId = 6;
        int divId = 3;
        int spanId = 4;
        int imgId = 5;

        applyChanges(
                ChangeUtil.putList(containerElementId, "CHILDREN",
                        containerChildrenId),
                ChangeUtil.listInsertNode(containerChildrenId, 0, divId),
                ChangeUtil.put(divId, "TAG", "div"));

        Element root = updater.getRootElement();
        assertEquals(1, root.getChildCount());

        Element divChild = root.getFirstChildElement();
        assertEquals("DIV", divChild.getTagName());
        assertFalse(divChild.hasAttribute("tag"));

        applyChanges(ChangeUtil.listInsertNode(containerChildrenId, 1, spanId),
                ChangeUtil.put(spanId, "TAG", "span"));
        assertEquals(2, root.getChildCount());

        Element spanChild = divChild.getNextSiblingElement();
        assertEquals("SPAN", spanChild.getTagName());

        applyChanges(ChangeUtil.listInsertNode(containerChildrenId, 2, imgId),
                ChangeUtil.put(imgId, "TAG", "img"));
        assertEquals(divId, root.getChildCount());

        Element imgChild = Element.as(root.getChild(2));
        assertEquals("IMG", imgChild.getTagName());

        // Remove middle child
        applyChanges(ChangeUtil.listRemove(containerChildrenId, 1));
        assertEquals(2, root.getChildCount());
        assertNull(spanChild.getParentElement());

        // Remove first child even though it's no longer in the DOM
        spanChild.removeFromParent();
        applyChanges(ChangeUtil.listRemove(containerChildrenId, 0));
        assertEquals(1, root.getChildCount());
        assertEquals(root, imgChild.getParentElement());
    }

    public void testAttributesAndProperties() {
        Element element = updater.getRootElement();

        // Should only set property, not attribute
        applyChanges(ChangeUtil.put(containerElementId, "foo", "bar"));
        assertFalse(element.hasAttribute("foo"));
        assertEquals("bar", element.getPropertyString("foo"));

        applyChanges(ChangeUtil.remove(containerElementId, "foo"));
        assertFalse(element.hasAttribute("foo"));
        assertNull(element.getPropertyString("foo"));

        // Style should be set as attribute, not property
        applyChanges(
                ChangeUtil.put(containerElementId, "style", "height: 100%"));
        assertEquals("100%", element.getStyle().getHeight());

        applyChanges(ChangeUtil.remove(containerElementId, "style"));
        assertEquals("", element.getStyle().getHeight());

        // attr. prefixed attributes should be set as attributes only
        applyChanges(ChangeUtil.put(containerElementId, "attr.fox", "bax"));
        assertFalse(element.hasAttribute("attr.fox"));
        assertTrue(element.hasAttribute("fox"));
        assertEquals("bax", element.getAttribute("fox"));
        assertNull(element.getPropertyString("class"));

        applyChanges(ChangeUtil.remove(containerElementId, "attr.fox"));
        assertFalse(element.hasAttribute("fox"));
        assertEquals("", element.getAttribute("fox"));
    }

    public void testEventHandling() {
        int eventDataId = 3;
        int listenersId = 4;
        int clickDataId = 5;

        applyChanges(
                ChangeUtil.putList(containerElementId, "LISTENERS",
                        listenersId),
                ChangeUtil.listInsert(listenersId, 0, "click"),
                ChangeUtil.putMap(containerElementId, "EVENT_DATA",
                        eventDataId),
                ChangeUtil.putList(eventDataId, "click", clickDataId),
                ChangeUtil.listInsert(clickDataId, 0, "clientX"));

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
        applyChanges(ChangeUtil.listRemove(listenersId, 0));

        // Fire a new event
        event = Document.get().createClickEvent(0, 1, 2, 3, 4, false, false,
                false, false);
        updater.getRootElement().dispatchEvent(event);

        assertEquals(0, enqueuedInvocations.size());
    }

    public void testReceivingRpc() {
        JsonArray invocation = Json.createArray();

        invocation.set(0, "$0.foobarProperty=$1");
        JsonObject elementDescriptor = Json.createObject();
        elementDescriptor.put("node", containerElementId);
        // BasicElementTemplate.id = 0
        elementDescriptor.put("template", 0);
        invocation.set(1, elementDescriptor);
        invocation.set(2, "Hello");

        JsonArray invocations = Json.createArray();
        invocations.set(0, invocation);

        applyRpc(invocations);

        assertEquals("Hello",
                updater.getRootElement().getPropertyString("foobarProperty"));
    }

    public void testGWTWorkaroundNeeded() {
        assertEquals(
                "GWT has been fixed, buildChanges no longer needs to re-parse the json",
                JsonType.OBJECT, Json.create(true).getType());
        assertEquals(
                "GWT has been fixed, buildChanges no longer needs to re-parse the json",
                JsonType.OBJECT, Json.create(1).getType());
    }

    public void testStructuredProperties() {
        Element element = updater.getRootElement();

        JsonObject existingObject = Json.createObject();
        existingObject.put("foo", "bar");
        element.setPropertyJSO("existingobject",
                (JavaScriptObject) existingObject);

        JsonArray existingArray = Json.createArray();
        element.setPropertyJSO("existingarray",
                (JavaScriptObject) existingArray);

        int existingObjectId = 3;
        int newObjectId = 4;
        int existingMemberId = 5;
        int newMemberId = 6;
        int existingArrayId = 7;
        int newArrayId = 8;

        applyChanges(
                ChangeUtil.putMap(containerElementId, "existingobject",
                        existingObjectId),
                ChangeUtil.put(existingObjectId, "baz", "asdf"),
                ChangeUtil.putMap(containerElementId, "newobject", newObjectId),
                ChangeUtil.put(newObjectId, "new", "value"),
                ChangeUtil.putList(containerElementId, "existingarray",
                        existingArrayId),
                ChangeUtil.listInsertNode(existingArrayId, 0, existingMemberId),
                ChangeUtil.put(existingMemberId, "value", "yes"),
                ChangeUtil.putList(containerElementId, "newarray", newArrayId),
                ChangeUtil.listInsertNode(newArrayId, 0, newMemberId),
                ChangeUtil.put(newMemberId, "checked", Json.create(true)));

        assertSame(existingObject, element.getPropertyJSO("existingobject"));
        assertEquals("bar", existingObject.getString("foo"));
        assertEquals("asdf", existingObject.getString("baz"));

        JsonObject newObject = element.getPropertyJSO("newobject").cast();
        assertEquals("value", newObject.getString("new"));

        assertSame(existingArray, element.getPropertyJSO("existingarray"));
        assertEquals(1, existingArray.length());
        assertEquals("yes", existingArray.getObject(0).getString("value"));

        JsonArray newArray = element.getPropertyJSO("newarray").cast();
        assertEquals(1, newArray.length());
        assertTrue(newArray.getObject(0).getBoolean("checked"));
    }

    public void testStructuredProperties_basicArray() {
        int arrayId = 3;
        applyChanges(ChangeUtil.putList(containerElementId, "array", arrayId),
                ChangeUtil.listInsert(arrayId, 0, "Hello"));

        Element element = updater.getRootElement();
        JsonArray array = element.getPropertyJSO("array").cast();

        assertEquals(1, array.length());
        assertEquals("Hello", array.getString(0));
    }

    public void testEventDataCallback() {
        int eventDataId = 3;
        int clickDataId = 5;
        int listenersId = 4;

        applyChanges(
                ChangeUtil.putList(containerElementId, "LISTENERS",
                        listenersId),
                ChangeUtil.listInsert(listenersId, 0, "click"),
                ChangeUtil.putMap(containerElementId, "EVENT_DATA",
                        eventDataId),
                ChangeUtil.putList(eventDataId, "click", clickDataId),
                ChangeUtil.listInsert(clickDataId, 0,
                        "[typeof event, element.tagName]"));

        NativeEvent event = Document.get().createClickEvent(0, 1, 2, 3, 4,
                false, false, false, false);
        updater.getRootElement().dispatchEvent(event);

        List<MethodInvocation> enqueuedInvocations = updater
                .getEnqueuedInvocations();
        assertEquals(1, enqueuedInvocations.size());

        MethodInvocation invocation = enqueuedInvocations.get(0);
        JsonArray parameters = invocation.getParameters();

        JsonObject eventData = parameters.getObject(2);
        assertEquals(1, eventData.keys().length);

        JsonArray jsonValue = eventData.get("[typeof event, element.tagName]");
        assertEquals(JsonType.ARRAY, jsonValue.getType());
        assertEquals(2, jsonValue.length());
        assertEquals("object", jsonValue.getString(0));
        assertEquals("DIV", jsonValue.getString(1));
    }

    private static native String typeOf(
            Object object) /*-{ return typeof object; }-*/;
}
