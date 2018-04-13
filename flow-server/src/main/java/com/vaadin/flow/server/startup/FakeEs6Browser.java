package com.vaadin.flow.server.startup;

import com.vaadin.flow.server.WebBrowser;

/**
 * Browser instance targeted for server side resolving of resources.
 */
public class FakeEs6Browser extends WebBrowser {
    @Override
    public boolean isEs6Supported() {
        return true;
    }
}
