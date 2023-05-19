/*
 * Copyright 2000-2023 Vaadin Ltd.
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
        assertResourceIsUnavailable("frontend/Flow.js");
        assertResourceIsUnavailable("frontend/Flow.js.map");
        assertResourceIsUnavailable("frontend/vaadin-dev-tools.js.map");
        assertResourceIsUnavailable("frontend/vaadin-dev-tools.d.ts");
        assertResourceIsUnavailable("frontend/FlowBootstrap.d.ts");
        assertResourceIsUnavailable("frontend/index.js");
        assertResourceIsUnavailable("frontend/Flow.d.ts");
        assertResourceIsUnavailable("frontend/index.js.map");
        assertResourceIsUnavailable("frontend/index.d.ts");
        assertResourceIsUnavailable("frontend/FlowClient.d.ts");
        assertResourceIsUnavailable("frontend/vaadin-dev-tools.js");
        assertResourceIsUnavailable("frontend/copy-to-clipboard.js");
        assertResourceIsUnavailable("frontend/FlowClient.js");
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
