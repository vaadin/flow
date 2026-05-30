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

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

import com.vaadin.client.JsObjectSupplier;

/**
 * Adapter handed to the TypeScript {@code XhrConnection} so it can reach back
 * into still-Java services and sibling TS communication classes without
 * depending on the {@link com.vaadin.client.Registry} facade.
 *
 * <p>
 * Backed by a plain JS object so every property name survives the GWT-OBF
 * mangling that affects regular Java instance methods and fields; each Java
 * setter goes through an explicit {@code @JsProperty(name=...)} hint.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public final class XhrConnectionCallbacks {

    @JsProperty(name = "getMessageHandler")
    public native void setGetMessageHandler(JsObjectSupplier supplier);

    @JsProperty(name = "getConnectionStateHandler")
    public native void setGetConnectionStateHandler(JsObjectSupplier supplier);

    @JsProperty(name = "getRequestResponseTracker")
    public native void setGetRequestResponseTracker(JsObjectSupplier supplier);

    @JsProperty(name = "getApplicationConfiguration")
    public native void setGetApplicationConfiguration(
            JsObjectSupplier supplier);
}
