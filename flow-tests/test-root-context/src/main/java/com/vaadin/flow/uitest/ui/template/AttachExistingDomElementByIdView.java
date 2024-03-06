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
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.AttachExistingDomElementByIdView", layout = ViewTestLayout.class)
@JsModule("./AttachExistingDomElementById.js")
@Tag("existing-dom-element")
public class AttachExistingDomElementByIdView
        extends PolymerTemplate<TemplateModel> {

    @Id("input")
    private Element input;

    @Id("label")
    private Element label;

    public AttachExistingDomElementByIdView() {
        setId("template");

        input.setProperty("placeholder", "Foo");
        label.setText("bar");
        input.addPropertyChangeListener("value", "change", event -> {
        });
    }

    @EventHandler
    private void clear() {
        label.setText("default");
        input.setProperty("value", "");
    }

    @EventHandler
    private void valueChange() {
        label.setText("Text from input " + input.getProperty("value"));
    }

}
