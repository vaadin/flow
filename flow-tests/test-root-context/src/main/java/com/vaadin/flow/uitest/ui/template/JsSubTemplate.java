/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.AllowClientUpdates;
import com.vaadin.flow.templatemodel.TemplateModel;

@Tag("js-sub-template")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/JsSubTemplate.html")
@JsModule("JsSubTemplate.js")
public class JsSubTemplate
        extends PolymerTemplate<JsSubTemplate.JsSubTemplateModel> {

    @Id("js-grand-child")
    private JsInjectedGrandChild component;

    public interface JsSubTemplateModel extends TemplateModel {
        @AllowClientUpdates
        String getFoo();

        void setFoo(String value);
    }

    public JsSubTemplate() {
        getModel().setFoo("bar");
    }

}
