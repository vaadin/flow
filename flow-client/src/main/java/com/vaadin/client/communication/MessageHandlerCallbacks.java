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
package com.vaadin.client.communication;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

import com.vaadin.client.JsObjectSupplier;

/**
 * Adapter handed to the TypeScript {@code MessageHandler} so it can reach back
 * into still-Java services and the sibling {@link MessageSender} without
 * depending on the {@link com.vaadin.client.Registry} facade.
 *
 * <p>
 * Backed by a plain JS object so every property name survives the GWT-OBF
 * mangling that affects regular Java instance methods and fields; each Java
 * setter goes through an explicit {@code @JsProperty(name=...)} hint. TS reads
 * each entry as a function (each value is a {@link JsObjectSupplier} or
 * matching {@code @JsFunction} lambda).
 *
 * <p>
 * Suppliers allow the values to be resolved lazily, so the same callbacks
 * object can be built before all of the dependencies have been constructed
 * (notably the sibling MessageSender).
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public final class MessageHandlerCallbacks {

    /** Callback supplying a URL to redirect the browser to. */
    @FunctionalInterface
    @JsFunction
    @SuppressWarnings("unusable-by-js")
    public interface Redirect {
        void redirect(String url);
    }

    @JsProperty(name = "getMessageSender")
    public native void setGetMessageSender(JsObjectSupplier supplier);

    @JsProperty(name = "getUiLifecycle")
    public native void setGetUiLifecycle(JsObjectSupplier supplier);

    @JsProperty(name = "getStateTree")
    public native void setGetStateTree(JsObjectSupplier supplier);

    @JsProperty(name = "getConstantPool")
    public native void setGetConstantPool(JsObjectSupplier supplier);

    @JsProperty(name = "getSystemErrorHandler")
    public native void setGetSystemErrorHandler(JsObjectSupplier supplier);

    @JsProperty(name = "getExecuteJavaScriptProcessor")
    public native void setGetExecuteJavaScriptProcessor(
            JsObjectSupplier supplier);

    @JsProperty(name = "getDependencyLoader")
    public native void setGetDependencyLoader(JsObjectSupplier supplier);

    @JsProperty(name = "getResourceLoader")
    public native void setGetResourceLoader(JsObjectSupplier supplier);

    @JsProperty(name = "getRequestResponseTracker")
    public native void setGetRequestResponseTracker(JsObjectSupplier supplier);

    @JsProperty(name = "getLoadingIndicatorStateHandler")
    public native void setGetLoadingIndicatorStateHandler(
            JsObjectSupplier supplier);

    @JsProperty(name = "getApplicationConfiguration")
    public native void setGetApplicationConfiguration(
            JsObjectSupplier supplier);

    @JsProperty(name = "redirect")
    public native void setRedirect(Redirect redirect);
}
