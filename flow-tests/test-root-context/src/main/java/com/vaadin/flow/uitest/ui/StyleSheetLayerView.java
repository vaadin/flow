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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.ui.LoadMode;

@Route("com.vaadin.flow.uitest.ui.StyleSheetLayerView")
public class StyleSheetLayerView extends Div {

    private Registration layeredRegistration;

    public StyleSheetLayerView() {
        // Unlayered rule with a lower specificity than the layered one
        UI.getCurrent().getPage().addStyleSheet("/style-layer-app.css");

        Div testDiv = new Div("Test Content");
        testDiv.setId("test-div");
        testDiv.addClassName("layered");

        NativeButton addLayered = new NativeButton("Add Layered Style", e -> {
            if (layeredRegistration == null) {
                layeredRegistration = UI.getCurrent().getPage().addStyleSheet(
                        "/style-layer-theme.css", LoadMode.EAGER, "theme");
            }
        });
        addLayered.setId("add-layered-style");

        NativeButton removeLayered = new NativeButton("Remove Layered Style",
                e -> {
                    if (layeredRegistration != null) {
                        layeredRegistration.remove();
                        layeredRegistration = null;
                    }
                });
        removeLayered.setId("remove-layered-style");

        add(testDiv, addLayered, removeLayered);
    }
}
