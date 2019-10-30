package com.vaadin.flow.connect;

import com.vaadin.flow.server.connect.VaadinService;

import java.util.Optional;

/**
 * Simple Vaadin Connect Service definition.
 */
@VaadinService
public class ConnectServices {
    
    public String hello(String name) {
        return "Hello, " + name + "!";
    }

    public void takeNull(String foo) {
        if (foo == null) {
            // Should not throw here
            throw new IllegalArgumentException("Unexpected null pointer argument");
        }
    }

    public String giveNull() {
        return null;
    }

    public void takeOptionalNull(Optional<String> foo) {
        if (foo == null) {
            // Should not throw here
            throw new IllegalArgumentException("Unexpected null pointer argument");
        }
    }

    public Optional<String> giveOptionalNull() {
        return Optional.empty();
    }
}
