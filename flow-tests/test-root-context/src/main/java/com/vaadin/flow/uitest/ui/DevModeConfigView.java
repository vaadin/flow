/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * For integration tests that need to be done on an empty page.
 */
@Route(value = "com.vaadin.flow.uitest.ui.DevModeConfigView", layout = ViewTestLayout.class)
public class DevModeConfigView extends Div {
    public DevModeConfigView() {
        Paragraph productionMode = new Paragraph(String.valueOf(VaadinService
                .getCurrent().getDeploymentConfiguration().isProductionMode()));
        productionMode.setId("productionMode");

        Paragraph devModeLiveReloadEnabled = new Paragraph(String
                .valueOf(VaadinService.getCurrent().getDeploymentConfiguration()
                        .isDevModeLiveReloadEnabled()));
        devModeLiveReloadEnabled.setId("devModeLiveReloadEnabled");

        add(productionMode, devModeLiveReloadEnabled);
    }
}
