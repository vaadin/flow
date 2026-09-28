/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.test.devmode;

import java.net.HttpURLConnection;
import java.net.URI;

import org.junit.jupiter.api.Assertions;

import com.vaadin.flow.component.html.testbench.NativeLabelElement;
import com.vaadin.flow.test.AbstractDefaultIT;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

@TestFor(BrowserLoggingView.class)
public class UrlValidationIT extends AbstractDefaultIT {

    @BrowserTest
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

    @BrowserTest
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
        HttpURLConnection connection = (HttpURLConnection) URI
                .create(getRootURL() + pathToResource).toURL().openConnection();
        connection.setRequestMethod("GET");
        Assertions.assertEquals(HttpURLConnection.HTTP_BAD_REQUEST,
                connection.getResponseCode(),
                "HTTP 400 Bad request expected for urls with directory change");
    }
}
