/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.routescope;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScopeOwner;

@Route(value = "invalid-layout", layout = Layout.class)
public class InvalidRouteScopeUsage extends Div {

    // Injection point is valid and there is a bean eligible for injection. But
    // the scope doesn't exist: so this should fail
    public InvalidRouteScopeUsage(
            @Autowired @RouteScopeOwner(ButtonInLayout.class) ButtonScopedBean bean) {
        setId("invalid-bean");
    }

}
