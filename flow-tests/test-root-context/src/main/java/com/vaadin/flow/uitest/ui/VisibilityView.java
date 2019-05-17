/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.VisibilityView", layout = ViewTestLayout.class)
public class VisibilityView extends AbstractDivView {

    public VisibilityView() {
        setId("main");

        Div div = new Div();
        div.setText("Target to make visible/invisible");
        div.setId("visibility");
        div.setVisible(false);

        Label label = new Label("Nested element");
        label.setId("nested-label");
        div.add(label);

        NativeButton updateVisibility = createButton("Update visibility",
                "updateVisibiity",
                event -> div.setVisible(!div.isVisible()));

        NativeButton updateLabelVisibility = createButton(
                "Update label visibility", "updateLabelVisibiity",
                event -> label.setVisible(!label.isVisible()));

        NativeButton updateStyle = createButton(
                "Update target element property", "updateProperty", event -> {
                    div.setClassName("foo");
                    label.setClassName("bar");
                });

        add(div, updateVisibility, updateStyle, updateLabelVisibility);
    }

}
