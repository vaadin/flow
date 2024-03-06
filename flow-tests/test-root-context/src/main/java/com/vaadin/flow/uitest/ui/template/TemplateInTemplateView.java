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
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.template.TemplateInTemplateView", layout = ViewTestLayout.class)
public class TemplateInTemplateView extends AbstractDivView {

    @Tag("parent-template")
    @Uses(ChildTemplate.class)
    @JsModule("./ParentTemplate.js")
    public static class ParentTemplate extends PolymerTemplate<Message> {

    }

    @Tag("child-template")
    @JsModule("./ChildTemplate.js")
    public static class ChildTemplate extends PolymerTemplate<Message> {

        @EventHandler
        private void handleClick() {
            getModel().setText("foo");
            Div div = new Div();
            div.setText("Click is handled by child template");
            div.setId("click-handler");
            getUI().get().add(div);
        }
    }

    @Override
    protected void onShow() {
        ParentTemplate template = new ParentTemplate();
        template.setId("template");
        add(template);
    }
}
