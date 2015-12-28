package com.vaadin.client.communication.tree;

import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Text;
import com.vaadin.client.ChangeUtil;
import com.vaadin.shared.communication.MethodInvocation;

import elemental.js.json.JsJsonObject;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;

public class TemplateTreeOperations extends AbstractTreeUpdaterTest {
    public void testBoundProperties() {
        String json = "{'type': 'BoundElementTemplate', 'tag':'span',"
                + "'defaultAttributes': {'foo': 'bar'},"
                + "'attributeBindings': {'bound': 'value'},"
                + "'classPartBindings': {'part': 'conditional'},"
                + "'modelStructure': ['value', 'conditional']}";
        JsonObject template = Json.parse(json.replace('\'', '"'));

        applyTemplate(1, template);

        int childId = 3;
        int childrenId = 4;

        applyChanges(
                ChangeUtil.putList(containerElementId, "CHILDREN", childrenId),
                ChangeUtil.listInsertNode(childrenId, 0, childId),
                ChangeUtil.put(childId, "TEMPLATE", Json.create(1)),
                ChangeUtil.put(childId, "value", "Hello"),
                ChangeUtil.put(childId, "conditional", Json.create(true)));

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

        applyChanges(ChangeUtil.put(childId, "value", "Bye"),
                ChangeUtil.put(childId, "conditional", Json.create(false)));

        assertEquals("Bye", templateElement.getPropertyString("bound"));
        assertEquals("", templateElement.getClassName());
    }

    public void testClasses() {
        String json = "{'type': 'BoundElementTemplate', 'tag':'span',"
                + "'defaultAttributes': {'class': 'baseClass second'},"
                + "'classPartBindings': {'part':'conditional'},"
                + "'modelStructure': ['conditional']}";
        JsonObject template = Json.parse(json.replace('\'', '"'));

        applyTemplate(1, template);

        int childId = 3;
        int childrenId = 4;
        int classListId = 5;

        applyChanges(
                ChangeUtil.putList(containerElementId, "CHILDREN", childrenId),
                ChangeUtil.listInsertNode(childrenId, 0, childId),
                ChangeUtil.put(childId, "TEMPLATE", Json.create(1)),
                ChangeUtil.put(childId, "conditional", Json.create(true)));

        Element rootElement = updater.getRootElement();
        assertEquals(1, rootElement.getChildCount());

        Element templateElement = rootElement.getFirstChildElement();
        assertEquals("SPAN", templateElement.getTagName());
        assertEquals("baseClass second part", templateElement.getClassName());

        JsonArray array = Json.createArray();
        array.set(0, "foo");
        array.set(1, "bar");
        applyChanges(ChangeUtil.putList(childId, "CLASS_LIST", classListId),
                ChangeUtil.listInsert(classListId, 0, array));
        assertEquals("baseClass second part foo bar",
                templateElement.getClassName());

        applyChanges(
                ChangeUtil.put(childId, "conditional", Json.create(false)));
        assertEquals("baseClass second foo bar",
                templateElement.getClassName());

        applyChanges(ChangeUtil.listRemove(classListId, 0));
        assertEquals("baseClass second bar", templateElement.getClassName());

        applyChanges(ChangeUtil.put(childId, "conditional", Json.create(true)));
        assertEquals("baseClass second bar part",
                templateElement.getClassName());
    }

    public void testBadClassParsingInTemplate() {
        String json = "{'type': 'BoundElementTemplate', 'tag':'span',"
                + "'defaultAttributes': {'class': ' baseClass     white-spacing   cannottype  a '}}";
        JsonObject template = Json.parse(json.replace('\'', '"'));

        applyTemplate(1, template);

        int childId = 3;
        int childrenId = 4;

        applyChanges(
                ChangeUtil.putList(containerElementId, "CHILDREN", childrenId),
                ChangeUtil.listInsertNode(childrenId, 0, childId),
                ChangeUtil.put(childId, "TEMPLATE", Json.create(1)));

        Element rootElement = updater.getRootElement();
        assertEquals(1, rootElement.getChildCount());

        Element templateElement = rootElement.getFirstChildElement();
        assertEquals("SPAN", templateElement.getTagName());
        assertEquals("baseClass white-spacing cannottype a",
                templateElement.getClassName());
    }

