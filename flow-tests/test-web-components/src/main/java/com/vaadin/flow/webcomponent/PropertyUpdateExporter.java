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

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.PropertyConfiguration;
import com.vaadin.flow.component.webcomponent.WebComponentDefinition;

import elemental.json.Json;
import elemental.json.JsonValue;

@Tag("property-update")
public class PropertyUpdateExporter implements WebComponentExporter<PropertyUpdateComponent> {
    @Override
    public void define(WebComponentDefinition<PropertyUpdateComponent> definition) {
        PropertyConfiguration<PropertyUpdateComponent, Integer> property =
                definition.addProperty("clicks", 0);

        PropertyConfiguration<PropertyUpdateComponent, JsonValue> jsonProperty =
                definition.addProperty("clicksJson", Json.createNull());

        definition.setInstanceConfigurator((webComponent, component) ->
                component.addListener(number -> {
                    webComponent.setProperty(property, number);
                    webComponent.setProperty(jsonProperty,
                            component.getNumberJson());
        }));

    }
}
