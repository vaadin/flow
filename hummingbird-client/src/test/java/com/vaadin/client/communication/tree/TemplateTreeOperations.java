package com.vaadin.client.communication.tree;

import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.vaadin.shared.communication.MethodInvocation;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class TemplateTreeOperations extends AbstractTreeUpdaterTest {
    public void testBoundProperties() {
        String json = "{'type': 'BoundElementTemplate', 'tag':'span',"
                + "'defaultAttributes': {'foo': 'bar'},"
                + "'attributeBindings': {'value': 'bound'},"
                + "'classPartBindings': {'conditional': 'part'},"
                + "'modelStructure': ['value', 'conditional']}";
        JsonObject template = Json.parse(json.replace('\'', '"'));

        applyTemplate(1, template);

        applyChanges(
                Changes.listInsertNode(containerElementId, "CHILDREN", 0, 3),
                Changes.put(3, "TEMPLATE", Json.create(1)),
                Changes.put(3, "value", "Hello"),
                Changes.put(3, "conditional", Json.create(true)));

        Element rootElement = updater.getRootElement();
        assertEquals(1, rootElement.getChildCount());

        Element templateElement = rootElement.getFirstChildElement();
        assertEquals("SPAN", templateElement.getTagName());

        // Default attribute should be an attribute
        assertEquals("bar", templateElement.getAttribute("foo"));
        assertNull(templateElement.getPropertyString("bar"));

        // Bound attribute should be a property
        assertFalse(templateElement.hasAttribute("bound"));
        assertEquals("Hello", templateElement.getPropertyString("bound"));

        assertEquals("part", templateElement.getClassName());

        applyChanges(Changes.put(3, "value", "Bye"),
                Changes.put(3, "conditional", Json.create(false)));

        assertEquals("Bye", templateElement.getPropertyString("bound"));
        assertEquals("", templateElement.getClassName());
    }

    public void testTemplateEvents() {
        String json = "{'type': 'BoundElementTemplate', 'tag':'span',"
                + "'events': {'click': ['element.something=10','server.doSomething(element.something)']},"
                + "'eventHandlerMethods': ['doSomething'],"
                + "'modelStructure': []}";
        JsonObject template = Json.parse(json.replace('\'', '"'));

        applyTemplate(1, template);

        applyChanges(
                Changes.listInsertNode(containerElementId, "CHILDREN", 0, 3),
                Changes.put(3, "TEMPLATE", Json.create(1)));

        Element templateElement = updater.getRootElement()
                .getFirstChildElement();

        NativeEvent event = Document.get().createClickEvent(0, 1, 2, 3, 4,
                false, false, false, false);
        templateElement.dispatchEvent(event);

        assertEquals(10, templateElement.getPropertyInt("something"));

        List<MethodInvocation> enqueuedInvocations = updater
                .getEnqueuedInvocations();
        assertEquals(1, enqueuedInvocations.size());

        MethodInvocation methodInvocation = enqueuedInvocations.get(0);
        assertEquals("vTemplateEvent",
                methodInvocation.getJavaScriptCallbackRpcName());

        JsonArray parameters = methodInvocation.getParameters();
        assertEquals(4, parameters.length());
        // Node id
        assertEquals(3, (int) parameters.getNumber(0));
        // Template id
        assertEquals(1, (int) parameters.getNumber(1));
        assertEquals("doSomething", parameters.getString(2));
        // Parameter (element.something)
        assertEquals(10, (int) parameters.getNumber(3));
    }
}
