/*
 * Copyright 2000-2017 Vaadin Ltd.
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
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.binding.Binder;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.nodefeature.NodeFeatures;
import com.vaadin.flow.nodefeature.NodeProperties;

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

        nextId = node.getId() + 1;

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

    public void testBindingBeforeFlush() {
        titleProperty.setValue("foo");

        Binder.bind(node, element);

        assertEquals("", element.getTitle());
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

        assertEquals("", element.getTitle());
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

    public void testSetAttributeWithoutFlush() {
        idAttribute.setValue("foo");

        Binder.bind(node, element);

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

    public void testInsertChild() {
        Binder.bind(node, element);

        createAndAppendElementToShadowRoot(element, null, "div");

        StateNode childNode = createChildNode("first");

        // One client side element, insert at the very beginning
        children.add(0, childNode);

        Reactive.flush();

        assertEquals(2, element.getChildElementCount());

        Element childElement = (Element) element.getChildren().at(0);

        assertEquals("SPAN", childElement.getTagName());
        assertEquals("first", childElement.getId());

        childNode = createChildNode("second", "a");
        // Insert before the bound node and pure client side node at the very
        // beginning
        children.add(0, childNode);
        Reactive.flush();

        assertEquals(3, element.getChildElementCount());

        childElement = (Element) element.getChildren().at(0);

        assertEquals("A", childElement.getTagName());
        assertEquals("second", childElement.getId());

        Element existingChild2 = Browser.getDocument().createElement("div");
        element.insertBefore(existingChild2,
                (Element) element.getChildren().at(1));

        childNode = createChildNode("third", "h1");
        // Insert at the first position.
        children.add(1, childNode);
        Reactive.flush();

        assertEquals(element.getChildElementCount(), 5);

        childElement = (Element) element.getChildren().at(1);

        assertEquals("H1", childElement.getTagName());
        assertEquals("third", childElement.getId());

        childNode = createChildNode("fourth", "br");
        // Insert after the last bound node
        children.add(3, childNode);
        Reactive.flush();

        assertEquals(6, element.getChildElementCount());

        childElement = (Element) element.getChildren().at(4);

        // Element should be before the client side element and after the bound
        // node
        assertEquals("BR", childElement.getTagName());
        assertEquals("fourth", childElement.getId());
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
        assertEquals("third", childElement.getId());

        childElement = (Element) children.at(1);
        assertEquals("first", childElement.getId());

        childElement = (Element) children.at(2);
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

        String constantPoolKey = "expressionsKey";

        JsonArray expressionConstantValue = Json.createArray();
        expressionConstantValue.set(0, booleanExpression);
        expressionConstantValue.set(1, numberExpression);
        expressionConstantValue.set(2, stringExpression);

        addToConstantPool(constantPoolKey, expressionConstantValue);

        node.getMap(NodeFeatures.ELEMENT_LISTENERS).getProperty("click")
                .setValue(constantPoolKey);
        Reactive.flush();
        Browser.getDocument().getBody().appendChild(element);

        element.click();

        assertEquals(1, tree.collectedNodes.length());

        assertSame(node, tree.collectedNodes.get(0));

        JsonObject eventData = tree.collectedEventData.get(0);

        assertEquals(3, eventData.keys().length);

        assertEquals(JsonType.NUMBER,
                eventData.get(numberExpression).getType());
        assertEquals("DIV", eventData.getString(stringExpression));
        assertEquals(true, eventData.getBoolean(booleanExpression));
    }

    private void addToConstantPool(String key, JsonValue value) {
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
        node.getMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES).getProperty("color")
                .setValue("green");

        Reactive.flush();
        Binder.bind(node, element);

        Reactive.flush();
        assertEquals("green", element.getStyle().getColor());
    }

    public void testAddStylesAfterBind() {
        Binder.bind(node, element);
        node.getMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES).getProperty("color")
                .setValue("green");

        Reactive.flush();
        assertEquals("green", element.getStyle().getColor());
    }

    public void testRemoveStyles() {
        Binder.bind(node, element);

        NodeMap styleMap = node.getMap(NodeFeatures.ELEMENT_STYLE_PROPERTIES);
        styleMap.getProperty("background").setValue("blue");
        styleMap.getProperty("color").setValue("white");

        Reactive.flush();
        assertEquals("background: blue; color: white;",
                element.getAttribute("style"));

        styleMap.getProperty("color").removeValue();

        Reactive.flush();
        assertEquals("background: blue;", element.getAttribute("style"));
    }

    public void testAddStylesAfterUnbind() {
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
        assertEquals(element.getChildElementCount(), 1);

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

    public void testBindVirtualElement() {
        addShadowRootElement(element);
        StateNode shadowRootNode = createAndAttachShadowRootNode();

        // start binder test
        Binder.bind(node, element);

        StateNode childNode = createChildNode("childElement");

        String tag = (String) childNode.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.TAG).getValue();

        ExistingElementMap existingElementMap = node.getTree().getRegistry()
                .getExistingElementMap();

        // create and add an existing element
        Element span = Browser.getDocument().createElement(tag);
        element.appendChild(span);

        existingElementMap.add(childNode.getId(), span);

        addVirtualChild(shadowRootNode, childNode);

        Reactive.flush();

        // nothing has changed: no new child
        assertEquals(element.getChildElementCount(), 1);

        assertNull(existingElementMap.getElement(childNode.getId()));

        Element childElement = element.getFirstElementChild();

        assertEquals(tag,
                childElement.getTagName().toLowerCase(Locale.ENGLISH));
        assertSame(span, childElement);
        assertEquals("childElement", childElement.getId());
        assertNull(existingElementMap.getElement(childNode.getId()));
    }

    public void testBindChild_noTagAndId() {
        addShadowRootElement(element);
        addVirtualChild(createAndAttachShadowRootNode(),
                createChildNode(null, null));

        try {
            Binder.bind(node, element);
            fail("Appending child state node with no tag and id should cause an exception");
        } catch (IllegalStateException ignored) {
            // expected
        }
    }

    public void testBindChild_noCorrespondingElementInShadowRoot_searchById() {
        Element shadowRootElement = addShadowRootElement(element);

        String childId = "childElement";
        addVirtualChild(createAndAttachShadowRootNode(),
                createChildNode(childId, null));

        Binder.bind(node, element);

        Element elementWithDifferentId = createAndAppendElementToShadowRoot(
                shadowRootElement, "otherId", null);
        assertNotSame(
                "Element added to shadow root should not have same id as virtual child node",
                childId, elementWithDifferentId.getId());

        try {
            Reactive.flush();
            fail("Appending state node for element with no corresponding element in shadow root should cause an exception");
        } catch (IllegalStateException e) {
            assertTrue(
                    "Exception message '" + e.getMessage()
                            + "' should contain id '" + childId + '\'',
                    e.getMessage().contains(childId));
        }
    }

    public void testBindChild_noCorrespondingElementInShadowRoot_searchByTag() {
        Element shadowRootElement = addShadowRootElement(element);

        String childTagName = "span";
        addVirtualChild(createAndAttachShadowRootNode(),
                createChildNode(null, childTagName));

        Binder.bind(node, element);

        Element elementWithDifferentTag = createAndAppendElementToShadowRoot(
                shadowRootElement, null, "div");
        assertNotSame(
                "Element added to shadow root should not have same tag name as virtual child node",
                childTagName, elementWithDifferentTag.getTagName());

        try {
            Reactive.flush();
            fail("Appending state node for element with no corresponding element in shadow root should cause an exception");
        } catch (IllegalStateException e) {
            assertTrue("Exception message '" + e.getMessage()
                    + "' should contain tag name '" + childTagName + '\'',
                    e.getMessage().contains(childTagName));
        }
    }

    public void testBindChild_withCorrespondingElementInShadowRoot_byTagName() {
        Element shadowRootElement = addShadowRootElement(element);
        StateNode childNode = createChildNode(null, element.getTagName());
        addVirtualChild(createAndAttachShadowRootNode(), childNode);

        Binder.bind(node, element);
        createAndAppendElementToShadowRoot(shadowRootElement, null,
                element.getTagName());

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
    }

    public void testBindChild_withCorrespondingElementInShadowRoot_byId() {
        Element shadowRootElement = addShadowRootElement(element);
        String childId = "childElement";
        StateNode childNode = createChildNode(childId, element.getTagName());
        addVirtualChild(createAndAttachShadowRootNode(), childNode);

        Binder.bind(node, element);
        createAndAppendElementToShadowRoot(shadowRootElement, childId,
                element.getTagName());

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
    }

    public void testBindChild_withAlreadyInitializedElement() {
        Element shadowRootElement = addShadowRootElement(element);
        String childId = "childElement";
        StateNode childNode = createChildNode(childId, element.getTagName());
        addVirtualChild(createAndAttachShadowRootNode(), childNode);
        childNode.setDomNode(createAndAppendElementToShadowRoot(
                shadowRootElement, childId, element.getTagName()));

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

        Binder.bind(node, element);

        expectedAfterBindingFeatures.forEach(expectedFeature -> assertTrue(
                "Child node should have all features from list "
                        + expectedAfterBindingFeatures
                        + " before binding, but missing feature "
                        + expectedFeature,
                childNode.hasFeature(expectedFeature)));
    }

    private Element createAndAppendElementToShadowRoot(Element shadowRoot,
            String id, String tagName) {
        Element childShadowRootElement = Browser.getDocument()
                .createElement(tagName);
        childShadowRootElement.setId(id);
        shadowRoot.appendChild(childShadowRootElement);
        return childShadowRootElement;
    }

    private void addVirtualChild(StateNode shadowRootNode,
            StateNode childNode) {
        NodeList virtualChildren = shadowRootNode
                .getList(NodeFeatures.VIRTUAL_CHILD_ELEMENTS);
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
        // GWT does not provide this method by default, so we use a stub
        shadowRoot.getElementById = function (id) {
            return this.querySelector('#' + id);
        };
        element.shadowRoot = shadowRoot;
        return shadowRoot;
    }-*/;

    private native String getToString(Object value)
    /*-{
        return value.toString();
    }-*/;
}
