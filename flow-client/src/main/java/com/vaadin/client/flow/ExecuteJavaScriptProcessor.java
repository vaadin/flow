/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import jsinterop.annotations.JsType;

import com.vaadin.client.Registry;
import com.vaadin.flow.component.page.Page;

import elemental.json.JsonArray;

/**
 * Processes the result of
 * {@link Page#executeJs(String, java.io.Serializable...)} on the client. Pure
 * {@code @JsType(isNative=true)} binding to the TypeScript implementation at
 * {@code src/main/frontend/internal/client/flow/ExecuteJavaScriptProcessor.ts}.
 *
 * <p>
 * Construction takes a {@link ExecuteJavaScriptCallbacks} adapter that
 * dispatches into the still-Java {@code ExecuteJavaScriptElementUtils} helpers,
 * so the TS class does not need to reach back through static-Java code.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow", name = "ExecuteJavaScriptProcessor")
public class ExecuteJavaScriptProcessor {

    public ExecuteJavaScriptProcessor(Registry registry,
            ExecuteJavaScriptCallbacks callbacks) {
        // Defined by the TS class constructor.
    }

    /**
     * Executes invocations received from the server.
     *
     * @param invocations
     *            a JSON array of invocation arrays; each inner array is
     *            {@code [param0, param1, ..., scriptBody]}
     */
    public native void execute(JsonArray invocations);
}
