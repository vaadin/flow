/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.server.communication;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.MockServletConfig;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceWriter;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;

public class StreamResourceHandlerTest {

    private StreamResourceHandler handler = new StreamResourceHandler();
    private MockVaadinSession session;
    private VaadinServletRequest request;
    private VaadinServletResponse response;

    @Before
    public void setUp() throws ServletException {
        ServletConfig servletConfig = new MockServletConfig();
        VaadinServlet servlet = new VaadinServlet();
        servlet.init(servletConfig);
        VaadinService service = servlet.getService();

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
}
