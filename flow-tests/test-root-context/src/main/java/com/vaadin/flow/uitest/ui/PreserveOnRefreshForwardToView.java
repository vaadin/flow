/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.PreserveOnRefreshForwardToView")
@PreserveOnRefresh
public class PreserveOnRefreshForwardToView extends Div
        implements BeforeEnterObserver {

    public PreserveOnRefreshForwardToView() {
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getPathWithQueryParameters()
                .contains("initial")) {
            QueryParameters queryParameters = QueryParameters.of("afterforward",
                    "true");
            Location location = new Location(event.getLocation().getPath(),
                    queryParameters);
            event.getUI().getPage().getHistory().replaceState(null, location);
        }
    }
}
