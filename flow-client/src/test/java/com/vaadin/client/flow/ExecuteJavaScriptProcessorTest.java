/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.client.ExistingElementMap;
import com.vaadin.client.Registry;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.flow.internal.JsonCodec;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.NodeProperties;

import elemental.js.dom.JsElement;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class ExecuteJavaScriptProcessorTest {
    private static class CollectingExecuteJavaScriptProcessor
            extends ExecuteJavaScriptProcessor {
        private final List<String[]> parameterNamesAndCodeList = new ArrayList<>();
        private final List<JsArray<Object>> parametersList = new ArrayList<>();
        private final List<JsMap<Object, StateNode>> nodeParametersList = new ArrayList<>();

        private final Registry registry;

        private boolean isBound = true;

        private CollectingExecuteJavaScriptProcessor() {
            this(new Registry() {
                {
                    set(StateTree.class, new StateTree(this));
                }
            });
        }

        private CollectingExecuteJavaScriptProcessor(Registry registry) {
            super(registry);
            this.registry = registry;
        }

        Registry getRegistry() {
            return registry;
        }

        @Override
        protected void invoke(String[] parameterNamesAndCode,
                JsArray<Object> parameters,
                JsMap<Object, StateNode> nodeParameters) {
            parameterNamesAndCodeList.add(parameterNamesAndCode);
            parametersList.add(parameters);
            nodeParametersList.add(nodeParameters);
        }

        @Override
        protected boolean isBound(StateNode node) {
            return isBound;
        }

    }

    private static class TestJsProcessor extends ExecuteJavaScriptProcessor {

        private final Registry registry;

        private TestJsProcessor(Registry registry) {
            super(registry);
            this.registry = registry;
        }

        private TestJsProcessor() {
            this(new Registry() {
                {
                    set(StateTree.class, new StateTree(this));
                }
            });
        }

        Registry getRegistry() {
            return registry;
        }
    }

    @Test
    public void execute_parametersAndCodeAreValidAndNoNodeParameters() {
        CollectingExecuteJavaScriptProcessor processor = new CollectingExecuteJavaScriptProcessor();

        JsonArray invocation1 = JsonUtils.createArray(Json.create("script1"));
        JsonArray invocation2 = Stream.of("param1", "param2", "script2")
                .map(Json::create).collect(JsonUtils.asArray());
        JsonArray invocations = JsonUtils.createArray(invocation1, invocation2);

        processor.execute(invocations);

        Assert.assertEquals(2, processor.parameterNamesAndCodeList.size());
        Assert.assertEquals(2, processor.parametersList.size());
        Assert.assertEquals(2, processor.nodeParametersList.size());

        Assert.assertArrayEquals(new String[] { "script1" },
                processor.parameterNamesAndCodeList.get(0));
        Assert.assertEquals(0, processor.parametersList.get(0).length());

        Assert.assertArrayEquals(new String[] { "$0", "$1", "script2" },
                processor.parameterNamesAndCodeList.get(1));
        Assert.assertEquals(2, processor.parametersList.get(1).length());
        Assert.assertEquals("param1", processor.parametersList.get(1).get(0));
        Assert.assertEquals("param2", processor.parametersList.get(1).get(1));

        Assert.assertEquals(0, processor.nodeParametersList.get(0).size());
        Assert.assertEquals(0, processor.nodeParametersList.get(1).size());
    }

    @Test
    public void execute_nodeParametersAreCorrectlyPassed() {
        Registry registry = new Registry() {
            {
                StateTree tree = new StateTree(this);
                set(StateTree.class, tree);
                set(ExistingElementMap.class, new ExistingElementMap());
            }

        };
        CollectingExecuteJavaScriptProcessor processor = new CollectingExecuteJavaScriptProcessor(
                registry);

        StateNode node = new StateNode(10, registry.getStateTree());
        registry.getStateTree().registerNode(node);

        JsElement element = new JsElement() {

        };
        node.setDomNode(element);

        JsonArray json = JsonUtils.createArray(Json.create(JsonCodec.NODE_TYPE),
                Json.create(node.getId()));

        JsonArray invocation = Stream.of(json, Json.create("$0"))
                .collect(JsonUtils.asArray());

        // JRE impl of the array uses

        processor.execute(JsonUtils.createArray(invocation));

        Assert.assertEquals(1, processor.nodeParametersList.size());

        Assert.assertEquals(1, processor.nodeParametersList.get(0).size());

        JsMap<Object, StateNode> map = processor.nodeParametersList.get(0);

        StateNode stateNode = map.get(element);
        Assert.assertEquals(node, stateNode);
    }

    @Test
    public void execute_nodeParameterIsVirtualChildAwaitingInit() {
        CollectingExecuteJavaScriptProcessor processor = new CollectingExecuteJavaScriptProcessor();

        Registry registry = processor.getRegistry();

        StateNode node = new StateNode(11, registry.getStateTree());

        JsonObject object = Json.createObject();
        object.put(NodeProperties.TYPE, NodeProperties.INJECT_BY_ID);
        node.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.PAYLOAD).setValue(object);

        registry.getStateTree().registerNode(node);

        JsonArray json = JsonUtils.createArray(Json.create(JsonCodec.NODE_TYPE),
                Json.create(node.getId()));

        JsonArray invocation = Stream.of(json, Json.create("$0"))
                .collect(JsonUtils.asArray());

        processor.execute(JsonUtils.createArray(invocation));

        // The invocation has not been executed
        Assert.assertEquals(0, processor.nodeParametersList.size());

        // emulate binding
        JsElement element = new JsElement() {

        };
        node.setDomNode(element);
        Reactive.flush();

        Assert.assertEquals(1, processor.nodeParametersList.size());

        Assert.assertEquals(1, processor.nodeParametersList.get(0).size());

        JsMap<Object, StateNode> map = processor.nodeParametersList.get(0);

        StateNode stateNode = map.get(element);
        Assert.assertEquals(node, stateNode);
    }

    @Test
    public void execute_nodeParameterIsHidden() {
        CollectingExecuteJavaScriptProcessor processor = new CollectingExecuteJavaScriptProcessor();

        Registry registry = processor.getRegistry();

        StateNode node = new StateNode(31, registry.getStateTree());

        processor.isBound = false;

        registry.getStateTree().registerNode(node);

        JsonArray json = JsonUtils.createArray(Json.create(JsonCodec.NODE_TYPE),
                Json.create(node.getId()));

        JsonArray invocation = Stream.of(json, Json.create("$0"))
                .collect(JsonUtils.asArray());

        processor.execute(JsonUtils.createArray(invocation));

        Assert.assertEquals(0, processor.nodeParametersList.size());

        // emulate binding
        JsElement element = new JsElement() {

        };
        node.setDomNode(element);

        processor.isBound = true;

        Reactive.flush();

        Assert.assertEquals(1, processor.nodeParametersList.size());
        Assert.assertEquals(1, processor.nodeParametersList.get(0).size());

        JsMap<Object, StateNode> map = processor.nodeParametersList.get(0);

        StateNode stateNode = map.get(element);
        Assert.assertEquals(node, stateNode);
    }

    @Test
    public void execute_nodeParameterIsNotVirtualChild() {
        CollectingExecuteJavaScriptProcessor processor = new CollectingExecuteJavaScriptProcessor();

        Registry registry = processor.getRegistry();

        StateNode node = new StateNode(12, registry.getStateTree());

        registry.getStateTree().registerNode(node);

        JsonArray json = JsonUtils.createArray(Json.create(JsonCodec.NODE_TYPE),
                Json.create(node.getId()));

        JsonArray invocation = Stream.of(json, Json.create("$0"))
                .collect(JsonUtils.asArray());

        processor.execute(JsonUtils.createArray(invocation));

        // The invocation has been executed
        Assert.assertEquals(1, processor.nodeParametersList.size());
    }

    @Test
    public void isBound_noElement_notBound() {
        TestJsProcessor processor = new TestJsProcessor();

        Registry registry = processor.getRegistry();

        StateNode node = new StateNode(37, registry.getStateTree());

        Assert.assertFalse(processor.isBound(node));
    }

    @Test
    public void isBound_hasElementHasNoFeature_bound() {
        TestJsProcessor processor = new TestJsProcessor();

        Registry registry = processor.getRegistry();

        StateNode node = new StateNode(37, registry.getStateTree());

        // emulate binding
        JsElement element = new JsElement() {

        };
        node.setDomNode(element);

        Assert.assertTrue(processor.isBound(node));
    }

    @Test
    public void isBound_hasElementHasFeatureAndBound_bound() {
        TestJsProcessor processor = new TestJsProcessor();

        Registry registry = processor.getRegistry();

        StateNode node = new StateNode(37, registry.getStateTree());

        // emulate binding
        JsElement element = new JsElement() {

        };
        node.setDomNode(element);

        node.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.VISIBILITY_BOUND_PROPERTY)
                .setValue(true);

        Assert.assertTrue(processor.isBound(node));
    }

    @Test
    public void isBound_hasElementHasFeatureAndNotBound_notBound() {
        TestJsProcessor processor = new TestJsProcessor();

        Registry registry = processor.getRegistry();

        StateNode node = new StateNode(37, registry.getStateTree());

        // emulate binding
        JsElement element = new JsElement() {

        };
        node.setDomNode(element);

        node.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.VISIBILITY_BOUND_PROPERTY)
                .setValue(false);

        Assert.assertFalse(processor.isBound(node));
    }

    @Test
    public void isBound_hasElementHasNoFeatureAndBoundParent_bound() {
        TestJsProcessor processor = new TestJsProcessor();

        Registry registry = processor.getRegistry();

        StateNode node = new StateNode(37, registry.getStateTree());

        StateNode parent = new StateNode(43, registry.getStateTree());

        node.setParent(parent);

        // emulate binding
        JsElement element = new JsElement() {

        };
        node.setDomNode(element);

        parent.setDomNode(element);

        Assert.assertTrue(processor.isBound(node));
    }

    @Test
    public void isBound_hasElementHasNoFeatureAndUnboundParent_notBound() {
        TestJsProcessor processor = new TestJsProcessor();

        Registry registry = processor.getRegistry();

        StateNode node = new StateNode(37, registry.getStateTree());

        StateNode parent = new StateNode(43, registry.getStateTree());

        node.setParent(parent);

        // emulate binding
        JsElement element = new JsElement() {

        };
        node.setDomNode(element);

        Assert.assertFalse(processor.isBound(node));
    }

    @Test
    public void isBound_hasElementHasFeatureAndBoundAndUnboundParent_notBound() {
        TestJsProcessor processor = new TestJsProcessor();

        Registry registry = processor.getRegistry();

        StateNode node = new StateNode(37, registry.getStateTree());

        node.getMap(NodeFeatures.ELEMENT_DATA)
                .getProperty(NodeProperties.VISIBILITY_BOUND_PROPERTY)
                .setValue(true);

        StateNode parent = new StateNode(43, registry.getStateTree());

        node.setParent(parent);

        // emulate binding
        JsElement element = new JsElement() {

        };
        node.setDomNode(element);

        Assert.assertFalse(processor.isBound(node));
    }
}
