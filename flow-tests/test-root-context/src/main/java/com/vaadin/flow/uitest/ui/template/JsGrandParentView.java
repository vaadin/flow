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
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Tag("js-grand-parent")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/JsGrandParent.html")
@Route(value = "com.vaadin.flow.uitest.ui.template.JsGrandParentView", layout = ViewTestLayout.class)
@Uses(JsSubTemplate.class)
@JsModule("JsGrandParent.js")
public class JsGrandParentView extends PolymerTemplate<TemplateModel> {

    public void updateChildViaClientSide() {
        getElement().callJsFunction("updateSubTempate");
    }
}
