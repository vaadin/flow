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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route("no-response")
public class TestNoResponseView extends Div {

    public static final String DELAY_NEXT_RESPONSE = "delay-next";
    public static final String ADD = "add";
    public static final String ADDED_PREDICATE = "added_";

    private int elements = 0;

    public TestNoResponseView() {
        NativeButton delayNext = new NativeButton("\"Delay\" next response",
                event -> CustomUidlRequestHandler.emptyResponse
                        .add(VaadinSession.getCurrent()));
        delayNext.setId(DELAY_NEXT_RESPONSE);

        NativeButton addElement = new NativeButton("Add element", event -> {
            Div addedElement = new Div("Added element");
            addedElement.setId(ADDED_PREDICATE + elements++);
            add(addedElement);
        });
        addElement.setId(ADD);

        add(delayNext, addElement);
    }
}
