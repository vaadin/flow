/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
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
                        .add(makeDiv("GC Collected UI: " + uiId)));
    }

    private void listUIs(UI currentUI, Div uis) {
        uis.removeAll();
        currentUI.getSession().getUIs().stream().filter(ui -> ui != currentUI)
                .map(ui -> makeDiv("UI: " + ui.getUIId() + ", Path: "
                        + ui.getInternals().getActiveViewLocation().getPath()))
                .forEach(uis::add);
    }

    private static Div makeDiv(String text) {
        Div div = new Div();
        div.setText(text);
        return div;
    }
}
