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
package com.vaadin.viteapp.views.template;

import org.vaadin.example.addon.AddonLitComponent;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route(TemplateView.ROUTE)
public class TemplateView extends Div {

    public static final String ROUTE = "template";

    public TemplateView() {
        LitComponent litComponent = new LitComponent();
        add(litComponent);

        PolymerComponent polymerComponent = new PolymerComponent();
        add(polymerComponent);

        AddonLitComponent addonLitComponent = new AddonLitComponent();
        add(addonLitComponent);

        Input setLabelInput = new Input();
        add(setLabelInput);

        NativeButton setLabelButton = new NativeButton("Set labels");
        setLabelButton.addClickListener(e -> {
            String newLabel = setLabelInput.getValue();
            litComponent.setLabel(newLabel);
            polymerComponent.setLabel(newLabel);
            addonLitComponent.setLabel(newLabel);
        });
        add(setLabelButton);

        // Add component by reflection to excercise fallback chunk
        try {
            Class<?> clazz = Class.forName(
                    "com.vaadin.viteapp.views.template.ReflectivelyReferencedComponent");
            add((Component) clazz.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
