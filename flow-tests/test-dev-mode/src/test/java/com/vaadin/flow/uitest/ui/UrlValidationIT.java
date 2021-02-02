/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class UrlValidationIT extends ChromeBrowserTest {

    @Test
    public void devModeUriValidation_uriWithDirectoryChange_statusForbidden() throws Exception {
        sendRequestAndValidateResponseStatusForbidden(
                "/VAADIN/build/%252E%252E/");
    }

    @Test
    public void staticResourceUriValidation_uriWithDirectoryChange_statusForbidden() throws Exception {
        sendRequestAndValidateResponseStatusForbidden(
                "/VAADIN/build/%252E%252E/some-resource.css");
    }

    private void sendRequestAndValidateResponseStatusForbidden(String pathToResource) throws Exception {
        final String urlString = getRootURL() + "/view" + pathToResource;
        URL url = new URL(urlString);

        // not using chromedriver, so we need to look at the response body
        // to know if the frontend is ready
        waitForFrontendCompilation(url);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();
        Assert.assertEquals("HTTP 403 Forbidden expected for urls with " +
                "directory change", HttpURLConnection.HTTP_FORBIDDEN, responseCode);
    }

    private void waitForFrontendCompilation(URL url) throws Exception {
        boolean frontendCompiled = false;
        int attemptsRemaining = 100;
        while (!frontendCompiled && attemptsRemaining-- > 0) {
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            frontendCompiled = responseCode != HttpServletResponse.SC_OK
                    || !getResponseBody(connection).contains(
                            "The frontend development build has not yet finished");
            if (!frontendCompiled) {
                Thread.sleep(200);
            }
        }
        if (!frontendCompiled)
            throw new Exception(
                    "Timeout while waiting for frontend compilation");
    }

    private String getResponseBody(HttpURLConnection connection) throws IOException {
        String body = String.join("\n", IOUtils.readLines(connection.getInputStream(), StandardCharsets.UTF_8));
        connection.getInputStream().close();
        return body;
    }
}
