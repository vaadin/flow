/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.communication.PushMode;

@Route("com.vaadin.flow.uitest.ui.ActivatePushView")
public class ActivatePushView extends Div {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        attachEvent.getUI().getPushConfiguration()
                .setPushMode(PushMode.AUTOMATIC);
    }
}
