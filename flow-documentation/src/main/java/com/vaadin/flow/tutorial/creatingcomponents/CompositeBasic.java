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

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("creating-components/tutorial-component-composite.asciidoc")
public class CompositeBasic {
    public class TextField extends Composite<Div> {

        private Label label;
        private Input input;

        public TextField(String labelText, String value) {
            label = new Label();
            label.setText(labelText);
            input = new Input();
            input.setValue(value);

            getContent().add(label, input);
        }

        public String getValue() {
            return input.getValue();
        }

        public void setValue(String value) {
            input.setValue(value);
        }

        public String getLabel() {
            return label.getText();
        }

        public void setLabel(String labelText) {
            label.setText(labelText);
        }
    }

}
