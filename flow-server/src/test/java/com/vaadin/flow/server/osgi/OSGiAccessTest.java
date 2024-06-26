/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.osgi;

import javax.servlet.ServletContext;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.EnableOSGiRunner;

@RunWith(EnableOSGiRunner.class)
public class OSGiAccessTest {

    @Test
    public void osgiContextHasLookup() {
        ServletContext context = OSGiAccess.getInstance()
                .getOsgiServletContext();
        VaadinServletContext vaadinServletContext = new VaadinServletContext(
                context);
        Assert.assertNotNull(vaadinServletContext.getAttribute(Lookup.class));
    }
}
