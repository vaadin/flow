/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.UI;

public class RouterLinksUI extends UI {

    public RouterLinksUI() {
        RouterLinksTemplate template = new RouterLinksTemplate();
        template.setId("template");
        add(template);
    }
}