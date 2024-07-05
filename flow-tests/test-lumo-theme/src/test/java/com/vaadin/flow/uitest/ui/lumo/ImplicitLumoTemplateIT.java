/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.lumo;

public class ImplicitLumoTemplateIT extends AbstractThemedTemplateIT {

    @Override
    protected String getTagName() {
        return "implicit-lumo-themed-template";
    }

    @Override
    protected String getThemedTemplate() {
        return "theme/lumo/ImplicitLumoThemedTemplate.html";
    }
}
