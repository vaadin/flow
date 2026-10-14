/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.prodbundle;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.uitest.ui.dependencies.TestVersion;

@Theme("vaadin-prod-bundle")
@PWA(name = "vaadin-prod-bundle", shortName = "vaadin-prod-bundle")
@JsModule("@vaadin/horizontal-layout")
@NpmPackage(value = "@vaadin/horizontal-layout", version = TestVersion.VAADIN)
public class FakeAppConf implements AppShellConfigurator {

}
