package com.vaadin.client.communication.tree;

import com.google.gwt.dom.client.Element;

import elemental.json.Json;
import elemental.json.JsonObject;

public class TemplateTreeOperations extends AbstractTreeUpdaterTest {
    public void testBoundProperties() {
        String json = "{'type': 'BoundElementTemplate', 'tag':'span',"
                + "'defaultAttributes': {'foo': 'bar'},"
                + "'attributeBindings': {'value': 'bound'},"
                + "'classPartBindings': {'conditional': 'part'},"
                + "'modelStructure': ['value', 'conditional']}";
        TreeUpdater.debug(json.replace('\'', '"'));
        JsonObject template = Json.parse(json.replace('\'', '"'));

        JsonObject templates = Json.createObject();
        templates.put("1", template);
        applyTemplate(templates);

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
}
