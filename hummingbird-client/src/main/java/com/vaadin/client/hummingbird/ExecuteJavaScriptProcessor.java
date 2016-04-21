/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.client.hummingbird;

import com.vaadin.client.Registry;
import com.vaadin.client.hummingbird.collection.JsArray;
import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.util.ClientJsonCodec;
import com.vaadin.client.hummingbird.util.NativeFunction;
import com.vaadin.ui.Page;

import elemental.json.JsonArray;
import elemental.json.JsonValue;

/**
 * Processes the result of
 * {@link Page#executeJavaScript(String, java.io.Serializable...)} on the
 * client.
 *
 * @since
 * @author Vaadin Ltd
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

        for (int i = 0; i < parameterCount; i++) {
            JsonValue parameterJson = invocation.get(i);
            Object parameter = ClientJsonCodec.decodeWithTypeInfo(tree,
                    parameterJson);
            parameters.push(parameter);
            parameterNamesAndCode[i] = "$" + i;
        }

        // Set the script source as the last parameter
        String expression = invocation.getString(invocation.length() - 1);
        parameterNamesAndCode[parameterNamesAndCode.length - 1] = expression;

        invoke(parameterNamesAndCode, parameters);
    }

    /**
     * Executes the actual invocation. This method is protected instead of
     * private for testing purposes.
     *
     * @param parameterNamesAndCode
     *            an array consisting of parameter names followed by the
     *            JavaScript expression to execute
     * @param parameters
     *            and array of parameter values
     */
    @SuppressWarnings("static-method")
    protected void invoke(String[] parameterNamesAndCode,
            JsArray<Object> parameters) {
        assert parameterNamesAndCode.length == parameters.length() + 1;

        NativeFunction function = new NativeFunction(parameterNamesAndCode);

        function.apply(null, parameters);
    }

}
