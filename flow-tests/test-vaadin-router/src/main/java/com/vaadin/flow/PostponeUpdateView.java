/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.PostponeUpdateView/:test?")
public class PostponeUpdateView extends Div
        implements BeforeLeaveObserver, BeforeEnterObserver {
    private ContinueNavigationAction continueNavigationAction;

    private int next = 1;
    private RouterLink link;
    private NativeButton proceedButton, cancelButton;

    public PostponeUpdateView() {
        link = new RouterLink();
        link.setText("initial");
        link.setId("link");
        link.setRoute(PostponeUpdateView.class,
                new RouteParameters(new RouteParam("test", next++)));
        add(link);

        proceedButton = new NativeButton("proceed", e -> {
            continueNavigationAction.proceed();
            cleanButtons();
        });
        proceedButton.setId("proceedButton");
        cancelButton = new NativeButton("cancel", e -> {
            continueNavigationAction.cancel();
            cleanButtons();
        });
        cancelButton.setId("cancelButton");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String parameter = event.getRouteParameters().get("test").orElse(null);
        if (parameter != null) {
            link.setText(parameter);
            if (Integer.parseInt(parameter) >= next) {
                next = Integer.parseInt(parameter) + 1;
            }
            link.setRoute(PostponeUpdateView.class,
                    new RouteParameters(new RouteParam("test", next++)));
        }
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        continueNavigationAction = event.postpone();

        add(proceedButton, cancelButton);
    }

    private void cleanButtons() {
        remove(proceedButton, cancelButton);
    }
}
