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
