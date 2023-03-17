/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.endpoints.denyall;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.fusion.Endpoint;

@Endpoint
@DenyAll
public class DenyAllEndpoint {

    public void shouldBeDenied() {
    }

    @PermitAll
    public void shouldBeDisplayed1() {
    }

    @RolesAllowed("test")
    public void shouldBeDisplayed2() {
    }

    @AnonymousAllowed
    public void shouldBeDisplayed3() {
    }
}
