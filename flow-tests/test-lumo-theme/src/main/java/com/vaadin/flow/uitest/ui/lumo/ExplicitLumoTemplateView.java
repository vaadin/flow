/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.lumo;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@Tag("explicit-lumo-themed-template")
@HtmlImport("frontend://bower_components/themed-template/src/LumoThemedTemplate.html")
@Route(value = "com.vaadin.flow.uitest.ui.lumo.ExplicitLumoTemplateView")
@Theme(value = Lumo.class, variant = Lumo.DARK)
public class ExplicitLumoTemplateView extends PolymerTemplate<TemplateModel> {

}
