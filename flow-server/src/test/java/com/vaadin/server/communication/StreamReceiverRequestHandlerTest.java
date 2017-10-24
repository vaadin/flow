package com.vaadin.server.communication;

import javax.servlet.ReadListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.StateTree;
import com.vaadin.server.MockServletConfig;
import com.vaadin.server.StreamReceiver;
import com.vaadin.server.StreamReceiverRegistry;
import com.vaadin.server.StreamResourceRegistry;
import com.vaadin.server.StreamVariable;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedHttpSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.UI;
import com.vaadin.ui.UIInternals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class StreamReceiverRequestHandlerTest {

    private StreamReceiverRequestHandler handler;
    @Mock
    private VaadinResponse response;
    @Mock
    private StreamVariable streamVariable;
    @Mock
    private StateNode stateNode;
//    @Mock
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
    private String contentType;

    @Before
    public void setup() throws Exception {
        contentLength = "6";
        inputStream = createInputStream("foobar");
        contentType = "foobar";
        MockitoAnnotations.initMocks(this);

        handler = new StreamReceiverRequestHandler();


        VaadinServlet mockServlet = new VaadinServlet();
        mockServlet.init(new MockServletConfig());
        mockService = mockServlet.getService();

        mockRequest();
        mockReceiverAndRegistry();
        mockUi();

        when(streamReceiver.getNode()).thenReturn(stateNode);
        when(stateNode.isAttached()).thenReturn(true);
        when(streamVariable.getOutputStream())
                .thenReturn(mock(OutputStream.class));
        when(response.getOutputStream()).thenReturn(responseOutput);
    }

    private void mockReceiverAndRegistry() {
        when(session.getResourceRegistry()).thenReturn(registry);
        when(registry.getReceiver(Mockito.any()))
                .thenReturn(Optional.of(streamReceiver));
        when(streamReceiver.getId()).thenReturn(expectedSecurityKey);
        when(streamReceiver.getStreamVariable()).thenReturn(streamVariable);
    }


    private void mockRequest() throws IOException {
        HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        when(servletRequest.getContentLength()).thenReturn(Integer.parseInt(contentLength));

        request = new VaadinServletRequest(
                servletRequest, mockService) {
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
                return "/"
                        + StreamReceiverRequestHandler.DYN_RES_PREFIX + uiId + "/"
                        + nodeId + "/" + variableName + "/" + expectedSecurityKey;
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
                if("content-length".equals(name.toLowerCase())){
                    return contentLength;
                }
                return super.getHeader(name);
            }

            @Override
            public String getContentType() {
                return contentType;
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
                            "-1 was ignored by StreamReceiverRequestHandler.");
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

    private void mockUi() {
        when(ui.getInternals()).thenReturn(uiInternals);
        when(uiInternals.getStateTree()).thenReturn(stateTree);
        when(stateTree.getNodeById(Mockito.anyInt())).thenReturn(stateNode);
        when(session.getUIById(uiId)).thenReturn(ui);
    }

    /**
     * Tests whether we get infinite loop if InputStream is already read
     * (#10096)
     */
    @Test(expected = IOException.class)
    public void exceptionIsThrownOnUnexpectedEnd() throws IOException {
        contentType = "multipart/form-data; boundary=----WebKitFormBoundary7NsWHeCJVZNwi6ll";
        inputStream = createInputStream("------WebKitFormBoundary7NsWHeCJVZNwi6ll\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"EBookJP.txt\"\n"
                + "Content-Type: text/plain\n" + "\n" + "\n"
                + "------WebKitFormBoundary7NsWHeCJVZNwi6ll--");
        contentLength = "99";

        handler.doHandleMultipartFileUpload(null, request, response, null, null);
    }

    @Test
    public void responseIsSentOnCorrectSecurityKey() throws IOException {
        handler.handleRequest(session, request, response, streamReceiver, String.valueOf(uiId), expectedSecurityKey);

        verify(responseOutput).close();
    }

    @Test
    public void responseIsNotSentOnIncorrectSecurityKey() throws IOException {
        when(streamReceiver.getId()).thenReturn("another key expected");

        handler.handleRequest(session, request, response, streamReceiver, String.valueOf(uiId), expectedSecurityKey);

        verifyZeroInteractions(responseOutput);
    }

    @Test
    public void responseIsNotSentOnMissingSecurityKey() throws IOException {
        when(streamReceiver.getId()).thenReturn(null);

        handler.handleRequest(session, request, response, streamReceiver, String.valueOf(uiId), expectedSecurityKey);

        verifyZeroInteractions(responseOutput);
    }
}
