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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.TemplateHasInjectedSubTemplateView", layout = ViewTestLayout.class)
@Tag("parent-inject-child")
@JsModule("./ParentTemplateInjectChild.js")
public class TemplateHasInjectedSubTemplateView
        extends PolymerTemplate<Message> {

    @Tag("injected-child")
    @JsModule("./InjectedChild.js")
    public static class InjectedChild extends PolymerTemplate<Message> {
        @Override
        protected Message getModel() {
            return super.getModel();
        }
    }

    @Id("child")
    private InjectedChild child;

    public TemplateHasInjectedSubTemplateView() {
        setId("template");
        child.getModel().setText("foo");
    }

    @EventHandler
    private void updateChild() {
        child.getModel().setText("bar");
    }
}
