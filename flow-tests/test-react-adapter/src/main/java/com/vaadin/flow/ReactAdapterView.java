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

    public static final String OWN_ROOT_VALUE = "own root value";

    public static final String OWN_ROOT_SERVER_VALUE = "own root server value";

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

        // An adapter element outside a Flow React container, such as one in a
        // dialog overlay in document.body, renders into its own React root.
        var ownRootInput = new ReactInput(OWN_ROOT_VALUE);
        ownRootInput.setId("ownRootInput");

        var ownRootListenerOutput = new Span();
        ownRootListenerOutput.setId("ownRootListenerOutput");
        ownRootInput.addValueChangeListener(ownRootListenerOutput::setText);

        var ownRootContainer = new Div();

        // Attaches the element, then moves it into document.body and removes
        // and re-adds it in the same task, before its root has been created.
        var moveToBodyButton = new NativeButton("Move to body", event -> {
            ownRootContainer.add(ownRootInput);
            ownRootInput.getElement()
                    .executeJs("document.body.appendChild(this); this.remove();"
                            + " document.body.appendChild(this);");
        });
        moveToBodyButton.setId("moveOwnRootInputToBodyButton");

        // Removes and re-adds the element after its root has rendered.
        var reconnectButton = new NativeButton("Reconnect in body",
                event -> ownRootInput.getElement().executeJs(
                        "this.remove(); document.body.appendChild(this);"));
        reconnectButton.setId("reconnectOwnRootInputButton");

        var setOwnRootValueButton = new NativeButton("Set own root value",
                event -> ownRootInput.setValue(OWN_ROOT_SERVER_VALUE));
        setOwnRootValueButton.setId("setOwnRootValueButton");

        add(new Div(input, listenerOutput), new Div(setValueButton),
                new Div(setNullButton), new Div(getValueButton, getOutput),
                new Div(openButton), teleportingContainer,
                new Div(moveToBodyButton, reconnectButton,
                        setOwnRootValueButton),
                ownRootContainer, ownRootListenerOutput);
    }

}
