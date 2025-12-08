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

@Route(value = "com.vaadin.flow.uitest.ui.WaitForVaadinView", layout = ViewTestLayout.class)
public class WaitForVaadinView extends AbstractDivView {
    private final Div message;
    private final NativeButton button;

    public WaitForVaadinView() {
        message = new Div();
        message.setText("Not updated");
        message.setId("message");

        button = new NativeButton("Click to update", e -> waitAndUpdate());

        add(message, button);
    }

    private void waitAndUpdate() {
        try {
            message.setText("Updated");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore
        }
    }
}
