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

import com.vaadin.flow.component.ComponentUtil;
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
        NativeButton listUIsButton = new NativeButton("List active UIs",
                event -> {
                    UI current = UI.getCurrent();
                    listUIs(current, uis);
                });
        listUIsButton.setId("list-uis");

        Div gcCollectedUIs = new Div();
        gcCollectedUIs.setId("gcuis");
        NativeButton listGCCollectedUIsButton = new NativeButton(
                "List GC collected UIs", event -> {
                    listGCCollectedUIs(gcCollectedUIs);
                });
        listGCCollectedUIsButton.setId("list-gc-collected-uis");
        NativeButton gcHintButton = new NativeButton("Run GC",
                event -> System.gc());
        gcHintButton.setId("gc-hint");

        add(listUIsButton, new H1("Active UIs (excluding current)"), uis,
                listGCCollectedUIsButton, gcHintButton,
                new H1("GC collected UIs"), gcCollectedUIs);
    }

    private void listGCCollectedUIs(Div gcCollectedUIs) {
        gcCollectedUIs.removeAll();
        UI ui = UI.getCurrent();
        ComponentUtil.getData(ui, UITrackerListener.UITracker.class)
                .getCollectedUIs(ui.getSession()).forEach(uiId -> gcCollectedUIs
                        .add(new Div("GC Collected UI: " + uiId)));
    }

    private void listUIs(UI currentUI, Div uis) {
        uis.removeAll();
        currentUI.getSession().getUIs().stream().filter(ui -> ui != currentUI)
                .map(ui -> new Div("UI: " + ui.getUIId() + ", Path: "
                        + ui.getActiveViewLocation().getPath()))
                .forEach(uis::add);
    }

}
