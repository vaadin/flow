/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.theme;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;

@Tag("parser-template")
@HtmlImport("com/vaadin/flow/uitest/ui/custom-theme/HtmlParserThemeTemplate.html")
@Route(value = "com.vaadin.flow.uitest.ui.theme.HtmlParserThemeTemplateView")
@Theme(MyTheme.class)
public class HtmlParserThemeTemplateView
        extends PolymerTemplate<TemplateModel> {
}
