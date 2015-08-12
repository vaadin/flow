/*
 * Copyright 2000-2014 Vaadin Ltd.
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

package com.vaadin.shared.communication;

import java.io.Serializable;
import java.util.Objects;

import elemental.json.JsonArray;

/**
 * Information needed by the framework to send an RPC method invocation from the
 * client to the server or vice versa.
 *
 * @since 7.0
 */
public class MethodInvocation implements Serializable {

    private final String interfaceName = "com.vaadin.ui.JavaScript$JavaScriptCallbackRpc";
    private final String methodName = "call";
    private String javaScriptCallbackRpcName;
    private JsonArray parameters;

    public MethodInvocation() {
    }

    public MethodInvocation(String javaScriptCallbackRpcName,
            JsonArray parameters) {
        this.javaScriptCallbackRpcName = javaScriptCallbackRpcName;
        this.parameters = parameters;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public JsonArray getParameters() {
        return parameters;
    }

    public void setParameters(JsonArray parameters) {
        this.parameters = parameters;
    }

    public void setJavaScriptCallbackRpcName(String javaScriptCallbackRpcName) {
        this.javaScriptCallbackRpcName = javaScriptCallbackRpcName;
    }

    public String getJavaScriptCallbackRpcName() {
        return javaScriptCallbackRpcName;
    }

    @Override
    public String toString() {
        return interfaceName + "." + methodName + "(" + parameters + ")";
    }

    /**
     * Gets a String tag that is used to uniquely identify previous method
     * invocations that should be purged from the queue if
     * <code>{@literal @}Delay(lastOnly = true)</code> is used.
     * <p>
     * The returned string should contain at least one non-number char to ensure
     * it doesn't collide with the keys used for invocations without lastOnly.
     *
     * @return a string identifying this method invocation
     */
    public String getLastOnlyTag() {
        return getInterfaceName() + "-" + getMethodName();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MethodInvocation)) {
            return false;
        }
        MethodInvocation other = (MethodInvocation) obj;

        if (!Objects.equals(getInterfaceName(), other.getInterfaceName())) {
            return false;
        }

        if (!Objects.equals(getMethodName(), other.getMethodName())) {
            return false;
        }

        if (!Objects.equals(getParameters(), other.getParameters())) {
            return false;
        }

        return true;

    }
}
