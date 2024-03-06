/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.pwatest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.PWA;

@PWA(name = ParentLayout.PWA_NAME, shortName = ParentLayout.PWA_SHORT_NAME, themeColor = ParentLayout.THEME_COLOR, backgroundColor = ParentLayout.BG_COLOR, offlinePath = "offline.html", offlineResources = {
        "yes.png" })
public class ParentLayout extends Div
        implements RouterLayout, AppShellConfigurator {
    static final String THEME_COLOR = "#1f1f1f";
    static final String BG_COLOR = "#ffffff";
    static final String PWA_NAME = "PWA test name";
    static final String PWA_SHORT_NAME = "PWA";

    public ParentLayout() {
    }
}
