/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
