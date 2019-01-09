package com.vaadin.flow.server.webcomponent;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.vaadin.flow.server.osgi.OSGiAccess;
import com.vaadin.flow.server.startup.EnableOSGiRunner;

@RunWith(EnableOSGiRunner.class)
public class OSGiWebComponentRegistryTest extends WebComponentRegistryTest {

    @After
    public void cleanUp() {
        if (OSGiAccess.getInstance().getOsgiServletContext() != null) {
            WebComponentRegistry.getInstance(
                    OSGiAccess.getInstance().getOsgiServletContext());
        }
    }

    @Test
    public void assertWebComponentRegistry() {
        Assert.assertEquals(OSGiWebComponentRegistry.class.getName(),
                registry.getClass().getName());
    }
}
