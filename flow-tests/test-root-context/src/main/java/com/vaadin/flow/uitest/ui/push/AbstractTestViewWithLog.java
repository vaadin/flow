/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.push;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;

public abstract class AbstractTestViewWithLog extends Div {

    private Log log = new Log();

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        /*
         * Read push settings from the UI instead of the the navigation target /
         * router layout to preserve the structure of these legacy testing UIs
         */
        CustomPush push = getClass().getAnnotation(CustomPush.class);

        if (push != null) {
            attachEvent.getUI().getPushConfiguration()
                    .setPushMode(push.value());
            attachEvent.getUI().getPushConfiguration()
                    .setTransport(push.transport());
        }

        add(log);
    }

    protected void log(String msg) {
        log.log(msg);
    }
}
