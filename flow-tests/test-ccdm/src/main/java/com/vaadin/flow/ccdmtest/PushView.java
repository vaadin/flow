/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.ccdmtest;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("push")
public class PushView extends Div {
    public PushView() {
        setId("pushView");
        add(new Text("Push View"));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        setId("pushedContent");
        setText("Message pushed from server");
        getUI().get().push();
    }
}
