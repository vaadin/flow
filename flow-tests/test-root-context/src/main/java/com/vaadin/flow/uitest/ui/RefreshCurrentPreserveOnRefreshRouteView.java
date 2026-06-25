/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.RefreshCurrentPreserveOnRefreshRouteView", layout = RefreshCurrentRouteLayout.class)
@PreserveOnRefresh
public class RefreshCurrentPreserveOnRefreshRouteView
        extends RefreshCurrentRouteView {

    protected String getNavigationTarget() {
        return "com.vaadin.flow.uitest.ui.RefreshCurrentPreserveOnRefreshRouteView";
    }
}
