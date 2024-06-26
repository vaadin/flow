/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ClientResourceIT extends ChromeBrowserTest {

    @Test
    public void clientResourcesAreNotExposed() throws IOException {
        assertResourceIsUnavailable("frontend/VaadinDevmodeGizmo.js.map");
        assertResourceIsUnavailable("frontend/VaadinDevmodeGizmo.d.ts");
        assertResourceIsUnavailable("frontend/VaadinDevmodeGizmo.js");
        assertResourceIsUnavailable("frontend/VaadinDevmodeGizmo.ts");
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
