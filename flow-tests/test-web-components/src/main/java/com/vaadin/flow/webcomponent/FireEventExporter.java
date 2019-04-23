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
import com.vaadin.flow.component.webcomponent.EventOptions;
import com.vaadin.flow.component.webcomponent.WebComponent;

import elemental.json.Json;

public class FireEventExporter
        extends WebComponentExporter<FireEventComponent> {

    public FireEventExporter() {
        super("fire-event");
    }

    @Override
    public void configureInstance(WebComponent<FireEventComponent> webComponent,
                                  FireEventComponent component) {
        component.setSumConsumer(number -> webComponent
                .fireEvent("sum-calculated", Json.create(number)));
        component.setErrorConsumer(err -> webComponent
                .fireEvent("sum" + "-error", Json.create(err)));

        component.setButtonConsumer(optionsType -> {
            EventOptions options = createEventOptions(optionsType);
            webComponent.fireEvent("button-event",
                    Json.create(optionsType.name()), options);
        });
    }

    private EventOptions createEventOptions(FireEventComponent.OptionsType id) {
        switch (id) {
        case NoBubble_NoCancel:
            return new EventOptions(false, false, false);
        case Bubble_NoCancel:
            return new EventOptions(true, false, false);
        case Bubble_Cancel:
            return new EventOptions(true, true, false);
        }
        return new EventOptions();
    }
}
