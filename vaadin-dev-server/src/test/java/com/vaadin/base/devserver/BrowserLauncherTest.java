/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.base.devserver;

import com.vaadin.base.devserver.util.BrowserLauncher;

import org.junit.Ignore;
import org.junit.Test;

public class BrowserLauncherTest {

    @Test
    @Ignore("Only for manual testing. Cannot open brower windows in the CI server")
    public void openVaadinCom() {
        BrowserLauncher.launch("https://vaadin.com");
    }
}
