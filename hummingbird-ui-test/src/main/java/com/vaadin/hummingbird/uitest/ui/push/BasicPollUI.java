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
import com.vaadin.server.VaadinRequest;

public class BasicPollUI extends ClientServerCounterUI {

    public static final String STOP_POLLING_BUTTON = "stopPolling";
    public static final String START_POLLING_BUTTON = "startPolling";

    Element pollInterval = new Element("div");
    Element pollCounter = new Element("div");
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
        pollCounter.setTextContent("Polls received: 0");

        Element stopPolling = new Element("button").setAttribute("id",
                STOP_POLLING_BUTTON);
        stopPolling.setTextContent("Stop polling");
        stopPolling.addEventListener("click", () -> {
            setPollInterval(-1);
            updatePollIntervalText();
        });
        Element startPolling = new Element("button").setAttribute("id",
                START_POLLING_BUTTON);
        startPolling.setTextContent("Start polling");
        startPolling.addEventListener("click", () -> {
            setPollInterval(500);
            updatePollIntervalText();
        });

        spacer();
        getElement().appendChild(
                new Element("div").appendChild(startPolling, stopPolling));

        updatePollIntervalText();
    }

    private void updatePollIntervalText() {
        pollInterval
                .setTextContent("Poll interval: " + getPollInterval() + "ms");
    }

}
