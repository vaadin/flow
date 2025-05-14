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

import java.util.UUID;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;

public abstract class AbstractReloadView extends Div {

    public static final String TRIGGER_RELOAD_ID = "triggerReload";

    // This ensures the view is not serializable
    private Object preventSerialization = new Object();

    public UUID viewId = UUID.randomUUID();

    protected void addTriggerButton() {
        final NativeButton triggerButton = new NativeButton("Trigger reload",
                event -> Application.triggerReload());
        triggerButton.setId(TRIGGER_RELOAD_ID);
        add(triggerButton);
    }

    protected void addViewId() {
        Div div = new Div();
        Span span = new Span(viewId.toString());
        span.setId("viewId");
        div.add(new Text("The view id is: "), span);
        add(div);
    }
}
