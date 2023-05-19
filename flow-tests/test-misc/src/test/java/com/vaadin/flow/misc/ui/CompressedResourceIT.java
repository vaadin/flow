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
