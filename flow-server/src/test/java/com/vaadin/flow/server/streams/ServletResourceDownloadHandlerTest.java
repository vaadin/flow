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

import jakarta.servlet.ServletContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.ResponseWriterTest.CapturingServletOutputStream;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

class ServletResourceDownloadHandlerTest {
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
    void setUp() throws IOException, URISyntaxException {
        request = mock(VaadinRequest.class);
        response = mock(VaadinResponse.class);
        session = mock(VaadinSession.class);
        service = mock(VaadinService.class);
        ServletContext servletContext = mock(ServletContext.class);
        VaadinServlet vaadinServlet = mock(VaadinServlet.class);
        VaadinServletService vaadinService = mock(VaadinServletService.class);
        when(request.getService()).thenReturn(vaadinService);
        when(vaadinService.getServlet()).thenReturn(vaadinServlet);
        when(vaadinServlet.getServletContext()).thenReturn(servletContext);
        when(servletContext.getResource(anyString())).thenReturn(
                getClass().getClassLoader().getResource(PATH_TO_FILE));

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
        DownloadHandler handler = DownloadHandler.forServletResource(
                PATH_TO_FILE, "download", new TransferProgressListener() {
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
        DownloadEvent downloadEvent = mock(DownloadEvent.class);
        when(downloadEvent.getRequest()).thenReturn(request);
        when(downloadEvent.getSession()).thenReturn(session);
        when(downloadEvent.getResponse()).thenReturn(response);
        when(downloadEvent.getOwningElement()).thenReturn(owner);
        when(downloadEvent.getUI()).thenReturn(ui);
        OutputStream outputStreamMock = mock(OutputStream.class);
        doThrow(new IOException("I/O exception")).when(outputStreamMock)
                .write(any(byte[].class), anyInt(), anyInt());
        when(downloadEvent.getOutputStream()).thenReturn(outputStreamMock);
        List<String> invocations = new ArrayList<>();
        DownloadHandler handler = DownloadHandler.forServletResource(
                PATH_TO_FILE, "download", new TransferProgressListener() {
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
            handler.handleDownloadRequest(downloadEvent);
            fail("Expected an IOException to be thrown");
        } catch (Exception e) {
        }
        assertEquals(List.of("onStart", "onError"), invocations);
        verify(downloadEvent).setException(any(IOException.class));
    }

    @Test
    void inline_setFileNameInvokedByDefault() throws IOException {
        DownloadHandler handler = DownloadHandler
                .forServletResource(PATH_TO_FILE, "my-download.bin");

        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getSession()).thenReturn(session);
        when(event.getRequest()).thenReturn(request);
        when(event.getResponse()).thenReturn(response);
        when(event.getOwningElement()).thenReturn(owner);
        when(event.getOutputStream()).thenReturn(outputStream);
        when(event.getUI()).thenReturn(ui);
        when(response.getOutputStream()).thenReturn(outputStream);
        when(response.getService()).thenReturn(service);
        when(service.getMimeType(anyString()))
                .thenReturn("application/octet-stream");

        handler.handleDownloadRequest(event);

        verify(event).setFileName("my-download.bin");
        verify(event).setContentType("application/octet-stream");
    }

    @Test
    void attachment_doesNotSetFileNameWhenInlined() throws IOException {
        DownloadHandler handler = DownloadHandler
                .forServletResource(PATH_TO_FILE, "my-download.bin").inline();

        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getSession()).thenReturn(session);
        when(event.getRequest()).thenReturn(request);
        when(event.getResponse()).thenReturn(response);
        when(event.getOwningElement()).thenReturn(owner);
        when(event.getOutputStream()).thenReturn(outputStream);
        when(event.getUI()).thenReturn(ui);
        when(response.getOutputStream()).thenReturn(outputStream);
        when(response.getService()).thenReturn(service);
        when(service.getMimeType(anyString()))
                .thenReturn("application/octet-stream");

        handler.handleDownloadRequest(event);

        verify(event, times(0)).setFileName("my-download.bin");
        verify(event).setContentType("application/octet-stream");
    }

    @Test
    void handleSetToInline_contentDispositionIsInlineWithFilename()
            throws IOException {
        DownloadHandler handler = DownloadHandler
                .forServletResource(PATH_TO_FILE, "my-download.bin").inline();

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

    @Test
    void handleDownloadRequest_smallRangeReadFails_propagatedWithoutListenerEvents(
            @TempDir Path tempDir) throws IOException {
        File file = Files.write(tempDir.resolve("content.bin"), new byte[1000])
                .toFile();
        ServletContext servletContext = ((VaadinServletService) request
                .getService()).getServlet().getServletContext();
        when(servletContext.getResource(anyString()))
                .thenReturn(file.toURI().toURL());
        VaadinServletResponse servletResponse = mock(
                VaadinServletResponse.class);
        CapturingServletOutputStream servletOutput = new CapturingServletOutputStream() {
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                // the out-of-order part reopens a file that is gone by then
                file.delete();
                super.write(b, off, len);
            }
        };
        when(servletResponse.getOutputStream()).thenReturn(servletOutput);
        when(servletResponse.getService())
                .thenReturn(mock(VaadinServletService.class));
        when(request.getHeader("Range")).thenReturn("bytes=500-509,0-9");
        ServletResourceDownloadHandler handler = DownloadHandler
                .forServletResource("/content.bin");
        AtomicBoolean listenerNotified = new AtomicBoolean();
        handler.whenStart(() -> listenerNotified.set(true));
        handler.whenComplete(success -> listenerNotified.set(true));

        assertThrows(IOException.class, () -> handler.handleDownloadRequest(
                new DownloadEvent(request, servletResponse, session, owner)));

        assertFalse(listenerNotified.get(),
                "A range the listeners never saw start must not end for them");
        verify(servletResponse).setStatus(500);
    }

    @Test
    void handleDownloadRequest_resourceInJar_rangesServedWithChecksumETag(
            @TempDir Path tempDir) throws IOException {
        Path jar = tempDir.resolve("resources.jar");
        try (JarOutputStream jarOutput = new JarOutputStream(
                Files.newOutputStream(jar))) {
            jarOutput.putNextEntry(new JarEntry("content.txt"));
            jarOutput.write("abcdefghij".getBytes(StandardCharsets.UTF_8));
        }
        CRC32 crc = new CRC32();
        crc.update("abcdefghij".getBytes(StandardCharsets.UTF_8));
        ServletContext servletContext = ((VaadinServletService) request
                .getService()).getServlet().getServletContext();
        when(servletContext.getResource(anyString()))
                .thenReturn(new URL("jar:" + jar.toUri() + "!/content.txt"));
        VaadinServletResponse servletResponse = mock(
                VaadinServletResponse.class);
        CapturingServletOutputStream servletOutput = new CapturingServletOutputStream();
        when(servletResponse.getOutputStream()).thenReturn(servletOutput);
        VaadinServletService servletService = mock(VaadinServletService.class);
        when(servletResponse.getService()).thenReturn(servletService);
        // out of order, so the second part is read from the jar again
        when(request.getHeader("Range")).thenReturn("bytes=6-7,1-2");

        DownloadHandler.forServletResource("/content.txt")
                .handleDownloadRequest(new DownloadEvent(request,
                        servletResponse, session, owner));

        String body = new String(servletOutput.getOutput(),
                StandardCharsets.UTF_8);
        assertTrue(body.contains("bytes 6-7/10\r\n\r\ngh"), body);
        assertTrue(body.contains("bytes 1-2/10\r\n\r\nbc"), body);
        verify(servletResponse).setStatus(206);
        verify(servletResponse).setHeader("ETag",
                "\"" + Long.toHexString(crc.getValue()) + "-a\"");
    }

    @ParameterizedTest
    @CsvSource({ "FILE, false", "STORED, false", "DEFLATED, true",
            "CONTAINER, true" })
    void handleDownloadRequest_largeResource_rangeServed(String kind,
            boolean extracted, @TempDir Path tempDir) throws IOException {
        byte[] content = new byte[(int) AbstractDownloadHandler.SeekableContent.MIN_EXTRACTED_LENGTH
                + 1];
        Path jar = tempDir.resolve("resources.jar");
        try (JarOutputStream jarOutput = new JarOutputStream(
                Files.newOutputStream(jar))) {
            JarEntry entry = new JarEntry("video.mp4");
            if ("STORED".equals(kind)) {
                CRC32 crc = new CRC32();
                crc.update(content);
                entry.setMethod(ZipEntry.STORED);
                entry.setSize(content.length);
                entry.setCrc(crc.getValue());
            }
            jarOutput.putNextEntry(entry);
            jarOutput.write(content);
        }
        URL jarUrl = new URL("jar:" + jar.toUri() + "!/video.mp4");
        // an archive opened by the servlet container, such as a packed war,
        // whose connection is not a JarURLConnection
        URL containerUrl = new URL(null, "war:" + jar.toUri() + "*/video.mp4",
                new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL url)
                            throws IOException {
                        URLConnection jarConnection = jarUrl.openConnection();
                        return new URLConnection(url) {
                            @Override
                            public void connect() {
                                connected = true;
                            }

                            @Override
                            public long getContentLengthLong() {
                                return jarConnection.getContentLengthLong();
                            }

                            @Override
                            public InputStream getInputStream()
                                    throws IOException {
                                return jarConnection.getInputStream();
                            }
                        };
                    }
                });
        ServletContext servletContext = ((VaadinServletService) request
                .getService()).getServlet().getServletContext();
        URL fileUrl = Files.write(tempDir.resolve("video.mp4"), content).toUri()
                .toURL();
        URL resourceUrl = switch (kind) {
        case "FILE" -> fileUrl;
        case "CONTAINER" -> containerUrl;
        default -> jarUrl;
        };
        when(servletContext.getResource(anyString())).thenReturn(resourceUrl);
        // only content that cannot be read from a position is extracted
        assertEquals(extracted,
                AbstractDownloadHandler.SeekableContent
                        .ofResource(resourceUrl, resourceUrl.openConnection())
                        .sequential());
        VaadinServletResponse servletResponse = mock(
                VaadinServletResponse.class);
        CapturingServletOutputStream servletOutput = new CapturingServletOutputStream();
        when(servletResponse.getOutputStream()).thenReturn(servletOutput);
        VaadinServletService servletService = mock(VaadinServletService.class);
        when(servletResponse.getService()).thenReturn(servletService);
        when(request.getHeader("Range")).thenReturn("bytes=2-5");

        DownloadHandler.forServletResource("/video.mp4").handleDownloadRequest(
                new DownloadEvent(request, servletResponse, session, owner));

        assertEquals(4, servletOutput.getOutput().length);
        verify(servletResponse).setStatus(206);
    }
}
