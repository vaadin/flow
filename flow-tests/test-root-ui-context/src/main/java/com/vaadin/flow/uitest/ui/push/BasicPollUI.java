/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.VaadinRequest;

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
            pollCounter.setText("Polls received: " + (++pollCount));
        });

        NativeButton stopPolling = new NativeButton("Stop polling", e -> {
            setPollInterval(-1);
            updatePollIntervalText();
        });
        stopPolling.setId(STOP_POLLING_BUTTON);

        NativeButton startPolling = new NativeButton("Start polling", e -> {
            setPollInterval(500);
            updatePollIntervalText();
        });
        startPolling.setId(START_POLLING_BUTTON);

        spacer();
        getElement()
                .appendChild(new Div(startPolling, stopPolling).getElement());

        updatePollIntervalText();
    }

    private void updatePollIntervalText() {
        pollInterval.setText("Poll interval: " + getPollInterval() + "ms");
    }

}
