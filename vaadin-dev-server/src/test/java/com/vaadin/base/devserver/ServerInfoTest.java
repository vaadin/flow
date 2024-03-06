/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ServerInfoTest {

    @Test
    public void testGetProductName() {
        ServerInfo serverInfo = new ServerInfo();
        var productNames = List.of("Vaadin", "Hilla");
        // This test is more to prevent regressions
        assertTrue("Product name should be there by default",
                productNames.contains(serverInfo.getProductName()));
    }
}
