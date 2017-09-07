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
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Id;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.flow.uitest.ui.AbstractDivView;

public class TemplateInTemplateWithIdView extends AbstractDivView {

    @Tag("parent-id-template")
    @HtmlImport("/com/vaadin/flow/uitest/ui/template/ParentIdTemplate.html")
    public static class ParentTemplate extends PolymerTemplate<TemplateModel> {

    }

    @Tag("child-id-template")
    @HtmlImport("/com/vaadin/flow/uitest/ui/template/ChildIdTemplate.html")
    public static class ChildTemplate extends PolymerTemplate<TemplateModel> {
        @Id("text")
        Div div;

        public ChildTemplate() {
            div.setText("@Id injected!");
        }
    }

    @Override
    protected void onShow() {
        ParentTemplate template = new ParentTemplate();
        template.setId("template");
        add(template);
    }
}
