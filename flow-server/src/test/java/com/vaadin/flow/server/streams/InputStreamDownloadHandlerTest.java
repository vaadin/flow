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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
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

public class InputStreamDownloadHandlerTest {
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
            throws URISyntaxException {
        List<String> invocations = new ArrayList<>();
        List<Long> transferredBytesRecords = new ArrayList<>();
        DownloadHandler handler = DownloadHandler.fromInputStream(request -> {
            byte[] data = getBytes();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            return new DownloadResponse(inputStream, "download",
                    "application/octet-stream", data.length);
        }, "download", new TransferProgressListener() {
            @Override
            public void onStart(TransferContext context) {
                Assert.assertEquals(-1, context.contentLength());
                Assert.assertEquals("download", context.fileName());
                invocations.add("onStart");
            }

            @Override
            public void onProgress(TransferContext context,
                    long transferredBytes, long totalBytes) {
                transferredBytesRecords.add(transferredBytes);
                Assert.assertEquals(-1, totalBytes);
                Assert.assertEquals("download", context.fileName());
                invocations.add("onProgress");
            }

            @Override
            public void onComplete(TransferContext context,
                    long transferredBytes) {
                Assert.assertEquals(-1, context.contentLength());
                Assert.assertEquals(165000, transferredBytes);
                Assert.assertEquals("download", context.fileName());
                invocations.add("onComplete");
            }

            @Override
            public void onError(TransferContext context, IOException reason) {
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
    }

    @Test
    public void transferProgressListener_addListener_errorOccured_errorlistenerInvoked()
            throws URISyntaxException, IOException {
        DownloadEvent event = Mockito.mock(DownloadEvent.class);
        Mockito.when(event.getSession()).thenReturn(session);
        Mockito.when(event.getResponse()).thenReturn(response);
        Mockito.when(event.getOwningElement()).thenReturn(owner);
        OutputStream outputStreamMock = Mockito.mock(OutputStream.class);
        Mockito.doThrow(new IOException("I/O exception")).when(outputStreamMock)
                .write(Mockito.any(byte[].class), Mockito.anyInt(),
                        Mockito.anyInt());
        Mockito.when(event.getOutputStream()).thenReturn(outputStreamMock);
        List<String> invocations = new ArrayList<>();
        DownloadHandler handler = DownloadHandler.fromInputStream(req -> {
            // Simulate a download of 165000 bytes
            byte[] data = getBytes();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            return new DownloadResponse(inputStream, "download",
                    "application/octet-stream", data.length);
        }, "download", new TransferProgressListener() {
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
            public void onError(TransferContext context, IOException reason) {
                invocations.add("onError");
                Assert.assertEquals("I/O exception", reason.getMessage());
            }
        });

        try {
            handler.handleDownloadRequest(event);
            Assert.fail("Expected an IOException to be thrown");
        } catch (Exception e) {
        }
        Assert.assertEquals(List.of("onStart", "onError"), invocations);
    }

    @Test
    public void inline_setFileNameInvokedByDefault() throws IOException {
        DownloadHandler handler = DownloadHandler.fromInputStream(request -> {
            byte[] data = getBytes();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            return new DownloadResponse(inputStream, "download",
                    "application/octet-stream", data.length);
        }, "my-download.bin");

        DownloadEvent event = Mockito.mock(DownloadEvent.class);
        Mockito.when(event.getSession()).thenReturn(session);
        Mockito.when(event.getRequest()).thenReturn(request);
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
    }

    @Test
    public void attachment_doesNotSetFileNameWhenInlined() throws IOException {
        DownloadHandler handler = DownloadHandler.fromInputStream(request -> {
            byte[] data = getBytes();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            return new DownloadResponse(inputStream, "download",
                    "application/octet-stream", data.length);
        }, "my-download.bin").inline();

        DownloadEvent event = Mockito.mock(DownloadEvent.class);
        Mockito.when(event.getSession()).thenReturn(session);
        Mockito.when(event.getRequest()).thenReturn(request);
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
    }

    private static byte[] getBytes() {
        // Simulate a download of 165000 bytes
        byte[] data = new byte[165000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }
        return data;
    }
}