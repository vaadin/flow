package com.vaadin.flow.connect;

import com.vaadin.flow.server.connect.VaadinService;
import com.vaadin.flow.server.connect.auth.AnonymousAllowed;

/**
 * Simple Vaadin Connect Service definition.
 */
@VaadinService
public class ConnectServices {

    public String hello(String name) {
        return "Hello, " + name + "!";
    }

    @AnonymousAllowed
    public String helloAnonymous() {
        return "Hello, stranger!";
    }
}
