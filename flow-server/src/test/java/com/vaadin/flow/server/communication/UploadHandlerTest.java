package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import net.jcip.annotations.NotThreadSafe;
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
import static org.mockito.Mockito.when;

@NotThreadSafe
public class UploadHandlerTest {

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
    public void doUploadHandleXhrFilePost_happyPath_setContentTypeNoExplicitSetStatus() {
        UploadHandler handler = (event) -> {
            event.getResponse().setContentType("text/html; charset=utf-8");
        };

        StateNode stateNode = Mockito.mock(StateNode.class);
        when(stateNode.isAttached()).thenReturn(true);
        Element element = Mockito.mock(Element.class);
        Mockito.when(element.getNode()).thenReturn(stateNode);

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

        UploadHandler downloadHandler = (event) -> {
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
                .registerResource(downloadHandler);
        AbstractStreamResource res = streamRegistration.getResource();

        StateNode stateNode = Mockito.mock(StateNode.class);
        Mockito.when(stateNode.isAttached()).thenReturn(true);
        Element element = Mockito.mock(Element.class);
        Mockito.when(element.getNode()).thenReturn(stateNode);

        ServletOutputStream outputStream = Mockito
                .mock(ServletOutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);
        Mockito.when(request.getPathInfo())
                .thenReturn(String.format("/%s%s/%s/%s", DYN_RES_PREFIX,
                        ui.getId().orElse("-1"), res.getId(), res.getName()));
        Mockito.when(request.getContentLengthLong())
                .thenReturn(Long.valueOf(testBytes.length));
        Mockito.when(request.getInputStream())
                .thenReturn(createInputStream(testString));

        handler.handleRequest(session, request, response);

        Assert.assertArrayEquals("Output differed from expected", testBytes,
                output);

        Assert.assertEquals("", testBytes.length, amount.get());
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

}
