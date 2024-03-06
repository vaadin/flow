/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.tests.util;

import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;

public class AlwaysLockedVaadinSession extends MockVaadinSession {

    public AlwaysLockedVaadinSession(VaadinService service) {
        super(service);
        lock();
    }

}
