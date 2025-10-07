/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.uitest.ui;

import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.NativeLabelElement;
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
        waitUntil(input -> $(NativeLabelElement.class).id("elementId")
                .isDisplayed());
        // check the forbidden url
        sendRequestAndValidateResponseStatusBadRequest(
                "/VAADIN/build/%252E%252E");
    }

    @Test
    public void staticResourceUriValidation_uriWithDirectoryChange_statusForbidden()
            throws Exception {
        // open a view and wait till the expected label is displayed
        open();
        waitUntil(input -> $(NativeLabelElement.class).id("elementId")
                .isDisplayed());
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
