package com.vaadin.server.communication;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.StateTree;
import com.vaadin.server.StreamReceiver;
import com.vaadin.server.StreamReceiverRegistry;
import com.vaadin.server.StreamVariable;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;
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
    @Mock
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
    private StreamReceiverRegistry registry;

    private final int uiId = 123;
    private final int nodeId = 1233;
    private final String variableName = "name";
    private final String expectedSecurityKey = "key";

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        handler = new StreamReceiverRequestHandler();

        mockRequest();
        mockReceiverAndRegistry();
        mockUi();

        when(stateNode.isAttached()).thenReturn(true);
        when(streamVariable.getOutputStream())
                .thenReturn(mock(OutputStream.class));
        when(response.getOutputStream()).thenReturn(responseOutput);
    }

    private void mockReceiverAndRegistry() {
        when(session.getReceiverRegistry()).thenReturn(registry);
        when(registry.getReceiver(Mockito.any()))
                .thenReturn(Optional.of(streamReceiver));
        when(streamReceiver.getId()).thenReturn(expectedSecurityKey);
        when(streamReceiver.getStreamVariable()).thenReturn(streamVariable);
    }

    private void mockRequest() throws IOException {
        when(request.getPathInfo()).thenReturn("/"
                + StreamReceiverRequestHandler.DYN_RES_PREFIX + uiId + "/"
                + nodeId + "/" + variableName + "/" + expectedSecurityKey);

        when(request.getInputStream()).thenReturn(createInputStream("foobar"));
        when(request.getHeader("Content-Length")).thenReturn("6");
        when(request.getContentType()).thenReturn("foobar");
    }

    private InputStream createInputStream(final String content) {
        return new InputStream() {
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
        when(request.getInputStream()).thenReturn(createInputStream(""));
        when(request.getHeader("Content-Length")).thenReturn("1");

        handler.doHandleMultipartFileUpload(null, request, null, null, null,
                null);
    }

    @Test
    public void responseIsSentOnCorrectSecurityKey() throws IOException {
        handler.handleRequest(session, request, response);

        verify(responseOutput).close();
    }

    @Test
    public void responseIsNotSentOnIncorrectSecurityKey() throws IOException {
        when(streamReceiver.getId()).thenReturn("another key expected");

        handler.handleRequest(session, request, response);

        verifyZeroInteractions(responseOutput);
    }

    @Test
    public void responseIsNotSentOnMissingSecurityKey() throws IOException {
        when(streamReceiver.getId()).thenReturn(null);

        handler.handleRequest(session, request, response);

        verifyZeroInteractions(responseOutput);
    }
}
