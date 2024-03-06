/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.uitest.ui.AbstractDivView;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.template.SubPropertyModelView")
public class SubPropertyModelView extends AbstractDivView {

    public SubPropertyModelView() {
        SubPropertyModelTemplate template = new SubPropertyModelTemplate();
        template.setId("template");
        add(template);
    }
}
