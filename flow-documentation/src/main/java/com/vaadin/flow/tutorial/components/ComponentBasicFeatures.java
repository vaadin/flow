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
package com.vaadin.flow.tutorial.components;

import java.util.stream.Collectors;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("components/tutorial-component-basic-features.asciidoc")
public class ComponentBasicFeatures {

    @Id("my-component")
    private Component mappedComponent;

    public void visibility() {
        Label label = new Label("My label");
        label.setVisible(false);
        // this is not transmitted to the client side
        label.setText("Changed my label");

        //@formatter:off
        Button makeVisible = new Button("Make visible", evt -> {
            // makes the label visible - only now the "Changed my label" text is transmitted
            label.setVisible(true);
        });
        //@formatter:on

        Div container = new Div();
        // the label is not transmitted to the client side. The corresponding
        // element will be created in the DOM only when it becomes visible
        container.add(label);

        // prints 1 - the server-side structure is preserved no matter if the
        // component is visible or not
        System.out.println("Number of children: "
                + container.getChildren().collect(Collectors.counting()));

        // sets the attribute "hidden" of the element on the client-side
        mappedComponent.setVisible(false);
    }

    public void id() {
        Label component = new Label();
        component.setId("my-component");
    }

}
