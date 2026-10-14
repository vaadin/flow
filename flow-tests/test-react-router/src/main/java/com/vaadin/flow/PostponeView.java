/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("com.vaadin.flow.PostponeView")
public class PostponeView extends Div implements BeforeLeaveObserver {

    public static String CONTINUE_ID = "continue-button";
    public static String STAY_ID = "stay-button";
    public static String NAVIGATION_ID = "anchor-to-navigation";
    public static String NAVIGATION_ROUTER_LINK_ID = "routerlink-to-navigation";

    private NativeButton navigate, stay;

    public PostponeView() {
        Anchor link = new Anchor("com.vaadin.flow.NavigationView",
                "Navigation");// NavigationView.class);
        link.setId(NAVIGATION_ID);
        RouterLink routerLink = new RouterLink("Navigation",
                NavigationView.class);
        routerLink.setId(NAVIGATION_ROUTER_LINK_ID);

        add(new Span("PostponeView"), new Div(), link, new Div(), routerLink);
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        BeforeLeaveEvent.ContinueNavigationAction postpone = event.postpone();
        navigate = new NativeButton("Continue", e -> {
            postpone.proceed();
            remove(navigate, stay);
        });
        navigate.setId(CONTINUE_ID);
        stay = new NativeButton("Stay", e -> {
            postpone.cancel();
            remove(navigate, stay);
        });
        stay.setId(STAY_ID);

        add(navigate, stay);
    }
}
