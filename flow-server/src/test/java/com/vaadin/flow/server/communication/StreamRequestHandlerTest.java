package com.vaadin.flow.server.communication;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.streams.ElementRequestHandler;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.server.communication.StreamRequestHandler.DYN_RES_PREFIX;

@NotThreadSafe
public class StreamRequestHandlerTest {

    private StreamRequestHandler handler = new StreamRequestHandler();
    private MockVaadinSession session;
    private VaadinServletRequest request;
    private VaadinResponse response;
    private StreamResourceRegistry streamResourceRegistry;
    private UI ui;

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
        response = Mockito.mock(VaadinResponse.class);
        ui = new MockUI();
        UI.setCurrent(ui);
    }

    @After
    public void cleanup() {
        CurrentInstance.clearAll();
    }

    @Test
    public void streamResourceNameEndsWithPluses_streamFactory_resourceIsStreamed()
            throws IOException {
        testStreamResourceInputStreamFactory("end with multiple pluses",
                "readme++.md");
    }

    @Test
    public void streamResourceNameEndsWithPluses_resourceWriter_resourceIsStreamed()
            throws IOException {
        testStreamResourceStreamResourceWriter("end with multiple pluses",
                "readme++.md");
    }

    @Test
    public void streamResourceNameContainsSpaceEndsWithPluses_streamFactory_resourceIsStreamed()
            throws IOException {
        testStreamResourceInputStreamFactory(
                "end with space and multiple pluses", "readme ++.md");
    }

    @Test
    public void streamResourceNameContainsSpaceEndsWithPluses_resourceWriter_resourceIsStreamed()
            throws IOException {
        testStreamResourceStreamResourceWriter(
                "end with space and multiple pluses", "readme ++.md");
    }

    @Test
    public void streamResourceNameEndsInPlus_streamFactory_resourceIsStreamed()
            throws IOException {
        testStreamResourceInputStreamFactory("end in plus", "readme+.md");
    }

    @Test
    public void streamResourceNameEndsInPlus_resourceWriter_resourceIsStreamed()
            throws IOException {
        testStreamResourceStreamResourceWriter("end in plus", "readme+.md");
    }

    @Test
    public void streamResourceNameContainsPlus_streamFactory_resourceIsStreamed()
            throws IOException {
        testStreamResourceInputStreamFactory("plus in middle",
                "readme+mine.md");
    }

    @Test
    public void streamResourceNameContainsPlus_resourceWriter_resourceIsStreamed()
            throws IOException {
        testStreamResourceStreamResourceWriter("plus in middle",
                "readme+mine.md");
    }

    @Test
    public void streamResourceNameContainsPlusAndSpaces_streamFactory_resourceIsStreamed()
            throws IOException {
        testStreamResourceInputStreamFactory("plus surrounded by spaces",
                "readme + mine.md");
    }

    @Test
    public void streamResourceNameContainsPlusAndSpaces_resourceWriter_resourceIsStreamed()
            throws IOException {
        testStreamResourceStreamResourceWriter("plus surrounded by spaces",
                "readme + mine.md");
    }

    @Test
    public void stateNodeStates_handlerMustNotReplyWhenNodeDisabled()
            throws IOException {
        stateNodeStatesTestInternal(false, true);
        Mockito.verify(response).sendError(403, "Resource not available");
    }

    @Test
    public void stateNodeStates_handlerMustNotReplyWhenNodeDetached()
            throws IOException {
        stateNodeStatesTestInternal(true, false);
        Mockito.verify(response).sendError(403, "Resource not available");
    }

    @Test
    public void stateNodeStates_handlerMustReplyWhenNodeAttachedAndEnabled()
            throws IOException {
        stateNodeStatesTestInternal(true, true);
        Mockito.verify(response, Mockito.never()).sendError(Mockito.anyInt(),
                Mockito.anyString());
    }

    private VaadinResponse stateNodeStatesTestInternal(boolean enabled,
            boolean attached) throws IOException {
        ElementRequestHandler stateHandler = (request, response, session,
                owner) -> {
        };

        Element owner = Mockito.mock(Element.class);
        StateNode stateNode = Mockito.mock(StateNode.class);
        Mockito.when(owner.getNode()).thenReturn(stateNode);

        Mockito.when(stateNode.isEnabled()).thenReturn(enabled);
        Mockito.when(stateNode.isAttached()).thenReturn(attached);

        StreamResourceRegistry.ElementStreamResource res = new StreamResourceRegistry.ElementStreamResource(
                stateHandler, owner);

        streamResourceRegistry.registerResource(res);

        ServletOutputStream outputStream = Mockito
                .mock(ServletOutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);
        Mockito.when(request.getPathInfo())
                .thenReturn(String.format("/%s%s/%s/%s", DYN_RES_PREFIX,
                        ui.getId().orElse("-1"), res.getId(), res.getName()));

        handler.handleRequest(session, request, response);

        return response;
    }

    private void testStreamResourceInputStreamFactory(String testString,
            String fileName) throws IOException {

        final byte[] testBytes = testString.getBytes();
        StreamResource res = new StreamResource(fileName,
                () -> new ByteArrayInputStream(testBytes));

        streamResourceRegistry.registerResource(res);

        ServletOutputStream outputStream = Mockito
                .mock(ServletOutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);
        Mockito.when(request.getPathInfo())
                .thenReturn(String.format("/%s%s/%s/%s", DYN_RES_PREFIX,
                        ui.getId().orElse("-1"), res.getId(), res.getName()));

        handler.handleRequest(session, request, response);

        Mockito.verify(response).getOutputStream();

        ArgumentCaptor<byte[]> argument = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(outputStream).write(argument.capture(), Mockito.anyInt(),
                Mockito.anyInt());

        byte[] buf = new byte[1024];
        for (int i = 0; i < testBytes.length; i++) {
            buf[i] = testBytes[i];
        }
        Assert.assertArrayEquals("Output differed from expected", buf,
                argument.getValue());
        Mockito.verify(response).setCacheTime(Mockito.anyLong());
        Mockito.verify(response).setContentType("application/octet-stream");
    }

    private void testStreamResourceStreamResourceWriter(String testString,
            String fileName) throws IOException {

        final byte[] testBytes = testString.getBytes();
        StreamResource res = new StreamResource(fileName,
                (stream, session) -> stream.write(testBytes));

        streamResourceRegistry.registerResource(res);

        ServletOutputStream outputStream = Mockito
                .mock(ServletOutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);
        Mockito.when(request.getPathInfo())
                .thenReturn(String.format("/%s%s/%s/%s", DYN_RES_PREFIX,
                        ui.getId().orElse("-1"), res.getId(), res.getName()));

        handler.handleRequest(session, request, response);

        Mockito.verify(response).getOutputStream();

        ArgumentCaptor<byte[]> argument = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(outputStream).write(argument.capture());

        Assert.assertArrayEquals("Output differed from expected", testBytes,
                argument.getValue());
        Mockito.verify(response).setCacheTime(Mockito.anyLong());
        Mockito.verify(response).setContentType("application/octet-stream");
    }

}
