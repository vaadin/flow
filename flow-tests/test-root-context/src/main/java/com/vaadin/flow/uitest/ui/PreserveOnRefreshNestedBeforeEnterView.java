package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

@Route(value = "com.vaadin.flow.uitest.ui.PreserveOnRefreshNestedBeforeEnterView", layout = PreserveOnRefreshNestedBeforeEnterView.NestedLayout.class)
public class PreserveOnRefreshNestedBeforeEnterView
        extends PreserveOnRefreshNestedBeforeEnterCounter {

    @PreserveOnRefresh
    public static class RootLayout extends
            PreserveOnRefreshNestedBeforeEnterCounter implements RouterLayout {
    }

    @ParentLayout(RootLayout.class)
    public static class NestedLayout extends
            PreserveOnRefreshNestedBeforeEnterCounter implements RouterLayout {
    }
}
