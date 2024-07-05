/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */

package com.vaadin.flow.testnpmonlyfeatures.general;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;

public class UrlValidationIT extends ChromeBrowserTest {

    @Test
    public void devModeUriValidation_uriWithDirectoryChange_statusForbidden()
            throws IOException {
        sendRequestAndValidateResponseStatusForbidden(
                "/VAADIN/build/%252E%252E/vaadin-bundle-1234.cache.js");
    }

    @Test
    public void staticResourceUriValidation_uriWithDirectoryChange_statusForbidden()
            throws IOException {
        sendRequestAndValidateResponseStatusForbidden(
                "/VAADIN/build/%252E%252E/some-resource.css");
    }

    private void sendRequestAndValidateResponseStatusForbidden(
            String pathToResource) throws IOException {
        final String urlString = getRootURL() + "/view" + pathToResource;
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();
        Assert.assertEquals(
                "HTTP 403 Forbidden expected for urls with "
                        + "directory change",
                HttpURLConnection.HTTP_FORBIDDEN, responseCode);
    }
}
