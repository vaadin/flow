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

import elemental.json.Json;
import elemental.json.JsonObject;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface TestElementTemplateNode
        extends ElementTemplateNode, TestTemplateNode {

    @JsProperty
    void setTag(String tag);

    @JsProperty
    void setProperties(JsonObject properties);

    @JsProperty
    void setAttributes(JsonObject attributes);

    @JsProperty
    void setClassNames(JsonObject classNames);

    @JsProperty
    void setEventHandlers(JsonObject handlers);

    @JsOverlay
    static TestElementTemplateNode create(String tag) {
        TestElementTemplateNode templateNode = TestTemplateNode
                .create("element");
        templateNode.setTag(tag);

        return templateNode;
    }

    @JsOverlay
    default void addProperty(String name, String staticValue) {
        doGetProperties().put(name,
                TestBinding.createStatic(staticValue).asJson());
    }

    @JsOverlay
    default void addProperty(String name, TestBinding binding) {
        doGetProperties().put(name, binding.asJson());
    }

    @JsOverlay
    default void addAttribute(String name, TestBinding binding) {
        doGetAttributes().put(name, binding.asJson());
    }

    @JsOverlay
    default void addClassName(String name, String staticValue) {
        doGetClassNames().put(name,
                TestBinding.createStatic(staticValue).asJson());
    }

    @JsOverlay
    default void addClassName(String name, TestBinding binding) {
        doGetClassNames().put(name, binding.asJson());
    }

    @JsOverlay
    default void addEventHandler(String name, String handler) {
        JsonObject eventHandlers = getEventHandlers();
        if (eventHandlers == null) {
            eventHandlers = Json.createObject();
            setEventHandlers(eventHandlers);
        }
        eventHandlers.put(name, handler);
    }

    @JsOverlay
    default JsonObject doGetProperties() {
        JsonObject properties = getProperties();
        if (properties == null) {
            properties = Json.createObject();
            setProperties(properties);
        }
        return properties;
    }

    @JsOverlay
    default JsonObject doGetClassNames() {
        JsonObject classNames = getClassNames();
        if (classNames == null) {
            classNames = Json.createObject();
            setClassNames(classNames);
        }
        return classNames;
    }

    @JsOverlay
    default JsonObject doGetAttributes() {
        JsonObject attributes = getAttributes();
        if (attributes == null) {
            attributes = Json.createObject();
            setAttributes(attributes);
        }
        return attributes;
    }

    @JsOverlay
    default void addAttribute(String name, String staticValue) {
        doGetAttributes().put(name,
                TestBinding.createStatic(staticValue).asJson());
    }
}
