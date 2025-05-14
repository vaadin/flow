/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
