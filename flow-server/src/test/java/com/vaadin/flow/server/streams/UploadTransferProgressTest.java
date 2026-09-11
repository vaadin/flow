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
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Part;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UploadTransferProgressTest {
    private static final int DUMMY_CONTENT_LENGTH = 160000;
    public static final String DUMMY_FILE_NAME = "test.tmp";

    private MockVaadinSession session;
    private VaadinServletRequest request;
    private VaadinResponse response;
    private StreamResourceRegistry streamResourceRegistry;
    private UI ui;
    private Element element;
    private Part part;
    private UploadEvent uploadEvent;
    @TempDir
    Path temporaryFolder;

    @BeforeEach
    void setUp() throws ServletException, ServiceException, IOException {
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
        // The upload content is served through the multipart Part; individual
        // tests re-stub this to control the bytes and reads they need.
        part = Mockito.mock(Part.class);
        Mockito.when(part.getInputStream())
                .thenReturn(createRandomBytes(DUMMY_CONTENT_LENGTH));
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

        uploadEvent = new UploadEvent(request, response, session,
                DUMMY_FILE_NAME, DUMMY_CONTENT_LENGTH,
                "application/octet-stream", element, part);
    }

    @Test
    void transferProgressListener_toFile_addListener_listenersInvoked()
            throws URISyntaxException, IOException {
        AtomicReference<File> actualFile = new AtomicReference<>();
        AtomicReference<File> expectedFile = new AtomicReference<>();
        List<String> invocations = new ArrayList<>();
        List<Long> transferredBytesRecords = new ArrayList<>();
        UploadHandler handler = UploadHandler.toFile(
                (meta, file) -> actualFile.set(file), (uploadMetadata) -> {
                    try {
                        File file = Files.createFile(
                                temporaryFolder.resolve(DUMMY_FILE_NAME))
                                .toFile();
                        expectedFile.set(file);
                        return file;
                    } catch (IOException e) {
                        fail("Failed to create temp file: " + e.getMessage());
                    }
                    return null;
                }, createTransferProgressListener(invocations,
                        transferredBytesRecords));

        handler.handleUploadRequest(uploadEvent);

        assertListenersInvoked(invocations, transferredBytesRecords);
        assertEquals(expectedFile.get(), actualFile.get());
    }

    @Test
    void transferProgressListener_toFile_addListener_errorOccured_errorlistenerInvoked()
            throws URISyntaxException {
        List<String> invocations = new ArrayList<>();
        UploadHandler handler = UploadHandler.toFile((meta, file) -> {
        }, (uploadMetadata) -> {
            throw new IOException("Test exception");
        }, createErrorTransferProgressListener(invocations));

        try {
            handler.handleUploadRequest(uploadEvent);
            fail("Expected an IOException to be thrown");
        } catch (Exception e) {
        }
        assertEquals(List.of("onError"), invocations);
    }

    @Test
    void transferProgressListener_toTempFile_addListener_listenersInvoked()
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
    void transferProgressListener_toTempFile_addListener_errorOccured_errorlistenerInvoked()
            throws URISyntaxException, IOException {
        List<String> invocations = new ArrayList<>();

        InputStream inputStream = mock(InputStream.class);
        Mockito.doThrow(new IOException("Test exception")).when(inputStream)
                .read(Mockito.any(byte[].class), Mockito.anyInt(),
                        Mockito.anyInt());
        Mockito.when(part.getInputStream()).thenReturn(inputStream);

        UploadHandler handler = UploadHandler.toTempFile((meta, file) -> {
        }, createErrorTransferProgressListener(invocations));

        try {
            handler.handleUploadRequest(uploadEvent);
            fail("Expected an IOException to be thrown");
        } catch (Exception e) {
        }
        assertEquals(List.of("onStart", "onError"), invocations);
    }

    @Test
    void transferProgressListener_inMemory_addListener_listenersInvoked()
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
    void transferProgressListener_inMemory_addListener_errorOccured_errorlistenerInvoked()
            throws URISyntaxException, IOException {
        List<String> invocations = new ArrayList<>();

        InputStream inputStream = mock(InputStream.class);
        Mockito.doThrow(new IOException("Test exception")).when(inputStream)
                .read(Mockito.any(byte[].class), Mockito.anyInt(),
                        Mockito.anyInt());
        Mockito.when(part.getInputStream()).thenReturn(inputStream);

        UploadHandler handler = UploadHandler.inMemory((meta, bytes) -> {
        }, createErrorTransferProgressListener(invocations));

        try {
            handler.handleUploadRequest(uploadEvent);
            fail("Expected an IOException to be thrown");
        } catch (Exception e) {
        }
        assertEquals(List.of("onStart", "onError"), invocations);
    }

    @Test
    void withValidator_rejectsAtStart_inMemory_callbackNotInvoked()
            throws IOException {
        AtomicBoolean completed = new AtomicBoolean();
        List<String> events = new ArrayList<>();
        UploadHandler handler = UploadHandler
                .inMemory((meta, bytes) -> completed.set(true),
                        recordingListener(events))
                .validateMetadata(event -> event.reject("rejected"));

        handler.handleUploadRequest(uploadEvent);

        assertFalse(completed.get(),
                "Success callback must not run for a rejected upload");
        assertTrue(uploadEvent.isRejected());
        // Rejected before the transfer starts, so no onStart; the rejection is
        // still reported as a terminal onError.
        assertEquals(List.of("onError:UploadRejectedException"), events);
    }

    @Test
    void withValidator_rejectsAtStart_toFile_noFileCreatedAndCallbackNotInvoked()
            throws IOException {
        AtomicReference<File> createdFile = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean();
        List<String> events = new ArrayList<>();
        UploadHandler handler = UploadHandler
                .toFile((meta, file) -> completed.set(true),
                        capturingFileFactory(createdFile),
                        recordingListener(events))
                .validateMetadata(event -> event.reject("rejected"));

        handler.handleUploadRequest(uploadEvent);

        assertFalse(completed.get(),
                "Success callback must not run for a rejected upload");
        assertNull(createdFile.get(),
                "No file should be created for a metadata-rejected upload");
        assertEquals(List.of("onError:UploadRejectedException"), events);
    }

    @Test
    void validateComplete_throwsIOException_toFile_fileDeletedAndErrorReported() {
        AtomicReference<File> createdFile = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean();
        List<String> events = new ArrayList<>();
        UploadHandler handler = UploadHandler
                .toFile((meta, file) -> completed.set(true),
                        capturingFileFactory(createdFile),
                        recordingListener(events))
                .validateComplete((event, content) -> {
                    throw new IOException("Validation failed");
                });

        IOException thrown = assertThrows(IOException.class,
                () -> handler.handleUploadRequest(uploadEvent));
        assertEquals("Validation failed", thrown.getMessage());

        assertFalse(completed.get(),
                "Success callback must not run when validation fails");
        assertFalse(createdFile.get().exists(),
                "File should be deleted when validation throws");
        assertTrue(events.contains("onError:IOException"),
                "A validator IOException should be reported via onError");
    }

    @Test
    void withValidator_multipleValidators_firstRejectionShortCircuits()
            throws IOException {
        AtomicBoolean secondInvoked = new AtomicBoolean();
        UploadHandler handler = UploadHandler.inMemory((meta, bytes) -> {
        }).validateMetadata(event -> event.reject("rejected"))
                .validateMetadata(event -> secondInvoked.set(true));

        handler.handleUploadRequest(uploadEvent);

        assertFalse(secondInvoked.get(),
                "A later validator must not run once an earlier one rejects");
    }

    @Test
    void validateHeader_rejects_stopsReadingEarly() throws IOException {
        AtomicInteger bytesRead = new AtomicInteger();
        InputStream counting = new FilterInputStream(
                createRandomBytes(DUMMY_CONTENT_LENGTH)) {
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int n = super.read(b, off, len);
                if (n > 0) {
                    bytesRead.addAndGet(n);
                }
                return n;
            }
        };
        when(part.getInputStream()).thenReturn(counting);
        AtomicBoolean completed = new AtomicBoolean();
        UploadHandler handler = UploadHandler
                .inMemory((meta, bytes) -> completed.set(true))
                .validateHeader(4, (event, header) -> event.reject("bad"));

        handler.handleUploadRequest(uploadEvent);

        assertFalse(completed.get(),
                "Success callback must not run for a rejected upload");
        assertTrue(uploadEvent.isRejected());
        // Only the 4 header bytes are read; the rest of the upload is not.
        assertEquals(4, bytesRead.get(),
                "Reading should stop after the header is validated");
    }

    @Test
    void validateComplete_throwsRuntimeException_toFile_fileDeleted() {
        AtomicReference<File> createdFile = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean();
        List<String> events = new ArrayList<>();
        UploadHandler handler = UploadHandler
                .toFile((meta, file) -> completed.set(true),
                        capturingFileFactory(createdFile),
                        recordingListener(events))
                .validateComplete((event, content) -> {
                    throw new IllegalStateException("Validation blew up");
                });

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> handler.handleUploadRequest(uploadEvent));
        assertEquals("Validation blew up", thrown.getMessage());

        assertFalse(completed.get(),
                "Success callback must not run when validation fails");
        assertFalse(createdFile.get().exists(),
                "File should be deleted when validation throws");
        // An unchecked failure is not reported via onError.
        // The transfer's onComplete already fired, so a complete-phase failure
        // is reported via onError to avoid a false "success" signal.
        assertTrue(events.contains("onError:IOException"),
                "A complete-phase failure should be reported via onError");
    }

    @Test
    void withValidator_accepts_uploadCompletes() throws IOException {
        AtomicReference<byte[]> received = new AtomicReference<>();
        UploadHandler handler = UploadHandler
                .inMemory((meta, bytes) -> received.set(bytes))
                .validateMetadata(event -> {
                    // accept everything
                });

        handler.handleUploadRequest(uploadEvent);

        assertNotNull(received.get(),
                "Accepted upload should reach the success callback");
        assertEquals(DUMMY_CONTENT_LENGTH, received.get().length);
    }

    @Test
    void validateComplete_rejectsInMemory_callbackNotInvoked()
            throws IOException {
        AtomicBoolean completed = new AtomicBoolean();
        long[] seenSize = { 0 };
        UploadHandler handler = UploadHandler
                .inMemory((meta, bytes) -> completed.set(true))
                .validateComplete((event, content) -> {
                    seenSize[0] = content.size();
                    assertTrue(content.asPath().isEmpty(),
                            "In-memory content has no path");
                    try (InputStream in = content.getInputStream()) {
                        assertEquals(DUMMY_CONTENT_LENGTH,
                                in.readAllBytes().length);
                    }
                    event.reject("infected");
                });

        handler.handleUploadRequest(uploadEvent);

        assertFalse(completed.get(),
                "Success callback must not run for a rejected upload");
        assertTrue(uploadEvent.isRejected());
        assertEquals(DUMMY_CONTENT_LENGTH, seenSize[0]);
    }

    @Test
    void validateComplete_rejectsFile_fileDeletedAndPathPresent()
            throws IOException {
        AtomicReference<File> createdFile = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean();
        AtomicBoolean pathPresent = new AtomicBoolean();
        UploadHandler handler = UploadHandler
                .toFile((meta, file) -> completed.set(true),
                        capturingFileFactory(createdFile))
                .validateComplete((event, content) -> {
                    pathPresent.set(content.asPath().isPresent());
                    event.reject("infected");
                });

        handler.handleUploadRequest(uploadEvent);

        assertFalse(completed.get(),
                "Success callback must not run for a rejected upload");
        assertTrue(pathPresent.get(),
                "File-backed content should expose a path");
        assertFalse(createdFile.get().exists(),
                "Rejected upload file should be deleted");
    }

    @Test
    void validateComplete_notInvoked_whenRejectedDuringHeader()
            throws IOException {
        AtomicBoolean completeRan = new AtomicBoolean();
        UploadHandler handler = UploadHandler.inMemory((meta, bytes) -> {
        }).validateHeader(4, (event, header) -> event.reject("bad"))
                .validateComplete((event, content) -> completeRan.set(true));

        handler.handleUploadRequest(uploadEvent);

        assertTrue(uploadEvent.isRejected());
        assertFalse(completeRan.get(),
                "Complete phase must not run when rejected during the header");
    }

    @Test
    void emptyUpload_headerInvokedWithEmpty_completeInvoked()
            throws IOException {
        when(part.getInputStream())
                .thenReturn(new ByteArrayInputStream(new byte[0]));
        AtomicBoolean headerRan = new AtomicBoolean();
        int[] headerRemaining = { -1 };
        AtomicBoolean completeRan = new AtomicBoolean();
        UploadHandler handler = UploadHandler.inMemory((meta, bytes) -> {
        }).validateHeader(8, (event, header) -> {
            headerRan.set(true);
            headerRemaining[0] = header.remaining();
        }).validateComplete((event, content) -> completeRan.set(true));

        handler.handleUploadRequest(uploadEvent);

        assertTrue(headerRan.get(),
                "Header validator must run even for a 0-byte upload");
        assertEquals(0, headerRemaining[0],
                "Header of a 0-byte upload should be empty");
        assertTrue(completeRan.get(),
                "Complete validator must run even for a 0-byte upload");
    }

    @Test
    void validateHeader_invokedExactlyOnce_onMultiChunkUpload()
            throws IOException {
        AtomicInteger headerCount = new AtomicInteger();
        long[] seenBytes = { 0 };
        UploadHandler handler = UploadHandler.inMemory((meta, bytes) -> {
        }).validateHeader(8, (event, header) -> {
            headerCount.incrementAndGet();
            seenBytes[0] = header.remaining();
        });

        handler.handleUploadRequest(uploadEvent);

        assertEquals(1, headerCount.get(),
                "Header validator should run exactly once");
        assertEquals(8, seenBytes[0],
                "Header validator should see the requested number of bytes");
    }

    @Test
    void validateHeader_budgetExceedsContent_seesFullContent()
            throws IOException {
        long[] seenBytes = { -1 };
        UploadHandler handler = UploadHandler.inMemory((meta, bytes) -> {
        }).validateHeader(DUMMY_CONTENT_LENGTH * 2,
                (event, header) -> seenBytes[0] = header.remaining());

        handler.handleUploadRequest(uploadEvent);

        assertEquals(DUMMY_CONTENT_LENGTH, seenBytes[0],
                "Header larger than the upload should see the full content");
    }

    @Test
    void validateHeader_seesExpectedLeadingBytes() throws IOException {
        byte[] content = deterministicBytes(100);
        when(part.getInputStream())
                .thenReturn(new ByteArrayInputStream(content));
        byte[][] seen = new byte[1][];
        UploadHandler handler = UploadHandler.inMemory((meta, bytes) -> {
        }).validateHeader(8, (event, header) -> seen[0] = toArray(header));

        handler.handleUploadRequest(uploadEvent);

        assertArrayEquals(Arrays.copyOf(content, 8), seen[0]);
    }

    @Test
    void validateHeader_multipleValidators_eachSeesOwnSize()
            throws IOException {
        byte[] content = deterministicBytes(100);
        when(part.getInputStream())
                .thenReturn(new ByteArrayInputStream(content));
        byte[][] four = new byte[1][];
        byte[][] eight = new byte[1][];
        UploadHandler handler = UploadHandler.inMemory((meta, bytes) -> {
        }).validateHeader(4, (event, header) -> four[0] = toArray(header))
                .validateHeader(8,
                        (event, header) -> eight[0] = toArray(header));

        handler.handleUploadRequest(uploadEvent);

        assertArrayEquals(Arrays.copyOf(content, 4), four[0]);
        assertArrayEquals(Arrays.copyOf(content, 8), eight[0]);
    }

    @Test
    void validateHeader_shortReads_accumulatesAcrossChunks()
            throws IOException {
        byte[] content = deterministicBytes(100);
        // Force reads of at most 3 bytes so the 8-byte header spans chunks.
        InputStream shortReader = new FilterInputStream(
                new ByteArrayInputStream(content)) {
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return super.read(b, off, Math.min(len, 3));
            }
        };
        when(part.getInputStream()).thenReturn(shortReader);
        byte[][] seen = new byte[1][];
        UploadHandler handler = UploadHandler.inMemory((meta, bytes) -> {
        }).validateHeader(8, (event, header) -> seen[0] = toArray(header));

        handler.handleUploadRequest(uploadEvent);

        assertArrayEquals(Arrays.copyOf(content, 8), seen[0]);
    }

    @Test
    void validateHeader_accepted_fullContentDelivered() throws IOException {
        byte[] content = deterministicBytes(100);
        when(part.getInputStream())
                .thenReturn(new ByteArrayInputStream(content));
        AtomicReference<byte[]> received = new AtomicReference<>();
        long[] seenHeader = { -1 };
        UploadHandler handler = UploadHandler
                .inMemory((meta, bytes) -> received.set(bytes))
                .validateHeader(8,
                        (event, header) -> seenHeader[0] = header.remaining());

        handler.handleUploadRequest(uploadEvent);

        assertEquals(8, seenHeader[0]);
        assertArrayEquals(content, received.get(),
                "Header + remainder must be delivered to the callback intact");
    }

    @Test
    void validateHeader_rejects_toFile_noFileCreated() throws IOException {
        AtomicReference<File> createdFile = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean();
        UploadHandler handler = UploadHandler
                .toFile((meta, file) -> completed.set(true),
                        capturingFileFactory(createdFile))
                .validateHeader(4, (event, header) -> event.reject("bad"));

        handler.handleUploadRequest(uploadEvent);

        assertFalse(completed.get(),
                "Success callback must not run for a rejected upload");
        assertNull(createdFile.get(),
                "No file should be created for a header-rejected upload");
    }

    @Test
    void handWrittenValidator_allPhasesInvokedInOrder() throws IOException {
        List<String> phases = new ArrayList<>();
        UploadHandler handler = UploadHandler.inMemory((meta, bytes) -> {
        }).withValidator(new UploadValidator() {
            @Override
            public void validateMetadata(UploadEvent event) {
                phases.add("metadata");
            }

            @Override
            public void validateHeader(UploadEvent event, ByteBuffer header) {
                phases.add("header:" + header.remaining());
            }

            @Override
            public void validateComplete(UploadEvent event,
                    UploadContent content) {
                phases.add("complete");
            }

            @Override
            public int headerSize() {
                return 8;
            }
        });

        handler.handleUploadRequest(uploadEvent);

        assertEquals(List.of("metadata", "header:8", "complete"), phases);
    }

    private static byte[] deterministicBytes(int size) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) i;
        }
        return bytes;
    }

    private static byte[] toArray(ByteBuffer buffer) {
        byte[] array = new byte[buffer.remaining()];
        buffer.get(array);
        return array;
    }

    private FileFactory capturingFileFactory(
            AtomicReference<File> createdFile) {
        return metadata -> {
            File file = assertDoesNotThrow(() -> Files
                    .createFile(temporaryFolder.resolve(DUMMY_FILE_NAME))
                    .toFile());
            createdFile.set(file);
            return file;
        };
    }

    private static TransferProgressListener recordingListener(
            List<String> events) {
        return new TransferProgressListener() {
            @Override
            public void onStart(TransferContext context) {
                events.add("onStart");
            }

            @Override
            public void onProgress(TransferContext context,
                    long transferredBytes, long totalBytes) {
                events.add("onProgress");
            }

            @Override
            public void onComplete(TransferContext context,
                    long transferredBytes) {
                events.add("onComplete");
            }

            @Override
            public void onError(TransferContext context, IOException reason) {
                events.add("onError:" + reason.getClass().getSimpleName());
            }
        };
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
                assertEquals(DUMMY_CONTENT_LENGTH, context.contentLength());
                assertEquals(DUMMY_FILE_NAME, context.fileName());
                invocations.add("onStart");
            }

            @Override
            public void onProgress(TransferContext context,
                    long transferredBytes, long totalBytes) {
                transferredBytesRecords.add(transferredBytes);
                assertEquals(DUMMY_CONTENT_LENGTH, totalBytes);
                assertEquals(DUMMY_FILE_NAME, context.fileName());
                invocations.add("onProgress");
            }

            @Override
            public void onComplete(TransferContext context,
                    long transferredBytes) {
                assertEquals(DUMMY_CONTENT_LENGTH, context.contentLength());
                assertEquals(DUMMY_CONTENT_LENGTH, transferredBytes);
                assertEquals(DUMMY_FILE_NAME, context.fileName());
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
                assertEquals("Test exception", reason.getMessage());
            }
        };
    }

    private static void assertListenersInvoked(List<String> invocations,
            List<Long> transferredBytesRecords) {
        // Two invocations with interval of 65536 bytes for total size 165000
        assertEquals(
                List.of("onStart", "onProgress", "onProgress", "onComplete"),
                invocations);
        assertArrayEquals(new long[] { 65536, 131072 }, transferredBytesRecords
                .stream().mapToLong(Long::longValue).toArray());
    }
}
