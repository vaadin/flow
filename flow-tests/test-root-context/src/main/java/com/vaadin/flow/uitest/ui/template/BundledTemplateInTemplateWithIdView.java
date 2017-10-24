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

import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;
import com.vaadin.router.Route;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.html.Div;
import com.vaadin.ui.polymertemplate.Id;
import com.vaadin.ui.polymertemplate.PolymerTemplate;

@Route(value = "com.vaadin.flow.uitest.ui.template.BundledTemplateInTemplateWithIdView", layout = ViewTestLayout.class)
public class BundledTemplateInTemplateWithIdView extends AbstractDivView {

    @Tag("parent-id-template-bundled")
    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/ParentIdTemplate.html")
    // This will make the DependencyFilter remove all other imports and add the
    // proper bundle
    @HtmlImport("bundle://something.html")
    public static class ParentTemplate extends PolymerTemplate<TemplateModel> {

    }

    @Tag("child-id-template-bundled")
    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/ChildIdTemplate.html")
    // This will make the DependencyFilter remove all other imports and add the
    // proper bundle
    @HtmlImport("bundle://something.html")
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
