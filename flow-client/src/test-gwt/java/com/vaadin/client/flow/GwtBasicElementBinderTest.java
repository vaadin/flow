/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.client.flow;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.vaadin.client.ExistingElementMap;
import com.vaadin.client.PolymerUtils;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.binding.Binder;
import com.vaadin.client.flow.binding.SimpleElementBindingStrategy;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.client.flow.util.NativeFunction;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.NodeProperties;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.html.HTMLCollection;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class GwtBasicElementBinderTest extends GwtPropertyElementBinderTest {

    private NodeMap attributes;
    private NodeMap elementData;
    private NodeList children;
    private MapProperty titleProperty;
    private MapProperty idAttribute;

    private int nextId;

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();

        attributes = node.getMap(NodeFeatures.ELEMENT_ATTRIBUTES);
        elementData = node.getMap(NodeFeatures.ELEMENT_DATA);
        children = node.getList(NodeFeatures.ELEMENT_CHILDREN);

        titleProperty = properties.getProperty("title");
        idAttribute = attributes.getProperty("id");

        nextId = node.getId() + 2;

        element = Browser.getDocument().createElement("div");
    }

    public void testBindExistingProperty() {
        titleProperty.setValue("foo");

        Binder.bind(node, element);

        Reactive.flush();

        assertEquals("foo", element.getTitle());
    }

    public void testBindNewProperty() {
        Binder.bind(node, element);

        properties.getProperty("lang").setValue("foo");

        Reactive.flush();

        assertEquals("foo", element.getLang());
    }

    public void testBindBeforeFlush() {
        titleProperty.setValue("foo");

        Binder.bind(node, element);

        assertEquals("foo", element.getTitle());
    }

    public void testSetBeforeFlush() {
        Binder.bind(node, element);

        titleProperty.setValue("foo");

        assertEquals("null", element.getTitle());
    }

    public void testUnbindBeforeFlush() {
        Binder.bind(node, element);

        titleProperty.setValue("foo");
        idAttribute.setValue("foo");

        node.unregister();

        titleProperty.setValue("bar");
        idAttribute.setValue("bar");
        attributes.getProperty("lang").setValue("newValue");

        Reactive.flush();

        assertEquals("null", element.getTitle());
        assertEquals("", element.getId());
        assertEquals("", element.getLang());
    }

    public void testUnbindAfterFlush() {
        Binder.bind(node, element);

        titleProperty.setValue("foo");
        idAttribute.setValue("foo");

        Reactive.flush();

        node.unregister();

        titleProperty.setValue("bar");
        idAttribute.setValue("bar");
        attributes.getProperty("lang").setValue("newValue");

        Reactive.flush();

        assertEquals("foo", element.getTitle());
        assertEquals("foo", element.getId());
        assertEquals("", element.getLang());
    }

    public void testRemoveArbitraryProperty() {
        MapProperty foo = properties.getProperty("foo");
        foo.setValue("bar");

        Binder.bind(node, element);

        Reactive.flush();

        assertTrue(WidgetUtil.hasOwnJsProperty(element, "foo"));

        foo.removeValue();

        Reactive.flush();

        assertFalse(WidgetUtil.hasOwnJsProperty(element, "foo"));
    }

    public void testRemoveBuiltInProperty() {
        titleProperty.setValue("foo");

        Binder.bind(node, element);

        Reactive.flush();

        titleProperty.removeValue();

        Reactive.flush();

        // Properties inherited from e.g. Element can't be removed
        // Assigning null to title produces "null"
        assertEquals("null", element.getTitle());
    }

    public void testBindWrongTagThrows() {
        elementData.getProperty(NodeProperties.TAG).setValue("span");

        try {
            Binder.bind(node, element);
            fail("Should have thrown");
        } catch (AssertionError expected) {
        }
    }

    public void testBindRightTagOk() {
        elementData.getProperty(NodeProperties.TAG).setValue("div");

        Binder.bind(node, element);
    }

    public void testBindExistingAttribute() {
        idAttribute.setValue("foo");

        Binder.bind(node, element);

        Reactive.flush();

        assertEquals("foo", element.getId());
    }

    public void testBindNewAttribute() {
        Binder.bind(node, element);

        attributes.getProperty("lang").setValue("foo");

        Reactive.flush();

        assertEquals("foo", element.getLang());
    }

    public void testBindAttributeWithoutFlush() {
        idAttribute.setValue("foo");

        Binder.bind(node, element);

        assertEquals("foo", element.getId());
    }

    public void testSetAttributeWithoutFlush() {
        Binder.bind(node, element);

        idAttribute.setValue("foo");

        assertEquals("", element.getId());
    }

    public void testRemoveAttribute() {
        Binder.bind(node, element);

        idAttribute.setValue("foo");

        Reactive.flush();

        idAttribute.removeValue();

        Reactive.flush();

        assertEquals("", element.getId());
    }

    private StateNode createChildNode(String id, String tag) {
        StateNode childNode = new StateNode(nextId++, node.getTree());
        node.getTree().registerNode(childNode);

        childNode.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.TAG).setValue(tag);
        if (id != null) {
            childNode.getMap(NodeFeatures.ELEMENT_ATTRIBUTES).getProperty("id")
                    .setValue(id);
        }

        return childNode;
    }

    private StateNode createChildNode(String id) {
        return createChildNode(id, "span");
    }

    public void testAddChild() {
        Binder.bind(node, element);

        StateNode childNode = createChildNode("child");

        children.add(0, childNode);

        Reactive.flush();

        assertEquals(element.getChildElementCount(), 1);

        Element childElement = element.getFirstElementChild();

        assertEquals("SPAN", childElement.getTagName());
        assertEquals("child", childElement.getId());
    }

    public void testVirtualChild() {
        Binder.bind(node, element);

        StateNode childNode = createChildNode("child");

        NodeMap elementData = childNode.getMap(NodeFeatures.ELEMENT_DATA);
        JsonObject object = Json.createObject();
        object.put(NodeProperties.TYPE, NodeProperties.IN_MEMORY_CHILD);
        elementData.getProperty(NodeProperties.PAYLOAD).setValue(object);

        NodeList virtialChildren = node.getList(NodeFeatures.VIRTUAL_CHILDREN);
        virtialChildren.add(0, childNode);

        Reactive.flush();

        assertEquals(element.getChildElementCount(), 0);

        Element childElement = (Element) childNode.getDomNode();
        assertEquals("SPAN", childElement.getTagName());
        assertEquals("child", childElement.getId());
    }

    public void testInsertChild() {
        Binder.bind(node, element);

        createAndAppendElementToShadowRoot(element, null, "div");

        StateNode childNode = createChildNode("first");

        // With one client side element, insert at 0 will translate to index 1
        children.add(0, childNode);

        // <div/><span>first</span>
        Reactive.flush();

        assertEquals(2, element.getChildElementCount());

        Element childElement = (Element) element.getChildren().at(0);
        assertEquals("DIV", childElement.getTagName());

        childElement = (Element) element.getChildren().at(1);
        assertEquals("SPAN", childElement.getTagName());
        assertEquals("first", childElement.getId());

        childNode = createChildNode("second", "a");
        // Insert at the first position (which will be translated to after the
        // client-side nodes)
        children.add(0, childNode);

        // <div/><a>second</a><span>first</span>
        Reactive.flush();

        assertEquals(3, element.getChildElementCount());

        childElement = (Element) element.getChildren().at(0);
        assertEquals("DIV", childElement.getTagName());

        childElement = (Element) element.getChildren().at(1);
        assertEquals("A", childElement.getTagName());
        assertEquals("second", childElement.getId());

        Element existingChild2 = Browser.getDocument().createElement("div");
        element.insertBefore(existingChild2,
                (Element) element.getChildren().at(1));

        childNode = createChildNode("third", "h1");
        // Insert at the second position.
        children.add(1, childNode);

        // <div/><div/><a>second</a><h1>third</h1><span>first</span>
        Reactive.flush();

        assertEquals(element.getChildElementCount(), 5);

        childElement = (Element) element.getChildren().at(1);
        assertEquals("DIV", childElement.getTagName());

        childElement = (Element) element.getChildren().at(3);
        assertEquals("H1", childElement.getTagName());
        assertEquals("third", childElement.getId());

        childNode = createChildNode("fourth", "br");
        // Insert after the last bound node
        children.add(3, childNode);

        // <div/><div/><a>second</a><h1>third</h1><span>first</span><br>fourth</br>
        Reactive.flush();

        assertEquals(6, element.getChildElementCount());

        childElement = (Element) element.getChildren().at(5);

        // Element should be before the client side element and after the bound
        // node
        assertEquals("BR", childElement.getTagName());
        assertEquals("fourth", childElement.getId());
    }

    public void testInsertChildAfterExistingChildren() {
        Element existingChild1 = Browser.getDocument().createElement("span");
        Element existingChild2 = Browser.getDocument().createElement("span");
        element.appendChild(existingChild1);
        element.appendChild(existingChild2);

        Binder.bind(node, element);
        Reactive.flush();

        assertEquals(2, element.getChildElementCount());

        StateNode childNode = createChildNode("first", "div");
        children.add(0, childNode);

        Reactive.flush();
        assertEquals(3, element.getChildElementCount());

        Element childElement = (Element) element.getChildren().at(0);
        assertEquals("SPAN", childElement.getTagName());

        childElement = (Element) element.getChildren().at(1);
        assertEquals("SPAN", childElement.getTagName());

        childElement = (Element) element.getChildren().at(2);
        assertEquals("DIV", childElement.getTagName());
    }

    /**
     * This is important test which checks that index of insertion is calculated
     * correctly.
     * <p>
     * The insertion index is calculated based on the {@link StateNode}s
     * positions. But it might be that nodes in the {@link StateNode}s list
     * don't have yet DOM node assigned (created and bound). In this case such
     * nodes should not cause any issues and insertion index should be correctly
     * calculated.
     */
    public void testInsertChild_recalculateIndex() {
        Binder.bind(node, element);

        createAndAppendElementToShadowRoot(element, null, "div");

        // The order is important: some StateNodes during event handling don't
        // have yet DOM node with this order
        children.add(0, createChildNode("first"));
        children.add(1, createChildNode("second"));
        children.add(0, createChildNode("third"));

        Reactive.flush();

        assertEquals(4, element.getChildElementCount());

        HTMLCollection children = element.getChildren();

        // check that order is the correct one
        Element childElement = (Element) children.at(0);
        assertEquals("DIV", childElement.getTagName());

        childElement = (Element) children.at(1);
        assertEquals("third", childElement.getId());

        childElement = (Element) children.at(2);
        assertEquals("first", childElement.getId());

        childElement = (Element) children.at(3);
        assertEquals("second", childElement.getId());
    }

    public void testRemoveChild() {
        Binder.bind(node, element);

        StateNode childNode = createChildNode(null);

        children.add(0, childNode);

        Reactive.flush();

        assertEquals(1, element.getChildElementCount());
        Element childElement = element.getFirstElementChild();

        children.splice(0, 1);

        Reactive.flush();

        assertEquals(0, element.getChildElementCount());
        assertNull(childElement.getParentElement());
    }

    public void testClearChildren() {
        element.appendChild(Browser.getDocument().createAnchorElement());
        element.appendChild(Browser.getDocument().createHRElement());

        StateNode childNode = createChildNode("foo");
        children.add(0, childNode);

        children.clear();

        childNode = createChildNode("bar");
        children.add(0, childNode);

        Binder.bind(node, element);

        assertEquals(1, element.getChildElementCount());
        Element childElement = element.getFirstElementChild();

        assertEquals("span",
                childElement.getTagName().toLowerCase(Locale.ENGLISH));

        Reactive.flush();
        assertEquals("bar", childElement.getAttribute("id"));

        children.splice(0, 1);

        Reactive.flush();

        assertEquals(0, element.getChildElementCount());
        assertNull(childElement.getParentElement());
    }

    public void testRemoveChildPosition() {
        Binder.bind(node, element);

        StateNode childNode = createChildNode("child");
        children.add(0, childNode);
        Reactive.flush();

        Element firstChildElement = element.getFirstElementChild();

        // Add an "unofficial" child to mess with index computations
        Element extraChild = Browser.getDocument().createElement("img");
        element.insertBefore(extraChild, firstChildElement);

        children.splice(0, 1);
        Reactive.flush();

        elemental.dom.NodeList childNodes = element.getChildNodes();

        assertEquals(1, childNodes.length());

        assertSame(extraChild, childNodes.item(0));
        assertNull(firstChildElement.getParentElement());
    }

    public void testAddRemoveMultiple() {
        Binder.bind(node, element);

        children.splice(0, 0,
                JsCollections.array(createChildNode("1"), createChildNode("2"),
                        createChildNode("3"), createChildNode("4")));

        Reactive.flush();

        assertEquals(4, element.getChildElementCount());

        Element child1 = (Element) element.getChildren().item(0);
        Element child2 = (Element) element.getChildren().item(1);
        Element child3 = (Element) element.getChildren().item(2);
        Element child4 = (Element) element.getChildren().item(3);

        assertEquals("1", child1.getId());
        assertEquals("2", child2.getId());
        assertEquals("3", child3.getId());
        assertEquals("4", child4.getId());

        children.splice(1, 2);

        Reactive.flush();

        assertEquals(2, element.getChildElementCount());

        assertSame(child1, element.getChildNodes().item(0));
        assertSame(child4, element.getChildNodes().item(1));
    }

    public void testAddBeforeSetTag() {
        Binder.bind(node, element);

        StateNode childNode = new StateNode(nextId++, node.getTree());

        children.add(0, childNode);

        childNode.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.TAG).setValue("span");

        // Should not throw
        Reactive.flush();

        assertEquals(1, element.getChildElementCount());
    }

    public void testReAddNode() {
        Binder.bind(node, element);

        StateNode childToReadd = createChildNode("2");
        children.splice(0, 0, JsCollections.array(createChildNode("1"),
                childToReadd, createChildNode("3")));

        Reactive.flush();

        Node node = childToReadd.getDomNode();

        assertEquals(3, element.getChildElementCount());

        children.splice(1, 1);
        children.splice(1, 0, JsCollections.array(childToReadd));

        Reactive.flush();

        StateNode statNode = (StateNode) children.get(1);
        assertSame(childToReadd, statNode);
        assertSame(node, statNode.getDomNode());
    }

    public void testEventFired() {
        Binder.bind(node, element);

        // User agent is "Mozilla/5.0..."
        String booleanExpression = "window.navigator.userAgent[0] === 'M'";
        String numberExpression = "event.button";
        String stringExpression = "element.tagName";

        String trueFilter = "true";
        String falseFilter = "false";
        String tagNameFilter = "element.tagName == 'DIV'";

        String constantPoolKey = "expressionsKey";

        JsonObject expressions = Json.createObject();
        // Data expressions
        expressions.put(booleanExpression, false);
        expressions.put(numberExpression, false);
        expressions.put(stringExpression, false);

        // Filter expressions

        expressions.put(trueFilter, true);
        expressions.put(falseFilter, true);
        expressions.put(tagNameFilter, true);

        addToConstantPool(constantPoolKey, expressions);

        node.getMap(NodeFeatures.ELEMENT_LISTENERS).getProperty("click")
                .setValue(constantPoolKey);
        Reactive.flush();
        Browser.getDocument().getBody().appendChild(element);

        element.click();

        assertEquals(1, tree.collectedNodes.length());

        assertSame(node, tree.collectedNodes.get(0));

        JsonObject eventData = tree.collectedEventData.get(0);

        // 3 data expressions and 3 filter expressions
        assertEquals(6, eventData.keys().length);

        assertEquals(JsonType.NUMBER,
                eventData.get(numberExpression).getType());
        assertEquals("DIV", eventData.getString(stringExpression));
        assertEquals(true, eventData.getBoolean(booleanExpression));

        assertEquals(true, eventData.getBoolean(tagNameFilter));
        assertEquals(true, eventData.getBoolean(trueFilter));
        assertEquals(false, eventData.getBoolean(falseFilter));
    }

    public void testFilterPreventsEvent() {
        Binder.bind(node, element);

        String constantPoolKey = "expressionsKey";

        JsonObject expressions = Json.createObject();
        expressions.put("false", true);

        addToConstantPool(constantPoolKey, expressions);

        node.getMap(NodeFeatures.ELEMENT_LISTENERS).getProperty("click")
                .setValue(constantPoolKey);
        Reactive.flush();
        Browser.getDocument().getBody().appendChild(element);

        element.click();

        assertEquals(0, tree.collectedNodes.length());
    }

    public void testEventFiredWithNoFilters() {
        Binder.bind(node, element);

        String constantPoolKey = "expressionsKey";

        JsonObject expressions = Json.createObject();
        // Expression is not used as a filter
        expressions.put("false", false);

        addToConstantPool(constantPoolKey, expressions);

        node.getMap(NodeFeatures.ELEMENT_LISTENERS).getProperty("click")
                .setValue(constantPoolKey);
        Reactive.flush();
        Browser.getDocument().getBody().appendChild(element);

        element.click();

        assertEquals(1, tree.collectedNodes.length());
    }

    private void addToConstantPool(String key, JsonValue value) {
        addToConstantPool(constantPool, key, value);
    }

    public static void addToConstantPool(ConstantPool constantPool, String key,
            JsonValue value) {
        // https://github.com/gwtproject/gwt/issues/9225
        value = Json.instance().parse(value.toJson());

        JsonObject update = Json.createObject();
        update.put(key, value);
        constantPool.importFromJson(update);
    }

    public void testRemovedEventNotFired() {
        Binder.bind(node, element);

        MapProperty clickEvent = node.getMap(NodeFeatures.ELEMENT_LISTENERS)
                .getProperty("click");
        clickEvent.setValue(Double.valueOf(1));

        Reactive.flush();

        clickEvent.removeValue();

        Reactive.flush();

        element.click();

        assertEquals(0, tree.collectedNodes.length());
    }

    public void testAddTextNode() {
        Binder.bind(node, element);

        StateNode textNode = new StateNode(nextId++, node.getTree());
        MapProperty textProperty = textNode.getMap(NodeFeatures.TEXT_NODE)
                .getProperty(NodeProperties.TEXT);

        textProperty.setValue("foo");

        node.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, textNode);
        Reactive.flush();

        assertEquals("foo", element.getTextContent());

        textProperty.setValue("bar");
        assertEquals("foo", element.getTextContent());

        Reactive.flush();
        assertEquals("bar", element.getTextContent());
    }

    public void testRemoveTextNode() {
        Binder.bind(node, element);

        StateNode textNode = new StateNode(nextId++, node.getTree());
        textNode.getMap(NodeFeatures.TEXT_NODE).getProperty(NodeProperties.TEXT)
                .setValue("foo");

        node.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, textNode);
        Reactive.flush();

        assertEquals(1, element.getChildNodes().getLength());

        node.getList(NodeFeatures.ELEMENT_CHILDREN).splice(0, 1);

        Reactive.flush();

        assertEquals(0, element.getChildNodes().getLength());
    }

    public void testAddClassesBeforeBind() {
        node.getList(NodeFeatures.CLASS_LIST).add(0, "foo");

        Binder.bind(node, element);

        assertEquals("foo", element.getClassName());
    }

    public void testAddClassesAfterBind() {
        Binder.bind(node, element);

        node.getList(NodeFeatures.CLASS_LIST).add(0, "foo");

        assertEquals("foo", element.getClassName());
    }

    public void testRemoveClasses() {
        Binder.bind(node, element);

        node.getList(NodeFeatures.CLASS_LIST).splice(0, 0,
                JsCollections.array("one", "two", "three"));

        assertEquals("one two three", element.getClassName());

        node.getList(NodeFeatures.CLASS_LIST).splice(1, 1);

        assertEquals("one three", element.getClassName());
    }

    public void testAddClassesAfterUnbind() {
        Binder.bind(node, element);

        node.getList(NodeFeatures.CLASS_LIST).add(0, "foo");

        node.unregister();

        node.getList(NodeFeatures.CLASS_LIST).add(0, "bar");

        assertEquals("foo", element.getClassName());
    }

    public void testAddStylesBeforeBind() {
        polyfillStyleSetProperty(element);
        node.getMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES).getProperty("color")
                .setValue("green");

        Reactive.flush();
        Binder.bind(node, element);

        Reactive.flush();
        assertEquals("green", element.getStyle().getColor());
    }

    public void testAddStylesAfterBind() {
        polyfillStyleSetProperty(element);
        Binder.bind(node, element);
        node.getMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES).getProperty("color")
                .setValue("green");

        Reactive.flush();
        assertEquals("green", element.getStyle().getColor());
    }

    public void testRemoveStyles() {
        polyfillStyleSetProperty(element);
        Binder.bind(node, element);

        NodeMap styleMap = node.getMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES);
        styleMap.getProperty("background").setValue("blue");
        styleMap.getProperty("color").setValue("white");

        Reactive.flush();
        assertEquals("background: blue;color: white;",
                element.getAttribute("style"));

        styleMap.getProperty("color").removeValue();

        Reactive.flush();
        assertEquals("background: blue;", element.getAttribute("style"));
    }

    private native void polyfillStyleSetProperty(Element element)
    /*-{
         // This polyfills just enough to make the tests pass and nothing else
         element.style.__proto__.setProperty = function(key,value) {
             var newValue = element.getAttribute("style");
             if (!newValue) {
                 newValue = "";
             }
             else if (!newValue.endsWith(";")) {
                 newValue +=";"
             }
             element.setAttribute("style", newValue + key+": "+value+";");
         };
     }-*/;

    public void testAddStylesAfterUnbind() {
        polyfillStyleSetProperty(element);
        Binder.bind(node, element);

        NodeMap styleMap = node.getMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES);

        styleMap.getProperty("color").setValue("red");
        Reactive.flush();

        node.unregister();

        styleMap.getProperty("color").setValue("blue");
        styleMap.getProperty("font-size").setValue("12px");

        Reactive.flush();
        assertEquals("color: red;", element.getAttribute("style"));
    }

    public void testAttachExistingElement() {
        Binder.bind(node, element);

        StateNode childNode = createChildNode("child");

        String tag = (String) childNode.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.TAG).getValue();

        ExistingElementMap existingElementMap = node.getTree().getRegistry()
                .getExistingElementMap();

        // create and add an existing element
        Element span = Browser.getDocument().createElement(tag);
        element.appendChild(span);

        existingElementMap.add(childNode.getId(), span);

        children.add(0, childNode);

        Reactive.flush();

        // nothing has changed: no new child
        assertEquals("No new child should appear in the element", 1,
                element.getChildElementCount());

        Element childElement = element.getFirstElementChild();

        assertEquals(tag,
                childElement.getTagName().toLowerCase(Locale.ENGLISH));
        assertSame(span, childElement);
        assertEquals("child", childElement.getId());
        assertNull(existingElementMap.getElement(childNode.getId()));
    }

    public void testPropertyValueHasPrototypeMethods() {
        NodeMap map = new NodeMap(0, new StateNode(0, new StateTree(null)));
        JsonObject object = Json.createObject();
        object.put("name", "bar");
        map.getProperty("foo").setValue(object);
        assertTrue(map.hasPropertyValue("foo"));
        String toString = getToString(map.getProperty("foo").getValue());
        assertEquals("[object Object]", toString);
    }

    public void testVirtualBindChild_wrongTag_searchById() {
        Element shadowRootElement = addShadowRootElement(element);

        String childId = "childElement";

        StateNode child = createChildNode(childId, "a");
        addVirtualChild(node, child, NodeProperties.INJECT_BY_ID,
                Json.create(childId));

        Element elementWithDifferentId = createAndAppendElementToShadowRoot(
                shadowRootElement, "otherId", "div");
        assertNotSame(
                "Element added to shadow root should not have same id as virtual child node",
                childId, elementWithDifferentId.getId());

        Binder.bind(node, element);

        Reactive.flush();

        assertEquals(
                "Unexpected 'sendExistingElementWithIdAttachToServer' method call number",
                4, tree.existingElementRpcArgs.size());
        assertEquals(
                "Unexpected node argument in the 'sendExistingElementWithIdAttachToServer' method call",
                node, tree.existingElementRpcArgs.get(0));
        assertEquals(
                "Unexpected requested node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                child.getId(), tree.existingElementRpcArgs.get(1));
        assertEquals(
                "Unexpected attached node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                -1, tree.existingElementRpcArgs.get(2));
        assertEquals(
                "Unexpected identifier value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                childId, tree.existingElementRpcArgs.get(3));
    }

    public void testVirtualBindChild_noCorrespondingElementInShadowRoot_searchById() {
        Element shadowRootElement = addShadowRootElement(element);

        String childId = "childElement";
        String tag = "a";

        StateNode child = createChildNode(childId, tag);
        addVirtualChild(node, child, NodeProperties.INJECT_BY_ID,
                Json.create(childId));

        Element elementWithDifferentId = createAndAppendElementToShadowRoot(
                shadowRootElement, "otherId", tag);
        assertNotSame(
                "Element added to shadow root should not have same id as virtual child node",
                childId, elementWithDifferentId.getId());

        Binder.bind(node, element);

        Reactive.flush();

        assertEquals(
                "Unexpected 'sendExistingElementWithIdAttachToServer' method call number",
                4, tree.existingElementRpcArgs.size());
        assertEquals(
                "Unexpected requested node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                node, tree.existingElementRpcArgs.get(0));
        assertEquals(
                "Unexpected requested node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                child.getId(), tree.existingElementRpcArgs.get(1));
        assertEquals(
                "Unexpected attached node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                -1, tree.existingElementRpcArgs.get(2));
        assertEquals(
                "Unexpected identifier value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                childId, tree.existingElementRpcArgs.get(3));
    }

    public void testVirtualBindChild_wrongTag_searchByIndicesPath() {
        Element shadowRootElement = addShadowRootElement(element);

        String childTagName = "span";

        StateNode child = createChildNode(null, childTagName);

        Binder.bind(node, element);

        JsonArray path = Json.createArray();
        path.set(0, 0);
        addVirtualChild(node, child, NodeProperties.TEMPLATE_IN_TEMPLATE, path);

        Element elementWithDifferentTag = createAndAppendElementToShadowRoot(
                shadowRootElement, null, "div");
        assertNotSame(
                "Element added to shadow root should not have same tag name as virtual child node",
                childTagName, elementWithDifferentTag.getTagName());

        Reactive.flush();

        assertEquals(
                "Unexpected 'sendExistingElementWithIdAttachToServer' method call number",
                4, tree.existingElementRpcArgs.size());
        assertEquals(
                "Unexpected requested node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                node, tree.existingElementRpcArgs.get(0));
        assertEquals(
                "Unexpected requested node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                child.getId(), tree.existingElementRpcArgs.get(1));
        assertEquals(
                "Unexpected attached node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                -1, tree.existingElementRpcArgs.get(2));
        assertEquals(
                "Unexpected identifier value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                null, tree.existingElementRpcArgs.get(3));
    }

    public void testBindVirtualChild_noCorrespondingElementInShadowRoot_searchByIndicesPath() {
        Element shadowRootElement = addShadowRootElement(element);

        String childTagName = "span";

        StateNode child = createChildNode(null, childTagName);

        Binder.bind(node, element);

        JsonArray path = Json.createArray();
        path.set(0, 1);
        addVirtualChild(node, child, NodeProperties.TEMPLATE_IN_TEMPLATE, path);

        Element elementWithDifferentTag = createAndAppendElementToShadowRoot(
                shadowRootElement, null, childTagName);
        assertNotSame(
                "Element added to shadow root should not have same tag name as virtual child node",
                childTagName, elementWithDifferentTag.getTagName());

        Reactive.flush();

        assertEquals(
                "Unexpected 'sendExistingElementWithIdAttachToServer' method call number",
                4, tree.existingElementRpcArgs.size());
        assertEquals(
                "Unexpected requested node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                node, tree.existingElementRpcArgs.get(0));
        assertEquals(
                "Unexpected requested node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                child.getId(), tree.existingElementRpcArgs.get(1));
        assertEquals(
                "Unexpected attached node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                -1, tree.existingElementRpcArgs.get(2));
        assertEquals(
                "Unexpected identifier value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                null, tree.existingElementRpcArgs.get(3));
    }

    public void testBindVirtualChild_doubleAttachRequest_searchByIndicesPath() {
        Element shadowRootElement = addShadowRootElement(element);

        StateNode childNode = createChildNode(null, element.getTagName());
        StateNode sameAttachDataChild = createChildNode(null,
                element.getTagName());

        JsonArray path = Json.createArray();
        path.set(0, 0);

        Binder.bind(node, element);

        addVirtualChild(node, childNode, NodeProperties.TEMPLATE_IN_TEMPLATE,
                path);

        createAndAppendElementToShadowRoot(shadowRootElement, null,
                element.getTagName());

        Reactive.flush();

        addVirtualChild(node, sameAttachDataChild,
                NodeProperties.TEMPLATE_IN_TEMPLATE, path);

        Reactive.flush();

        assertEquals(
                "Unexpected 'sendExistingElementWithIdAttachToServer' method call number",
                4, tree.existingElementRpcArgs.size());
        assertEquals(
                "Unexpected requested node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                node, tree.existingElementRpcArgs.get(0));
        assertEquals(
                "Unexpected requested node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                sameAttachDataChild.getId(),
                tree.existingElementRpcArgs.get(1));
        assertEquals(
                "Unexpected attached node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                childNode.getId(), tree.existingElementRpcArgs.get(2));
        assertEquals(
                "Unexpected identifier value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                null, tree.existingElementRpcArgs.get(3));
    }

    public void testBindVirtualChild_doubleAttachRequest_searchById() {
        Element shadowRootElement = addShadowRootElement(element);

        String id = "@id";

        StateNode childNode = createChildNode(id, element.getTagName());
        StateNode sameAttachDataChild = createChildNode(id,
                element.getTagName());

        Binder.bind(node, element);

        addVirtualChild(node, childNode, NodeProperties.INJECT_BY_ID,
                Json.create(id));

        createAndAppendElementToShadowRoot(shadowRootElement, id,
                element.getTagName());

        Reactive.flush();

        addVirtualChild(node, sameAttachDataChild, NodeProperties.INJECT_BY_ID,
                Json.create(id));

        Reactive.flush();

        assertEquals(
                "Unexpected 'sendExistingElementWithIdAttachToServer' method call number",
                4, tree.existingElementRpcArgs.size());
        assertEquals(
                "Unexpected requested node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                node, tree.existingElementRpcArgs.get(0));
        assertEquals(
                "Unexpected requested node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                sameAttachDataChild.getId(),
                tree.existingElementRpcArgs.get(1));
        assertEquals(
                "Unexpected attached node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                childNode.getId(), tree.existingElementRpcArgs.get(2));
        assertEquals(
                "Unexpected identifier value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                id, tree.existingElementRpcArgs.get(3));
    }

    public void testBindVirtualChild_existingShadowRootChildren_searchById() {
        addShadowRootElement(element);

        String id = "@id";

        StateNode childNode = createChildNode(id, element.getTagName());
        StateNode virtualChild = createChildNode(id, element.getTagName());

        StateNode shadowRoot = createAndAttachShadowRootNode();

        shadowRoot.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, childNode);

        Binder.bind(node, element);

        Reactive.flush();

        JsonObject obj = Json.createObject();
        WidgetUtil.setJsProperty(obj, id.toString(), childNode.getDomNode());
        WidgetUtil.setJsProperty(element, "$", obj);

        addVirtualChild(node, virtualChild, NodeProperties.INJECT_BY_ID,
                Json.create(id));

        Reactive.flush();

        assertEquals(
                "Unexpected 'sendExistingElementWithIdAttachToServer' method call number",
                4, tree.existingElementRpcArgs.size());
        assertEquals(
                "Unexpected requested node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                node, tree.existingElementRpcArgs.get(0));
        assertEquals(
                "Unexpected requested node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                virtualChild.getId(), tree.existingElementRpcArgs.get(1));
        assertEquals(
                "Unexpected attached node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                childNode.getId(), tree.existingElementRpcArgs.get(2));
        assertEquals(
                "Unexpected identifier value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                id, tree.existingElementRpcArgs.get(3));
    }

    public void testBindVirtualChild_existingShadowRootChildren_searchByIndicesPath() {
        addShadowRootElement(element);

        StateNode childNode = createChildNode(null, element.getTagName());
        StateNode virtualChild = createChildNode(null, element.getTagName());

        StateNode shadowRoot = createAndAttachShadowRootNode();

        shadowRoot.getList(NodeFeatures.ELEMENT_CHILDREN).add(0, childNode);

        Binder.bind(node, element);

        Reactive.flush();

        JsonArray path = Json.createArray();
        path.set(0, 0);

        addVirtualChild(node, virtualChild, NodeProperties.TEMPLATE_IN_TEMPLATE,
                path);

        Reactive.flush();

        assertEquals(
                "Unexpected 'sendExistingElementWithIdAttachToServer' method call number",
                4, tree.existingElementRpcArgs.size());
        assertEquals(
                "Unexpected requested node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                node, tree.existingElementRpcArgs.get(0));
        assertEquals(
                "Unexpected requested node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                virtualChild.getId(), tree.existingElementRpcArgs.get(1));
        assertEquals(
                "Unexpected attached node id value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                childNode.getId(), tree.existingElementRpcArgs.get(2));
        assertEquals(
                "Unexpected identifier value argument in the 'sendExistingElementWithIdAttachToServer' method call",
                null, tree.existingElementRpcArgs.get(3));
    }

    public void testBindVirtualChild_withCorrespondingElementInShadowRoot_byTagNameAndIndicesPath() {
        Element shadowRootElement = addShadowRootElement(element);
        StateNode childNode = createChildNode(null, element.getTagName());
        JsonArray path = Json.createArray();
        path.set(0, 0);

        Binder.bind(node, element);

        addVirtualChild(node, childNode, NodeProperties.TEMPLATE_IN_TEMPLATE,
                path);

        Element addressedElement = createAndAppendElementToShadowRoot(
                shadowRootElement, null, element.getTagName());

        List<Integer> expectedAfterBindingFeatures = Arrays.asList(
                NodeFeatures.POLYMER_SERVER_EVENT_HANDLERS,
                NodeFeatures.ELEMENT_CHILDREN,
                NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS);

        expectedAfterBindingFeatures.forEach(notExpectedFeature -> assertFalse(
                "Child node should not have any features from list "
                        + expectedAfterBindingFeatures
                        + " before binding, but got feature "
                        + notExpectedFeature,
                childNode.hasFeature(notExpectedFeature)));

        Reactive.flush();

        expectedAfterBindingFeatures.forEach(expectedFeature -> assertTrue(
                "Child node should have all features from list "
                        + expectedAfterBindingFeatures
                        + " before binding, but missing feature "
                        + expectedFeature,
                childNode.hasFeature(expectedFeature)));

        // nothing has changed: no new child
        assertEquals("No new child should be added to the element after attach",
                0, element.getChildElementCount());
        assertEquals(
                "No new child should be added to the shadow root after attach",
                1, shadowRootElement.getChildElementCount());

        Element childElement = shadowRootElement.getFirstElementChild();

        assertSame(
                "Existing element should be the same as element in the StateNode object",
                addressedElement, childElement);
    }

    public void testBindVirtualChild_withCorrespondingElementInShadowRoot_byId() {
        Element shadowRootElement = addShadowRootElement(element);
        String childId = "childElement";
        StateNode childNode = createChildNode(childId, element.getTagName());

        NodeMap properties = childNode.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        MapProperty fooProperty = properties.getProperty("foo");
        fooProperty.setValue("bar");

        Binder.bind(node, element);

        addVirtualChild(node, childNode, NodeProperties.INJECT_BY_ID,
                Json.create(childId));

        Element addressedElement = createAndAppendElementToShadowRoot(
                shadowRootElement, childId, element.getTagName());

        List<Integer> expectedAfterBindingFeatures = Arrays.asList(
                NodeFeatures.POLYMER_SERVER_EVENT_HANDLERS,
                NodeFeatures.ELEMENT_CHILDREN,
                NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS);

        expectedAfterBindingFeatures.forEach(notExpectedFeature -> assertFalse(
                "Child node should not have any features from list "
                        + expectedAfterBindingFeatures
                        + " before binding, but got feature "
                        + notExpectedFeature,
                childNode.hasFeature(notExpectedFeature)));

        Reactive.flush();

        expectedAfterBindingFeatures.forEach(expectedFeature -> assertTrue(
                "Child node should have all features from list "
                        + expectedAfterBindingFeatures
                        + " before binding, but missing feature "
                        + expectedFeature,
                childNode.hasFeature(expectedFeature)));

        // nothing has changed: no new child
        assertEquals("No new child should be added to the element after attach",
                0, element.getChildElementCount());
        assertEquals(
                "No new child should be added to the shadow root after attach",
                1, shadowRootElement.getChildElementCount());

        Element childElement = shadowRootElement.getFirstElementChild();

        assertSame(
                "Existing element should be the same as element in the StateNode object",
                addressedElement, childElement);
    }

    public void testBindVirtualChild_withDeferredElementInShadowRoot_byId() {
        String childId = "childElement";
        String tag = element.getTagName();
        StateNode childNode = createChildNode(childId, tag);

        NodeMap properties = childNode.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        MapProperty fooProperty = properties.getProperty("foo");
        fooProperty.setValue("bar");

        addVirtualChild(node, childNode, NodeProperties.INJECT_BY_ID,
                Json.create(childId));

        Element shadowRoot = Browser.getDocument().createElement("div");

        WidgetUtil.setJsProperty(element, "root", shadowRoot);

        List<Integer> expectedAfterBindingFeatures = Arrays.asList(
                NodeFeatures.POLYMER_SERVER_EVENT_HANDLERS,
                NodeFeatures.ELEMENT_CHILDREN,
                NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS);

        Binder.bind(node, element);

        Reactive.flush();

        expectedAfterBindingFeatures.forEach(notExpectedFeature -> assertFalse(
                "Child node should not have any features from list "
                        + expectedAfterBindingFeatures
                        + " before binding, but got feature "
                        + notExpectedFeature,
                childNode.hasFeature(notExpectedFeature)));

        Element addressedElement = createAndAppendElementToShadowRoot(
                shadowRoot, childId, tag);

        // add flush listener which register the property to revert its initial
        // value back if it has been changed during binding "from the client
        // side" and do update the property emulating client side update
        // The property value should be reverted back in the end
        Reactive.addFlushListener(() -> {
            tree.getRegistry().getInitialPropertiesHandler()
                    .handlePropertyUpdate(fooProperty);
            fooProperty.setValue("baz");
        });

        PolymerUtils.fireReadyEvent(element);

        // the property value should be the same as initially
        assertEquals("bar", fooProperty.getValue());

        expectedAfterBindingFeatures.forEach(expectedFeature -> assertTrue(
                "Child node should have all features from list "
                        + expectedAfterBindingFeatures
                        + " before binding, but missing feature "
                        + expectedFeature,
                childNode.hasFeature(expectedFeature)));

        // nothing has changed: no new child
        assertEquals("No new child should be added to the element after attach",
                0, element.getChildElementCount());
        assertEquals(
                "No new child should be added to the shadow root after attach",
                1, shadowRoot.getChildElementCount());

        Element childElement = shadowRoot.getFirstElementChild();

        assertSame(
                "Existing element should be the same as element in the StateNode object",
                addressedElement, childElement);
    }

    public void testBindVirtualChild_withDeferredElementInShadowRoot_byIndicesPath() {
        String childId = "childElement";
        StateNode childNode = createChildNode(childId, element.getTagName());

        NodeMap properties = childNode.getMap(NodeFeatures.ELEMENT_PROPERTIES);
        MapProperty fooProperty = properties.getProperty("foo");
        fooProperty.setValue("bar");

        WidgetUtil.setJsProperty(element, "ready", NativeFunction.create(""));

        Binder.bind(node, element);

        JsonArray path = Json.createArray();
        path.set(0, 0);

        addVirtualChild(node, childNode, NodeProperties.TEMPLATE_IN_TEMPLATE,
                path);

        Element shadowRoot = Browser.getDocument().createElement("div");

        List<Integer> expectedAfterBindingFeatures = Arrays.asList(
                NodeFeatures.POLYMER_SERVER_EVENT_HANDLERS,
                NodeFeatures.ELEMENT_CHILDREN,
                NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS);

        Reactive.flush();

        expectedAfterBindingFeatures.forEach(notExpectedFeature -> assertFalse(
                "Child node should not have any features from list "
                        + expectedAfterBindingFeatures
                        + " before binding, but got feature "
                        + notExpectedFeature,
                childNode.hasFeature(notExpectedFeature)));

        WidgetUtil.setJsProperty(element, "root", shadowRoot);
        Element addressedElement = createAndAppendElementToShadowRoot(
                shadowRoot, childId, element.getTagName());

        // add flush listener which register the property to revert its initial
        // value back if it has been changed during binding "from the client
        // side" and do update the property emulating client side update
        // The property value should be reverted back in the end
        Reactive.addFlushListener(() -> {
            tree.getRegistry().getInitialPropertiesHandler()
                    .handlePropertyUpdate(fooProperty);
            fooProperty.setValue("baz");
        });

        PolymerUtils.fireReadyEvent(element);

        // the property value should be the same as initially
        assertEquals("bar", fooProperty.getValue());

        expectedAfterBindingFeatures.forEach(expectedFeature -> assertTrue(
                "Child node should have all features from list "
                        + expectedAfterBindingFeatures
                        + " before binding, but missing feature "
                        + expectedFeature,
                childNode.hasFeature(expectedFeature)));

        // nothing has changed: no new child
        assertEquals("No new child should be added to the element after attach",
                0, element.getChildElementCount());
        assertEquals(
                "No new child should be added to the shadow root after attach",
                1, shadowRoot.getChildElementCount());

        Element childElement = shadowRoot.getFirstElementChild();

        assertSame(
                "Existing element should be the same as element in the StateNode object",
                addressedElement, childElement);
    }

    public void testBindInvisibleNode() {
        node.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.VISIBLE).setValue(false);

        Binder.bind(node, element);

        assertEquals(Boolean.TRUE.toString(), element.getAttribute("hidden"));
    }

    public void testBindVisibleNode() {
        Binder.bind(node, element);

        assertNull(element.getAttribute("hidden"));
    }

    public void testBindInvisibleElement_elementIsNotBound_elementBecomesBoundWhenVisible() {
        setVisible(false);

        setTag();

        StateNode childNode = createChildNode("child");
        children.add(0, childNode);

        properties.getProperty("foo").setValue("bar");

        node.setDomNode(element);

        List<Integer> list = Arrays.asList(0);
        node.addDomNodeSetListener(node -> {
            list.set(0, list.get(0) + 1);
            return false;
        });

        Binder.bind(node, element);

        Reactive.flush();

        assertEquals(Integer.valueOf(0), list.get(0));

        assertEquals(0, element.getChildElementCount());
        assertNull(WidgetUtil.getJsProperty(element, "foo"));

        setVisible(true);

        Reactive.flush();

        // DOM node set listener is notified at least once
        assertTrue(list.get(0) > 1);

        assertEquals(1, element.getChildren().length());
        assertTrue(element.getFirstElementChild().getTagName()
                .equalsIgnoreCase(childNode.getMap(NodeFeatures.ELEMENT_DATA)
                        .getProperty(NodeProperties.TAG).getValue()
                        .toString()));
        assertEquals("bar", WidgetUtil.getJsProperty(element, "foo"));
    }

    public void testBindInvisibleElement_unbind() {
        setVisible(false);

        setTag();

        node.setDomNode(element);

        // Now the node is partially bound (it has "visibility" listener)
        Binder.bind(node, element);

        Reactive.flush();

        // it will rebind the element (and has to remove the initial visibility
        // listener)
        setVisible(true);

        Reactive.flush();

        // unregister the node
        node.unregister();

        // make the node invisible, in fact it should not do anything since the
        // node is unregistered: all listener after REBOUND are removed, but we
        // should check that initial visibility listener (in partial binding) is
        // removed as well
        setVisible(false);

        Reactive.flush();

        // The latter visibility value has no effect on element attribute
        // If the listener had been called then the attribute value would have
        // been "true".
        assertNull(element.getAttribute("hidden"));
    }

    /**
     * The StateNode is visible (the visibility is true).
     *
     * The HTML element has "hidden" attribute.
     *
     * After binding the element should stay hidden
     */
    public void testBindHiddenElement_stateNodeIsVisible_elementStaysHidden() {
        element.setAttribute("hidden", Boolean.TRUE.toString());
        Binder.bind(node, element);

        assertEquals(Boolean.TRUE.toString(), element.getAttribute("hidden"));
    }

    /**
     * The StateNode is visible (the visibility is true).
     *
     * The HTML element has "hidden" attribute.
     *
     * Element changes its visibility. The "hidden" attribute should keep its
     * value.
     *
     */
    public void testBindHiddenElement_stateNodeChangesVisibility_elementStaysHidden() {
        element.setAttribute("hidden", Boolean.TRUE.toString());

        setTag();

        node.setDomNode(element);

        Binder.bind(node, element);

        setVisible(false);

        Reactive.flush();

        assertEquals(Boolean.TRUE.toString(), element.getAttribute("hidden"));

        setVisible(true);

        Reactive.flush();

        assertEquals(Boolean.TRUE.toString(), element.getAttribute("hidden"));
    }

    /**
     * The StateNode is visible (the visibility is true).
     *
     * The HTML element has no "hidden" attribute.
     *
     * Element changes its visibility. The "hidden" attribute should keep its
     * value.
     */
    public void testBindNotHiddenElement_stateNodeChangesVisibility_elementIsNotHidden() {
        setTag();

        node.setDomNode(element);

        Binder.bind(node, element);

        setVisible(false);

        Reactive.flush();

        assertEquals(Boolean.TRUE.toString(), element.getAttribute("hidden"));

        setVisible(true);

        Reactive.flush();

        assertNull(element.getAttribute("hidden"));
    }

    /**
     * The StateNode is visible (the visibility is true).
     *
     * The HTML element has no "hidden" attribute.
     *
     * Element changes its visibility. The "hidden" attribute should keep its
     * value.
     */
    public void testBindNotHiddenElement_stateNodeChangesVisibilityAndElementChangesHiddenValue_elementKeepsHiddenValue() {
        setTag();

        node.setDomNode(element);

        Binder.bind(node, element);

        // make it invisible, make it visible, change "hidden" attribute value

        setVisible(false);

        Reactive.flush();

        setVisible(true);

        Reactive.flush();

        element.setAttribute("hidden", Boolean.TRUE.toString());

        // hide/unhide again
        setVisible(false);

        Reactive.flush();

        setVisible(true);

        Reactive.flush();

        assertEquals(Boolean.TRUE.toString(), element.getAttribute("hidden"));
    }

    /**
     * The StateNode is initially invisible (the visibility is false).
     *
     * The HTML element has "hidden" attribute.
     *
     * After binding the element should stay hidden
     */
    public void testBindHiddenElement_stateNodeIsInvisible_elementStaysHidden() {
        setVisible(false);
        element.setAttribute("hidden", Boolean.TRUE.toString());

        setTag();

        node.setDomNode(element);

        // Now the node is partially bound (it has "visibility" listener)
        Binder.bind(node, element);

        Reactive.flush();
        assertEquals(Boolean.TRUE.toString(), element.getAttribute("hidden"));

        setVisible(true);

        Reactive.flush();

        assertEquals(Boolean.TRUE.toString(), element.getAttribute("hidden"));
    }

    public void testSimpleElementBindingStrategy_regularElement_needsBind() {
        assertFalse(SimpleElementBindingStrategy.needsRebind(node));

        node.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.VISIBILITY_BOUND_PROPERTY)
                .setValue(false);

        assertTrue(SimpleElementBindingStrategy.needsRebind(node));
    }

    public void testSimpleElementBindingStrategy_elementWithoutFeature_needsBind() {
        StateNode emptyNode = new StateNode(45, tree);
        // self control
        assertFalse(emptyNode.hasFeature(NodeFeatures.ELEMENT_DATA));

        assertFalse(SimpleElementBindingStrategy.needsRebind(node));
    }

    public void testReadyCallback_polymerElementAndNoListeners_readyIsCalled() {
        assertPolymerElement_originalReadyIsCalled();
    }

    public void testReadyCallback_polymerElement_readyIsCalledAndNotified() {
        PolymerUtils.addReadyListener(element,
                () -> WidgetUtil.setJsProperty(element, "baz", "foobar"));

        assertPolymerElement_originalReadyIsCalled();

        assertEquals("foobar", WidgetUtil.getJsProperty(element, "baz"));
    }

    public void testReadyCallback_deferredPolymerElementAndNoListeners_readyIsCalled() {
        element = Browser.getDocument().createElement("x-my");
        WidgetUtil.setJsProperty(element, "localName", "x-my");

        assertDeferredPolymerElement_originalReadyIsCalled(element);
    }

    public void testReadyCallback_deferredPolymerElement_readyIsCalledAndNotified() {
        element = Browser.getDocument().createElement("x-my");
        WidgetUtil.setJsProperty(element, "localName", "x-my");

        PolymerUtils.addReadyListener(element,
                () -> WidgetUtil.setJsProperty(element, "baz", "foobar"));

        assertDeferredPolymerElement_originalReadyIsCalled(element);

        assertEquals("foobar", WidgetUtil.getJsProperty(element, "baz"));
    }

    private void assertDeferredPolymerElement_originalReadyIsCalled(
            Element element) {
        initPolymer(element);
        mockWhenDefined(element);

        NativeFunction function = NativeFunction.create("this['foo']='bar';");
        WidgetUtil.setJsProperty(element, "ready", function);

        Binder.bind(node, element);

        runWhenDefined(element);

        NativeFunction readyCall = new NativeFunction("this.ready();");
        readyCall.call(element);

        assertEquals("bar", WidgetUtil.getJsProperty(element, "foo"));
    }

    private void assertPolymerElement_originalReadyIsCalled() {
        initPolymer(element);

        NativeFunction function = NativeFunction.create("this['foo']='bar';");
        WidgetUtil.setJsProperty(element, "ready", function);

        Binder.bind(node, element);

        NativeFunction readyCall = new NativeFunction("this.ready();");
        readyCall.call(element);

        assertEquals("bar", WidgetUtil.getJsProperty(element, "foo"));
    }

    private void setTag() {
        node.getMap(NodeFeatures.ELEMENT_DATA).getProperty(NodeProperties.TAG)
                .setValue(element.getTagName());
    }

    private void setVisible(boolean visible) {
        NodeMap map = node.getMap(NodeFeatures.ELEMENT_DATA);
        MapProperty visibility = map.getProperty(NodeProperties.VISIBLE);
        visibility.setValue(visible);
    }

    private Element createAndAppendElementToShadowRoot(Element shadowRoot,
            String id, String tagName) {
        Element childShadowRootElement = Browser.getDocument()
                .createElement(tagName);
        childShadowRootElement.setId(id);
        shadowRoot.appendChild(childShadowRootElement);

        if (id != null) {
            JsonObject obj = Json.createObject();
            WidgetUtil.setJsProperty(obj, id.toString(),
                    childShadowRootElement);
            WidgetUtil.setJsProperty(element, "$", obj);
        }
        return childShadowRootElement;
    }

    private void addVirtualChild(StateNode shadowRootNode, StateNode childNode,
            String type, JsonValue payload) {
        NodeList virtualChildren = shadowRootNode
                .getList(NodeFeatures.VIRTUAL_CHILDREN);
        JsonObject object = Json.createObject();
        childNode.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.PAYLOAD).setValue(object);
        object.put(NodeProperties.TYPE, type);
        object.put(NodeProperties.PAYLOAD, payload);
        virtualChildren.add(virtualChildren.length(), childNode);
    }

    private StateNode createAndAttachShadowRootNode() {
        StateNode shadowRootNode = new StateNode(34, tree);
        node.getMap(NodeFeatures.SHADOW_ROOT_DATA)
                .getProperty(NodeProperties.SHADOW_ROOT)
                .setValue(shadowRootNode);
        return shadowRootNode;
    }

    private native Element addShadowRootElement(Element element)
    /*-{
        var shadowRoot = $doc.createElement("div");
        element.shadowRoot = shadowRoot;
        element.root = shadowRoot;
        return shadowRoot;
    }-*/;

    private native String getToString(Object value)
    /*-{
        return value.toString();
    }-*/;

    private native void mockWhenDefined(Element element)
    /*-{
        $wnd.OldPolymer = $wnd.Polymer;
        $wnd.Polymer = null;
        $wnd.customElements = {
            whenDefined: function() {
                return {
                    then: function (callback) {
                        element.callback = callback;
                    }
                }
            }
        };
    }-*/;

    private native void runWhenDefined(Element element)
    /*-{
        $wnd.Polymer = $wnd.OldPolymer;
        element.callback();
    }-*/;

    private native void initPolymer(Element element)
    /*-{
        $wnd.Polymer = function() {};
        $wnd.Polymer.dom = function(node){
            return node;
        };
        $wnd.Polymer.Element = {
          set: function() {}
        };
        element.__proto__ = $wnd.Polymer.Element;
        if( !element.removeAttribute ) {
            element.removeAttribute = function(attribute){
                element[attribute] = null;
            };
        }
        if ( !element.getAttribute ){
            element.getAttribute = function( attribute ){
                return element[attribute];
            };
        }
        if ( !element.root ){
            element.root=element;
        }
        if ( !element.querySelector ){
            element.querySelector = function(){
                return null;
            }
        }
        if ( !element.addEventListener){
            element.addEventListener = function(){
            }
        }
    }-*/;

}
