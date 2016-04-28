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

import elemental.json.Json;
import elemental.json.JsonObject;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface TestElementTemplateNode
        extends ElementTemplateNode, TestTemplateNode {

    @JsProperty
    public void setTag(String tag);

    @JsProperty
    public void setProperties(JsonObject properties);

    @JsProperty
    public void setAttributes(JsonObject attributes);

    @JsOverlay
    public static TestElementTemplateNode create(String tag) {
        TestElementTemplateNode templateNode = WidgetUtil
                .crazyJsCast(JavaScriptObject.createObject());
        templateNode.setType("element");
        templateNode.setTag(tag);

        return templateNode;
    }

    @JsOverlay
    public default void addProperty(String name, String staticValue) {
        JsonObject properties = getProperties();
        if (properties == null) {
            properties = Json.createObject();
            setProperties(properties);
        }
        properties.put(name, TestBinding.createStatic(staticValue).asJson());
    }

    @JsOverlay
    public default void addAttribute(String name, String staticValue) {
        JsonObject attributes = getAttributes();
        if (attributes == null) {
            attributes = Json.createObject();
            setAttributes(attributes);
        }
        attributes.put(name, TestBinding.createStatic(staticValue).asJson());
    }
}
