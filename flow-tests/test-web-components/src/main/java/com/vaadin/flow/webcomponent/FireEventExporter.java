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
import com.vaadin.flow.component.webcomponent.EventOptions;
import com.vaadin.flow.component.webcomponent.WebComponentDefinition;

import elemental.json.Json;

@Tag("fire-event")
public class FireEventExporter implements WebComponentExporter<FireEventComponent> {
    @Override
    public void define(WebComponentDefinition<FireEventComponent> definition) {
        definition.setInstanceConfigurator((webComponent, component) -> {
            component.setSumConsumer(number -> webComponent.fireEvent("sum-calculated",
                    Json.create(number)));
            component.setErrorConsumer(err -> webComponent.fireEvent("sum" +
                    "-error", Json.create(err)));

            component.setButtonConsumer(buttonId -> {
                switch (buttonId) {
                    case 1:
                        webComponent.fireEvent("button-event",
                                Json.create(buttonId),
                                new EventOptions(false));
                        break;
                    case 2:
                        webComponent.fireEvent("button-event",
                                Json.create(buttonId),
                                new EventOptions(true));
                        break;
                    case 3:
                        webComponent.fireEvent("button-event",
                                Json.create(buttonId),
                                new EventOptions(true, true, false));
                        break;
                }
            });
        });
    }
}
