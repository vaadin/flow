/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package org.vaadin.example;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

/**
 * Class for testing issues in a spring-boot container.
 */
public class AppViewIT extends ChromeBrowserTest {

    // https://github.com/vaadin/flow/issues/9005
    @Test
    public void should_load_image_from_custom_mvc_resource_handler()
            throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(getRootURL() + "/foo" + "/img/yes.png");
        CloseableHttpResponse response = httpclient.execute(httpGet);
        try {
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
        } finally {
            response.close();
        }
    }

}
