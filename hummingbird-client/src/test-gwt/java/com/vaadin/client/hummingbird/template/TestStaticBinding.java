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

import com.vaadin.client.WidgetUtil;

import elemental.json.JsonValue;
import jsinterop.annotations.JsType;

@JsType
public class TestStaticBinding {

    public final String type;
    public final String value;

    public TestStaticBinding(String value) {
        type = "static";
        this.value = value;
    }

    public native JsonValue asJson()
    /*-{
        // Must pass through the JSON parser to avoid GWT type metadata
        return JSON.parse(JSON.stringify(this));
    }-*/;

    public Binding asBinding() {
        return WidgetUtil.crazyJsCast(this);
    }

}
