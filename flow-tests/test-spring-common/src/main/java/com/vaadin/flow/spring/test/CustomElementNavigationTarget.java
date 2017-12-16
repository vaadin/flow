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
package com.vaadin.flow.spring.test;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.startup.CustomElementRegistry;
import com.vaadin.ui.Component;

@Route("custom-element")
public class CustomElementNavigationTarget extends Div {

    public CustomElementNavigationTarget() {
        // Don't test it via the functionality which uses it (@Id, tempalates in
        // templates) because of #2653
        Class<? extends Component> customElement = CustomElementRegistry
                .getInstance().getRegisteredCustomElement("custom-element");
        if (customElement != null) {
            Label label = new Label(customElement.getName());
            label.setId("registered-custom-element");
            add(label);
        }
    }
}
