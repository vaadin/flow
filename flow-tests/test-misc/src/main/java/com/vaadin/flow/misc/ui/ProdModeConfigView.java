/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.misc.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;

@Route(value = "prod-mode-config-test")
public class ProdModeConfigView extends Div {
    public ProdModeConfigView() {
        Paragraph productionMode = new Paragraph(String.valueOf(VaadinService
                .getCurrent().getDeploymentConfiguration().isProductionMode()));
        productionMode.setId("productionMode");

        Paragraph devModeLiveReloadEnabled = new Paragraph(String
                .valueOf(VaadinService.getCurrent().getDeploymentConfiguration()
                        .isDevModeLiveReloadEnabled()));
        devModeLiveReloadEnabled.setId("devModeLiveReloadEnabled");

        Paragraph devToolsEnabled = new Paragraph(
                String.valueOf(VaadinService.getCurrent()
                        .getDeploymentConfiguration().isDevToolsEnabled()));
        devToolsEnabled.setId("devToolsEnabled");

        add(productionMode, devModeLiveReloadEnabled, devToolsEnabled);
    }
}
