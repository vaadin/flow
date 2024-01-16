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
