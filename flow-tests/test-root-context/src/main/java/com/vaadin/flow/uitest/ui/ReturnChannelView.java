/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ReturnChannelView", layout = ViewTestLayout.class)
public class ReturnChannelView extends AbstractDivView {
    public ReturnChannelView() {
        Element button = new Element("button");
        button.setAttribute("id", "button");
        button.setText("Send message to channel");

        ReturnChannelRegistration channel = button.getNode()
                .getFeature(ReturnChannelMap.class)
                .registerChannel(arguments -> button.setText(
                        "Click registered: " + arguments.getString(0)));

        button.executeJs(
                "this.addEventListener('click', function() { $0('hello') })",
                channel);

        getElement().appendChild(button);
    }
}
