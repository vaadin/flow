/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.dependencies;

import org.jsoup.Jsoup;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.TemplateParser.TemplateData;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.dependencies.FrontendInlineApiView", layout = ViewTestLayout.class)
@Tag("frontend-inline-api")
public class FrontendInlineApiView extends PolymerTemplate<TemplateModel> {

    public FrontendInlineApiView() {
        super((clazz, tag, service) -> new TemplateData(
                "components/frontend-inline-api.html", Jsoup.parse(
                        "<dom-module id='frontend-inline-api'></dom-module>")));
        setId("template");
        UI.getCurrent().getPage().addHtmlImport(
                "components/frontend-inline-api.html", LoadMode.INLINE);
        UI.getCurrent().getPage().addJavaScript("components/frontend-inline.js",
                LoadMode.INLINE);
        UI.getCurrent().getPage().addStyleSheet(
                "components/frontend-inline.css", LoadMode.INLINE);
    }
}
