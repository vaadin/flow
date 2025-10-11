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
package com.vaadin.flow.misc.ui;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.AbstractParallelTestBenchTest;

public class CompressedResourceIT extends AbstractParallelTestBenchTest {

    @Test
    public void compressedResourcesAreServedCompressed() throws IOException {
        URL textFileUrl = new URL(getRootURL() + "/textfile.txt");
        HttpURLConnection connection = (HttpURLConnection) textFileUrl
                .openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept-Encoding", "gzip");

        int status = connection.getResponseCode();
        Assert.assertEquals(200, status);
        Assert.assertEquals("gzip",
                connection.getHeaderField("Content-Encoding"));

        byte[] bytes = new byte[1024];
        int count = new GZIPInputStream(connection.getInputStream()).read(bytes,
                0, bytes.length);
        String content = new String(bytes, 0, count, StandardCharsets.UTF_8);
        Assert.assertEquals("Text file contents.\n", content);
    }
}
