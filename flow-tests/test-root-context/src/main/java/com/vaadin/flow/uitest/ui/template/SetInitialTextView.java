/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.SetInitialTextView", layout = ViewTestLayout.class)
@Tag("set-initial-text")
@JsModule("./SetInitialText.js")
public class SetInitialTextView extends PolymerTemplate<TemplateModel>
        implements HasComponents {

    @Id("child")
    private Div child;

    public SetInitialTextView() {
        // this is no-op since the text is an empty string by default but it
        // removes all children
        child.setText("");
        setId("set-initial-text");

        NativeButton button = new NativeButton("Add a new child",
                event -> addChild());
        button.setId("add-child");
        add(button);
    }

    private void addChild() {
        Div div = new Div();

        div.setId("new-child");
        div.setText("New child");
        child.add(div);
    }

}
