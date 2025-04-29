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

package com.vaadin.flow.server.streams;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.DownloadHandler;
import com.vaadin.flow.server.DownloadRequest;
import com.vaadin.flow.server.TransferProgressListener;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;

public class ServletResourceDownloadHandlerTest {
    private static final String PATH_TO_FILE = "downloads/generated_text_file.txt";

    private VaadinRequest request;
    private VaadinResponse response;
    private VaadinSession session;
    private DownloadRequest downloadRequest;
    private OutputStream outputStream;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        request = Mockito.mock(VaadinRequest.class);
        response = Mockito.mock(VaadinResponse.class);
        session = Mockito.mock(VaadinSession.class);
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        VaadinServlet vaadinServlet = Mockito.mock(VaadinServlet.class);
        VaadinServletService vaadinService = Mockito
                .mock(VaadinServletService.class);
        Mockito.when(request.getService()).thenReturn(vaadinService);
        Mockito.when(vaadinService.getServlet()).thenReturn(vaadinServlet);
        Mockito.when(vaadinServlet.getServletContext())
                .thenReturn(servletContext);
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream(PATH_TO_FILE);
        Mockito.when(servletContext.getResourceAsStream(Mockito.anyString()))
                .thenReturn(stream);
        downloadRequest = new DownloadRequest(request, response, session,
                "download", "text/plain", null);
        outputStream = new ByteArrayOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);
    }

    @Test
    public void transferProgressListener_addListener_listenersInvoked()
            throws URISyntaxException {
        List<String> invocations = new ArrayList<>();
        List<Long> transferredBytesRecords = new ArrayList<>();
        DownloadHandler handler = DownloadHandler.forServletResource(
                PATH_TO_FILE, "download", new TransferProgressListener() {
                    @Override
                    public void onStart(TransferContext context) {
                        Assert.assertEquals(-1, context.totalBytes());
                        Assert.assertEquals("download", context.fileName());
                        invocations.add("onStart");
                    }

                    @Override
                    public void onProgress(TransferContext context,
                            long transferredBytes) {
                        transferredBytesRecords.add(transferredBytes);
                        Assert.assertEquals(-1, context.totalBytes());
                        Assert.assertEquals("download", context.fileName());
                        invocations.add("onProgress");
                    }

                    @Override
                    public void onComplete(TransferContext context,
                            long transferredBytes) {
                        Assert.assertEquals(-1, context.totalBytes());
                        Assert.assertEquals(165000, transferredBytes);
                        Assert.assertEquals("download", context.fileName());
                        invocations.add("onComplete");
                    }

                    @Override
                    public void onError(TransferContext context,
                            IOException reason) {
                        invocations.add("onError");
                    }
                });

        handler.handleDownloadRequest(downloadRequest);

        // Two invocations with interval of 65536 bytes for total size 165000
        Assert.assertEquals(
                List.of("onStart", "onProgress", "onProgress", "onComplete"),
                invocations);
        Assert.assertArrayEquals(new long[] { 65536, 131072 },
                transferredBytesRecords.stream().mapToLong(Long::longValue)
                        .toArray());
        Mockito.verify(response).setContentType("text/plain");
    }

    @Test
    public void transferProgressListener_addListener_errorOccured_errorlistenerInvoked()
            throws URISyntaxException, IOException {
        DownloadRequest downloadRequest = Mockito.mock(DownloadRequest.class);
        Mockito.when(downloadRequest.getRequest()).thenReturn(request);
        Mockito.when(downloadRequest.getSession()).thenReturn(session);
        Mockito.when(downloadRequest.getResponse()).thenReturn(response);
        OutputStream outputStreamMock = Mockito.mock(OutputStream.class);
        Mockito.doThrow(new IOException("I/O exception")).when(outputStreamMock)
                .write(Mockito.any(byte[].class), Mockito.anyInt(),
                        Mockito.anyInt());
        Mockito.when(downloadRequest.getOutputStream())
                .thenReturn(outputStreamMock);
        List<String> invocations = new ArrayList<>();
        DownloadHandler handler = DownloadHandler.forServletResource(
                PATH_TO_FILE, "download", new TransferProgressListener() {
                    @Override
                    public void onStart(TransferContext context) {
                        invocations.add("onStart");
                    }

                    @Override
                    public void onProgress(TransferContext context,
                            long transferredBytes) {
                        invocations.add("onProgress");
                    }

                    @Override
                    public void onComplete(TransferContext context,
                            long transferredBytes) {
                        invocations.add("onComplete");
                    }

                    @Override
                    public void onError(TransferContext context,
                            IOException reason) {
                        invocations.add("onError");
                        Assert.assertEquals("I/O exception",
                                reason.getMessage());
                    }
                });

        try {
            handler.handleDownloadRequest(downloadRequest);
            Assert.fail("Expected an IOException to be thrown");
        } catch (Exception e) {
        }
        Assert.assertEquals(List.of("onStart", "onError"), invocations);
    }
}