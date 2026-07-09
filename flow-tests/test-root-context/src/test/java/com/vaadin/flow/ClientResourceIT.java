/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ClientResourceIT extends ChromeBrowserTest {

    @Test
    public void clientResourcesAreNotExposed() throws IOException {
        assertResourceIsUnavailable(
                FrontendUtils.DEFAULT_FRONTEND_DIR + "Flow.js");
        assertResourceIsUnavailable(
                FrontendUtils.DEFAULT_FRONTEND_DIR + "Flow.js.map");
        assertResourceIsUnavailable(
                FrontendUtils.DEFAULT_FRONTEND_DIR + "vaadin-dev-tools.js.map");
        assertResourceIsUnavailable(
                FrontendUtils.DEFAULT_FRONTEND_DIR + "vaadin-dev-tools.d.ts");
        assertResourceIsUnavailable(
                FrontendUtils.DEFAULT_FRONTEND_DIR + "FlowBootstrap.d.ts");
        assertResourceIsUnavailable(
                FrontendUtils.DEFAULT_FRONTEND_DIR + "index.js");
        assertResourceIsUnavailable(
                FrontendUtils.DEFAULT_FRONTEND_DIR + "Flow.d.ts");
        assertResourceIsUnavailable(
                FrontendUtils.DEFAULT_FRONTEND_DIR + "index.js.map");
        assertResourceIsUnavailable(
                FrontendUtils.DEFAULT_FRONTEND_DIR + "index.d.ts");
        assertResourceIsUnavailable(
                FrontendUtils.DEFAULT_FRONTEND_DIR + "FlowClient.d.ts");
        assertResourceIsUnavailable(
                FrontendUtils.DEFAULT_FRONTEND_DIR + "vaadin-dev-tools.js");
        assertResourceIsUnavailable(
                FrontendUtils.DEFAULT_FRONTEND_DIR + "copy-to-clipboard.js");
        assertResourceIsUnavailable(
                FrontendUtils.DEFAULT_FRONTEND_DIR + "FlowClient.js");
    }

    private void assertResourceIsUnavailable(String path) throws IOException {
        URL url = getResourceURL(path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        int responseCode = connection.getResponseCode();
        Assert.assertEquals(HttpURLConnection.HTTP_NOT_FOUND, responseCode);
    }

    private URL getResourceURL(String path) throws MalformedURLException {
        String url = getRootURL() + "/" + path;
        return new URL(url);
    }
}
