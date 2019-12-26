/*
 * Copyright 2000-2019 Vaadin Ltd.
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
        UI.getCurrent().getPage().addJavaScript("components/frontend-inline.js",
                LoadMode.INLINE);
        UI.getCurrent().getPage().addStyleSheet(
                "components/frontend-inline.css", LoadMode.INLINE);
    }
}
