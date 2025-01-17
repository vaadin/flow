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

import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ClientSideValueChangeView", layout = ViewTestLayout.class)
public class ClientSideValueChangeView extends AbstractDivView {

    @Override
    protected void onShow() {
        Input input = new Input();
        input.setId("inputfield");

        input.addValueChangeListener(e -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            Span span = new Span("done");
            span.setId("status");
            add(span);
        });

        add(input);

        Input input2 = new Input();
        input2.setId("inputfieldserversetsvalue");

        input2.addValueChangeListener(e -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            input2.setValue("fromserver");
            Span span = new Span("done");
            span.setId("statusserversetsvalue");
            add(span);
        });

        add(input2);
    }

}