    public void testEmptyClassParsingInTemplate() {
        String json = "{'type': 'BoundElementTemplate', 'tag':'span',"
                + "'defaultAttributes': {'class': ''}}";
        JsonObject template = Json.parse(json.replace('\'', '"'));

        applyTemplate(1, template);

        int childId = 3;
        int childrenId = 4;

        applyChanges(
                ChangeUtil.putList(containerElementId, "CHILDREN", childrenId),
                ChangeUtil.listInsertNode(childrenId, 0, childId),
                ChangeUtil.put(childId, "TEMPLATE", Json.create(1)));

        Element rootElement = updater.getRootElement();
        assertEquals(1, rootElement.getChildCount());

        Element templateElement = rootElement.getFirstChildElement();
        assertEquals("SPAN", templateElement.getTagName());
        assertEquals("", templateElement.getClassName());
    }

    public void testTemplateEvents() {
        String json = "{'type': 'BoundElementTemplate', 'tag':'span',"
                + "'events': {'click': ['element.something=10','server.doSomething(element.something)', 'model.value=1']},"
                + "'attributeBindings': {'value': 'value'},"
                + "'eventHandlerMethods': ['doSomething'],"
                + "'modelStructure': ['value']}";
        JsonObject template = Json.parse(json.replace('\'', '"'));

        applyTemplate(1, template);

        int childId = 3;
        applyChanges(ChangeUtil.putList(containerElementId, "CHILDREN", 4),
                ChangeUtil.put(childId, "value", Json.createNull()),
                ChangeUtil.listInsertNode(4, 0, childId),
                ChangeUtil.put(childId, "TEMPLATE", Json.create(1)));

        Element templateElement = updater.getRootElement()
                .getFirstChildElement();
        assertNull(templateElement.getPropertyObject("value"));

        NativeEvent event = Document.get().createClickEvent(0, 1, 2, 3, 4,
                false, false, false, false);
        templateElement.dispatchEvent(event);

        assertEquals(1, templateElement.getPropertyInt("value"));

        assertEquals(10, templateElement.getPropertyInt("something"));

        List<JsonObject> enqueuedNodeChanges = updater.getEnqueuedNodeChanges();
        assertEquals(1, enqueuedNodeChanges.size());

        JsonObject enquedNodeChange = enqueuedNodeChanges.get(0);
        assertEquals(childId, (int) enquedNodeChange.getNumber("id"));
        assertEquals("put", enquedNodeChange.getString("type"));
        assertEquals("value", enquedNodeChange.getString("key"));
        assertEquals(1, (int) enquedNodeChange.getNumber("value"));

        List<MethodInvocation> enqueuedInvocations = updater
                .getEnqueuedInvocations();
        assertEquals(1, enqueuedInvocations.size());

        MethodInvocation methodInvocation = enqueuedInvocations.get(0);
        assertEquals("vTemplateEvent",
                methodInvocation.getJavaScriptCallbackRpcName());

        JsonArray parameters = methodInvocation.getParameters();
        assertEquals(5, parameters.length());
        // Node id
        assertEquals(childId, (int) parameters.getNumber(0));
        // Template id
        assertEquals(1, (int) parameters.getNumber(1));
        assertEquals("doSomething", parameters.getString(2));

        // Parameter (element.something)
        JsonArray arguments = parameters.getArray(3);
        assertEquals(JsonType.ARRAY, arguments.getType());
        assertEquals(1, arguments.length());
        assertEquals(10, (int) arguments.getNumber(0));

        // Promise id
        assertEquals(0, (int) parameters.getNumber(4));
    }

