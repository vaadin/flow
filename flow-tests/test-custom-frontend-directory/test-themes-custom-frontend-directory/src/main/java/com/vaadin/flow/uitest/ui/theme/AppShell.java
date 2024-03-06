/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.theme;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;

import com.vaadin.flow.uitest.ui.dependencies.TestVersion;

@Theme(value = "app-theme")
@NpmPackage(value = "@vaadin/vaadin-themable-mixin", version = TestVersion.VAADIN)
@NpmPackage(value = "@fortawesome/fontawesome-free", version = TestVersion.FONTAWESOME)
public class AppShell implements AppShellConfigurator {
}
