/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.PreserveOnRefreshShortcutView")
@PreserveOnRefresh
public class PreserveOnRefreshShortcutView extends Div {

    public PreserveOnRefreshShortcutView() {
        NativeButton button = new NativeButton(
                "Press ENTER, reload the page, and press ENTER again",
                event -> handleClick());
        button.addClickShortcut(Key.ENTER);
        button.setId("trigger");
        add(button);
    }

    private void handleClick() {
        Div div = new Div();
        div.addClassName("info");
        div.setText("Clicked");
        add(div);
    }
}
