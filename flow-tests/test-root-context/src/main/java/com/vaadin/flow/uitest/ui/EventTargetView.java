/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.EventTargetView", layout = ViewTestLayout.class)
public class EventTargetView extends AbstractEventDataView {

    public static final String TARGET_ID = "target";

    public EventTargetView() {
        final Div eventTarget = new Div();
        eventTarget.setId(TARGET_ID);
        addComponentAtIndex(1, new H3("Event.target reported for any child."));
        addComponentAtIndex(2, eventTarget);

        getElement().addEventListener("click", event -> {
            eventTarget.setText(event.getEventTarget()
                    .map(element -> element.getText()).orElse(EMPTY_VALUE));
        }).mapEventTargetElement();

        createComponents();
    }

}
