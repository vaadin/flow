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
