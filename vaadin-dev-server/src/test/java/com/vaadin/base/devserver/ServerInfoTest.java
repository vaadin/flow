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
        assertTrue("Product name should be there by default", productNames.contains(serverInfo.getProductName()));
    }
}
