/*
 * Copyright 2000-2024 Vaadin Ltd.
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
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route("active-uis")
public class ActiveUIsView extends Div {

    public ActiveUIsView() {
        Div uis = new Div();
        uis.setId("uis");
        NativeButton button = new NativeButton("List active UIs", event -> {
            UI current = UI.getCurrent();
            uis.removeAll();
            current.getSession().getUIs().stream().filter(ui -> ui != current)
                    .map(ui -> new Div("UI: " + ui.getUIId() + ", Path: "
                            + ui.getActiveViewLocation().getPath()))
                    .forEach(uis::add);
        });
        button.setId("list-uis");
        add(button, new H1("Active UIs (excluding current)"), uis);
    }
}
