/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.PropertyConfiguration;
import com.vaadin.flow.component.webcomponent.WebComponent;

import elemental.json.Json;
import elemental.json.JsonValue;

public class PropertyUpdateExporter
        extends WebComponentExporter<PropertyUpdateComponent> {

    private PropertyConfiguration<PropertyUpdateComponent, Integer> property;
    private PropertyConfiguration<PropertyUpdateComponent, JsonValue> jsonProperty;

    public PropertyUpdateExporter() {
        super("property-update");

        property = addProperty("clicks", 0);
        jsonProperty = addProperty("clicksJson", Json.createNull());
    }

    @Override
    public void configureInstance(WebComponent<PropertyUpdateComponent> webComponent,
                                  PropertyUpdateComponent component) {
        component.addListener(number -> {
            webComponent.setProperty(property, number);
            webComponent.setProperty(jsonProperty, component.getNumberJson());
        });
    }
}
