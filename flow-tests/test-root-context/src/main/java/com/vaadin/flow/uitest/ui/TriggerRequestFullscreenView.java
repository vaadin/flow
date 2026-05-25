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
import com.vaadin.flow.component.page.Fullscreen;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Wires {@code Fullscreen.on(button).requestComponent(panel, ...)} so a click
 * on the button asks for fullscreen on the target {@link Div}. The success
 * /error consumers write the outcome into a status div so the IT can assert
 * both paths. The IT replaces {@code Element.prototype.requestFullscreen} with
 * a recording shim so the assertions don't depend on browser fullscreen
 * permissions (which CI Chrome routinely denies).
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerRequestFullscreenView", layout = ViewTestLayout.class)
public class TriggerRequestFullscreenView extends AbstractDivView {

    @Override
    protected void onShow() {
        Div panel = new Div();
        panel.setId("panel");
        panel.setText("panel");
        NativeButton goButton = new NativeButton("Fullscreen");
        goButton.setId("go");
        Div status = new Div();
        status.setId("status");

        add(panel, goButton, status);

        Fullscreen.on(goButton).requestComponent(panel,
                () -> status.setText("ok"),
                err -> status.setText("err:" + err));
    }
}
