/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.humminbird.tutorial.template;

import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.html.Label;
import com.vaadin.ui.Component;
import com.vaadin.ui.Template;

@CodeFor("tutorial-template-components.asciidoc")
public class TemplateComponent extends Template {

    public class MainPage extends Template {

        public void setContent(Component content) {

            //@formatter:off - custom line wrapping
            Element contentContainer = getElement().getChildren().filter(
                    element -> "content".equals(element.getAttribute("id"))).
                    findFirst().get();
           //@formatter:on
            contentContainer.removeAllChildren();
            contentContainer.appendChild(content.getElement());
        }
    }

    void tutorialCode() {
        MainPage page = new MainPage();
        page.setContent(new Label("Hello!"));
    }
}
