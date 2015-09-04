package com.vaadin.client.communication.tree;

import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Text;
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

    public void testChildTemplates() {
        String boundTextJson = "{'type': 'DynamicTextTemplate', 'binding':'boundText'}";
        applyTemplate(1, Json.parse(boundTextJson.replace('\'', '"')));

        String basicChildJson = "{'type': 'BoundElementTemplate', 'tag':'input',"
                + "'defaultAttributes': {'type': 'password'},"
                + "'attributeBindings': {'value': 'value'}}";
        applyTemplate(2, Json.parse(basicChildJson.replace('\'', '"')));

        String staticTextJson = "{'type': 'StaticTextTemplate', 'content': 'static text'}";
        applyTemplate(3, Json.parse(staticTextJson.replace('\'', '"')));

        String parentJson = "{'type': 'BoundElementTemplate', 'tag':'div',"
                + "'children': [1, 2, 3],"
                + "'modelStructure': ['value', 'boundText']}";

        applyTemplate(4, Json.parse(parentJson.replace('\'', '"')));

        applyChanges(
                Changes.listInsertNode(containerElementId, "CHILDREN", 0, 3),
                Changes.put(3, "TEMPLATE", Json.create(4)),
                Changes.put(3, "value", "Hello"),
                Changes.put(3, "boundText", "dynamic text"));

        Element parent = updater.getRootElement().getFirstChildElement();
        assertEquals(3, parent.getChildCount());

        Text boundText = Text.as(parent.getChild(0));
        assertEquals("dynamic text", boundText.getData());

        Element basicChild = Element.as(parent.getChild(1));
        assertEquals("password", basicChild.getAttribute("type"));
        assertEquals("Hello", basicChild.getPropertyString("value"));

        Text staticText = Text.as(parent.getChild(2));
        assertEquals("static text", staticText.getData());

        applyChanges(Changes.put(3, "value", "new value"),
                Changes.put(3, "boundText", "very dynamic text"));

        assertEquals("new value", basicChild.getPropertyString("value"));
        assertEquals("very dynamic text", boundText.getData());
    }

    public void testForTemplate() {
        String forJson = "{'type': 'ForElementTemplate', 'tag':'input',"
                + "'modelKey': 'items', 'innerScope':'item',"
                + "'defaultAttributes': {'type': 'checkbox'},"
                + "'attributeBindings': {'item.checked': 'checked'}" + "}";
        applyTemplate(1, Json.parse(forJson.replace('\'', '"')));

        String parentJson = "{'type': 'BoundElementTemplate', 'tag':'div',"
                + "'children': [1],"
                + "'modelStructure': [{'items': ['checked']}]}";
        applyTemplate(2, Json.parse(parentJson.replace('\'', '"')));

        applyChanges(
                Changes.listInsertNode(containerElementId, "CHILDREN", 0, 3),
                Changes.put(3, "TEMPLATE", Json.create(2)));

        Element parent = updater.getRootElement().getFirstChildElement();
        assertEquals(1, parent.getChildCount());
        // 8 = comment node
        assertEquals(8, parent.getChild(0).getNodeType());

        applyChanges(Changes.listInsertNode(3, "items", 0, 4),
                Changes.put(4, "checked", Json.create(true)));

        assertEquals(2, parent.getChildCount());

        Element firstChild = Element.as(parent.getChild(1));
        assertEquals("INPUT", firstChild.getTagName());
        assertEquals("checkbox", firstChild.getAttribute("type"));
        assertTrue(firstChild.getPropertyBoolean("checked"));

        applyChanges(Changes.listInsertNode(3, "items", 1, 5));
        assertEquals(3, parent.getChildCount());
        Element secondChild = firstChild.getNextSiblingElement();
        assertTrue(secondChild == parent.getChild(2));

        applyChanges(Changes.listRemove(3, "items", 0));

        assertEquals(2, parent.getChildCount());
        assertSame(parent, secondChild.getParentElement());
        assertNull(firstChild.getParentElement());
    }
}
