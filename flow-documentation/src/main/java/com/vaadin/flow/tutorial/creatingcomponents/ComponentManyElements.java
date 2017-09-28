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
package com.vaadin.flow.tutorial.creatingcomponents;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("creating-components/tutorial-component-many-elements.asciidoc")
public class ComponentManyElements {

    @Tag("div")
    public class TextField extends Component {

        Element labelElement = new Element("label");
        Element inputElement = new Element("input");

        public TextField() {
            inputElement.synchronizeProperty("value", "change");
            getElement().appendChild(labelElement, inputElement);
        }

        public String getLabel() {
            return labelElement.getText();
        }

        public String getValue() {
            return inputElement.getProperty("value");
        }

        public TextField setLabel(String label) {
            labelElement.setText(label);
            return this;
        }

        public TextField setValue(String value) {
            inputElement.setProperty("value", value);
            return this;
        }

    }

    private Element labelElement;
    private Element inputElement;

    public String getLabel() {
        return labelElement.getText();
    }

    public String getValue() {
        return inputElement.getProperty("value");
    }

    public void setLabel(String label) {
        labelElement.setText(label);
    }

    public void setValue(String value) {
        inputElement.setProperty("value", value);
    }

    {
        // @formatter:off
        new TextField()
            .setLabel("Zip code")
            .setValue("12345");
        // @formatter:on

    }
}
