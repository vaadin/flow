/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.templatemodel.TemplateModel;

@Tag("template-scalability-panel")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/template-scalability-panel.html")
@JsModule("./template-scalability-panel.js")
public class TemplateScalabilityPanel extends PolymerTemplate<TemplateModel> {

    @Id("ack-btn")
    private NativeButton ackBtn;

    public TemplateScalabilityPanel(String name) {
        ackBtn.setText(name);
        setId(name);
    }
}
