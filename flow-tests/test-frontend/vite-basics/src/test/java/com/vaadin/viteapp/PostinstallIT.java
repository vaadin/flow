/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.viteapp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class PostinstallIT extends ViteDevModeIT {

    @Test
    public void postinstallRanForProject() throws IOException {
        waitForDevServer(); // This is what runs the postinstall script
        Assert.assertEquals("hello", IOUtils.toString(
                getClass().getClassLoader().getResource("main.postinstall"),
                StandardCharsets.UTF_8));
    }
}
