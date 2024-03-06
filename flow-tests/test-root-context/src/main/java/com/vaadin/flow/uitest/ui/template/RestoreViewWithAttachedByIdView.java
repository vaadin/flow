/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.template.RestoreViewWithAttachedByIdView", layout = ViewTestLayout.class)
public class RestoreViewWithAttachedByIdView extends AbstractDivView {

    private TemplateWithInjectedId template;
    private Label label;

    private Component current;

    @Tag("template-with-injected-id")
    @JsModule("./TemplateWithInjectedId.js")
    public static class TemplateWithInjectedId
            extends PolymerTemplate<TemplateModel> {

        @Id("ignore")
        private Div ignore;

        @Id("target")
        private Div target;

        public TemplateWithInjectedId() {
            target.setText("Server Side Text");
        }
    }

    public RestoreViewWithAttachedByIdView() {
        template = new TemplateWithInjectedId();
        label = new Label("Switched component");
        label.setId("info");
        template.setId("template");

        add(template);

        NativeButton button = new NativeButton("Switch components");
        current = template;
        button.addClickListener(event -> {
            remove(current);
            if (current == template) {
                current = label;
            } else {
                current = template;
            }
            add(current);
        });
        add(button);
    }
}
