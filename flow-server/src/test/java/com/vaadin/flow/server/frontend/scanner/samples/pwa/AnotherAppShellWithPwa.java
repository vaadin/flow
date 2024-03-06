/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.scanner.samples.pwa;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;

@PWA(name = "Another PWA Application", shortName = "Another PWA", description = "Testing another PWA", display = "minimal-ui", backgroundColor = "#eee", themeColor = "#369", iconPath = "another_pwa.png", manifestPath = "appmanifest.json", offlinePath = "another_pwa.html", offlineResources = {
        "another_pwa.js", "another_pwa.css" })
public class AnotherAppShellWithPwa implements AppShellConfigurator {
}
