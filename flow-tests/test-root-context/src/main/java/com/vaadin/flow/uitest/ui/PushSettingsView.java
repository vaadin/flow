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
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;

@Push
@Route("com.vaadin.flow.uitest.ui.PushSettingsView")
public class PushSettingsView extends AbstractDivView {
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        setId("pushMode");
        setText("Push mode: "
                + attachEvent.getUI().getPushConfiguration().getPushMode());
    }
}
