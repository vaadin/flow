package com.vaadin.flow.server.communication;

import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.MockServletConfig;
import com.vaadin.flow.server.StreamReceiver;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.StreamVariable;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ApplicationConstants;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class StreamReceiverHandlerTest {

    private StreamReceiverHandler handler;
    @Mock
    private VaadinResponse response;
    @Mock
    private StreamVariable streamVariable;
    @Mock
    private StateNode stateNode;
    // @Mock
    private VaadinRequest request;
    @Mock
    private UI ui;
    @Mock
    private UIInternals uiInternals;
    @Mock
    private StateTree stateTree;
    @Mock
    private VaadinSession session;
    @Mock
    private OutputStream responseOutput;
    @Mock
    private StreamReceiver streamReceiver;
    @Mock
    private StreamResourceRegistry registry;

    private VaadinServletService mockService;

    private final int uiId = 123;
    private final int nodeId = 1233;
    private final String variableName = "name";
    private final String expectedSecurityKey = "key";

    private String contentLength;
    private ServletInputStream inputStream;
    private OutputStream outputStream;
    private String contentType;
    private List<Part> parts;

    @Before
    public void setup() throws Exception {
        contentLength = "6";
        inputStream = createInputStream("foobar");
        outputStream = mock(OutputStream.class);
        contentType = "foobar";
        parts = Collections.emptyList();
        MockitoAnnotations.initMocks(this);

        handler = new StreamReceiverHandler();

        VaadinServlet mockServlet = new VaadinServlet();
        mockServlet.init(new MockServletConfig());
        mockService = mockServlet.getService();

        mockRequest();
        mockReceiverAndRegistry();
        mockUi();

        when(streamReceiver.getNode()).thenReturn(stateNode);
        when(stateNode.isAttached()).thenReturn(true);
        when(streamVariable.getOutputStream())
                .thenAnswer(invocationOnMock -> outputStream);
        when(response.getOutputStream()).thenReturn(responseOutput);

        Mockito.when(session.getErrorHandler())
                .thenReturn(Mockito.mock(ErrorHandler.class));
    }

    private void mockReceiverAndRegistry() {
        when(session.getResourceRegistry()).thenReturn(registry);
        when(registry.getResource(Mockito.any()))
                .thenReturn(Optional.of(streamReceiver));
        when(streamReceiver.getId()).thenReturn(expectedSecurityKey);
        when(streamReceiver.getStreamVariable()).thenReturn(streamVariable);
    }

    private void mockRequest() throws IOException {
        HttpServletRequest servletRequest = Mockito
                .mock(HttpServletRequest.class);
        when(servletRequest.getContentLength()).thenAnswer(
                invocationOnMock -> Integer.parseInt(contentLength));

        request = new VaadinServletRequest(servletRequest, mockService) {
            @Override
            public String getParameter(String name) {
                if ("restartApplication".equals(name)
                        || "ignoreRestart".equals(name)
                        || "closeApplication".equals(name)) {
                    return null;
                }
                return "1";
            }

            @Override
            public String getPathInfo() {
                return "/" + StreamRequestHandler.DYN_RES_PREFIX + uiId + "/"
                        + nodeId + "/" + variableName + "/"
                        + expectedSecurityKey;
            }

            @Override
            public String getMethod() {
                return "POST";
            }

            @Override
            public ServletInputStream getInputStream() throws IOException {
                return inputStream;
            }

            @Override
            public String getHeader(String name) {
                if ("content-length".equals(name.toLowerCase())) {
                    return contentLength;
                }
                return super.getHeader(name);
            }

            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public Collection<Part> getParts()
                    throws IOException, ServletException {
                return parts;
            }
        };
    }

    private ServletInputStream createInputStream(final String content) {
        return new ServletInputStream() {
            boolean finished = false;

            @Override
            public boolean isFinished() {
                return finished;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }

            int counter = 0;
            byte[] msg = content.getBytes();

            @Override
            public int read() throws IOException {
                if (counter > msg.length + 1) {
                    throw new AssertionError(
                            "-1 was ignored by StreamReceiverHandler.");
                }

                if (counter >= msg.length) {
                    counter++;
                    finished = true;
                    return -1;
                }

                return msg[counter++];
            }
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

    private void mockUi() {
        when(ui.getInternals()).thenReturn(uiInternals);
        when(uiInternals.getStateTree()).thenReturn(stateTree);
        when(stateTree.getNodeById(Mockito.anyInt())).thenReturn(stateNode);
        when(session.getUIById(uiId)).thenReturn(ui);
    }

    @Test
    public void doHandleXhrFilePost_outputStreamGetterThrows_responseStatusIs500()
            throws IOException {
        Mockito.doThrow(RuntimeException.class).when(streamVariable)
                .getOutputStream();

        handler.doHandleXhrFilePost(session, request, response, streamReceiver,
                stateNode, 1);

        Mockito.verify(response)
                .setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void doHandleXhrFilePost_outputStreamThrowsOnWrite_responseStatusIs500()
            throws IOException {

        Mockito.doThrow(RuntimeException.class).when(outputStream)
                .write(Mockito.any(), Mockito.anyInt(), Mockito.anyInt());

        handler.doHandleXhrFilePost(session, request, response, streamReceiver,
                stateNode, 1);

        Mockito.verify(response)
                .setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void doHandleXhrFilePost_happyPath_setContentTypeNoExplicitSetStatus()
            throws IOException {
        handler.doHandleXhrFilePost(session, request, response, streamReceiver,
                stateNode, 1);

        Mockito.verify(response).setContentType(
                ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8);
        Mockito.verify(response, Mockito.times(0)).setStatus(Mockito.anyInt());
    }

    @Test
    public void doHandleMultipartFileUpload_noPart_uploadFailed_responseStatusIs500()
            throws IOException {

        handler.doHandleMultipartFileUpload(session, request, response,
                streamReceiver, stateNode);

        Mockito.verify(response)
                .setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void doHandleMultipartFileUpload_hasParts_uploadFailed_responseStatusIs500()
            throws IOException {
        contentType = "multipart/form-data; boundary=----WebKitFormBoundary7NsWHeCJVZNwi6ll";
        inputStream = createInputStream(
                "------WebKitFormBoundary7NsWHeCJVZNwi6ll\n"
                        + "Content-Disposition: form-data; name=\"file\"; filename=\"EBookJP.txt\"\n"
                        + "Content-Type: text/plain\n" + "\n" + "\n"
                        + "------WebKitFormBoundary7NsWHeCJVZNwi6ll--");
        Mockito.doThrow(RuntimeException.class).when(outputStream)
                .write(Mockito.any(), Mockito.anyInt(), Mockito.anyInt());
        contentLength = "99";

        parts = new ArrayList<>();
        parts.add(createPart(createInputStream("foobar"), "text/plain",
                "EBookJP.txt", 6));

        handler.doHandleMultipartFileUpload(session, request, response,
                streamReceiver, stateNode);

        Mockito.verify(response)
                .setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Tests whether we get infinite loop if InputStream is already read
     * (#10096)
     */
    @Test
    public void exceptionIsThrownOnUnexpectedEnd() throws IOException {
        contentType = "multipart/form-data; boundary=----WebKitFormBoundary7NsWHeCJVZNwi6ll";
        inputStream = createInputStream(
                "------WebKitFormBoundary7NsWHeCJVZNwi6ll\n"
                        + "Content-Disposition: form-data; name=\"file\"; filename=\"EBookJP.txt\"\n"
                        + "Content-Type: text/plain\n" + "\n" + "\n"
                        + "------WebKitFormBoundary7NsWHeCJVZNwi6ll--");
        contentLength = "99";

        handler.doHandleMultipartFileUpload(session, request, response, null,
                null);

        Mockito.verify(response)
                .setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void responseIsSentOnCorrectSecurityKey() throws IOException {
        handler.handleRequest(session, request, response, streamReceiver,
                String.valueOf(uiId), expectedSecurityKey);

        verify(responseOutput).close();
    }

    @Test
    public void responseIsNotSentOnIncorrectSecurityKey() throws IOException {
        when(streamReceiver.getId()).thenReturn("another key expected");

        handler.handleRequest(session, request, response, streamReceiver,
                String.valueOf(uiId), expectedSecurityKey);

        verifyZeroInteractions(responseOutput);
    }

    @Test
    public void responseIsNotSentOnMissingSecurityKey() throws IOException {
        when(streamReceiver.getId()).thenReturn(null);

        handler.handleRequest(session, request, response, streamReceiver,
                String.valueOf(uiId), expectedSecurityKey);

        verifyZeroInteractions(responseOutput);
    }

    @Test // Vaadin Spring #381
    public void partsAreUsedDirectlyIfPresentWithoutParsingInput()
            throws IOException {
        contentType = "multipart/form-data; boundary=----WebKitFormBoundary7NsWHeCJVZNwi6ll";
        inputStream = createInputStream(
                "------WebKitFormBoundary7NsWHeCJVZNwi6ll\n"
                        + "Content-Disposition: form-data; name=\"file\"; filename=\"EBookJP.txt\"\n"
                        + "Content-Type: text/plain\n" + "\n" + "\n"
                        + "------WebKitFormBoundary7NsWHeCJVZNwi6ll--");
        outputStream = new ByteArrayOutputStream();
        contentLength = "99";
        final String fileName = "EBookJP.txt";
        final String contentType = "text/plain";

        parts = new ArrayList<>();
        parts.add(createPart(createInputStream("foobar"), contentType, fileName,
                6));

        handler.handleRequest(session, request, response, streamReceiver,
                String.valueOf(uiId), expectedSecurityKey);

        verify(responseOutput).close();
        ArgumentCaptor<StreamVariable.StreamingEndEvent> endEventArgumentCaptor = ArgumentCaptor
                .forClass(StreamVariable.StreamingEndEvent.class);
        verify(streamVariable)
                .streamingFinished(endEventArgumentCaptor.capture());
        Assert.assertEquals("foobar", new String(
                ((ByteArrayOutputStream) outputStream).toByteArray()));
        Assert.assertEquals(fileName,
                endEventArgumentCaptor.getValue().getFileName());
        Assert.assertEquals(contentType,
                endEventArgumentCaptor.getValue().getMimeType());

        Mockito.verify(response).setContentType(
                ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8);
        Mockito.verify(response, Mockito.times(0)).setStatus(Mockito.anyInt());
    }
}
