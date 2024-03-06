/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.PolymerPropertiesView", layout = ViewTestLayout.class)
@Tag("template-properties")
@JsModule("./TemplateProperties.js")
public class PolymerPropertiesView extends PolymerTemplate<Message> {

    public PolymerPropertiesView() {
        setId("template");
    }

    @EventHandler
    private void handleClick() {
        getElement().setProperty("name", "foo");
    }
}
