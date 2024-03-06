/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.ccdmtest;

import org.junit.Test;
import org.openqa.selenium.By;

public class ServerSideForwardIT extends CCDMTest {

    @Test
    public void should_openClientPage_when_forwardFromServerToClientUrl() {
        openVaadinRouter();

        // Navigate client to server and forward to client.
        findAnchor("serverforwardview/true").click();
        assertView("clientView", "Client view", "client-view");

        // Navigate server to server and forward to client.
        findAnchor("serverforwardview").click();
        findElement(By.id("goToServerForwardView")).click();
        assertView("clientView", "Client view", "client-view");
    }

}
