/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
