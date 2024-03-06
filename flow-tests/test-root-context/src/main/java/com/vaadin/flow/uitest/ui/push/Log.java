/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.html.Div;

public class Log extends Div {

    private int logCount;

    public void log(String msg) {
        Div div = new Div();
        div.addClassName("log");
        logCount++;
        div.setText(logCount + ". " + msg);
        add(div);
    }
}
