/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "server.port = 1235",
        "server.ssl.enabled = true" })
public class DevModeBrowserLauncherHttpsProtoTest
        extends AbstractDevModeBrowserLauncherTest {

    @Test
    public void getUrl_withHttpsProto_givesUrlWithHttpsInUrl() {
        String url = DevModeBrowserLauncher.getUrl(app);
        Assertions.assertEquals("https://localhost:1235/", url);
    }
}
