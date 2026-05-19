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
package com.vaadin.client;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

import com.vaadin.client.flow.collection.JsArray;

/**
 * JsInterop binding for the TypeScript {@code ApplicationConnection} module
 * published at
 * {@code window.Vaadin.Flow.internal.client.ApplicationConnection}. Source
 * lives in {@code src/main/frontend/internal/client/ApplicationConnection.ts}.
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client", name = "ApplicationConnection")
final class NativeApplicationConnection {

    private NativeApplicationConnection() {
        // Native, not instantiated from Java
    }

    // CHECKSTYLE.OFF: ParameterNumber
    static native void publishJavascriptMethods(String applicationId,
            boolean productionMode, boolean requestTiming,
            String[] exportedWebComponents, JsBooleanSupplier isActive,
            JsIntToObjectFn getByNodeId, JsObjectToIntFn getNodeId,
            JsIntSupplier getUIId,
            JsAddDomBindingListenerFn addDomBindingListener, JsRunnable poll,
            JsObjectConsumer connectWebComponent, JsStringToStringFn resolveUri,
            JsSendEventMessageFn sendEventMessage,
            JsProfilingDataSupplier getProfilingData);

    static native void publishDevelopmentModeJavascriptMethods(
            String applicationId, String servletVersion,
            JsBooleanSupplier isActive, JsObjectSupplier getDebugJson,
            JsIntToObjectFn getDomElementByNodeId, JsIntToStringFn getJavaClass,
            JsIntToBooleanFn isHiddenByServer,
            JsIntToObjectFn getElementStyleProperties);
    // CHECKSTYLE.ON: ParameterNumber
}

@FunctionalInterface
@JsFunction
@SuppressWarnings("unusable-by-js")
interface JsBooleanSupplier {
    boolean get();
}

@FunctionalInterface
@JsFunction
@SuppressWarnings("unusable-by-js")
interface JsIntSupplier {
    int get();
}

@FunctionalInterface
@JsFunction
@SuppressWarnings("unusable-by-js")
interface JsObjectSupplier {
    Object get();
}

@FunctionalInterface
@JsFunction
@SuppressWarnings("unusable-by-js")
interface JsProfilingDataSupplier {
    JsArray<Object> get();
}

@FunctionalInterface
@JsFunction
@SuppressWarnings("unusable-by-js")
interface JsIntToObjectFn {
    Object apply(int nodeId);
}

@FunctionalInterface
@JsFunction
@SuppressWarnings("unusable-by-js")
interface JsObjectToIntFn {
    int apply(Object element);
}

@FunctionalInterface
@JsFunction
@SuppressWarnings("unusable-by-js")
interface JsIntToStringFn {
    String apply(int nodeId);
}

@FunctionalInterface
@JsFunction
@SuppressWarnings("unusable-by-js")
interface JsIntToBooleanFn {
    boolean apply(int nodeId);
}

@FunctionalInterface
@JsFunction
@SuppressWarnings("unusable-by-js")
interface JsAddDomBindingListenerFn {
    void accept(int nodeId, Object callback);
}

@FunctionalInterface
@JsFunction
@SuppressWarnings("unusable-by-js")
interface JsObjectConsumer {
    void accept(Object value);
}

@FunctionalInterface
@JsFunction
@SuppressWarnings("unusable-by-js")
interface JsStringToStringFn {
    String apply(String value);
}

@FunctionalInterface
@JsFunction
@SuppressWarnings("unusable-by-js")
interface JsSendEventMessageFn {
    void accept(int nodeId, String eventType, Object eventData);
}
