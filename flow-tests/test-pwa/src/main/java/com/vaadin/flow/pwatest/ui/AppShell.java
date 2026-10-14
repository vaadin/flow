/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.pwatest.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;

@PWA(name = AppShell.PWA_NAME, shortName = AppShell.PWA_SHORT_NAME, themeColor = AppShell.THEME_COLOR, backgroundColor = AppShell.BG_COLOR, offlinePath = "offline.html", offlineResources = {
        "yes.png" })
public class AppShell implements AppShellConfigurator {
    static final String THEME_COLOR = "#1f1f1f";
    static final String BG_COLOR = "#ffffff";
    static final String PWA_NAME = "PWA test name";
    static final String PWA_SHORT_NAME = "PWA";

    public AppShell() {
    }
}
