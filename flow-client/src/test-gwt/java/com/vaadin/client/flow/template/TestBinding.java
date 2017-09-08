/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.client.flow.template;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.WidgetUtil;
import com.vaadin.flow.template.angular.ModelValueBindingProvider;
import com.vaadin.flow.template.angular.StaticBindingValueProvider;

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
    static TestBinding createStatic(String value) {
        return createBinding(StaticBindingValueProvider.TYPE, value);
    }

    @JsOverlay
    static TestBinding createTextValueBinding(String value) {
        return createBinding(ModelValueBindingProvider.TYPE, value);
    }

    @JsOverlay
    static TestBinding createBinding(String type, String value) {
        TestBinding binding = WidgetUtil
                .crazyJsCast(JavaScriptObject.createObject());
        binding.setType(type);
        binding.setValue(value);

        return binding;
    }

    @JsOverlay
    default JsonValue asJson() {
        return WidgetUtil.crazyJsCast(this);
    }
}
