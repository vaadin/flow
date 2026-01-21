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
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.FocusBlurView")
public class FocusBlurView extends Div {

    public FocusBlurView() {
        NativeButton serverSideTest = new NativeButton("Server Side Events");
        serverSideTest.addClickListener(event -> {
            createTest(true);
        });
        serverSideTest.setId("server-side");
        NativeButton clientSideTest = new NativeButton("Client Side Events");
        clientSideTest.addClickListener(event -> {
            createTest(false);
        });
        clientSideTest.setId("client-side");
        add(serverSideTest, clientSideTest);
    }
    
    private void createTest(boolean serverEvents) {
        Input input = new Input();
        input.addFocusListener(event -> {
            Span span = new Span("Focused: " + event.isFromClient());
            span.setId("focus-event");
            add(span);
            if (serverEvents) {
                input.blur();
            }
        });
        input.addBlurListener(event -> {
            Span span = new Span("Blurred: " + event.isFromClient());
            span.setId("blur-event");
            add(span);
        });
        input.setId("input");
        var focus = new NativeButton("Focus");
        focus.setId("focus");
        focus.addClickListener(event -> input.focus());
        add(input, focus);
    }
}
