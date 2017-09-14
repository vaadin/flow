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
package com.vaadin.flow.tutorial.polymer;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Id;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.html.Label;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.ui.Component;

@CodeFor("polymer-templates/tutorial-template-components.asciidoc")
public class PolymerComponents {
    @Tag("main-page")
    @HtmlImport("/com/example/MainPage.html")
    public class MainPage extends PolymerTemplate<TemplateModel> {

        @Id("content")
        private Div content;

        public void setContent(Component content) {
            this.content.removeAll();
            this.content.add(content);
        }
    }

    private void snippets() {
        MainPage page = new MainPage();
        page.setContent(new Label("Hello!"));
    }
}
