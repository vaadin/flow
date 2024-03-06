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

public class ServerSideNavigationExceptionHandlingIT extends CCDMTest {

    @Test
    public void should_showErrorView_when_targetViewThrowsException() {
        openVaadinRouter();

        findAnchor("view-with-server-view-button").click();

        // Navigate to a server-side view that throws exception.
        findElement(By.id("serverViewThrowsExcpetionButton")).click();

        assertView("errorView",
                "Tried to navigate to a view without being authenticated",
                "view-throws-exception");
    }

}
