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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.BrowserWindowResizeView", layout = ViewTestLayout.class)
public class BrowserWindowResizeView extends AbstractDivView {

    @Override
    protected void onShow() {
        Div windowSize = new Div();

        windowSize.setId("size-info");

        getPage().addBrowserWindowResizeListener(event -> windowSize.setText(
                "%sx%s".formatted(event.getWidth(), event.getHeight())));

        add(windowSize);

        var modalBtn = new NativeButton("Open modal (should keep working");
        modalBtn.setId("modal");
        modalBtn.addClickListener(e -> {
            add(new Div("Now modal, but resize events should still flow in"));
            getUI().get().addModal(new Div());
        });
        add(modalBtn);
    }
}
