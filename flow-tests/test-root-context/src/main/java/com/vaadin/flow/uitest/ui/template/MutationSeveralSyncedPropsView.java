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
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Tag("multiple-props-mutation")
@JsModule("./MultiplePropsMutation.js")
@Route(value = "com.vaadin.flow.uitest.ui.template.MutationSeveralSyncedPropsView", layout = ViewTestLayout.class)
public class MutationSeveralSyncedPropsView
        extends PolymerTemplate<TemplateModel> {

    public MutationSeveralSyncedPropsView() {
        getElement().addPropertyChangeListener("name", "name-changed",
                event -> {
                });
        getElement().addPropertyChangeListener("message", "message-changed",
                event -> {
                });

        getElement().setProperty("name", "foo");
        getElement().setProperty("message", "msg");

        setId("template");

        NativeButton button = AbstractDivView.createButton(
                "Update two synchronized properties simultaneously", "update",
                event -> {
                    getElement().setProperty("name", "bar");
                    getElement().setProperty("message", "baz");
                });
        getElement().appendChild(button.getElement());
    }
}
