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
package com.vaadin.client.hummingbird.template;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.WidgetUtil;
import com.vaadin.hummingbird.template.StaticBinding;
import com.vaadin.hummingbird.template.TextValueBinding;

import elemental.json.JsonValue;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface TestBinding extends Binding {
    @JsProperty
    void setType(String type);

    @JsProperty
    void setValue(String value);

    @JsOverlay
    public static TestBinding createStatic(String value) {
        return createBinding(StaticBinding.TYPE, value);
    }

    @JsOverlay
    public static TestBinding createTextValueBinding(String value) {
        return createBinding(TextValueBinding.TYPE, value);
    }

    @JsOverlay
    public static TestBinding createBinding(String type, String value) {
        TestBinding binding = WidgetUtil
                .crazyJsCast(JavaScriptObject.createObject());
        binding.setType(type);
        binding.setValue(value);

        return binding;
    }

    @JsOverlay
    public default JsonValue asJson() {
        return WidgetUtil.crazyJsCast(this);
    }
}
