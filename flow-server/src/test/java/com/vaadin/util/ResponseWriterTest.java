/*
 * Copyright 2000-2017 Vaadin Ltd.
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

package com.vaadin.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.util.ResponseWriter;

/**
 * @author Vaadin Ltd.
 */
public class ResponseWriterTest {
    private ServletContext servletContext;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private AtomicLong responseContentLength;
    private OverrideableResponseWriter responseWriter;

    private static class OverrideableResponseWriter extends ResponseWriter {
        private Boolean overrideAcceptsGzippedResource;

        @Override
        protected boolean acceptsGzippedResource(HttpServletRequest request) {
            if (overrideAcceptsGzippedResource != null) {
                return overrideAcceptsGzippedResource;
            }
            return super.acceptsGzippedResource(request);
        }
    }

    private static class CapturingServletOutputStream
            extends ServletOutputStream {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        @Override
        public void write(int b) throws IOException {
            baos.write(b);
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
        }

        @Override
        public boolean isReady() {
            return true;
        }

        public byte[] getOutput() {
            return baos.toByteArray();
        }
    };

    @Before
    public void setUp() {
        responseWriter = new OverrideableResponseWriter();
        servletContext = Mockito.mock(ServletContext.class);
        request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getServletContext()).thenReturn(servletContext);
        // No header == getDateHeader returns -1 (Mockito default is 0)
        Mockito.when(request.getDateHeader(Matchers.anyString()))
                .thenReturn(-1L);

        response = Mockito.mock(HttpServletResponse.class);
        responseContentLength = new AtomicLong(-1L);
        Mockito.doAnswer(invocation -> {
            responseContentLength.set((long) invocation.getArguments()[0]);
            return null;
        }).when(response).setContentLengthLong(Matchers.anyLong());

