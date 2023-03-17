/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.startup;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.fusion.auth.CsrfIndexHtmlRequestListener;

/**
 * A listener that creates and registers a {@link CsrfIndexHtmlRequestListener}.
 */
public class CsrfServiceInitListener implements VaadinServiceInitListener {
    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addIndexHtmlRequestListener(new CsrfIndexHtmlRequestListener());
    }
}
