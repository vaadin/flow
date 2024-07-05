/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.osgi.OSGiAccess;

@RunWith(EnableOSGiRunner.class)
public class OSGiApplicationRouteRegistryTest
        extends ApplicationRouteRegistryTest {

    @After
    public void cleanUp() {
        if (OSGiAccess.getInstance().getOsgiServletContext() != null) {
            ApplicationRouteRegistry
                    .getInstance(new VaadinServletContext(
                            OSGiAccess.getInstance().getOsgiServletContext()))
                    .clean();
        }
    }

    @Override
    @Test
    public void assertApplicationRegistry() {
        Assert.assertEquals(
                ApplicationRouteRegistry.class.getName() + "$OSGiRouteRegistry",
                getTestedRegistry().getClass().getName());
    }
}
