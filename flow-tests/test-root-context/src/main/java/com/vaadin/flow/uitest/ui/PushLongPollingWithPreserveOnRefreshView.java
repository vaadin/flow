/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;

@PreserveOnRefresh
@Push(transport = Transport.LONG_POLLING)
@Route("com.vaadin.flow.uitest.ui.PushLongPollingWithPreserveOnRefreshView")
public class PushLongPollingWithPreserveOnRefreshView extends Div {

    public static final String ADD_BUTTON_ID = "add-button-id";
    public static final String TEST_DIV_ID = "test-div-id";
    public static final String TEXT_IN_DIV = "text in div";

    public PushLongPollingWithPreserveOnRefreshView() {
        NativeButton button = new NativeButton("Open Dialog",
                e -> e.getSource().getUI().ifPresent(ui -> {
                    Div div = new Div();
                    div.setText(TEXT_IN_DIV);
                    div.setId(TEST_DIV_ID);
                    ui.add(div);
                }));
        button.setId(ADD_BUTTON_ID);
        add(button);
    }
}
