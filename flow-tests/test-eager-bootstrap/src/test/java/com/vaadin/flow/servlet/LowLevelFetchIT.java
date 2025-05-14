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

package com.vaadin.flow.servlet;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class LowLevelFetchIT extends ChromeBrowserTest {

    public static class OtherResponseCodeException extends IOException {

        private int responseCode;

        public OtherResponseCodeException(int responseCode) {
            this.responseCode = responseCode;
        }

        public int getResponseCode() {
            return responseCode;
        }

    }

    @Test
    public void rootViewContainsRootUIDL() throws Exception {
        open(); // This is only to wait for the dev server...
        String source = getUrl(getRootURL() + "/");
        Assert.assertTrue(source.contains(
                "\"key\":\"text\",\"feat\":7,\"value\":\"This is the root view\""));
    }

    private String getUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();

        if (conn.getResponseCode() != 200) {
            throw new OtherResponseCodeException(conn.getResponseCode());
        }

        return IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8);
    }

    @Test
    public void helloViewContainsHelloViewUidl() throws Exception {
        open(); // This is only to wait for the dev server...
        String source = getUrl(getRootURL() + "/hello");
        Assert.assertTrue(source.contains(
                "\"key\":\"text\",\"feat\":7,\"value\":\"This is the Hello view\""));
    }

    @Test
    public void notFoundViewReturns404() throws Exception {
        open(); // This is only to wait for the dev server...
        try {
            getUrl(getRootURL() + "/notfound");
        } catch (OtherResponseCodeException e) {
            Assert.assertEquals(404, e.getResponseCode());
            return;
        }
        Assert.fail("Should have thrown OtherResponseCodeException");
    }

    @Override
    protected String getTestPath() {
        return "";
    }

}
