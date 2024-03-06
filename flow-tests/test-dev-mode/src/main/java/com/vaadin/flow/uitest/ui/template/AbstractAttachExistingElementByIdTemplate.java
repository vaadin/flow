/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;

public abstract class AbstractAttachExistingElementByIdTemplate
        extends PolymerTemplate<TemplateModel> {

    @Id("input")
    private Input input;

    @Id("label")
    private Label label;

    protected AbstractAttachExistingElementByIdTemplate(String id) {
        setId(id);
        input.setPlaceholder("Type here to update label");
        label.setText("default");
    }

    @EventHandler
    private void clear() {
        label.setText("default");
        input.setValue("");
    }

    @EventHandler
    private void valueChange() {
        label.setText("Text from input " + input.getValue());
    }

}
