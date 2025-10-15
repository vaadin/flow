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
package org.vaadin.example;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

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
