/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.shared.communication.PushMode;

@Push(value = PushMode.AUTOMATIC)
public class PushExporter extends WebComponentExporter<PushComponent> {
    public PushExporter() {
        super("embedded-push");
    }

    @Override
    public void configureInstance(WebComponent<PushComponent> webComponent,
            PushComponent component) {

    }
}
