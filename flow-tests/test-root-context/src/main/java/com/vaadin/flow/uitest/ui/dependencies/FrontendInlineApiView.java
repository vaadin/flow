/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.router.Route;
import com.vaadin.ui.Tag;
import com.vaadin.ui.UI;
import com.vaadin.ui.polymertemplate.PolymerTemplate;

@Route(value = "com.vaadin.flow.uitest.ui.dependencies.FrontendInlineApiView", layout = ViewTestLayout.class)
@Tag("frontend-inline-api")
public class FrontendInlineApiView extends PolymerTemplate<TemplateModel> {

    public FrontendInlineApiView() {
        super((clazz, tag) -> Jsoup
                .parse("<dom-module id='frontend-inline-api'></dom-module>"));
        setId("template");
        UI.getCurrent().getPage().addHtmlImport(
                "components/frontend-inline-api.html", LoadMode.INLINE);
        UI.getCurrent().getPage().addJavaScript("components/frontend-inline.js",
                LoadMode.INLINE);
        UI.getCurrent().getPage().addStyleSheet(
                "components/frontend-inline.css", LoadMode.INLINE);
    }
}
