/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileDownloadHandlerTest {

    private static final String PATH_TO_FILE = "downloads/generated_binary_file.bin";

    private VaadinRequest request;
    private VaadinResponse response;
    private VaadinSession session;
    private VaadinService service;
    private DownloadEvent downloadEvent;
    private OutputStream outputStream;
    private Element owner;

    @BeforeEach
    void setUp() throws IOException {
        request = mock(VaadinRequest.class);
        response = mock(VaadinResponse.class);
        session = mock(VaadinSession.class);
        service = mock(VaadinService.class);

        UI ui = mock(UI.class);
        // run the command immediately
        doAnswer(invocation -> {
            Command command = invocation.getArgument(0);
            command.execute();
            return null;
        }).when(ui).access(any(Command.class));

        owner = mock(Element.class);
        Component componentOwner = mock(Component.class);
        when(owner.getComponent()).thenReturn(Optional.of(componentOwner));
        when(componentOwner.getUI()).thenReturn(Optional.of(ui));

        downloadEvent = new DownloadEvent(request, response, session, owner);
        outputStream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(outputStream);
        when(response.getService()).thenReturn(service);
        when(service.getMimeType(anyString()))
                .thenReturn("application/octet-stream");
    }

    @Test
    void transferProgressListener_addListener_listenersInvoked()
            throws URISyntaxException, IOException {
        List<String> invocations = new ArrayList<>();
        List<Long> transferredBytesRecords = new ArrayList<>();
        URL resource = getClass().getClassLoader().getResource(PATH_TO_FILE);
        DownloadHandler handler = DownloadHandler.forFile(
                new File(resource.toURI()), "download",
                new TransferProgressListener() {
                    @Override
                    public void onStart(TransferContext context) {
                        assertEquals(165000, context.contentLength());
                        assertEquals("download", context.fileName());
                        invocations.add("onStart");
                    }

                    @Override
                    public void onProgress(TransferContext context,
                            long transferredBytes, long totalBytes) {
                        transferredBytesRecords.add(transferredBytes);
                        assertEquals(165000, totalBytes);
                        assertEquals("download", context.fileName());
                        invocations.add("onProgress");
                    }

                    @Override
                    public void onComplete(TransferContext context,
                            long transferredBytes) {
                        assertEquals(165000, context.contentLength());
                        assertEquals(165000, transferredBytes);
                        assertEquals("download", context.fileName());
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
        assertEquals(
                List.of("onStart", "onProgress", "onProgress", "onComplete"),
                invocations);
        assertArrayEquals(new long[] { 65536, 131072 }, transferredBytesRecords
                .stream().mapToLong(Long::longValue).toArray());
        verify(response).setContentType("application/octet-stream");
        verify(response).setContentLengthLong(165000);
        assertNull(downloadEvent.getException());
    }

    @Test
    void handleDownloadRequest_rangeRequested_sendsRequestedBytes()
            throws URISyntaxException, IOException {
        URL resource = getClass().getClassLoader().getResource(PATH_TO_FILE);
        File file = new File(resource.toURI());
        when(request.getHeader("Range")).thenReturn("bytes=100000-100099");
        List<Long> completed = new ArrayList<>();
        DownloadHandler handler = DownloadHandler.forFile(file).whenComplete(
                (context, success) -> completed.add(context.contentLength()));

        handler.handleDownloadRequest(downloadEvent);

        byte[] expected = Arrays.copyOfRange(Files.readAllBytes(file.toPath()),
                100000, 100100);
        assertArrayEquals(expected,
                ((ByteArrayOutputStream) outputStream).toByteArray());
        verify(response).setStatus(206);
        verify(response).setHeader("Content-Range",
                "bytes 100000-100099/165000");
        verify(response).setContentLengthLong(100);
        assertEquals(List.of(100L), completed);
    }

    @Test
    void transferProgressListener_addListener_errorOccured_errorlistenerInvoked()
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
                        assertEquals(expectedMessage, reason.getMessage());
                        assertNotNull(context.exception());
                        assertEquals(FileNotFoundException.class,
                                context.exception().getClass());
                    }
                });

        try {
            handler.handleDownloadRequest(downloadEvent);
            fail("Expected an IOException to be thrown");
        } catch (Exception e) {
        }
        assertEquals(List.of("onError"), invocations);
        assertNotNull(downloadEvent.getException());
        assertEquals(FileNotFoundException.class,
                downloadEvent.getException().getClass());
        verify(response).setStatus(500);
    }

    @Test
    void inline_setFileNameInvokedByDefault()
            throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(PATH_TO_FILE);
        DownloadHandler handler = DownloadHandler
                .forFile(new File(resource.toURI()), "my-download.bin");

        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getRequest()).thenReturn(request);
        when(event.getSession()).thenReturn(session);
        when(event.getResponse()).thenReturn(response);
        when(event.getOwningElement()).thenReturn(owner);
        when(event.getOutputStream()).thenReturn(outputStream);
        when(response.getOutputStream()).thenReturn(outputStream);
        when(response.getService()).thenReturn(service);
        when(service.getMimeType(anyString()))
                .thenReturn("application/octet-stream");

        handler.handleDownloadRequest(event);

        verify(event).setFileName("my-download.bin");
        verify(event).setContentType("application/octet-stream");
        verify(event).setContentLength(165000);
    }

    @Test
    void attachment_doesNotSetFileNameWhenInlined()
            throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(PATH_TO_FILE);
        DownloadHandler handler = DownloadHandler
                .forFile(new File(resource.toURI()), "my-download.bin")
                .inline();

        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getRequest()).thenReturn(request);
        when(event.getSession()).thenReturn(session);
        when(event.getResponse()).thenReturn(response);
        when(event.getOwningElement()).thenReturn(owner);
        when(event.getOutputStream()).thenReturn(outputStream);
        when(response.getOutputStream()).thenReturn(outputStream);
        when(response.getService()).thenReturn(service);
        when(service.getMimeType(anyString()))
                .thenReturn("application/octet-stream");

        handler.handleDownloadRequest(event);

        verify(event, times(0)).setFileName("my-download.bin");
        verify(event).setContentType("application/octet-stream");
        verify(event).setContentLength(165000);
    }

    @Test
    void handleSetToInline_contentDispositionIsInlineWithFilename()
            throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(PATH_TO_FILE);
        DownloadHandler handler = DownloadHandler
                .forFile(new File(resource.toURI()), "my-download.bin")
                .inline();

        DownloadEvent event = new DownloadEvent(request, response, session,
                new Element("t"));
        when(response.getOutputStream()).thenReturn(outputStream);
        when(response.getService()).thenReturn(service);
        when(service.getMimeType(anyString()))
                .thenReturn("application/octet-stream");

        handler.handleDownloadRequest(event);

        verify(response).setHeader("Content-Disposition",
                "inline; filename=\"my-download.bin\"");
    }
}
