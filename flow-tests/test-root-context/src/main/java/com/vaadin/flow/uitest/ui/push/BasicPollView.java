/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.push.components.ClientServerCounter;

@Route(value = "com.vaadin.flow.uitest.ui.push.BasicPollView", layout = ViewTestLayout.class)
public class BasicPollView extends ClientServerCounter {

    public static final String STOP_POLLING_BUTTON = "stopPolling";
    public static final String START_POLLING_BUTTON = "startPolling";

    Element pollInterval = ElementFactory.createDiv();
    Element pollCounter = ElementFactory.createDiv("Polls received: 0");
    int pollCount = 0;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        getElement().insertChild(0, pollInterval);

        getElement().insertChild(0, pollCounter);
        ui.setPollInterval(500);
        ui.addPollListener(e -> {
            pollCounter.setText("Polls received: " + (++pollCount));
        });

        NativeButton stopPolling = new NativeButton("Stop polling", e -> {
            ui.setPollInterval(-1);
            updatePollIntervalText();
        });
        stopPolling.setId(STOP_POLLING_BUTTON);

        NativeButton startPolling = new NativeButton("Start polling", e -> {
            ui.setPollInterval(500);
            updatePollIntervalText();
        });
        startPolling.setId(START_POLLING_BUTTON);

        spacer();
        getElement()
                .appendChild(new Div(startPolling, stopPolling).getElement());

        updatePollIntervalText();
    }

    private void updatePollIntervalText() {
        pollInterval.setText(
                "Poll interval: " + getUI().get().getPollInterval() + "ms");
    }

}
