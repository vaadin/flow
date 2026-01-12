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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.TemplateAttributeView", layout = ViewTestLayout.class)
@Tag("attribute-template")
@JsModule("./AttributeTemplate.js")
public class TemplateAttributeView extends PolymerTemplate<TemplateModel>
        implements HasComponents {

    @Id("div")
    private Div injectedDiv;

    @Id("disabled")
    private Div disabledDiv;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        setId("template");
        Div div = new Div();
        div.setText(injectedDiv.getTitle().get() + " "
                + injectedDiv.getElement().getProperty("foo") + " "
                + injectedDiv.getElement().getProperty("baz"));
        div.setId("info");
        add(div);

        div = new Div();
        div.setId("disabledInfo");
        div.setText("Enabled: " + disabledDiv.isEnabled());
        add(div);
    }
}
