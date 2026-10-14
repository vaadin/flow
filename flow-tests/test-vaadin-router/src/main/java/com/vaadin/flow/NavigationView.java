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
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("com.vaadin.flow.NavigationView")
public class NavigationView extends Div {

    public static final String SERVER_ID = "server-navigation";
    public static final String ANCHOR_ID = "anchor-navigation";
    public static final String ANCHOR_QUERY_ID = "anchor-query-navigation";
    public static final String ROUTER_LINK_QUERY_ID = "router-link-query-navigation";
    public static final String ROUTER_LINK_ID = "router-link-navigation";
    public static final String POSTPONE_ID = "postpone-view-link";

    public static final String SET_PARAMETER_COUNTER_ID = "set-parameter-counter";

    public NavigationView() {
        Anchor anchorNavigation = new Anchor("com.vaadin.flow.AnchorView",
                "Navigate to AnchorView");
        anchorNavigation.setId(ANCHOR_ID);
        NativeButton serverNavigation = new NativeButton(
                "Navigate through Server", event -> {
                    event.getSource().getUI().get().navigate(ServerView.class);
                });
        serverNavigation.setId(SERVER_ID);
        RouterLink link = new RouterLink("RouterView", RouterView.class);
        link.setId(ROUTER_LINK_ID);

        RouterLink postponeView = new RouterLink("PostponeView",
                PostponeView.class);
        postponeView.setId(POSTPONE_ID);

        Span viewName = new Span("NavigationView");
        viewName.setVisible(false);
        add(viewName, new Div(), anchorNavigation, new Div(), serverNavigation,
                new Div(), link, new Div(), postponeView);

        RouterLink rlViewQuery = new RouterLink("AnchorQuery",
                AnchorView.class);
        rlViewQuery.setQueryParameters(QueryParameters.of("test", "value"));
        rlViewQuery.setId(ROUTER_LINK_QUERY_ID);
        add(new Div(), rlViewQuery);

        Anchor anchorViewQuery = new Anchor(
                "com.vaadin.flow.AnchorView?test=anchor", "AnchorQuery");
        anchorViewQuery.setId(ANCHOR_QUERY_ID);
        add(new Div(), anchorViewQuery);

        // vaadin-router fires a vaadin-router-location-changed for client side
        // navigation, whereas server side History changes fires a
        // vaadin-navigated
        getElement()
                .executeJs(
                        """
                                if(!window.test) {
                                    window.addEventListener('vaadin-navigated', (e) => {
                                        console.log('vaadin-navigated :: navigated to ' + window.location.pathname, e);
                                        window.testMessage = 'navigated to ' + window.location.pathname;
                                    });
                                    window.addEventListener('vaadin-router-location-changed', (e) => {
                                        console.log('location-changed :: navigated to ' + window.location.pathname, e);
                                        window.testMessage = 'navigated to ' + window.location.pathname;
                                    });
                                    window.test = true;
                                }
                                """)
                // Make element visible only after the listeners are installed
                // so the test can wait for this condition before starting
                // asserting
                .then(ignore -> viewName.setVisible(true));

    }

}
