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
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;

/**
 * Tests a scalability bug #5806 with adding many buttons to a view.
 */
@Tag("template-scalability-view")
@JsModule("./template-scalability-view.js")
@Route(value = "com.vaadin.flow.uitest.ui.template.TemplateScalabilityView")
@PageTitle("Template scalability")
public class TemplateScalabilityView extends PolymerTemplate<TemplateModel>
        implements AfterNavigationObserver {

    public static final String COMPLETED = "completed";

    public static final int NUM_ITEMS = 50;

    @Id("content")
    private Div div;

    public TemplateScalabilityView() {
        setId("scalability-view");
    }

    private void generateChildren() {
        div.removeAll();
        for (int i = 0; i < NUM_ITEMS; ++i) {
            TemplateScalabilityPanel p = new TemplateScalabilityPanel(
                    "Panel " + i);
            div.add(p);
        }

        Div complete = new Div();
        complete.setId(COMPLETED);
        div.addComponentAsFirst(complete);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        generateChildren();
    }
}
