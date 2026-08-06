/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.ReactAdapterView")
public class ReactAdapterView extends Div {

    public static final String TELEPORTED_VALUE = "teleported value";

    public ReactAdapterView() {
        var input = new ReactInput("initialValue");

        var listenerOutput = new Span();
        listenerOutput.setId("listenerOutput");

        input.addValueChangeListener(listenerOutput::setText);

        var setValueButton = new NativeButton("Set value",
                (event) -> input.setValue("set value"));
        setValueButton.setId("setValueButton");

        var setNullButton = new NativeButton("Set null",
                (event) -> input.setValue(null));
        setNullButton.setId("setNullValueButton");

        var getOutput = new Span();
        getOutput.setId("getOutput");

        var getValueButton = new NativeButton("Get value",
                (event) -> getOutput.setText(input.getValue()));
        getValueButton.setId("getValueButton");

        // An overlay-based component such as a dialog moves its content into
        // the overlay right after attaching it, so the adapter element is
        // disconnected and reconnected within the same task, before React has
        // committed the portal. A single React component must be rendered, not
        // one per connect.
        var teleportingContainer = new TeleportingContainer();
        teleportingContainer.setId("teleportingContainer");

        var openButton = new NativeButton("Open teleporting container",
                (event) -> {
                    teleportingContainer.add(new ReactInput(TELEPORTED_VALUE));
                    teleportingContainer.open();
                });
        openButton.setId("openTeleportingContainerButton");

        add(new Div(input, listenerOutput), new Div(setValueButton),
                new Div(setNullButton), new Div(getValueButton, getOutput),
                new Div(openButton), teleportingContainer);
    }

}
