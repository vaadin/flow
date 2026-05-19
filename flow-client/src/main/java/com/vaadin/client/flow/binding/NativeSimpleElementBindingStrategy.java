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
package com.vaadin.client.flow.binding;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

import com.vaadin.client.JsRunnable;

import elemental.dom.Element;

/**
 * JsInterop binding for the TypeScript {@code SimpleElementBindingStrategy}
 * module published at
 * {@code window.Vaadin.Flow.internal.client.flow.binding.SimpleElementBindingStrategy}.
 * Source lives in
 * {@code src/main/frontend/internal/client/flow/binding/SimpleElementBindingStrategy.ts}.
 */
@JsType(isNative = true, namespace = "Vaadin.Flow.internal.client.flow.binding", name = "SimpleElementBindingStrategy")
final class NativeSimpleElementBindingStrategy {

    /**
     * Callback receiving the {@code changedProps} JS object from a Polymer
     * {@code _propertiesChanged} invocation.
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    interface JsChangedPropsConsumer {
        void accept(Object changedProps);
    }

    /**
     * Callback receiving a (nodeId, host, propertyName, value) tuple from the
     * dom-repeat prototype's {@code _propertiesChanged}.
     */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    interface JsListItemChangeConsumer {
        void accept(double nodeId, Object host, String propertyName,
                Object value);
    }

    private NativeSimpleElementBindingStrategy() {
        // Native, not instantiated from Java
    }

    static native void bindPolymerModelProperties(Element element,
            JsRunnable onHookUp);

    static native void hookUpPolymerElement(Element element,
            JsChangedPropsConsumer handlePropertiesChanged,
            JsRunnable fireReadyEvent,
            JsListItemChangeConsumer handleListItemPropertyChange);
}
