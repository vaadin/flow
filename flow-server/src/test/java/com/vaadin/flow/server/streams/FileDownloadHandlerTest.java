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
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.frontend.FrontendUtils;

public class FileDownloadHandlerTest {

    private static final String PATH_TO_FILE = "downloads/generated_binary_file.bin";

    private VaadinRequest request;
    private VaadinResponse response;
    private VaadinSession session;
    private VaadinService service;
    private DownloadEvent downloadEvent;
    private OutputStream outputStream;
    private Element owner;

    @Before
    public void setUp() throws IOException {
        request = Mockito.mock(VaadinRequest.class);
        response = Mockito.mock(VaadinResponse.class);
        session = Mockito.mock(VaadinSession.class);
        service = Mockito.mock(VaadinService.class);

        UI ui = Mockito.mock(UI.class);
        // run the command immediately
        Mockito.doAnswer(invocation -> {
            Command command = invocation.getArgument(0);
            command.execute();
            return null;
        }).when(ui).access(Mockito.any(Command.class));

        owner = Mockito.mock(Element.class);
        Component componentOwner = Mockito.mock(Component.class);
        Mockito.when(owner.getComponent())
                .thenReturn(Optional.of(componentOwner));
        Mockito.when(componentOwner.getUI()).thenReturn(Optional.of(ui));

        downloadEvent = new DownloadEvent(request, response, session, owner);
        outputStream = new ByteArrayOutputStream();
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);
        Mockito.when(response.getService()).thenReturn(service);
        Mockito.when(service.getMimeType(Mockito.anyString()))
                .thenReturn("application/octet-stream");
    }

    @Test
    public void transferProgressListener_addListener_listenersInvoked()
            throws URISyntaxException, IOException {
        List<String> invocations = new ArrayList<>();
        List<Long> transferredBytesRecords = new ArrayList<>();
        URL resource = getClass().getClassLoader().getResource(PATH_TO_FILE);
        DownloadHandler handler = DownloadHandler.forFile(
                new File(resource.toURI()), "download",
                new TransferProgressListener() {
                    @Override
                    public void onStart(TransferContext context) {
                        Assert.assertEquals(165000, context.contentLength());
                        Assert.assertEquals("download", context.fileName());
                        invocations.add("onStart");
                    }

                    @Override
                    public void onProgress(TransferContext context,
                            long transferredBytes, long totalBytes) {
                        transferredBytesRecords.add(transferredBytes);
                        Assert.assertEquals(165000, totalBytes);
                        Assert.assertEquals("download", context.fileName());
                        invocations.add("onProgress");
                    }

                    @Override
                    public void onComplete(TransferContext context,
                            long transferredBytes) {
                        Assert.assertEquals(165000, context.contentLength());
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

        handler.handleDownloadRequest(downloadEvent);

        // Two invocations with interval of 65536 bytes for total size 165000
        Assert.assertEquals(
                List.of("onStart", "onProgress", "onProgress", "onComplete"),
                invocations);
        Assert.assertArrayEquals(new long[] { 65536, 131072 },
                transferredBytesRecords.stream().mapToLong(Long::longValue)
                        .toArray());
        Mockito.verify(response).setContentType("application/octet-stream");
        Mockito.verify(response).setContentLengthLong(165000);
    }

    @Test
    public void transferProgressListener_addListener_errorOccured_errorlistenerInvoked()
            throws URISyntaxException {
        List<String> invocations = new ArrayList<>();
        DownloadHandler handler = DownloadHandler.forFile(
                new File("non-existing-file"), "download",
                new TransferProgressListener() {
                    @Override
                    public void onStart(TransferContext context) {
                        invocations.add("onStart");
                    }

                    @Override
                    public void onProgress(TransferContext context,
                            long transferredBytes, long totalBytes) {
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
                        String expectedMessage = "non-existing-file (No such file or directory)";
                        if (FrontendUtils.isWindows()) {
                            expectedMessage = "non-existing-file (The system cannot find the file specified)";
                        }
                        Assert.assertEquals(expectedMessage,
                                reason.getMessage());
                    }
                });

        try {
            handler.handleDownloadRequest(downloadEvent);
            Assert.fail("Expected an IOException to be thrown");
        } catch (Exception e) {
        }
        Assert.assertEquals(List.of("onError"), invocations);
        Mockito.verify(response).setStatus(500);
    }

    @Test
    public void inline_setFileNameInvokedByDefault()
            throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(PATH_TO_FILE);
        DownloadHandler handler = DownloadHandler
                .forFile(new File(resource.toURI()), "my-download.bin");

        DownloadEvent event = Mockito.mock(DownloadEvent.class);
        Mockito.when(event.getSession()).thenReturn(session);
        Mockito.when(event.getResponse()).thenReturn(response);
        Mockito.when(event.getOwningElement()).thenReturn(owner);
        Mockito.when(event.getOutputStream()).thenReturn(outputStream);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);
        Mockito.when(response.getService()).thenReturn(service);
        Mockito.when(service.getMimeType(Mockito.anyString()))
                .thenReturn("application/octet-stream");

        handler.handleDownloadRequest(event);

        Mockito.verify(event).setFileName("my-download.bin");
        Mockito.verify(event).setContentType("application/octet-stream");
        Mockito.verify(event).setContentLength(165000);
    }

    @Test
    public void attachment_doesNotSetFileNameWhenInlined()
            throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(PATH_TO_FILE);
        DownloadHandler handler = DownloadHandler
                .forFile(new File(resource.toURI()), "my-download.bin")
                .inline();

        DownloadEvent event = Mockito.mock(DownloadEvent.class);
        Mockito.when(event.getSession()).thenReturn(session);
        Mockito.when(event.getResponse()).thenReturn(response);
        Mockito.when(event.getOwningElement()).thenReturn(owner);
        Mockito.when(event.getOutputStream()).thenReturn(outputStream);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);
        Mockito.when(response.getService()).thenReturn(service);
        Mockito.when(service.getMimeType(Mockito.anyString()))
                .thenReturn("application/octet-stream");

        handler.handleDownloadRequest(event);

        Mockito.verify(event, Mockito.times(0)).setFileName("my-download.bin");
        Mockito.verify(event).setContentType("application/octet-stream");
        Mockito.verify(event).setContentLength(165000);
    }

    @Test
    public void handleSetToInline_contentTypeIsInline()
            throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(PATH_TO_FILE);
        DownloadHandler handler = DownloadHandler
                .forFile(new File(resource.toURI()), "my-download.bin")
                .inline();

        DownloadEvent event = new DownloadEvent(request, response, session,
                new Element("t"));
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);
        Mockito.when(response.getService()).thenReturn(service);
        Mockito.when(service.getMimeType(Mockito.anyString()))
                .thenReturn("application/octet-stream");

        handler.handleDownloadRequest(event);

        Mockito.verify(response).setHeader("Content-Disposition", "inline");
    }
}
