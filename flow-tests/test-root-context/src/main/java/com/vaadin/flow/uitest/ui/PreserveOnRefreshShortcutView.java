/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
