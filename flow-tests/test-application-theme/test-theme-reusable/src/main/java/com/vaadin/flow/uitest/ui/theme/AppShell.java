/**
 * Copyright (C) 2000-2023 Vaadin Ltd
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

@Theme(value = "reusable-theme")
@NpmPackage(value = "@vaadin/vaadin-themable-mixin", version = TestVersion.VAADIN)
public class AppShell implements AppShellConfigurator {
}