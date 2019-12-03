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

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.dependencies.AnnotatedFrontendInlineView", layout = ViewTestLayout.class)
@Tag("frontend-inline")
@JavaScript(value = "components/frontend-inline.js", loadMode = LoadMode.INLINE)
@HtmlImport(value = "components/frontend-inline.html", loadMode = LoadMode.INLINE)
@StyleSheet(value = "components/frontend-inline.css", loadMode = LoadMode.INLINE)
public class AnnotatedFrontendInlineView
        extends PolymerTemplate<TemplateModel> {

    public AnnotatedFrontendInlineView() {
        setId("template");
    }
}
