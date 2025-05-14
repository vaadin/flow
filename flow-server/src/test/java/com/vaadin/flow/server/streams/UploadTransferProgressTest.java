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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Part;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.streams.TransferProgressListener;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.communication.StreamRequestHandler;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.server.communication.StreamRequestHandler.DYN_RES_PREFIX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UploadTransferProgressTest {
    private static final int DUMMY_CONTENT_LENGTH = 160000;
    public static final String DUMMY_FILE_NAME = "test.tmp";

    private MockVaadinSession session;
    private VaadinServletRequest request;
    private VaadinResponse response;
    private StreamResourceRegistry streamResourceRegistry;
    private UI ui;
    private Element element;
    private UploadEvent uploadEvent;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws ServletException, ServiceException {
        VaadinService service = new MockVaadinServletService();
        session = new AlwaysLockedVaadinSession(service) {
            @Override
            public StreamResourceRegistry getResourceRegistry() {
                return streamResourceRegistry;
            }
        };
        streamResourceRegistry = new StreamResourceRegistry(session);
        request = Mockito.mock(VaadinServletRequest.class);
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getMimeType(Mockito.anyString()))
                .thenReturn(null);
        Mockito.when(request.getServletContext()).thenReturn(servletContext);
        element = Mockito.mock(Element.class);
        response = Mockito.mock(VaadinResponse.class);
        uploadEvent = Mockito.mock(UploadEvent.class);
        Mockito.doReturn(DUMMY_FILE_NAME).when(uploadEvent).getFileName();
        Mockito.when(uploadEvent.getContentType())
                .thenReturn("application/octet-stream");
        Mockito.when(uploadEvent.getFileSize())
                .thenReturn((long) DUMMY_CONTENT_LENGTH);
        Mockito.when(uploadEvent.getInputStream())
                .thenReturn(createRandomBytes(DUMMY_CONTENT_LENGTH));
        Mockito.when(uploadEvent.getResponse()).thenReturn(response);
        Mockito.when(uploadEvent.getSession()).thenReturn(session);
        Mockito.when(uploadEvent.getOwningElement()).thenReturn(element);
        Mockito.when(uploadEvent.getRequest()).thenReturn(request);
        Component componentOwner = Mockito.mock(Component.class);
        Mockito.when(element.getComponent())
                .thenReturn(Optional.of(componentOwner));
        ui = Mockito.mock(UI.class);
        // run the command immediately
        Mockito.doAnswer(invocation -> {
            Command command = invocation.getArgument(0);
            command.execute();
            return null;
        }).when(ui).access(Mockito.any(Command.class));

        Mockito.when(componentOwner.getUI()).thenReturn(Optional.of(ui));
        Mockito.when(uploadEvent.getOwningComponent())
                .thenReturn(componentOwner);
    }

    @Test
    public void transferProgressListener_toFile_addListener_listenersInvoked()
            throws URISyntaxException, IOException {
        AtomicReference<File> actualFile = new AtomicReference<>();
        AtomicReference<File> expectedFile = new AtomicReference<>();
        List<String> invocations = new ArrayList<>();
        List<Long> transferredBytesRecords = new ArrayList<>();
        UploadHandler handler = UploadHandler
                .toFile((meta, file) -> actualFile.set(file), (fileName) -> {
                    try {
                        File file = temporaryFolder.newFile(DUMMY_FILE_NAME);
                        expectedFile.set(file);
                        return file;
                    } catch (IOException e) {
                        Assert.fail("Failed to create temp file: "
                                + e.getMessage());
                    }
                    return null;
                }, createTransferProgressListener(invocations,
                        transferredBytesRecords));

        handler.handleUploadRequest(uploadEvent);

        assertListenersInvoked(invocations, transferredBytesRecords);
        Assert.assertEquals(expectedFile.get(), actualFile.get());
    }

    @Test
    public void transferProgressListener_toFile_addListener_errorOccured_errorlistenerInvoked()
            throws URISyntaxException {
        List<String> invocations = new ArrayList<>();
        UploadHandler handler = UploadHandler.toFile((meta, file) -> {
        }, (fileName) -> {
            throw new IOException("Test exception");
        }, createErrorTransferProgressListener(invocations));

        try {
            handler.handleUploadRequest(uploadEvent);
            Assert.fail("Expected an IOException to be thrown");
        } catch (Exception e) {
        }
        Assert.assertEquals(List.of("onError"), invocations);
    }

    @Test
    public void transferProgressListener_toTempFile_addListener_listenersInvoked()
            throws URISyntaxException, IOException {
        List<String> invocations = new ArrayList<>();
        List<Long> transferredBytesRecords = new ArrayList<>();
        UploadHandler handler = UploadHandler.toTempFile((meta, file) -> {
        }, createTransferProgressListener(invocations,
                transferredBytesRecords));

        handler.handleUploadRequest(uploadEvent);

        // Two invocations with interval of 65536 bytes for total size 165000
        assertListenersInvoked(invocations, transferredBytesRecords);
    }

    @Test
    public void transferProgressListener_toTempFile_addListener_errorOccured_errorlistenerInvoked()
            throws URISyntaxException, IOException {
        List<String> invocations = new ArrayList<>();

        InputStream inputStream = mock(InputStream.class);
        Mockito.doThrow(new IOException("Test exception")).when(inputStream)
                .read(Mockito.any(byte[].class), Mockito.anyInt(),
                        Mockito.anyInt());
        Mockito.when(uploadEvent.getInputStream()).thenReturn(inputStream);

        UploadHandler handler = UploadHandler.toTempFile((meta, file) -> {
        }, createErrorTransferProgressListener(invocations));

        try {
            handler.handleUploadRequest(uploadEvent);
            Assert.fail("Expected an IOException to be thrown");
        } catch (Exception e) {
        }
        Assert.assertEquals(List.of("onStart", "onError"), invocations);
    }

    @Test
    public void transferProgressListener_inMemory_addListener_listenersInvoked()
            throws URISyntaxException, IOException {
        List<String> invocations = new ArrayList<>();
        List<Long> transferredBytesRecords = new ArrayList<>();
        UploadHandler handler = UploadHandler.inMemory((meta, bytes) -> {
        }, createTransferProgressListener(invocations,
                transferredBytesRecords));

        handler.handleUploadRequest(uploadEvent);

        // Two invocations with interval of 65536 bytes for total size 165000
        assertListenersInvoked(invocations, transferredBytesRecords);
    }

    @Test
    public void transferProgressListener_inMemory_addListener_errorOccured_errorlistenerInvoked()
            throws URISyntaxException, IOException {
        List<String> invocations = new ArrayList<>();

        InputStream inputStream = mock(InputStream.class);
        Mockito.doThrow(new IOException("Test exception")).when(inputStream)
                .read(Mockito.any(byte[].class), Mockito.anyInt(),
                        Mockito.anyInt());
        Mockito.when(uploadEvent.getInputStream()).thenReturn(inputStream);

        UploadHandler handler = UploadHandler.inMemory((meta, bytes) -> {
        }, createErrorTransferProgressListener(invocations));

        try {
            handler.handleUploadRequest(uploadEvent);
            Assert.fail("Expected an IOException to be thrown");
        } catch (Exception e) {
        }
        Assert.assertEquals(List.of("onStart", "onError"), invocations);
    }

    private ByteArrayInputStream createRandomBytes(int size) {
        byte[] bytes = new byte[size];
        new Random().nextBytes(bytes);
        return new ByteArrayInputStream(bytes);
    }

    private static TransferProgressListener createTransferProgressListener(
            List<String> invocations, List<Long> transferredBytesRecords) {
        return new TransferProgressListener() {
            @Override
            public void onStart(TransferContext context) {
                Assert.assertEquals(DUMMY_CONTENT_LENGTH,
                        context.contentLength());
                Assert.assertEquals(DUMMY_FILE_NAME, context.fileName());
                invocations.add("onStart");
            }

            @Override
            public void onProgress(TransferContext context,
                    long transferredBytes, long totalBytes) {
                transferredBytesRecords.add(transferredBytes);
                Assert.assertEquals(DUMMY_CONTENT_LENGTH, totalBytes);
                Assert.assertEquals(DUMMY_FILE_NAME, context.fileName());
                invocations.add("onProgress");
            }

            @Override
            public void onComplete(TransferContext context,
                    long transferredBytes) {
                Assert.assertEquals(DUMMY_CONTENT_LENGTH,
                        context.contentLength());
                Assert.assertEquals(DUMMY_CONTENT_LENGTH, transferredBytes);
                Assert.assertEquals(DUMMY_FILE_NAME, context.fileName());
                invocations.add("onComplete");
            }

            @Override
            public void onError(TransferContext context, IOException reason) {
                invocations.add("onError");
            }
        };
    }

    private static TransferProgressListener createErrorTransferProgressListener(
            List<String> invocations) {
        return new TransferProgressListener() {
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
                Assert.assertEquals("Test exception", reason.getMessage());
            }
        };
    }

    private static void assertListenersInvoked(List<String> invocations,
            List<Long> transferredBytesRecords) {
        // Two invocations with interval of 65536 bytes for total size 165000
        Assert.assertEquals(
                List.of("onStart", "onProgress", "onProgress", "onComplete"),
                invocations);
        Assert.assertArrayEquals(new long[] { 65536, 131072 },
                transferredBytesRecords.stream().mapToLong(Long::longValue)
                        .toArray());
    }
}
