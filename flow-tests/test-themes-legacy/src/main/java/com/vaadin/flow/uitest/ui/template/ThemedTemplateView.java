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
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.uitest.ui.theme.MyTheme;

@Tag("themed-template")
@HtmlImport("frontend://bower_components/themed-template/src/com/ThemedTemplate.html")
@Route(value = "com.vaadin.flow.uitest.ui.template.ThemedTemplateView")
@Theme(MyTheme.class)
public class ThemedTemplateView extends PolymerTemplate<TemplateModel> {

}
