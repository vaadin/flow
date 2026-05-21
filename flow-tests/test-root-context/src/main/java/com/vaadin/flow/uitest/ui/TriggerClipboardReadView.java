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
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.ClipboardReadAction;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Wires a {@link ClickTrigger} on a button to a {@link ClipboardReadAction}
 * that writes the received {@code ClipboardPayload} (or {@code "null"}) into a
 * status div, so the IT can assert both paths. The IT replaces
 * {@code navigator.clipboard.read} with a recording shim so the assertions
 * don't depend on browser clipboard permissions.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerClipboardReadView", layout = ViewTestLayout.class)
public class TriggerClipboardReadView extends AbstractDivView {

    @Override
    protected void onShow() {
        NativeButton readButton = new NativeButton("Read");
        readButton.setId("read");
        Div status = new Div();
        status.setId("status");

        add(readButton, status);

        new ClickTrigger(readButton).triggers(new ClipboardReadAction(p -> {
            if (p == null) {
                status.setText("null");
            } else {
                status.setText("text=" + p.text() + ";html=" + p.html());
            }
        }));
    }
}
