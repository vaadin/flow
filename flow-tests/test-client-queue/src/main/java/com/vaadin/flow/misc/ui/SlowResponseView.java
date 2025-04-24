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
package com.vaadin.flow.misc.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("slow-response")
public class SlowResponseView extends Div {

    public static final String SLOW_ADD = "slowAdd";
    public static final String ADD = "add";
    public static final String ADDED_PREDICATE = "added_";

    private int elements = 0;

    public SlowResponseView() {
        int messageTimeoutMillis = UI.getCurrent().getSession().getService()
                .getDeploymentConfiguration().getMaxMessageSuspendTimeout();
        add(new Span("Max message suspend timeout: " + messageTimeoutMillis));
        NativeButton slowAddElement = new NativeButton(
                "Add element (slow response)", event -> {
                    slowAddElement(messageTimeoutMillis + 1000);
                });
        slowAddElement.setId(SLOW_ADD);

        NativeButton addElement = new NativeButton("Add element", event -> {
            addElement();
        });
        addElement.setId(ADD);

        add(slowAddElement, addElement);
    }

    private void addElement() {
        Div addedElement = new Div("Added element");
        addedElement.setId(ADDED_PREDICATE + elements++);
        add(addedElement);
    }

    private void slowAddElement(long delayMillis) {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        addElement();
    }
}
