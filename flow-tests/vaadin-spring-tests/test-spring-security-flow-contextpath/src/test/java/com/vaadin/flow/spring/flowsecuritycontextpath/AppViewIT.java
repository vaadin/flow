/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.flowsecuritycontextpath;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Ignore;

@NotThreadSafe
@Ignore
public class AppViewIT extends com.vaadin.flow.spring.flowsecurity.AppViewIT {

    @Override
    protected String getRootURL() {
        return super.getRootURL() + "/context";
    }
}
