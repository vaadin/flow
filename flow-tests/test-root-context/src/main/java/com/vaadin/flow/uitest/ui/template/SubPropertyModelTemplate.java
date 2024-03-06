/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.ModelItem;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.ui.template.SubPropertyModelTemplate.SubPropertyModel;

@Tag("sub-property-model")
@JsModule("./SubPropertyModel.js")
public class SubPropertyModelTemplate
        extends PolymerTemplate<SubPropertyModel> {

    public interface Status {
        void setMessage(String message);

        String getMessage();
    }

    public interface SubPropertyModel extends TemplateModel {
        void setStatus(Status status);
    }

    public SubPropertyModelTemplate() {
        setMessage("message");
    }

    @EventHandler
    private void update() {
        setMessage("Updated");
    }

    @EventHandler
    private void sync() {
        Div div = new Div();
        div.setId("synced-msg");
        div.setText(getStatus().getMessage());
        ((HasComponents) getParent().get()).add(div);
    }

    @EventHandler
    private void valueUpdated() {
        Div div = new Div();
        div.setId("value-update");
        div.setText(getStatus().getMessage());
        ((HasComponents) getParent().get()).add(div);
    }

    @EventHandler
    private void click(@ModelItem("status") Status statusItem) {
        Div div = new Div();
        div.setId("statusClick");
        div.setText(statusItem.getMessage());
        ((HasComponents) getParent().get()).add(div);
    }

    private void setMessage(String message) {
        getStatus().setMessage(message);
    }

    private Status getStatus() {
        return getModel().getProxy("status", Status.class);
    }
}
