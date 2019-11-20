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

import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.ExistingElementMap;
import com.vaadin.client.Registry;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.flow.binding.Binder;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.nodefeature.MapProperty;
import com.vaadin.client.flow.nodefeature.NodeList;
import com.vaadin.client.flow.nodefeature.NodeMap;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.client.flow.util.NativeFunction;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.NodeProperties;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.dom.Text;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class GwtMultipleBindingTest extends ClientEngineTestBase {

    private TestStateNode node;

    private Element element;

    private StateTree tree;

    private Registry registry;

    private static class TestStateNode extends StateNode {

        private boolean isBound;

        public TestStateNode(StateTree tree) {
            this(0, tree);
        }

        public TestStateNode(int id, StateTree tree) {
            super(0, tree);
        }

        @Override
        public NodeList getList(int id) {
            if (isBound) {
                fail();
            }
            return super.getList(id);
        }

        @Override
        public NodeMap getMap(int id) {
            if (id != NodeFeatures.ELEMENT_DATA
                    && id != NodeFeatures.ELEMENT_DATA && isBound) {
                fail();
            }
            return super.getMap(id);
        }

        void setBound() {
            isBound = true;
        }

    }

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        Reactive.reset();

        registry = new Registry() {
            {
                set(ConstantPool.class, new ConstantPool());
                set(ExistingElementMap.class, new ExistingElementMap());
            }
        };

        tree = new StateTree(registry) {
            @Override
            public void sendTemplateEventToServer(StateNode node,
                    String methodName, JsArray<?> argValues, int promiseId) {
            }
        };

        node = new TestStateNode(tree);
        // populate "element data" feature to be able to bind node as a plain
        // element
        node.getMap(NodeFeatures.ELEMENT_DATA);
        element = Browser.getDocument().createElement("div");

    }

    public void testAddStylesDoubleBind() {
        assertMapPropertiesDoubleBind(NodeFeatures.ELEMENT_STYLE_PROPERTIES);
    }

    public void testSetPropertyDoubleBind() {
        assertMapPropertiesDoubleBind(NodeFeatures.ELEMENT_PROPERTIES);
    }

    public void testSetAttributeDoubleBind() {
        assertMapPropertiesDoubleBind(NodeFeatures.ELEMENT_PROPERTIES);
    }

    public void testAddChildDoubleBind() {
        Binder.bind(node, element);

        NodeList children = node.getList(NodeFeatures.ELEMENT_CHILDREN);
        children.add(0, createChild(1));

        Reactive.flush();

        node.setBound();

        Binder.bind(node, element);
    }

    public void testSynchronizedPropertyDoubleBind() {
        NodeList synchronizedPropertyList = node
                .getList(NodeFeatures.SYNCHRONIZED_PROPERTIES);
        NodeList synchronizedPropertyEventsList = node
                .getList(NodeFeatures.SYNCHRONIZED_PROPERTY_EVENTS);

        Binder.bind(node, element);

        synchronizedPropertyEventsList.add(0, "event");
        synchronizedPropertyList.add(0, "tagName");
        Reactive.flush();

        node.setBound();
        Binder.bind(node, element);
    }

    public void testDomEventHandlerDoubleBind() {
        Binder.bind(node, element);

        String booleanExpression = "window.navigator.userAgent[0] === 'M'";

        String constantPoolKey = "expressionsKey";

        JsonArray expressionConstantValue = Json.createArray();
        expressionConstantValue.set(0, booleanExpression);

        addToConstantPool(constantPoolKey, expressionConstantValue);

        node.getMap(NodeFeatures.ELEMENT_LISTENERS).getProperty("click")
                .setValue(constantPoolKey);
        Reactive.flush();

        node.setBound();
        Binder.bind(node, element);
    }

    public void testAddClassListDoubleBind() {
        assertListPropertiesDoubleBind(NodeFeatures.CLASS_LIST);
    }

    public void testClientCallableMethodDoubleBind() {
        assertListPropertiesDoubleBind(NodeFeatures.CLIENT_DELEGATE_HANDLERS);
    }

    public void testEventHandlerMethodDoubleBind() {
        assertListPropertiesDoubleBind(
                NodeFeatures.POLYMER_SERVER_EVENT_HANDLERS);
    }

    public void testBindShadowRootDoubleBind() {
        Binder.bind(node, element);

        NodeMap shadow = node.getMap(NodeFeatures.SHADOW_ROOT_DATA);

        shadow.getProperty(NodeProperties.SHADOW_ROOT).setValue(createChild(1));

        Reactive.flush();

        node.setBound();
        Binder.bind(node, element);
    }

    public void testBindModelPropertiesDoubleBind() {
        String name = "custom-div";
        Element element = Browser.getDocument().createElement(name);
        WidgetUtil.setJsProperty(element, "localName", name);
        initPolymer(element);

        NativeFunction function = NativeFunction.create("");
        WidgetUtil.setJsProperty(element, "set", function);

        Binder.bind(node, element);

        node.getMap(NodeFeatures.ELEMENT_PROPERTIES).getProperty("foo")
                .setValue("bar");

        Reactive.flush();

        node.setBound();
        Binder.bind(node, element);
    }

    public void testBindTextNodeDoubleBind() {
        TestStateNode textNode = new TestStateNode(1, node.getTree());

        Text domNode = Browser.getDocument().createTextNode("");

        MapProperty textProperty = textNode.getMap(NodeFeatures.TEXT_NODE)
                .getProperty(NodeProperties.TEXT);

        Binder.bind(textNode, domNode);

        textProperty.setValue("foo");

        Reactive.flush();

        node.setBound();
        Binder.bind(textNode, domNode);
    }

    private void assertListPropertiesDoubleBind(int featureId) {
        node.getList(featureId).add(0, "foo");
        Binder.bind(node, element);
        Reactive.flush();

        node.setBound();
        Binder.bind(node, element);
    }

    private TestStateNode createChild(int id) {
        TestStateNode childNode = new TestStateNode(1, node.getTree());

        childNode.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.TAG).setValue("div");
        return childNode;
    }

    private void assertMapPropertiesDoubleBind(int featureId) {
        Binder.bind(node, element);

        NodeMap props = node.getMap(featureId);

        props.getProperty("foo").setValue("bar");
        Reactive.flush();

        node.setBound();

        Binder.bind(node, element);
    }

    private void addToConstantPool(String key, JsonValue value) {
        JsonObject update = Json.createObject();
        update.put(key, value);
        registry.getConstantPool().importFromJson(update);
    }

    private native void initPolymer(Element element)
    /*-{
        $wnd.Polymer = function() {};
        $wnd.Polymer.dom = function(node){
            return node;
        };
        $wnd.Polymer.Element = {};
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
        if ( !element.setAttribute ){
            element.setAttribute = function( attribute , value){
                element[attribute] = value;
            };
        }
    }-*/;

}
