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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.VisibilityView", layout = ViewTestLayout.class)
public class VisibilityView extends Div {

    public VisibilityView() {
        setId("main");

        Div div = new Div();
        div.setText("Target to make visible/invisible");
        div.setId("visibility");
        div.setVisible(false);

        NativeButton button = new NativeButton("Update visibility",
                event -> div.setVisible(!div.isVisible()));
        button.setId("update");
        add(div, button);
    }

}
