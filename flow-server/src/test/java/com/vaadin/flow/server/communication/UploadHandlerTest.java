package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.server.communication.StreamRequestHandler.DYN_RES_PREFIX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NotThreadSafe
public class UploadHandlerTest {

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
        stateNode = Mockito.mock(StateNode.class);
        when(stateNode.isAttached()).thenReturn(true);
        element = Mockito.mock(Element.class);
        Mockito.when(element.getNode()).thenReturn(stateNode);
        response = Mockito.mock(VaadinResponse.class);
        ui = new MockUI();
        UI.setCurrent(ui);
    }

    @After
    public void cleanup() {
        CurrentInstance.clearAll();
    }

    @Test
    public void doUploadHandleXhrFilePost_happyPath_setContentTypeNoExplicitSetStatus() {
        UploadHandler handler = (event) -> {
            event.getResponse().setContentType("text/html; charset=utf-8");
        };

        handler.handleRequest(request, response, session, element);

        Mockito.verify(response).setContentType(
                ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8);
        Mockito.verify(response, Mockito.times(0)).setStatus(Mockito.anyInt());
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
    public void mulitpartData_forInputIterator_dataIsGottenCorrectly()
            throws IOException {
        String contentType = "multipart/form-data; boundary=-----bound";
        String content = """
                -------bound
                Content-Disposition: form-data; name="file"; filename="sound.txt"
                Content-Type: text/plain

                Sound
                -------bound
                Content-Disposition: form-data; name="file"; filename="bytes.txt"
                Content-Type: text/plain

                Bytes
                -------bound--
                """
                .replaceAll("\n", "\r\n");

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

        mockRequest(res, content);
        Mockito.when(request.getContentType()).thenReturn(contentType);

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
        String contentType = "multipart/form-data; boundary=-----bound";

        String testContent = "testBytes";

        List<Part> parts = new ArrayList<>();
        parts.add(createPart(createInputStream("one"), contentType, "one.txt",
                3));
        parts.add(createPart(createInputStream("two"), contentType, "two.txt",
                3));

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
        Mockito.when(request.getContentType()).thenReturn(contentType);

        handler.handleRequest(session, request, response);

        Assert.assertEquals(2, outList.size());
        Assert.assertEquals(2, fileNames.size());

        Assert.assertEquals("one", outList.get(0));
        Assert.assertEquals("one.txt", fileNames.get(0));

        Assert.assertEquals("two", outList.get(1));
        Assert.assertEquals("two.txt", fileNames.get(1));
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
