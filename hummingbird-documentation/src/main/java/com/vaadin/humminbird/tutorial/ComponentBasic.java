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
package com.vaadin.humminbird.tutorial;

import com.vaadin.annotations.Tag;
import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.ui.Component;

@CodeFor("tutorial-component-basic.asciidoc")
public class ComponentBasic {

    @Tag("input")
    public class TextField extends Component {

        public TextField(String value) {
            getElement().setProperty("value", value);
            getElement().synchronizeProperty("value", "change");
        }

        public String getValue() {
            return getElement().getProperty("value");
        }

        public void setValue(String value) {
            getElement().setProperty("value", value);
        }
    }

    @Tag("div")
    public class TextFieldWithLabel extends Component {

        Element labelElement = new Element("label");
        Element inputElement = new Element("input");

        public TextFieldWithLabel() {
            inputElement.synchronizeProperty("value", "change");
            getElement().appendChild(labelElement, inputElement);
        }

        public String getLabel() {
            return labelElement.getOwnTextContent();
        }

        public String getValue() {
            return inputElement.getProperty("value");
        }

        public TextFieldWithLabel setLabel(String label) {
            labelElement.setTextContent(label);
            return this;
        }

        public TextFieldWithLabel setValue(String value) {
            inputElement.setProperty("value", value);
            return this;
        }

    }

    private Element labelElement;
    private Element inputElement;

    public void setLabel(String label) {
        labelElement.setTextContent(label);
    }

    public void setValue(String value) {
        inputElement.setProperty("value", value);
    }

    {
        // @formatter:off
        new TextFieldWithLabel()
            .setLabel("Zip code")
            .setValue("12345");
        // @formatter:on

    }
}
