/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.InvisibleSlotAttributeView", layout = ViewTestLayout.class)
public class InvisibleSlotAttributeView extends AbstractDivView {

    public InvisibleSlotAttributeView() {
        Div target = new Div("Initially invisible");
        target.setId("target");
        target.getElement().setAttribute("slot", "drawer");
        target.getElement().setAttribute("data-info", "sensitive");
        target.setVisible(false);

        NativeButton showButton = new NativeButton("Make visible",
                e -> target.setVisible(true));
        showButton.setId("show-button");

        add(showButton, target);
    }
}
