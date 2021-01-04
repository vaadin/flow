package com.vaadin.flow.connect;

import javax.annotation.Nullable;

import com.vaadin.flow.server.connect.Endpoint;
import com.vaadin.flow.server.connect.auth.AnonymousAllowed;

/**
 * Simple Vaadin Connect Service definition.
 */
@Endpoint
class PackagePrivateEndpoint {

    public String hello(String name, @Nullable String title) {
        return "Hello, " + (title != null ? title + " " : "") + name + "!";
    }

    @AnonymousAllowed
    public String helloAnonymous() {
        return "Hello from package private endpoint!";
    }

}
