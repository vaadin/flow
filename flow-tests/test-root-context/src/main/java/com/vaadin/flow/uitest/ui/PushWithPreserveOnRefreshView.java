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
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@PreserveOnRefresh
@Push
@Route("com.vaadin.flow.uitest.ui.PushWithPreserveOnRefreshView")
public class PushWithPreserveOnRefreshView extends Div {

    private int times = 0;

    public PushWithPreserveOnRefreshView() {
        NativeButton button = new NativeButton("click me", event -> log(
                "Button has been clicked " + (++times) + " times"));
        button.setId("click");
        add(button);
    }

    private void log(String msg) {
        Div div = new Div();
        div.addClassName("log");
        div.setText(msg);
        add(div);
    }

}
