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

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

import com.vaadin.client.JsRunnable;
import com.vaadin.client.flow.collection.JsMap;

import elemental.json.JsonObject;

/**
 * JsInterop binding for the TypeScript {@code ExecuteJavaScriptProcessor}
 * module published at
 * {@code window.Vaadin.Flow.internal.client.flow.ExecuteJavaScriptProcessor}.
 * Source lives in
 * {@code src/main/frontend/internal/client/flow/ExecuteJavaScriptProcessor.ts}.
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow", name = "ExecuteJavaScriptProcessor")
final class NativeExecuteJavaScriptProcessor {

    /**
     * Callback shape for invoking
     * {@link com.vaadin.client.ExecuteJavaScriptElementUtils#attachExistingElement}
     * from the TS side.
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    interface JsAttachExisting {
        void accept(Object parent, Object previousSibling, String tagName,
                int id);
    }

    /**
     * Callback shape for the two-argument element-utility methods
     * (populateModelProperties / registerUpdatableModelProperties).
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    interface JsNodePropertiesConsumer {
        void accept(Object node, Object properties);
    }

    /**
     * Callback shape for
     * {@link com.vaadin.client.ExecuteJavaScriptElementUtils#registerInitializer}.
     * The cleanup parameter is the no-arg JS callback the user JS supplied;
     * its type matches {@code ExecuteJavaScriptElementUtils.JsCallback}.
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    interface JsInitializerRegistrar {
        void accept(Object node, double id, Object cleanup);
    }

    /**
     * Callback shape for
     * {@link com.vaadin.client.ExecuteJavaScriptElementUtils#disposeInitializer}.
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    interface JsInitializerDisposer {
        void accept(Object node, double id);
    }

    private NativeExecuteJavaScriptProcessor() {
        // Native, not instantiated from Java
    }

    // CHECKSTYLE.OFF: ParameterNumber
    static native JsonObject getContextExecutionObject(
            JsMap<Object, Object> nodeParameters, String appId, Object registry,
            JsAttachExisting attachExisting,
            JsNodePropertiesConsumer populateModel,
            JsNodePropertiesConsumer registerUpdatable,
            JsRunnable stopApplication,
            JsInitializerRegistrar registerInitializer,
            JsInitializerDisposer disposeInitializer);
    // CHECKSTYLE.ON: ParameterNumber
}