    public void testTemplateEventParameters() {
        int templateId = 1;

        String json = "{'type': 'BoundElementTemplate', 'tag':'span',"
                + "'events': {'click': ['server.doSomething(element, model, model.child)']},"
                + "'eventHandlerMethods': ['doSomething']}";
        JsonObject template = Json.parse(json.replace('\'', '"'));

        applyTemplate(templateId, template);

        int childId = 3;
        int childrenId = 4;
        int modelChildId = 5;
        applyChanges(
                ChangeUtil.putList(containerElementId, "CHILDREN", childrenId),
                ChangeUtil.putMap(childId, "child", modelChildId),
                ChangeUtil.listInsertNode(childrenId, 0, childId),
                ChangeUtil.put(childId, "TEMPLATE", Json.create(1)));

        Element templateElement = updater.getRootElement()
                .getFirstChildElement();

        NativeEvent event = Document.get().createClickEvent(0, 1, 2, 3, 4,
                false, false, false, false);
        templateElement.dispatchEvent(event);

        List<MethodInvocation> enqueuedInvocations = updater
                .getEnqueuedInvocations();

        assertEquals(1, enqueuedInvocations.size());

        MethodInvocation methodInvocation = enqueuedInvocations.get(0);
        assertEquals("vTemplateEvent",
                methodInvocation.getJavaScriptCallbackRpcName());

        JsonArray parameters = methodInvocation.getParameters();
        assertEquals(5, parameters.length());
        // Node id
        assertEquals(childId, (int) parameters.getNumber(0));
        // Template id
        assertEquals(templateId, (int) parameters.getNumber(1));
        assertEquals("doSomething", parameters.getString(2));

        // Arguments
        JsonArray arguments = parameters.getArray(3);
        assertEquals(JsonType.ARRAY, arguments.getType());
        assertEquals(3, arguments.length());

        // Parameter element
        JsonArray elementParam = arguments.getArray(0);
        assertEquals(JsonType.ARRAY, elementParam.getType());
        assertEquals(2, elementParam.length());
        assertEquals(childId, (int) elementParam.getNumber(0));
        assertEquals(templateId, (int) elementParam.getNumber(1));

        // Parameter model
        assertEquals(childId, (int) arguments.getNumber(1));
        // Parameter model.child
        assertEquals(modelChildId, (int) arguments.getNumber(2));

        // Promise id
        assertEquals(0, (int) parameters.getNumber(4));
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

        int childId = 3;
        int childrenId = 4;

        applyChanges(
                ChangeUtil.putList(containerElementId, "CHILDREN", childrenId),
                ChangeUtil.listInsertNode(childrenId, 0, childId),
                ChangeUtil.put(childId, "TEMPLATE", Json.create(4)),
                ChangeUtil.put(childId, "value", "Hello"),
                ChangeUtil.put(childId, "boundText", "dynamic text"));

        Element parent = updater.getRootElement().getFirstChildElement();
        assertEquals(3, parent.getChildCount());

        Text boundText = Text.as(parent.getChild(0));
        assertEquals("dynamic text", boundText.getData());

        Element basicChild = Element.as(parent.getChild(1));
        assertEquals("password", basicChild.getAttribute("type"));
        assertEquals("Hello", basicChild.getPropertyString("value"));

        Text staticText = Text.as(parent.getChild(2));
        assertEquals("static text", staticText.getData());

        applyChanges(ChangeUtil.put(childId, "value", "new value"),
                ChangeUtil.put(childId, "boundText", "very dynamic text"));

        assertEquals("new value", basicChild.getPropertyString("value"));
        assertEquals("very dynamic text", boundText.getData());
    }

