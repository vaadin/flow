/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;

@Route(value = "com.vaadin.flow.uitest.ui.template.PolymerTemplateWithoutShadowRootView")
@PageTitle("PolymerTemplate without a shadow root")
public class PolymerTemplateWithoutShadowRootView extends Div {

    @JsModule("./template-without-shadow-root-view.js")
    @Tag("template-without-shadow-root-view")
    public static class Template extends PolymerTemplate<TemplateModel> {

        @Id("content")
        private Div div;
        @Id("special!#id")
        private Div specialId;

        public Template() {
            div.setText("Hello");
            specialId.setText("Special");
            div.addClickListener(e -> {
                div.setText("Goodbye");
            });
        }
    }

    public PolymerTemplateWithoutShadowRootView() {
        Div distractor1 = new Div();
        Div distractor2 = new Div();
        distractor1.setId("content");
        distractor2.setId("content");
        add(distractor1);
        add(new Template());
        add(distractor2);
    }

}
