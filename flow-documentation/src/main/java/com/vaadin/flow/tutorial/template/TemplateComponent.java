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
package com.vaadin.flow.tutorial.template;

import com.vaadin.annotations.Id;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.html.Label;
import com.vaadin.ui.AngularTemplate;
import com.vaadin.ui.Component;

@Deprecated
@CodeFor("deprecated/tutorial-template-components.asciidoc")
public class TemplateComponent extends AngularTemplate {

    public class MainPage extends AngularTemplate {

        @Id("content")
        private Div container;

        public void setContent(Component content) {
            container.removeAll();
            container.add(content);
        }
    }

    void tutorialCode() {
        MainPage page = new MainPage();
        page.setContent(new Label("Hello!"));
    }
}
