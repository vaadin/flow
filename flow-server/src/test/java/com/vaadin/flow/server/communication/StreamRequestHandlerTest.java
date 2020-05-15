package com.vaadin.flow.server.communication;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
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
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.MockServletConfig;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
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
    public void setUp() throws ServletException {
        ServletConfig servletConfig = new MockServletConfig();
        VaadinServlet servlet = new VaadinServlet();
        servlet.init(servletConfig);
        VaadinService service = servlet.getService();

        session = new AlwaysLockedVaadinSession(service) {
            @Override
            public StreamResourceRegistry getResourceRegistry() {
                return streamResourceRegistry;
            }
        };
        streamResourceRegistry = new StreamResourceRegistry(session);
        request = Mockito.mock(VaadinServletRequest.class);
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getMimeType(Mockito.anyString())).thenReturn(null);
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
    public void streamResourceNameContainsPlusses_resourceIsStreamed()
            throws IOException {
        final String testString = "test";

        final byte[] testBytes = testString.getBytes();
        StreamResource res = new StreamResource("readme ++.md",
                () -> new ByteArrayInputStream(testBytes));

        final StreamRegistration streamRegistration = streamResourceRegistry
                .registerResource(res);

        ServletOutputStream outputStream = Mockito
                .mock(ServletOutputStream.class);
        Mockito.when(response.getOutputStream()).thenReturn(outputStream);
        Mockito.when(request.getPathInfo()).thenReturn(
                String.format("/%s%s/%s/%s", DYN_RES_PREFIX,
                        ui.getId().orElse("-1"), res.getId(), res.getName()));

        handler.handleRequest(session, request, response);

        Mockito.verify(response).getOutputStream();

        ArgumentCaptor<byte[]> argument = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(outputStream)
                .write(argument.capture(), Mockito.anyInt(), Mockito.anyInt());

        byte[] buf = new byte[1024];
        for (int i = 0; i < testBytes.length; i++) {
            buf[i] = testBytes[i];
        }
        Assert.assertArrayEquals("Output differed from expected", buf,
                argument.getValue());
        Mockito.verify(response).setCacheTime(Mockito.anyInt());
        Mockito.verify(response).setContentType("application/octet-stream");
    }
}
