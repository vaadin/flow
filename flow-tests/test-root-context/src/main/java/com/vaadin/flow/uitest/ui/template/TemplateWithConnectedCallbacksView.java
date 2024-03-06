/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.template.TemplateWithConnectedCallbacksView", layout = ViewTestLayout.class)
public class TemplateWithConnectedCallbacksView extends AbstractDivView {

    private TemplateWithConnectedCallbacks component;

    public TemplateWithConnectedCallbacksView() {
        add(createButton("Toggle template", "toggle-button", evt -> {
            if (component == null) {
                addNewTemplate();
            } else {
                remove(component);
                component = null;
            }
        }));
        addNewTemplate();
    }

    private void addNewTemplate() {
        component = new TemplateWithConnectedCallbacks();
        add(component);
    }
}
