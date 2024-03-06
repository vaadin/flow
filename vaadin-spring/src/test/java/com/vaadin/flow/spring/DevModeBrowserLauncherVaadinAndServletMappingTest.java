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
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "server.port = 1234",
        "vaadin.url-mapping=/ui/*" })
public class DevModeBrowserLauncherVaadinAndServletMappingTest
        extends AbstractDevModeBrowserLauncherTest {

    @Test
    public void getUrl_withContextPathAndUrlMapping_givesUrlWithContextPathAndUrlMapping() {
        MockServletContext ctx = (MockServletContext) app.getServletContext();
        ctx.setContextPath("/contextpath");
        String url = DevModeBrowserLauncher.getUrl(app);
        Assert.assertEquals("http://localhost:1234/contextpath/ui/", url);
    }

}
