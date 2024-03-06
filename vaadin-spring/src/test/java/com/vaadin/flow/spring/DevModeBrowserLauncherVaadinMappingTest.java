/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "server.port = 1235",
        "vaadin.urlMapping=/ui/*" })
public class DevModeBrowserLauncherVaadinMappingTest
        extends AbstractDevModeBrowserLauncherTest {

    @Test
    public void getUrl_withUrlMapping_givesUrlWithUrlMappingAndNoContextPath() {
        String url = DevModeBrowserLauncher.getUrl(app);
        Assert.assertEquals("http://localhost:1235/ui/", url);
    }

}
