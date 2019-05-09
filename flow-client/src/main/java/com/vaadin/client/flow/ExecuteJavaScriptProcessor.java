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

import com.vaadin.client.Console;
import com.vaadin.client.Registry;
import com.vaadin.client.flow.binding.SimpleElementBindingStrategy;
import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;
import com.vaadin.client.flow.reactive.Reactive;
import com.vaadin.client.flow.util.ClientJsonCodec;
import com.vaadin.client.flow.util.NativeFunction;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.internal.nodefeature.NodeFeatures;
import com.vaadin.flow.internal.nodefeature.NodeProperties;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Processes the result of
 * {@link Page#executeJs(String, java.io.Serializable...)} on the
 * client.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ExecuteJavaScriptProcessor {

    private final Registry registry;

    /**
     * Creates a new processor with the given registry.
     *
     * @param registry
     *            the registry to use
     */
    public ExecuteJavaScriptProcessor(Registry registry) {
        this.registry = registry;
    }

    /**
     * Executes invocations received from the server.
     *
     * @param invocations
     *            a JSON containing invocation data
     */
    public void execute(JsonArray invocations) {
        for (int i = 0; i < invocations.length(); i++) {
            JsonArray invocation = invocations.getArray(i);
            handleInvocation(invocation);
        }
    }

    private void handleInvocation(JsonArray invocation) {
        StateTree tree = registry.getStateTree();

        // Last item is the script, the rest is parameters
        int parameterCount = invocation.length() - 1;

        String[] parameterNamesAndCode = new String[parameterCount + 1];
        JsArray<Object> parameters = JsCollections.array();

        JsMap<Object, StateNode> map = JsCollections.map();
        for (int i = 0; i < parameterCount; i++) {
            JsonValue parameterJson = invocation.get(i);
            Object parameter = ClientJsonCodec.decodeWithTypeInfo(tree,
                    parameterJson);
            parameters.push(parameter);
            parameterNamesAndCode[i] = "$" + i;
            StateNode stateNode = ClientJsonCodec.decodeStateNode(tree,
                    parameterJson);
            if (stateNode != null) {
                if (isVirtualChildAwaitingInitialization(stateNode)
                        || !isBound(stateNode)) {
                    stateNode.addDomNodeSetListener(node -> {
                        Reactive.addPostFlushListener(
                                () -> handleInvocation(invocation));
                        return true;
                    });
                    return;
                }
                map.set(parameter, stateNode);
            }
        }

        // Set the script source as the last parameter
        String expression = invocation.getString(invocation.length() - 1);
        parameterNamesAndCode[parameterNamesAndCode.length - 1] = expression;

        invoke(parameterNamesAndCode, parameters, map);
    }

    private boolean isVirtualChildAwaitingInitialization(StateNode node) {
        if (node.getDomNode() != null
                || node.getTree().getNode(node.getId()) == null) {
            return false;
        }
        if (node.getMap(NodeFeatures.ELEMENT_DATA)
                .hasPropertyValue(NodeProperties.PAYLOAD)) {
            Object value = node.getMap(NodeFeatures.ELEMENT_DATA)
                    .getProperty(NodeProperties.PAYLOAD).getValue();
            if (value instanceof JsonObject) {
                JsonObject object = (JsonObject) value;
                String type = object.getString(NodeProperties.TYPE);
                return NodeProperties.INJECT_BY_ID.equals(type)
                        || NodeProperties.TEMPLATE_IN_TEMPLATE.equals(type);
            }
        }
        return false;
    }

    protected boolean isBound(StateNode node) {
        boolean isNodeBound = node.getDomNode() != null
                && !SimpleElementBindingStrategy.needsRebind(node);
        if (!isNodeBound || node.getParent() == null) {
            return isNodeBound;
        }
        return isBound(node.getParent());
    }

    /**
     * Executes the actual invocation. This method is protected instead of
     * private for testing purposes.
     *
     * @param parameterNamesAndCode
     *            an array consisting of parameter names followed by the
     *            JavaScript expression to execute
     * @param parameters
     *            an array of parameter values
     * @param nodeParameters
     *            the node parameters
     */
    protected void invoke(String[] parameterNamesAndCode,
            JsArray<Object> parameters,
            JsMap<Object, StateNode> nodeParameters) {
        assert parameterNamesAndCode.length == parameters.length() + 1;

        try {
            NativeFunction function = new NativeFunction(parameterNamesAndCode);

            function.apply(getContextExecutionObject(nodeParameters),
                    parameters);
        } catch (Exception exception) {
            Console.reportStacktrace(exception);
            Console.error(
                    "Exception is thrown during JavaScript execution. Stacktrace will be dumped separately.");
            registry.getSystemErrorHandler().handleError(exception);
            if (!registry.getApplicationConfiguration().isProductionMode()) {
                StringBuilder codeBuilder = new StringBuilder("[");
                String delimiter = "";
                for (String snippet : parameterNamesAndCode) {
                    codeBuilder.append(delimiter).append(snippet);
                    delimiter = ", ";
                }
                codeBuilder.append("]");
                String code = codeBuilder.toString();

                if (code.charAt(0) == '[') {
                    code = code.substring(1);
                }
                if (code.charAt(code.length() - 1) == ']') {
                    code = code.substring(0, code.length() - 1);
                }
                Console.warn("The error has occurred in the JS code: '" + code
                        + "'");
            }
        }
    }

    private boolean handleRemoveExistingNode(Integer removedId, int nodeId,
            JsonArray invocation) {
        if (removedId.intValue() == nodeId) {
            Reactive.addPostFlushListener(() -> handleInvocation(invocation));
            return true;
        }
        return false;
    }

    private String getAppId() {
        return registry.getApplicationConfiguration().getApplicationId();
    }

    private native JsonObject getContextExecutionObject(
            JsMap<Object, StateNode> nodeParameters)
    /*-{
          var object = {};
          object.getNode = function (element){
              var node = nodeParameters.get(element);
              if ( node == null ){
                  throw new ReferenceError("There is no a StateNode for the given argument.");
              }
              return node;
          };
          object.$appId = this.@ExecuteJavaScriptProcessor::getAppId()().replace(/-\d+$/, '');
          object.attachExistingElement = function(parent, previousSibling, tagName, id){
              @com.vaadin.client.ExecuteJavaScriptElementUtils::attachExistingElement(*)(object.getNode(parent), previousSibling, tagName, id);
          };
          object.populateModelProperties = function(element, properties){
              @com.vaadin.client.ExecuteJavaScriptElementUtils::populateModelProperties(*)(object.getNode(element), properties);
          };
          object.registerUpdatableModelProperties = function(element, properties){
              @com.vaadin.client.ExecuteJavaScriptElementUtils::registerUpdatableModelProperties(*)(object.getNode(element), properties);
          };
          return object;
    }-*/;
}
