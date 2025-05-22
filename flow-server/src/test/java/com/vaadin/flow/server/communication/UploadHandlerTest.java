package com.vaadin.flow.server.communication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Part;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.streams.FileUploadHandler;
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import com.vaadin.flow.server.streams.TemporaryFileUploadHandler;
import com.vaadin.flow.server.streams.UploadEvent;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.server.communication.StreamRequestHandler.DYN_RES_PREFIX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NotThreadSafe
public class UploadHandlerTest {

    public static final String MULTIPART_STREAM_CONTENT = """
            -------bound
            Content-Disposition: form-data; name="file"; filename="sound.txt"
            Content-Type: text/plain

            Sound
            -------bound
            Content-Disposition: form-data; name="file"; filename="bytes.txt"
            Content-Type: text/plain

            Bytes
            -------bound--
            """.replaceAll("\n", "\r\n");
    public static final String MULTIPART_CONTENT_TYPE = "multipart/form-data; boundary=-----bound";
    private StreamRequestHandler handler = new StreamRequestHandler();
    private MockVaadinSession session;
    private VaadinServletRequest request;
    private VaadinResponse response;
    private StreamResourceRegistry streamResourceRegistry;
    private UI ui;
    private StateNode stateNode;
    private Element element;

    @Before
    public void setUp() throws ServletException, ServiceException {
        VaadinService service = new MockVaadinServletService();
        ui = new MockUI() {
            @Override
            public Future<Void> access(Command command) {
                command.execute();
                return null;
            }
        };

        session = new AlwaysLockedVaadinSession(service) {
            @Override
            public StreamResourceRegistry getResourceRegistry() {
                return streamResourceRegistry;
            }

            @Override
            public UI getUIById(int uiId) {
                return ui;
            }
        };
        streamResourceRegistry = new StreamResourceRegistry(session);
        request = Mockito.mock(VaadinServletRequest.class);
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getMimeType(Mockito.anyString()))
                .thenReturn(null);
        Mockito.when(request.getServletContext()).thenReturn(servletContext);
        stateNode = Mockito.mock(StateNode.class);
        when(stateNode.isAttached()).thenReturn(true);
        element = Mockito.mock(Element.class);
        Mockito.when(element.getNode()).thenReturn(stateNode);
        response = Mockito.mock(VaadinResponse.class);
    }

    @After
    public void cleanup() {
        CurrentInstance.clearAll();
    }

    @Test
    public void doUploadHandleXhrFilePost_happyPath_setContentTypeAndResponseHandled()
            throws IOException {
        UploadHandler handler = (event) -> {
            event.getResponse().setContentType("text/html; charset=utf-8");
        };

        handler.handleRequest(request, response, session, element);

        Mockito.verify(response).setContentType(
                ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8);
        Mockito.verify(response, Mockito.times(1)).setStatus(200);
    }

    @Test
    public void doUploadHandleXhrFilePost_unhappyPath_responseHandled()
            throws IOException {
        UploadHandler handler = (event) -> {
            throw new RuntimeException("Exception in xrh upload");
        };

        handler.handleRequest(request, response, session, element);

        Mockito.verify(response, Mockito.times(1)).setStatus(500);
    }

    @Test
    public void createUploadHandlerToCopyStream_streamMatchesInput()
            throws IOException {
        String testString = "Test string for upload";

        final byte[] testBytes = testString.getBytes();

        byte[] output = new byte[testBytes.length];
        AtomicInteger amount = new AtomicInteger();

        UploadHandler uploadHandler = (event) -> {
            try (InputStream inputStream = event.getInputStream()) {
                amount.set(inputStream.read(output));
            } catch (IOException ioe) {
                // Set status before output is closed (see #8740)
                response.setStatus(
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
                throw new RuntimeException(ioe);
            }
        };

        StreamRegistration streamRegistration = streamResourceRegistry
                .registerResource(uploadHandler);
        AbstractStreamResource res = streamRegistration.getResource();

        mockRequest(res, testString);

        handler.handleRequest(session, request, response);

        Assert.assertArrayEquals("Output differed from expected", testBytes,
                output);

        Assert.assertEquals("", testBytes.length, amount.get());
    }

    @Test
    public void createInMemoryUploadHandler_streamMatchesInput()
            throws IOException {
        String testString = "Test string for upload";

        final byte[] testBytes = testString.getBytes();

        final byte[] output = new byte[testBytes.length];

        // No lambda as that would require strange things from output
        InMemoryUploadHandler uploadHandler = UploadHandler
                .inMemory(new SerializableBiConsumer<UploadMetadata, byte[]>() {
                    @Override
                    public void accept(UploadMetadata uploadMetadata,
                            byte[] bytes) {
                        Assert.assertEquals(output.length, bytes.length);
                        System.arraycopy(bytes, 0, output, 0, bytes.length);
                    }
                });

        StreamRegistration streamRegistration = streamResourceRegistry
                .registerResource(uploadHandler);
        AbstractStreamResource res = streamRegistration.getResource();

        mockRequest(res, testString);

        handler.handleRequest(session, request, response);
        session.getPendingAccessQueue()
                .forEach(futureAccess -> futureAccess.run());

        Assert.assertArrayEquals("Output differed from expected", testBytes,
                output);
    }

    @Test
    public void createTempFileUploadHandler_streamMatchesInput()
            throws IOException {
        String testString = "Test string for upload";

        final byte[] testBytes = testString.getBytes();

        List<File> outputFiles = new ArrayList<>(1);

        TemporaryFileUploadHandler uploadHandler = UploadHandler
                .toTempFile((uploadMetadata, file) -> outputFiles.add(file));

        StreamRegistration streamRegistration = streamResourceRegistry
                .registerResource(uploadHandler);
        AbstractStreamResource res = streamRegistration.getResource();

        mockRequest(res, testString);

        try {
            handler.handleRequest(session, request, response);

            Assert.assertEquals("Only one uploaded file expected.", 1,
                    outputFiles.size());
            System.out.println(outputFiles.get(0).getPath());

            Assert.assertArrayEquals("Output differed from expected", testBytes,
                    Files.readAllBytes(outputFiles.get(0).toPath()));
        } finally {
            // Cleanup temp file after test
            for (File file : outputFiles) {
                file.delete();
            }
        }
    }

    @Test
    public void createFileUploadHandler_streamMatchesInput()
            throws IOException {
        String testString = "Test string for upload";

        final byte[] testBytes = testString.getBytes();

        List<File> outputFiles = new ArrayList<>(1);

        FileUploadHandler uploadHandler = UploadHandler.toFile(
                (uploadMetadata, file) -> outputFiles.add(file),
                (fileName) -> new File(System.getProperty("java.io.tmpdir"),
                        fileName));

        StreamRegistration streamRegistration = streamResourceRegistry
                .registerResource(uploadHandler);
        AbstractStreamResource res = streamRegistration.getResource();

        mockRequest(res, testString);

        try {
            handler.handleRequest(session, request, response);

            Assert.assertEquals("Only one uploaded file expected.", 1,
                    outputFiles.size());
            System.out.println(outputFiles.get(0).getPath());

            Assert.assertArrayEquals("Output differed from expected", testBytes,
                    Files.readAllBytes(outputFiles.get(0).toPath()));
        } finally {
            // Cleanup temp file after test
            for (File file : outputFiles) {
                file.delete();
            }
        }
    }

    @Test
    public void mulitpartData_forInputIterator_dataIsGottenCorrectly()
            throws IOException {
        List<String> outList = new ArrayList<>(2);
        List<String> fileNames = new ArrayList<>(2);

        UploadHandler uploadHandler = (event) -> {
            fileNames.add(event.getFileName());
            try (InputStream inputStream = event.getInputStream()) {
                outList.add(
                        IOUtils.toString(inputStream, StandardCharsets.UTF_8));
            } catch (IOException ioe) {
                // Set status before output is closed (see #8740)
                response.setStatus(
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
                throw new RuntimeException(ioe);
            }
        };

        StreamRegistration streamRegistration = streamResourceRegistry
                .registerResource(uploadHandler);
        AbstractStreamResource res = streamRegistration.getResource();

        mockRequest(res, MULTIPART_STREAM_CONTENT);
        Mockito.when(request.getContentType())
                .thenReturn(MULTIPART_CONTENT_TYPE);

        handler.handleRequest(session, request, response);

        Assert.assertEquals(2, outList.size());
        Assert.assertEquals(2, fileNames.size());

        Assert.assertEquals("Sound", outList.get(0));
        Assert.assertEquals("sound.txt", fileNames.get(0));

        Assert.assertEquals("Bytes", outList.get(1));
        Assert.assertEquals("bytes.txt", fileNames.get(1));
    }

    @Test
    public void mulitpartData_asParts_dataIsGottenCorrectly()
            throws IOException, ServletException {
        String testContent = "testBytes";

        List<Part> parts = new ArrayList<>();
        parts.add(createPart(createInputStream("one"), MULTIPART_CONTENT_TYPE,
                "one.txt", 3));
        parts.add(createPart(createInputStream("two"), MULTIPART_CONTENT_TYPE,
                "two.txt", 3));

        Mockito.when(request.getParts()).thenReturn(parts);

        List<String> outList = new ArrayList<>(2);
        List<String> fileNames = new ArrayList<>(2);

        UploadHandler uploadHandler = (event) -> {
            fileNames.add(event.getFileName());
            try (InputStream inputStream = event.getInputStream()) {
                outList.add(
                        IOUtils.toString(inputStream, StandardCharsets.UTF_8));
            } catch (IOException ioe) {
                // Set status before output is closed (see #8740)
                response.setStatus(
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
                throw new RuntimeException(ioe);
            }
        };

        StreamRegistration streamRegistration = streamResourceRegistry
                .registerResource(uploadHandler);
        AbstractStreamResource res = streamRegistration.getResource();

        mockRequest(res, testContent);
        Mockito.when(request.getContentType())
                .thenReturn(MULTIPART_CONTENT_TYPE);

        handler.handleRequest(session, request, response);

        Assert.assertEquals(2, outList.size());
        Assert.assertEquals(2, fileNames.size());

        Assert.assertEquals("one", outList.get(0));
        Assert.assertEquals("one.txt", fileNames.get(0));

        Assert.assertEquals("two", outList.get(1));
        Assert.assertEquals("two.txt", fileNames.get(1));
    }

    @Test
    public void responseHandled_calledAfterAllPartsHaveBeenHandled()
            throws IOException, ServletException {

        String testContent = "testBytes";

        List<Part> parts = new ArrayList<>();
        parts.add(createPart(createInputStream("one"), MULTIPART_CONTENT_TYPE,
                "one.txt", 3));
        parts.add(createPart(createInputStream("two"), MULTIPART_CONTENT_TYPE,
                "two.txt", 3));

        Mockito.when(request.getParts()).thenReturn(parts);

        AtomicBoolean handled = new AtomicBoolean(false);

        UploadHandler uploadHandler = new UploadHandler() {
            @Override
            public void handleUploadRequest(UploadEvent event) {
                Assert.assertFalse(
                        "Handled should not be called before a upload request",
                        handled.get());
            }

            @Override
            public void responseHandled(boolean success,
                    VaadinResponse response) {
                handled.set(true);
            }
        };

        StreamRegistration streamRegistration = streamResourceRegistry
                .registerResource(uploadHandler);
        AbstractStreamResource res = streamRegistration.getResource();

        mockRequest(res, testContent);
        Mockito.when(request.getContentType())
                .thenReturn(MULTIPART_CONTENT_TYPE);

        handler.handleRequest(session, request, response);

        Assert.assertTrue("Handled was not called at the end", handled.get());
    }

    @Test
    public void responseHandled_calledAfterWholeStreamHasBeenHandled()
            throws IOException, ServletException {

        AtomicBoolean handled = new AtomicBoolean(false);

        UploadHandler uploadHandler = new UploadHandler() {
            @Override
            public void handleUploadRequest(UploadEvent event) {
                Assert.assertFalse(
                        "Handled should not be called before a upload request",
                        handled.get());
            }

            @Override
            public void responseHandled(boolean success,
                    VaadinResponse response) {
                handled.set(true);
            }
        };

        StreamRegistration streamRegistration = streamResourceRegistry
                .registerResource(uploadHandler);
        AbstractStreamResource res = streamRegistration.getResource();

        mockRequest(res, MULTIPART_STREAM_CONTENT);
        Mockito.when(request.getContentType())
                .thenReturn(MULTIPART_CONTENT_TYPE);

        handler.handleRequest(session, request, response);

        Assert.assertTrue("Handled was not called at the end", handled.get());
    }

    @Test
    public void multipartRequest_responseHandled_calledWhenExceptionIsThrown()
            throws IOException, ServletException {

        String testContent = "testBytes";

        List<Part> parts = new ArrayList<>();
        parts.add(createPart(createInputStream("one"), MULTIPART_CONTENT_TYPE,
                "one.txt", 3));
        parts.add(createPart(createInputStream("two"), MULTIPART_CONTENT_TYPE,
                "two.txt", 3));

        Mockito.when(request.getParts()).thenReturn(parts);

        AtomicBoolean handled = new AtomicBoolean(false);

        UploadHandler uploadHandler = new UploadHandler() {
            @Override
            public void handleUploadRequest(UploadEvent event) {
                throw new RuntimeException("Exception in multipart upload");
            }

            @Override
            public void responseHandled(boolean success,
                    VaadinResponse response) {
                handled.set(true);
            }
        };

        StreamRegistration streamRegistration = streamResourceRegistry
                .registerResource(uploadHandler);
        AbstractStreamResource res = streamRegistration.getResource();

        mockRequest(res, testContent);
        Mockito.when(request.getContentType())
                .thenReturn(MULTIPART_CONTENT_TYPE);

        handler.handleRequest(session, request, response);

        Assert.assertTrue("Handled was not called at the end", handled.get());
    }

    @Test
    public void multipartStreamRequest_responseHandled_calledWhenExceptionIsThrown()
            throws IOException, ServletException {

        AtomicBoolean handled = new AtomicBoolean(false);

        UploadHandler uploadHandler = new UploadHandler() {
            @Override
            public void handleUploadRequest(UploadEvent event) {
                throw new RuntimeException(
                        "Exception in multipart stream upload");
            }

            @Override
            public void responseHandled(boolean success,
                    VaadinResponse response) {
                handled.set(true);
            }
        };

        StreamRegistration streamRegistration = streamResourceRegistry
                .registerResource(uploadHandler);
        AbstractStreamResource res = streamRegistration.getResource();

        mockRequest(res, MULTIPART_STREAM_CONTENT);
        Mockito.when(request.getContentType())
                .thenReturn(MULTIPART_CONTENT_TYPE);

        handler.handleRequest(session, request, response);

        Assert.assertTrue("Handled was not called at the end", handled.get());
    }

    @Test
    public void doesNotRequireToCatchIOException() {
        UploadHandler handler = event -> {
            new FileInputStream(new File("foo"));
        };
    }

    private Part createPart(InputStream inputStream, String contentType,
            String name, long size) throws IOException {
        Part part = mock(Part.class);
        when(part.getInputStream()).thenReturn(inputStream);
        when(part.getContentType()).thenReturn(contentType);
        when(part.getSubmittedFileName()).thenReturn(name);
        when(part.getSize()).thenReturn(size);
        return part;
    }

    private void mockRequest(AbstractStreamResource res, String content)
            throws IOException {

        ServletOutputStream outputStream = Mockito
                .mock(ServletOutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);
        Mockito.when(request.getPathInfo())
                .thenReturn(String.format("/%s%s/%s/%s", DYN_RES_PREFIX,
                        ui.getId().orElse("-1"), res.getId(), res.getName()));
        Mockito.when(request.getContentLengthLong())
                .thenReturn(Long.valueOf(content.length()));
        Mockito.when(request.getContentLength()).thenReturn(content.length());
        Mockito.when(request.getHeader("Content-length"))
                .thenReturn(String.valueOf(content.length()));
        Mockito.when(request.getInputStream())
                .thenReturn(createInputStream(content));
        Mockito.when(request.getMethod()).thenReturn("POST");
    }

    private ServletInputStream createInputStream(final String content) {
        StringReader stringReader = new StringReader(content);
        return new ServletInputStream() {
            boolean finished = false;

            @Override
            public boolean isFinished() {
                return finished;
            }

            @Override
            public boolean isReady() {
                try {
                    return stringReader.ready();
                } catch (IOException e) {
                    return true;
                }
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }

            @Override
            public void reset() throws IOException {
                stringReader.reset();
                finished = false;
            }

            @Override
            public int read() throws IOException {
                int read = stringReader.read();
                if (read == -1) {
                    finished = true;
                }
                return read;
            }
        };
    }

}
