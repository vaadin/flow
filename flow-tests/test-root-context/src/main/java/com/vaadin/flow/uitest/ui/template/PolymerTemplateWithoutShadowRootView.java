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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;

@Route(value = "com.vaadin.flow.uitest.ui.template.PolymerTemplateWithoutShadowRootView")
@PageTitle("PolymerTemplate without a shadow root")
public class PolymerTemplateWithoutShadowRootView extends Div {

    @JsModule("./template-without-shadow-root-view.js")
    @Tag("template-without-shadow-root-view")
    public static class Template extends PolymerTemplate<TemplateModel> {

        @Id("content")
        private Div div;
        @Id("special!#id")
        private Div specialId;
        @Id("map")
        private Div map;

        public Template() {
            div.setText("Hello");
            specialId.setText("Special");
            map.setText("Map");
            div.addClickListener(e -> {
                div.setText("Goodbye");
            });
        }
    }

    public PolymerTemplateWithoutShadowRootView() {
        Div distractor1 = new Div();
        Div distractor2 = new Div();
        distractor1.setId("content");
        distractor2.setId("content");
        add(distractor1);
        add(new Template());
        add(distractor2);
    }

}
