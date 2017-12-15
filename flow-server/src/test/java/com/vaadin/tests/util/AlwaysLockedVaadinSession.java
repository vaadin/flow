package com.vaadin.tests.util;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.server.MockVaadinSession;

public class AlwaysLockedVaadinSession extends MockVaadinSession {

    public AlwaysLockedVaadinSession(VaadinService service) {
        super(service);
        lock();
    }

}
