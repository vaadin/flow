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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.RemoveAddVisibilityView")
public class RemoveAddVisibilityView extends Div {

    public RemoveAddVisibilityView() {
        Span hidden = new Span("Initially hidden");
        hidden.setVisible(false);

        NativeButton toggle = new NativeButton("Make Element visible",
                event -> {
                    remove(hidden);
                    add(hidden);
                    hidden.setVisible(true);
                });
        toggle.setId("make-visible");

        add(toggle, hidden);
    }
}
