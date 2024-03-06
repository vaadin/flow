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
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.TwoWayPolymerBindingView", layout = ViewTestLayout.class)
@Tag("my-template")
@JsModule("./TwoWayPolymerBinding.js")
public class TwoWayPolymerBindingView
        extends PolymerTemplate<TwoWayPolymerBindingView.TwoWayModel> {
    public TwoWayPolymerBindingView() {
        setId("template");

        getElement().addPropertyChangeListener("value",
                event -> valueUpdated());
    }

    public interface TwoWayModel extends TemplateModel {
        void setValue(String value);

        String getValue();

        void setStatus(String status);
    }

    @EventHandler
    private void valueUpdated() {
        getModel().setStatus("Value: " + getModel().getValue());
    }

    @EventHandler
    private void resetValue() {
        getModel().setValue("");
        valueUpdated();
    }
}
