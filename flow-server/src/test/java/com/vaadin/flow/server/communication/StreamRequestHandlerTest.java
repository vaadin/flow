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
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
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
    public void nodeDisabled_shouldReplyForDisabledUpdateModeAlways()
            throws IOException {
        TestElementHandlerBuilder builder = new TestElementHandlerBuilder()
                .withDisabledUpdateMode(DisabledUpdateMode.ALWAYS);
        stateNodeStatesTestInternal(builder);
        Mockito.verify(response, Mockito.never()).sendError(Mockito.anyInt(),
                Mockito.anyString());
    }

    @Test
    public void nodeInert_shouldRespondWithResourceNotAvailable()
            throws IOException {
        TestElementHandlerBuilder builder = new TestElementHandlerBuilder()
                .withInert(true);
        stateNodeStatesTestInternal(builder);
        Mockito.verify(response).sendError(403, "Resource not available");
    }

    @Test
    public void nodeInert_handlerShouldReplyForAllowInert() throws IOException {
        TestElementHandlerBuilder builder = new TestElementHandlerBuilder()
                .withInert(true).withAllowInert(true);
        stateNodeStatesTestInternal(builder);
        Mockito.verify(response, Mockito.never()).sendError(Mockito.anyInt(),
                Mockito.anyString());
    }

    @Test
    public void nodeHidden_shouldRespondWithResourceNotAvailable()
            throws IOException {
        TestElementHandlerBuilder builder = new TestElementHandlerBuilder()
                .withVisible(false);
        stateNodeStatesTestInternal(builder);
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
        TestElementHandlerBuilder builder = new TestElementHandlerBuilder()
                .withEnalbed(enabled).withAttached(attached);
        return stateNodeStatesTestInternal(builder);
    }

    private VaadinResponse stateNodeStatesTestInternal(
            TestElementHandlerBuilder builder) throws IOException {
        ElementRequestHandler stateHandler = builder
                .buildElementRequestHandler();
        TestStateNodeProperties testStateNodeProperties = builder
                .buildStateNodeProperties();

        Element owner = Mockito.mock(Element.class);
        StateNode stateNode = Mockito.mock(StateNode.class);
        Mockito.when(owner.getNode()).thenReturn(stateNode);

        Mockito.when(stateNode.isEnabled())
                .thenReturn(testStateNodeProperties.enabled);
        Mockito.when(stateNode.isAttached())
                .thenReturn(testStateNodeProperties.attached);
        Mockito.when(stateNode.isVisible())
                .thenReturn(testStateNodeProperties.visible);
        Mockito.when(stateNode.isInert())
                .thenReturn(testStateNodeProperties.inert);

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

    private static final class TestElementHandlerBuilder {
        private final TestElementHandlerProperties elementHandlerProperties;
        private final TestStateNodeProperties stateNodeProperties;

        public TestElementHandlerBuilder() {
            this.elementHandlerProperties = new TestElementHandlerProperties(
                    DisabledUpdateMode.ONLY_WHEN_ENABLED, false);
            this.stateNodeProperties = new TestStateNodeProperties(true, true,
                    false, true);
        }

        public TestElementHandlerBuilder withEnalbed(boolean enabled) {
            stateNodeProperties.enabled = enabled;
            return this;
        }

        public TestElementHandlerBuilder withAttached(boolean attached) {
            stateNodeProperties.attached = attached;
            return this;
        }

        public TestElementHandlerBuilder withInert(boolean inert) {
            stateNodeProperties.inert = inert;
            return this;
        }

        public TestElementHandlerBuilder withVisible(boolean visible) {
            stateNodeProperties.visible = visible;
            return this;
        }

        public TestElementHandlerBuilder withDisabledUpdateMode(
                DisabledUpdateMode disabledUpdateMode) {
            elementHandlerProperties.disabledUpdateMode = disabledUpdateMode;
            return this;
        }

        public TestElementHandlerBuilder withAllowInert(boolean allowInert) {
            elementHandlerProperties.allowInert = allowInert;
            return this;
        }

        public TestStateNodeProperties buildStateNodeProperties() {
            return new TestStateNodeProperties(stateNodeProperties.enabled,
                    stateNodeProperties.attached, stateNodeProperties.inert,
                    stateNodeProperties.visible);
        }

        public ElementRequestHandler buildElementRequestHandler() {
            return new ElementRequestHandler() {
                @Override
                public void handleRequest(VaadinRequest request,
                        VaadinResponse response, VaadinSession session,
                        Element owner) {
                    // Handle the request
                }

                @Override
                public boolean allowInert() {
                    return elementHandlerProperties.allowInert;
                }

                @Override
                public DisabledUpdateMode getDisabledUpdateMode() {
                    return elementHandlerProperties.disabledUpdateMode;
                }
            };
        }
    }

    private static final class TestStateNodeProperties {
        private boolean enabled;
        private boolean attached;
        private boolean inert;
        private boolean visible;

        private TestStateNodeProperties(boolean enabled, boolean attached,
                boolean inert, boolean visible) {
            this.enabled = enabled;
            this.attached = attached;
            this.inert = inert;
            this.visible = visible;
        }
    }

    private static final class TestElementHandlerProperties {
        private DisabledUpdateMode disabledUpdateMode;
        private boolean allowInert;

        private TestElementHandlerProperties(
                DisabledUpdateMode disabledUpdateMode, boolean allowInert) {
            this.disabledUpdateMode = disabledUpdateMode;
            this.allowInert = allowInert;
        }
    }

}
