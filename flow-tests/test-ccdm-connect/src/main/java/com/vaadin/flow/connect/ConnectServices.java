package com.vaadin.flow.connect;

import com.vaadin.flow.server.connect.VaadinService;

/**
 * Simple Vaadin Connect Service definition.
 */
@VaadinService
public class ConnectServices {
    
    public String hello(String name) {
        return "Hello, " + name + "!";
    }
}
