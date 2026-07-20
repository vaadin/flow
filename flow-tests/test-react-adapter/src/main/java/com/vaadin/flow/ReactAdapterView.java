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

        // Reproduces the dialog scenario where the adapter element is attached
        // and then immediately moved in the DOM (disconnect + reconnect) within
        // the same task, before React has committed the portal. A single React
        // component must be rendered, not one per connect.
        var moveTarget = new Div();
        moveTarget.setId("moveTarget");
        var moveButton = new NativeButton("Move while connecting");
        moveButton.setId("moveWhileConnectingButton");
        moveButton.getElement().addEventListener("click",
                event -> moveTarget.getElement().executeJs(
                        // language=JavaScript
                        """
                                const target = $0;
                                target.textContent = '';
                                const first = document.createElement('div');
                                const second = document.createElement('div');
                                target.append(first, second);
                                const adapter = document.createElement('react-input');
                                // First connect schedules a portal asynchronously.
                                first.appendChild(adapter);
                                // Moving it synchronously disconnects and reconnects it
                                // before the portal is committed.
                                second.appendChild(adapter);
                                """,
                        moveTarget.getElement()));

        add(new Div(input, listenerOutput), new Div(setValueButton),
                new Div(setNullButton), new Div(getValueButton, getOutput),
                new Div(moveButton), moveTarget);
    }

}