    public void testForTemplate() {
        String forJson = "{'type': 'ForElementTemplate', 'tag':'input',"
                + "'modelKey': 'items', 'innerScope':'item',"
                + "'defaultAttributes': {'type': 'checkbox'},"
                + "'events': {'click': ['model.foo = 1; item.foo = 2;']},"
                + "'attributeBindings': {'checked': 'item.checked'}" + "}";
        applyTemplate(1, Json.parse(forJson.replace('\'', '"')));

        String parentJson = "{'type': 'BoundElementTemplate', 'tag':'div',"
                + "'children': [1],"
                + "'modelStructure': [{'items': ['checked']}]}";
        applyTemplate(2, Json.parse(parentJson.replace('\'', '"')));

        int childrenId = 6;
        int itemsId = 7;
        int templateRootNode = 3;

        applyChanges(
                ChangeUtil.putList(containerElementId, "CHILDREN", childrenId),
                ChangeUtil.listInsertNode(childrenId, 0, templateRootNode),
                ChangeUtil.putList(templateRootNode, "items", itemsId),
                ChangeUtil.put(templateRootNode, "TEMPLATE", Json.create(2)));

        Element parent = updater.getRootElement().getFirstChildElement();
        assertEquals(1, parent.getChildCount());
        // 8 = comment node
        assertEquals(8, parent.getChild(0).getNodeType());

        applyChanges(ChangeUtil.listInsertNode(itemsId, 0, 4),
                ChangeUtil.put(4, "checked", Json.create(true)));

        assertEquals(2, parent.getChildCount());

        Element firstChild = Element.as(parent.getChild(1));
        assertEquals("INPUT", firstChild.getTagName());
        assertEquals("checkbox", firstChild.getAttribute("type"));
        assertTrue(firstChild.getPropertyBoolean("checked"));

        applyChanges(ChangeUtil.listInsertNode(itemsId, 1, 5));
        assertEquals(3, parent.getChildCount());
        Element secondChild = firstChild.getNextSiblingElement();
        assertTrue(secondChild == parent.getChild(2));

        applyChanges(ChangeUtil.listRemove(itemsId, 0));

        assertEquals(2, parent.getChildCount());
        assertSame(parent, secondChild.getParentElement());
        assertNull(firstChild.getParentElement());

        // Click and verify that the click handler has updated the right objects
        NativeEvent event = Document.get().createClickEvent(0, 1, 2, 3, 4,
                false, false, false, false);
        secondChild.dispatchEvent(event);

        // Use as JsonObject for convenient property access
        JsJsonObject model = updater.getNode(3).getProxy().cast();
        assertEquals(1, (int) model.getNumber("foo"));

        JsJsonObject item = updater.getNode(5).getProxy().cast();
        assertEquals(2, (int) item.getNumber("foo"));
    }

    public void testOverrideNode() {
        String json = "{'type': 'BoundElementTemplate', 'tag':'span',"
                + "'defaultAttributes': {'foo': 'bar'},"
                + "'modelStructure': ['value', 'conditional']}";
        JsonObject template = Json.parse(json.replace('\'', '"'));

        int templateId = 1;
        int templateNodeId = 3;
        int overrideNodeId = 4;
        int childrenId = 5;

        applyTemplate(templateId, template);

        applyChanges(
                ChangeUtil.putList(containerElementId, "CHILDREN", childrenId),
                ChangeUtil.listInsertNode(childrenId, 0, templateNodeId),
                ChangeUtil.put(templateNodeId, "TEMPLATE",
                        Json.create(templateId)));

        Element templateElement = updater.getRootElement()
                .getFirstChildElement();
        assertEquals("bar", templateElement.getAttribute("foo"));
        assertNull(templateElement.getPropertyString("asdf"));

        applyChanges(
                ChangeUtil.putOverrideNode(templateNodeId, templateId,
                        overrideNodeId),
                ChangeUtil.put(overrideNodeId, "asdf", "attrValue"));

        assertEquals("attrValue", templateElement.getPropertyString("asdf"));
    }

    public void testMoveNode() {
        String json = "{'type': 'BoundElementTemplate', 'tag':'span',"
                + "'attributeBindings': {'value1': 'child1.value', 'value2': 'child2.value'},"
                + "'events': {'click': ['var temp = model.child2; model.child2 = model.child1; model.child1 = temp;']},"
                + "'modelStructure': [{'child1': ['value'], 'child2': ['value']}]}";
        JsonObject template = Json.parse(json.replace('\'', '"'));

        int templateId = 1;
        int templateNodeId = 3;
        int child1NodeId = 4;
        int child2NodeId = 6;
        int childrenId = 5;

        applyTemplate(templateId, template);

        applyChanges(ChangeUtil.put(child1NodeId, "value", "childValue1"),
                ChangeUtil.put(child2NodeId, "value", "childValue2"),
                ChangeUtil.put(templateNodeId, "TEMPLATE",
                        Json.create(templateId)),
                ChangeUtil.putMap(templateNodeId, "child1", child1NodeId),
                ChangeUtil.putMap(templateNodeId, "child2", child2NodeId),
                ChangeUtil.putList(containerElementId, "CHILDREN", childrenId),
                ChangeUtil.listInsertNode(childrenId, 0, templateNodeId));

        Element templateElement = updater.getRootElement()
                .getFirstChildElement();
        assertEquals("SPAN", templateElement.getTagName());
        assertEquals("childValue1",
                templateElement.getPropertyString("value1"));
        assertEquals("childValue2",
                templateElement.getPropertyString("value2"));

        NativeEvent event = Document.get().createClickEvent(0, 1, 2, 3, 4,
                false, false, false, false);
        templateElement.dispatchEvent(event);

        assertEquals("childValue2",
                templateElement.getPropertyString("value1"));
        assertEquals("childValue1",
                templateElement.getPropertyString("value2"));
    }

