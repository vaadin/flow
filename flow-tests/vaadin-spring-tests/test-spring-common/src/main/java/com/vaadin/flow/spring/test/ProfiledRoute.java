/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import org.springframework.context.annotation.Profile;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("profiled")
@Profile("enabled")
public class ProfiledRoute extends Div {

    public ProfiledRoute() {
        setId("profiled-enabled");
        setText("Profiled route is enabled");
    }

}
