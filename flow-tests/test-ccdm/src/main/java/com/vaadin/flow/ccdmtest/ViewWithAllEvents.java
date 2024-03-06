/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.ccdmtest;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

@Route(value = "view-with-all-events", layout = MainLayout.class)
public class ViewWithAllEvents extends Div implements BeforeEnterObserver,
        AfterNavigationObserver, HasUrlParameter<String>, BeforeLeaveObserver {
    private Div logger;

    public ViewWithAllEvents() {
        logger = new Div();
        add(logger);
    }

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter String parameter) {
        addLog("1 setParameter");
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        addLog("4 afterNavigation");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        addLog("2 beforeEnter");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        addLog("3 onAttach");
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        addLog("onDetach");
    }

    private void addLog(String log) {
        logger.add(new Paragraph("ViewWithAllEvents: " + log));
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        addLog("beforeLeave");
    }
}
