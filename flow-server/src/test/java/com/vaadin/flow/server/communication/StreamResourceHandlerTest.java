/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server.communication;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceWriter;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;

public class StreamResourceHandlerTest {

    private StreamResourceHandler handler = new StreamResourceHandler();
    private MockVaadinSession session;
    private VaadinServletRequest request;
    private VaadinServletResponse response;

    @Before
    public void setUp() throws ServletException, ServiceException {
        VaadinService service = new MockVaadinServletService();
        service.init();

        session = new AlwaysLockedVaadinSession(service);
        request = Mockito.mock(VaadinServletRequest.class);
        ServletContext context = Mockito.mock(ServletContext.class);
        Mockito.when(request.getServletContext()).thenReturn(context);
        response = Mockito.mock(VaadinServletResponse.class);
    }

    @Test
    public void inputStreamFactoryThrowsException_responseStatusIs500()
            throws IOException {
        StreamResource res = new StreamResource("readme.md",
                (InputStreamFactory) () -> {
                    throw new RuntimeException("Simulated");
                });
        try {
            handler.handleRequest(session, request, response, res);
        } catch (RuntimeException ignore) {
            // Ignore exception, it's expected. We need to check the status
        }
        Mockito.verify(response)
                .setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void inputStreamResourceWriterThrows_responseStatusIs500()
            throws IOException {
        StreamResource res = new StreamResource("readme.md",
                (StreamResourceWriter) (stream, session) -> {
                    throw new RuntimeException("Simulated");
                });
        try {
            handler.handleRequest(session, request, response, res);
        } catch (RuntimeException ignore) {
            // Ignore exception, it's expected. We need to check the status
        }
        Mockito.verify(response)
                .setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void inputStreamResourceWriterIsNull_responseStatusIs500()
            throws IOException {
        @SuppressWarnings("serial")
        StreamResource res = new StreamResource("readme.md",
                () -> new ByteArrayInputStream(new byte[0])) {
            @Override
            public StreamResourceWriter getWriter() {
                return null;
            }
        };
        try {
            handler.handleRequest(session, request, response, res);
        } catch (IOException ignore) {
            // Ignore exception, it's expected. We need to check the status
        }
        Mockito.verify(response)
                .setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void inputStreamResourceHasHeader_headerIsWritten()
            throws IOException {
        StreamResource res = new StreamResource("readme.md",
                () -> new ByteArrayInputStream(new byte[0]));

        res.setHeader("foo", "bar");

        handler.handleRequest(session, request, response, res);

        Mockito.verify(response).setHeader("foo", "bar");
    }
}
