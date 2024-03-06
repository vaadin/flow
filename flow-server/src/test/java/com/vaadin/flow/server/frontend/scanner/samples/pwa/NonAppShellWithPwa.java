/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.scanner.samples.pwa;

import com.vaadin.flow.server.PWA;

@PWA(name = "PWA Application", shortName = "PWA", description = "Testing PWA", display = "minimal-ui", backgroundColor = "#eee", themeColor = "#369", iconPath = "pwa.png", manifestPath = "appmanifest.json", offlinePath = "pwa.html", offlineResources = {
        "pwa.js", "pwa.css" })
public class NonAppShellWithPwa {
}