    public void testScriptTag() {
        String scriptContentJson = "{'type': 'StaticTextTemplate', 'content':'window.scriptLoaded = 5'}";
        applyTemplate(1, Json.parse(scriptContentJson.replace('\'', '"')));

        String scriptTagJson = "{'type': 'BoundElementTemplate', 'tag':'script',"
                + "'defaultAttributes': {'type': 'text/javascript'},"
                + "'children': [1]}";

        applyTemplate(2, Json.parse(scriptTagJson.replace('\'', '"')));

        String parentTagJson = "{'type': 'BoundElementTemplate', 'tag':'div',"
                + "'children': [2]}";
        applyTemplate(3, Json.parse(parentTagJson.replace('\'', '"')));

        assertFalse(isScriptLoaded());

        applyChanges(ChangeUtil.put(3, "TEMPLATE", Json.create(3)),
                ChangeUtil.putList(containerElementId, "CHILDREN", 4),
                ChangeUtil.listInsertNode(4, 0, 3));

        assertTrue(isScriptLoaded());
    }

    public void testBoundExpressions() {
        int parentTemplateId = 1;
        int childTemplateId = 2;

        int childId = 3;
        int childrenId = 4;

        String childTemplate = "{'type': 'DynamicTextTemplate', 'binding':'value * 2'}";
        applyTemplate(childTemplateId,
                Json.parse(childTemplate.replace('\'', '"')));

        String parentTemplate = "{'type': 'BoundElementTemplate', 'tag':'span',"
                + "'attributeBindings': {'foo': 'value + 1'},"
                + "'classPartBindings': {'foo': 'value  % 2 == 0'},"
                + "'children': [" + childTemplateId + "]}";
        applyTemplate(parentTemplateId,
                Json.parse(parentTemplate.replace('\'', '"')));

        applyChanges(
                ChangeUtil.put(childId, "TEMPLATE",
                        Json.create(parentTemplateId)),
                ChangeUtil.put(childId, "value", Json.create(4)),
                ChangeUtil.putList(containerElementId, "CHILDREN", childrenId),
                ChangeUtil.listInsertNode(childrenId, 0, childId));

        Element templateElement = updater.getRootElement()
                .getFirstChildElement();

        assertEquals(5, templateElement.getPropertyInt("foo"));
        assertTrue(templateElement.hasClassName("foo"));
        assertEquals("8", templateElement.getInnerText());

        applyChanges(ChangeUtil.put(childId, "value", Json.create(5)));

        assertEquals(6, templateElement.getPropertyInt("foo"));
        assertFalse(templateElement.hasClassName("foo"));
        assertEquals("10", templateElement.getInnerText());
    }

    public void testArrayLengthBinding() {
        int templateId = 1;

        String template = "{'type': 'BoundElementTemplate', 'tag':'span',"
                + "'attributeBindings': {'foo': 'list.length'}}";

        applyTemplate(templateId, Json.parse(template.replace('\'', '"')));

        int childId = 3;
        int listId = 4;
        int childrenId = 5;

        applyChanges(
                ChangeUtil.put(childId, "TEMPLATE", Json.create(templateId)),
                ChangeUtil.putList(childId, "list", listId),
                ChangeUtil.putList(containerElementId, "CHILDREN", childrenId),
                ChangeUtil.listInsertNode(childrenId, 0, childId));

        Element templateElement = updater.getRootElement()
                .getFirstChildElement();

        assertEquals(0, templateElement.getPropertyInt("foo"));

        applyChanges(ChangeUtil.listInsert(listId, 0, "asdf"));

        assertEquals(1, templateElement.getPropertyInt("foo"));
    }

    private static native boolean isScriptLoaded()
    /*-{
        return $wnd.scriptLoaded === 5;
    }-*/;
}
