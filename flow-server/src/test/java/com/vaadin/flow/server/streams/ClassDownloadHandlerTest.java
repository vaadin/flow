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
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClassDownloadHandlerTest {
    private static final String PATH_TO_FILE = "downloads/generated_binary_file.bin";

    private VaadinRequest request;
    private VaadinResponse response;
    private VaadinSession session;
    private VaadinService service;
    private DownloadEvent downloadEvent;
    private OutputStream outputStream;
    private Element owner;
    private UI ui;

    @BeforeEach
    void setUp() throws IOException {
        request = mock(VaadinRequest.class);
        response = mock(VaadinResponse.class);
        session = mock(VaadinSession.class);
        service = mock(VaadinService.class);

        ui = mock(UI.class);
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
        DownloadHandler handler = DownloadHandler.forClassResource(
                this.getClass(), PATH_TO_FILE, "download",
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
        assertNull(downloadEvent.getException());
    }

    @Test
    void transferProgressListener_addListener_errorOccured_errorlistenerInvoked()
            throws URISyntaxException, IOException {
        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getRequest()).thenReturn(request);
        when(event.getSession()).thenReturn(session);
        when(event.getResponse()).thenReturn(response);
        when(event.getOwningElement()).thenReturn(owner);
        when(event.getUI()).thenReturn(ui);
        OutputStream outputStreamMock = mock(OutputStream.class);
        doThrow(new IOException("I/O exception")).when(outputStreamMock)
                .write(any(byte[].class), anyInt(), anyInt());
        when(event.getOutputStream()).thenReturn(outputStreamMock);
        List<String> invocations = new ArrayList<>();
        DownloadHandler handler = DownloadHandler.forClassResource(
                this.getClass(), PATH_TO_FILE, "download",
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
                        assertEquals("I/O exception", reason.getMessage());
                    }
                });

        try {
            handler.handleDownloadRequest(event);
            fail("Expected an IOException to be thrown");
        } catch (Exception e) {
        }
        assertEquals(List.of("onStart", "onError"), invocations);
        verify(event).setException(any(IOException.class));
    }

    @Test
    void inline_setFileNameInvokedByDefault() throws IOException {
        DownloadHandler handler = DownloadHandler.forClassResource(
                this.getClass(), PATH_TO_FILE, "my-download.pdf");

        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getRequest()).thenReturn(request);
        when(event.getSession()).thenReturn(session);
        when(event.getResponse()).thenReturn(response);
        when(event.getOwningElement()).thenReturn(owner);
        when(event.getOutputStream()).thenReturn(outputStream);
        when(response.getOutputStream()).thenReturn(outputStream);
        when(response.getService()).thenReturn(service);
        when(service.getMimeType(anyString())).thenReturn("application/pdf");

        handler.handleDownloadRequest(event);

        verify(event).setFileName("my-download.pdf");
        verify(event).setContentType("application/pdf");
    }

    @Test
    void attachment_doesNotSetFileNameWhenInlined() throws IOException {
        DownloadHandler handler = DownloadHandler.forClassResource(
                this.getClass(), PATH_TO_FILE, "my-download.pdf").inline();

        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getRequest()).thenReturn(request);
        when(event.getSession()).thenReturn(session);
        when(event.getResponse()).thenReturn(response);
        when(event.getOwningElement()).thenReturn(owner);
        when(event.getOutputStream()).thenReturn(outputStream);
        when(response.getOutputStream()).thenReturn(outputStream);
        when(response.getService()).thenReturn(service);
        when(service.getMimeType(anyString())).thenReturn("application/pdf");

        handler.handleDownloadRequest(event);

        verify(event, times(0)).setFileName("my-download.pdf");
        verify(event).setContentType("application/pdf");
    }

    @Test
    void handleSetToInline_contentDispositionIsInlineWithFilename()
            throws IOException {
        DownloadHandler handler = DownloadHandler.forClassResource(
                this.getClass(), PATH_TO_FILE, "my-download.pdf").inline();

        DownloadEvent event = new DownloadEvent(request, response, session,
                new Element("t"));
        when(response.getOutputStream()).thenReturn(outputStream);
        when(response.getService()).thenReturn(service);
        when(service.getMimeType(anyString()))
                .thenReturn("application/octet-stream");

        handler.handleDownloadRequest(event);

        verify(response).setHeader("Content-Disposition",
                "inline; filename=\"my-download.pdf\"");
    }
}
