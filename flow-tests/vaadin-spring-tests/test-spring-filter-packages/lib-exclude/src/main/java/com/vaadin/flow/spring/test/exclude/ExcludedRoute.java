/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.exclude;

import com.vaadin.flow.router.Route;

/**
 * Test class in a jar that is blocked from scanning via vaadin.blocked-jar=true
 * in package.properties.
 */
@Route("excluded-route")
public class ExcludedRoute {
}
