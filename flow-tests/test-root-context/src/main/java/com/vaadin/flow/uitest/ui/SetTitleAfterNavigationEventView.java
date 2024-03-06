/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.SetTitleAfterNavigationEventView", layout = ViewTestLayout.class)
public class SetTitleAfterNavigationEventView extends AbstractDivView
        implements HasDynamicTitle, AfterNavigationObserver {

    private String title = "my-initial-title";

    @Override
    public String getPageTitle() {
        return title;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        title = "my-changed-title-after-AfterNavigationEvent";
    }
}
