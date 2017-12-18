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
package com.vaadin.flow.tutorial.element;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("element-api/tutorial-shadow-root.asciidoc")
public class ShadowRootSamples {

    @Tag("my-label")
    public class MyLabel extends Component {

        public MyLabel() {
            ShadowRoot shadowRoot = getElement().attachShadow();
            Label textLabel = new Label("In the shadow");
            shadowRoot.appendChild(textLabel.getElement());
        }
    }

    @SuppressWarnings("unused")
    void tutorialCode() {
        Element element = new Element("custom-element");
        ShadowRoot shadowRoot = element.attachShadow();
    }
}
