/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.vaadin.flow.server.startup.EnableOSGiRunner;

@RunWith(EnableOSGiRunner.class)
public class OSGiStaticFileServerTest {

    @Test
    public void getStaticPath_returnsNull() throws MalformedURLException {
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            public URL getStaticResource(String path) {
                try {
                    return new URL("http://bar");
                } catch (MalformedURLException e) {
                    Assert.fail();
                    return null;
                }
            }
        };
        StaticFileServer server = new StaticFileServer(service);

        Assert.assertNull(server.getStaticResource("foo"));

    }
}
