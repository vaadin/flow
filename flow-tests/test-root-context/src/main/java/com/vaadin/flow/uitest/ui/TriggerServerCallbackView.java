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

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.trigger.ClickTrigger;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Wires a {@link ClickTrigger} on a button to a server-side {@code Runnable}
 * via {@link com.vaadin.flow.component.trigger.Trigger
 * #triggers(com.vaadin.flow.function.SerializableRunnable)}. The runnable
 * updates a count and a result label so the IT can verify the callback ran on
 * the server.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerServerCallbackView", layout = ViewTestLayout.class)
public class TriggerServerCallbackView extends AbstractDivView {

    @Override
    protected void onShow() {
        NativeButton button = new NativeButton("Fire");
        button.setId("fire");

        Span result = new Span("(none)");
        result.setId("result");

        add(button, result);

        AtomicInteger count = new AtomicInteger();
        new ClickTrigger(button).triggers(
                () -> result.setText("fired " + count.incrementAndGet()));
    }
}
