/*
 * Copyright 2000-2014 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui.push;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.server.VaadinRequest;

public class BasicPollUI extends ClientServerCounterUI {

    public static final String STOP_POLLING_BUTTON = "stopPolling";
    public static final String START_POLLING_BUTTON = "startPolling";

    Element pollInterval = ElementFactory.createDiv();
    Element pollCounter = ElementFactory.createDiv("Polls received: 0");
    int pollCount = 0;

    @Override
    protected void init(VaadinRequest request) {
        super.init(request);
        getElement().insertChild(0, pollInterval);

        getElement().insertChild(0, pollCounter);
        setPollInterval(500);
        addPollListener(e -> {
            pollCounter.setTextContent("Polls received: " + (++pollCount));
        });

        Element stopPolling = ElementFactory.createButton("Stop polling")
                .setAttribute("id", STOP_POLLING_BUTTON);
        stopPolling.addEventListener("click", e -> {
            setPollInterval(-1);
            updatePollIntervalText();
        });
        Element startPolling = ElementFactory.createButton("Start polling")
                .setAttribute("id", START_POLLING_BUTTON);
        startPolling.addEventListener("click", e -> {
            setPollInterval(500);
            updatePollIntervalText();
        });

        spacer();
        getElement().appendChild(ElementFactory.createDiv()
                .appendChild(startPolling, stopPolling));

        updatePollIntervalText();
    }

    private void updatePollIntervalText() {
        pollInterval
                .setTextContent("Poll interval: " + getPollInterval() + "ms");
    }

}
