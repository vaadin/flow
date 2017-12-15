package com.vaadin.tests.util;

import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;

public class AlwaysLockedVaadinSession extends MockVaadinSession {

    public AlwaysLockedVaadinSession(VaadinService service) {
        super(service);
        lock();
    }

}
