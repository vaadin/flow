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
package com.vaadin.client.bootstrap;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Typed view over the bootstrap configuration's error-message object. Backed
 * directly by the underlying JS instance — getters compile to property reads.
 *
 * @since 1.0
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public final class ErrorMessage {

    @JsProperty(name = "caption")
    public native String getCaption();

    @JsProperty(name = "message")
    public native String getMessage();

    @JsProperty(name = "url")
    public native String getUrl();

    @JsProperty(name = "querySelector")
    public native String getQuerySelector();
}
