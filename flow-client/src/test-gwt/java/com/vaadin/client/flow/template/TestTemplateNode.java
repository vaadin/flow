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
import com.vaadin.flow.shared.JsonConstants;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface TestTemplateNode extends TemplateNode {
    @JsProperty
    void setType(String type);

    @JsProperty(name = JsonConstants.CHILD_TEMPLATE_KEY)
    void setChildrenIds(double[] children);

    @SuppressWarnings("unchecked")
    @JsOverlay
    static <T extends TestTemplateNode> T create(String type) {
        TestTemplateNode templateNode = WidgetUtil
                .crazyJsCast(JavaScriptObject.createObject());
        templateNode.setType(type);

        return (T) templateNode;
    }
}