        Assert.assertNull(VaadinService.getCurrent());
    }

    @After
    public void tearDown() {
        Assert.assertNull(VaadinService.getCurrent());
    }

    @Test
    public void contentType() {
        AtomicReference<String> contentType = new AtomicReference<>(null);
        Mockito.doAnswer(invocation -> {
            contentType.set((String) invocation.getArguments()[0]);
            return null;
        }).when(response).setContentType(Matchers.anyString());

        Mockito.when(servletContext.getMimeType("/file.png"))
                .thenReturn("image/png");

        responseWriter.writeContentType("/file.png", request, response);

        Assert.assertEquals("image/png", contentType.get());
    }

    @Test
    public void noContentType() {
        AtomicReference<String> contentType = new AtomicReference<>(null);
        Mockito.doAnswer(invocation -> {
            contentType.set((String) invocation.getArguments()[0]);
            return null;
        }).when(response).setContentType(Matchers.anyString());

        Mockito.when(servletContext.getMimeType("/file")).thenReturn(null);

        responseWriter.writeContentType("/file", request, response);

        Assert.assertNull(contentType.get());
    }

    @Test
    public void acceptsGzippedResource() {
        Assert.assertTrue(acceptsGzippedResource("compress, gzip"));
        Assert.assertTrue(acceptsGzippedResource("gzip"));
        Assert.assertTrue(acceptsGzippedResource("gzip;"));
        Assert.assertTrue(acceptsGzippedResource("gzip;q"));
        Assert.assertFalse(acceptsGzippedResource("compress; q=1 , gzip;q=0"));
        Assert.assertFalse(acceptsGzippedResource(""));
        Assert.assertFalse(acceptsGzippedResource("compress"));

        Assert.assertTrue(
                acceptsGzippedResource("compress;q = 0.5, gzip;q=0.6"));
        Assert.assertTrue(
                acceptsGzippedResource("gzip;q=1.0, identity;q=0.5, *;q=0"));
        Assert.assertTrue(acceptsGzippedResource("*"));
        Assert.assertTrue(acceptsGzippedResource("*;q=0;gzip"));
        Assert.assertFalse(acceptsGzippedResource("*;q=0"));
        Assert.assertFalse(acceptsGzippedResource("*;q=0.0"));
        Assert.assertFalse(acceptsGzippedResource("*;q=0.00"));
        Assert.assertFalse(acceptsGzippedResource("*;q=0.000"));
    }

    private boolean acceptsGzippedResource(String acceptEncodingHeader) {
        Mockito.when(request.getHeader("Accept-Encoding"))
                .thenReturn(acceptEncodingHeader);
        return responseWriter.acceptsGzippedResource(request);
    }

    @Test
    public void writeDataGzipped() throws IOException {
        responseWriter.overrideAcceptsGzippedResource = true;
        String fileJsContents = "File.js contents";
        byte[] fileJsGzippedContents = gzip(fileJsContents);

        URL fileJsURL = createFileURLWithDataAndLength("/static/file.js",
                fileJsContents.getBytes(StandardCharsets.UTF_8));
        URL fileJsGzURL = createFileURLWithDataAndLength("/static/file.js.gz",
                fileJsGzippedContents);

        Mockito.when(servletContext.getResource("/static/file.js"))
                .thenReturn(fileJsURL);
        Mockito.when(servletContext.getResource("/static/file.js.gz"))
                .thenReturn(fileJsGzURL);

        CapturingServletOutputStream out = new CapturingServletOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(out);
        responseWriter.writeResponseContents("/static/file.js", fileJsURL, request, response);

        Assert.assertArrayEquals(fileJsGzippedContents, out.getOutput());
        Assert.assertEquals(fileJsGzippedContents.length,
                responseContentLength.get());
    }

    @Test
    public void writeDataNoGzippedVersion() throws IOException {
        String fileJsContents = "File.js contents";

        byte[] fileJsContentsBytes = fileJsContents
                .getBytes(StandardCharsets.UTF_8);
        URL fileJsURL = createFileURLWithDataAndLength("/static/file.js",
                fileJsContentsBytes);

        Mockito.when(servletContext.getResource("/static/file.js"))
                .thenReturn(fileJsURL);

        CapturingServletOutputStream out = new CapturingServletOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(out);
        responseWriter.writeResponseContents("/static/file.js", fileJsURL, request, response);

        Assert.assertArrayEquals(fileJsContentsBytes, out.getOutput());
        Assert.assertEquals(fileJsContentsBytes.length,
                responseContentLength.get());
    }

    @Test
    public void writeDataBrowserDoesNotAcceptGzippedVersion()
            throws IOException {
        responseWriter.overrideAcceptsGzippedResource = false;
        String fileJsContents = "File.js contents";
        byte[] fileJsContentsBytes = fileJsContents
                .getBytes(StandardCharsets.UTF_8);

        URL fileJsURL = createFileURLWithDataAndLength("/static/file.js",
                fileJsContentsBytes);
        URL fileJsGzURL = createFileURLWithDataAndLength("/static/file.js.gz",
                gzip(fileJsContents));

        Mockito.when(servletContext.getResource("/static/file.js"))
                .thenReturn(fileJsURL);
        Mockito.when(servletContext.getResource("/static/file.js.gz"))
                .thenReturn(fileJsGzURL);

        CapturingServletOutputStream out = new CapturingServletOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(out);
        responseWriter.writeResponseContents("/static/file.js", fileJsURL, request, response);

        Assert.assertArrayEquals(fileJsContentsBytes, out.getOutput());
        Assert.assertEquals(fileJsContentsBytes.length,
                responseContentLength.get());
    }

    private byte[] gzip(String input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream stream = new GZIPOutputStream(baos)) {
            stream.write(input.getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }

    private static URL createFileURLWithDataAndLength(String name, byte[] data)
            throws MalformedURLException {
        return createFileURLWithDataAndLength(name, data, -1);
    }

    private static URL createFileURLWithDataAndLength(String name, byte[] data,
                                                      long lastModificationTime) throws MalformedURLException {
        return new URL("file", "", -1, name, new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                URLConnection connection = Mockito.mock(URLConnection.class);
                Mockito.when(connection.getInputStream())
                        .thenReturn(new ByteArrayInputStream(data));
                Mockito.when(connection.getContentLengthLong())
                        .thenReturn((long) data.length);
                Mockito.when(connection.getLastModified())
                        .thenReturn(lastModificationTime);
                return connection;
            }
        });
    }
}
