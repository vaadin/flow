package com.vaadin.flow.connect;

import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.connect.Endpoint;
import com.vaadin.flow.server.connect.auth.AnonymousAllowed;

/**
 * Simple Vaadin Connect Service definition.
 */
@Endpoint
public class AppEndpoint {
    @AnonymousAllowed
    public String helloAnonymous() {
        return "Hello, stranger!";
    }
}
