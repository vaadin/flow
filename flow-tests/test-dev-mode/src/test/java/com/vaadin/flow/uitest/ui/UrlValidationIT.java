/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.LabelElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class UrlValidationIT extends ChromeBrowserTest {
    @Override
    protected String getTestPath() {
        return "/view/com.vaadin.flow.uitest.ui.frontend.BrowserLoggingView";
    }

    @Test
    public void devModeUriValidation_uriWithDirectoryChange_statusForbidden()
            throws Exception {
        // open a view and wait till the expected label is displayed
        open();
        waitUntil(input -> $(LabelElement.class).id("elementId").isDisplayed());
        // check the forbidden url
        sendRequestAndValidateResponseStatusBadRequest(
                "/VAADIN/build/%252E%252E");
    }

    @Test
    public void staticResourceUriValidation_uriWithDirectoryChange_statusForbidden()
            throws Exception {
        // open a view and wait till the expected label is displayed
        open();
        waitUntil(input -> $(LabelElement.class).id("elementId").isDisplayed());
        // check the forbidden url
        sendRequestAndValidateResponseStatusBadRequest(
                "/VAADIN/build/%252E%252E/some-resource.css");
    }

    private void sendRequestAndValidateResponseStatusBadRequest(
            String pathToResource) throws Exception {
        final String urlString = getRootURL() + "/view" + pathToResource;
        URL url = new URL(urlString);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();
        Assert.assertEquals(
                "HTTP 400 Bad request expected for urls with "
                        + "directory change",
                HttpURLConnection.HTTP_BAD_REQUEST, responseCode);
    }
}
