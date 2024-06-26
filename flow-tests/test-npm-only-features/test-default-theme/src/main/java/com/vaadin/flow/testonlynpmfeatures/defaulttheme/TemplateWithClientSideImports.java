/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testonlynpmfeatures.defaulttheme;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;

@JsModule("./src/template-with-client-side-imports.js")
@NpmPackage(value = "@vaadin/vaadin-button", version = "2.2.0")
@Tag("template-with-client-side-imports")
public class TemplateWithClientSideImports
        extends PolymerTemplate<TemplateModel> {

}
