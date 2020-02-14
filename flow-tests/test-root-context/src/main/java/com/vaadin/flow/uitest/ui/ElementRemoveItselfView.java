/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.ElementRemoveItselfView", layout = ViewTestLayout.class)
public class ElementRemoveItselfView extends AbstractDivView {

    private Element layout = new Element("div");
    private Element button = new Element("button");

    public ElementRemoveItselfView() {
        button.setText("Remove me");
        button.setAttribute("id", "remove-me");

        layout.appendChild(button);
        button.addEventListener("click", evt -> {
            layout.removeAllChildren();
            Label label = new Label("All removed!");
            label.setId("all-removed");
            add(label);
        });
        getElement().appendChild(layout);
    }
}
