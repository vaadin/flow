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

import com.vaadin.flow.component.fullscreen.Fullscreen;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

/**
 * Wires {@code Fullscreen.onClick(button).enter(this, ...)} so the component
 * being fullscreened is the route root itself. With no parent layout the view
 * is the wrapper's direct child, which is the case where
 * {@code requestComponentFullscreen} used to crash because the placeholder
 * comment became the wrapper's first node. The button and status div live
 * inside this root so the IT can drive and assert the outcome.
 */
@Route("com.vaadin.flow.uitest.ui.TriggerRequestFullscreenRootView")
public class TriggerRequestFullscreenRootView extends AbstractDivView {

    @Override
    protected void onShow() {
        setId("root");
        NativeButton goButton = new NativeButton("Fullscreen");
        goButton.setId("go");
        Div status = new Div();
        status.setId("status");

        add(goButton, status);

        Fullscreen.onClick(goButton).enter(this, () -> status.setText("ok"),
                err -> status
                        .setText("err:" + err.name() + ":" + err.message()));
    }
}
