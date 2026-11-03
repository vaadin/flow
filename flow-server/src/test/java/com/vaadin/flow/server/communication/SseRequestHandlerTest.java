/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.SsePushConnectionTest.SseEvent;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SseRequestHandlerTest {

    private MockVaadinServletService service;
    private MockVaadinSession session;
    private UI ui;
    private SseRequestHandler handler;

    private HttpServletRequest httpRequest;
    private HttpServletResponse httpResponse;
    private AsyncContext asyncContext;
    private StringWriter responseBody;

    @BeforeEach
    void setup() throws Exception {
        service = spy(new MockVaadinServletService());
        session = new MockVaadinSession(service);
        session.lock();
        ui = new MockUI(session);
        ui.getPushConfiguration().setTransport(Transport.SSE_PURE);
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        String pushId = session.getPushId();
        session.unlock();
        UI.setCurrent(null);

        handler = new SseRequestHandler(service);

        httpRequest = mock(HttpServletRequest.class);
        when(httpRequest
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(ApplicationConstants.REQUEST_TYPE_SSE);
        when(httpRequest.getParameter(ApplicationConstants.PUSH_ID_PARAMETER))
                .thenReturn(pushId);
        when(httpRequest.isAsyncSupported()).thenReturn(true);

        httpResponse = mock(HttpServletResponse.class);
        responseBody = new StringWriter();
        when(httpResponse.getWriter())
                .thenReturn(new PrintWriter(responseBody));

        asyncContext = mock(AsyncContext.class);
        when(asyncContext.getResponse()).thenReturn(httpResponse);
        when(httpRequest.startAsync()).thenReturn(asyncContext);
    }

    @AfterEach
    void tearDown() {
        handler.destroy();
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
    }

    @Test
    void handleRequest_otherRequestType_notHandled() throws Exception {
        when(httpRequest
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(ApplicationConstants.REQUEST_TYPE_PUSH);

        assertFalse(handler.handleRequest(session, request(), response()));
        verify(httpRequest, never()).startAsync();
    }

    @Test
    void handleRequest_noServletRequest_throws() {
        VaadinRequest request = mock(VaadinRequest.class);
        when(request.getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(ApplicationConstants.REQUEST_TYPE_SSE);

        assertThrows(IllegalArgumentException.class, () -> handler
                .handleRequest(session, request, mock(VaadinResponse.class)));
    }

    @Test
    void handleRequest_asyncNotSupported_sendsServerError() throws Exception {
        when(httpRequest.isAsyncSupported()).thenReturn(false);

        assertTrue(handler.handleRequest(session, request(), response()));

        verify(httpResponse).sendError(
                eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
        verify(httpRequest, never()).startAsync();
    }

    @Test
    void handleRequest_validRequest_connectsSsePushConnection()
            throws Exception {
        mockFindUI(ui);

        assertTrue(handler.handleRequest(session, request(), response()));

        verify(asyncContext).setTimeout(0);
        SsePushConnection connection = (SsePushConnection) ui.getInternals()
                .getPushConnection();
        assertTrue(connection.isConnected());
        assertEquals(asyncContext, connection.getAsyncContext());
        verify(httpResponse).setContentType("text/event-stream");
        assertEquals(":\n\n", responseBody.toString());
    }

    @Test
    void handleRequest_streamCompleted_connectionLost() throws Exception {
        mockFindUI(ui);
        handler.handleRequest(session, request(), response());

        listener().onComplete(mock(AsyncEvent.class));

        assertFalse(ui.getInternals().getPushConnection().isConnected());
    }

    @Test
    void handleRequest_streamError_connectionLost() throws Exception {
        mockFindUI(ui);
        handler.handleRequest(session, request(), response());

        listener().onError(mock(AsyncEvent.class));

        assertFalse(ui.getInternals().getPushConnection().isConnected());
    }

    @Test
    void handleRequest_streamTimeout_connectionLost() throws Exception {
        mockFindUI(ui);
        handler.handleRequest(session, request(), response());

        listener().onTimeout(mock(AsyncEvent.class));

        assertFalse(ui.getInternals().getPushConnection().isConnected());
    }

    @Test
    void handleRequest_noSession_sendsSessionExpiredMessage() throws Exception {
        assertTrue(handler.handleRequest(null, request(), response()));

        // The client gets the notification through the event stream like any
        // other UIDL message, as EventSource cannot read other responses
        verify(httpResponse).setContentType("text/event-stream");
        List<SseEvent> events = events();
        assertEquals(1, events.size());
        assertTrue(
                parse(events.get(0).data()).get("meta").has("sessionExpired"));
        verify(httpRequest, never()).startAsync();
    }

    @Test
    void handleSessionExpired_sseRequest_sendsSessionExpiredMessage()
            throws Exception {
        assertTrue(handler.handleSessionExpired(request(), response()));

        assertTrue(parse(events().get(0).data()).get("meta")
                .has("sessionExpired"));
    }

    @Test
    void handleSessionExpired_otherRequestType_notHandled() throws Exception {
        when(httpRequest
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(ApplicationConstants.REQUEST_TYPE_UIDL);

        assertFalse(handler.handleSessionExpired(request(), response()));
    }

    @Test
    void handleRequest_uiNotFound_sendsUiNotFoundMessage() throws Exception {
        mockFindUI(null);

        assertTrue(handler.handleRequest(session, request(), response()));

        verify(httpResponse).setContentType("text/event-stream");
        // A missing UI is reported to the client like an expired session
        JsonNode meta = parse(events().get(0).data()).get("meta");
        assertTrue(meta.has("sessionExpired"),
                "UI not found should be reported as an expired session, was: "
                        + meta);
        verify(httpRequest, never()).startAsync();
    }

    @Test
    void handleRequest_invalidPushId_sendsRefreshMessage() throws Exception {
        mockFindUI(ui);
        when(httpRequest.getParameter(ApplicationConstants.PUSH_ID_PARAMETER))
                .thenReturn("invalid");

        assertTrue(handler.handleRequest(session, request(), response()));

        verify(httpResponse).setContentType("text/event-stream");
        JsonNode meta = parse(events().get(0).data()).get("meta");
        assertTrue(meta.has("appError"),
                "An invalid push id should make the client refresh, was: "
                        + meta);
        verify(httpRequest, never()).startAsync();
        assertFalse(ui.getInternals().getPushConnection().isConnected());
    }

    @Test
    void handleRequest_missingPushId_sendsRefreshMessage() throws Exception {
        mockFindUI(ui);
        when(httpRequest.getParameter(ApplicationConstants.PUSH_ID_PARAMETER))
                .thenReturn(null);

        assertTrue(handler.handleRequest(session, request(), response()));

        verify(httpRequest, never()).startAsync();
        assertFalse(ui.getInternals().getPushConnection().isConnected());
    }

    @Test
    void handleRequest_uiWithoutSsePushConnection_sendsBadRequest()
            throws Exception {
        session.lock();
        ui.getPushConfiguration().setTransport(Transport.WEBSOCKET);
        session.unlock();
        mockFindUI(ui);

        assertTrue(handler.handleRequest(session, request(), response()));

        verify(httpResponse).sendError(eq(HttpServletResponse.SC_BAD_REQUEST),
                anyString());
        verify(httpRequest, never()).startAsync();
    }

    @Test
    void handleRequest_pendingPush_isSentWhenConnected() throws Exception {
        mockFindUI(ui);
        session.lock();
        try {
            ui.push();
        } finally {
            session.unlock();
        }

        handler.handleRequest(session, request(), response());

        assertEquals(1, events().size());
    }

    @Test
    void keepAlive_openStream_sendsComment() throws Exception {
        mockFindUI(ui);
        handler.handleRequest(session, request(), response());

        ((Runnable) listener()).run();

        assertEquals(":\n\n:\n\n", responseBody.toString());
    }

    @Test
    void keepAlive_endedStream_sendsNothing() throws Exception {
        mockFindUI(ui);
        handler.handleRequest(session, request(), response());
        AsyncListener stream = listener();
        stream.onComplete(mock(AsyncEvent.class));

        ((Runnable) stream).run();

        assertEquals(":\n\n", responseBody.toString());
    }

    @Test
    void keepAlive_isScheduledWithConfiguredInterval() throws Exception {
        handler = handlerWithKeepAliveInterval("1");
        mockFindUI(ui);

        handler.handleRequest(session, request(), response());

        long deadline = System.currentTimeMillis() + 5000;
        while (!responseBody.toString().equals(":\n\n:\n\n")
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertEquals(":\n\n:\n\n", responseBody.toString());
    }

    @Test
    void keepAlive_intervalZero_isNotScheduled() throws Exception {
        handler = spy(handlerWithKeepAliveInterval("0"));
        mockFindUI(ui);

        handler.handleRequest(session, request(), response());

        verify(handler, never()).getKeepAliveExecutor();
    }

    @Test
    void keepAlive_defaultInterval_isScheduled() throws Exception {
        handler = spy(handler);
        mockFindUI(ui);

        handler.handleRequest(session, request(), response());

        verify(handler).getKeepAliveExecutor();
    }

    @Test
    void keepAlive_stalledStream_doesNotDelayOtherStreams() throws Exception {
        handler = handlerWithKeepAliveInterval("1");
        CountDownLatch clientReads = new CountDownLatch(1);
        Executor executor = service.getExecutor();
        AtomicInteger unfinishedTasks = new AtomicInteger();
        doReturn((Executor) task -> {
            unfinishedTasks.incrementAndGet();
            executor.execute(() -> {
                try {
                    task.run();
                } finally {
                    unfinishedTasks.decrementAndGet();
                }
            });
        }).when(service).getExecutor();
        // A client that has gone without closing the connection: writes wait
        // until the container gives up, after the first comment
        PrintWriter stalling = new PrintWriter(responseBody) {
            private boolean opened;

            @Override
            public void write(String text) {
                if (opened) {
                    try {
                        clientReads.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                opened = true;
                super.write(text);
            }
        };
        when(httpResponse.getWriter()).thenReturn(stalling);
        try {
            mockFindUI(ui);
            handler.handleRequest(session, request(), response());

            session.lock();
            UI other = new MockUI(session);
            other.getPushConfiguration().setTransport(Transport.SSE_PURE);
            other.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
            session.unlock();
            UI.setCurrent(null);
            mockFindUI(other);
            StringWriter otherBody = new StringWriter();
            HttpServletResponse otherResponse = mock(HttpServletResponse.class);
            when(otherResponse.getWriter())
                    .thenReturn(new PrintWriter(otherBody));
            AsyncContext otherContext = mock(AsyncContext.class);
            when(otherContext.getResponse()).thenReturn(otherResponse);
            when(httpRequest.startAsync()).thenReturn(otherContext);
            handler.handleRequest(session, request(),
                    new VaadinServletResponse(otherResponse, service));

            String twoKeepAlives = ":\n\n".repeat(3);
            long deadline = System.currentTimeMillis() + 4000;
            while (!otherBody.toString().equals(twoKeepAlives)
                    && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
            assertEquals(twoKeepAlives, otherBody.toString(),
                    "Keep-alive should reach the other stream");
            // Only the write to the stalled stream remains, once the task
            // that wrote to the other stream has returned
            deadline = System.currentTimeMillis() + 500;
            while (unfinishedTasks.get() != 1
                    && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            assertEquals(1, unfinishedTasks.get(),
                    "Keep-alive comments should not pile up on a stalled stream");
        } finally {
            clientReads.countDown();
        }
    }

    private SseRequestHandler handlerWithKeepAliveInterval(String seconds) {
        ((MockDeploymentConfiguration) service.getDeploymentConfiguration())
                .setApplicationOrSystemProperty(
                        InitParameters.SERVLET_PARAMETER_PUSH_SSE_KEEP_ALIVE_INTERVAL,
                        seconds);
        return new SseRequestHandler(service);
    }

    @Test
    void handleRequest_releasesSessionLock() throws Exception {
        mockFindUI(ui);

        handler.handleRequest(session, request(), response());

        assertFalse(session.hasLock());
    }

    private void mockFindUI(UI result) {
        doReturn(result).when(service).findUI(any());
    }

    private VaadinServletRequest request() {
        return new VaadinServletRequest(httpRequest, service);
    }

    private VaadinServletResponse response() {
        return new VaadinServletResponse(httpResponse, service);
    }

    private AsyncListener listener() {
        ArgumentCaptor<AsyncListener> captor = ArgumentCaptor
                .forClass(AsyncListener.class);
        verify(asyncContext).addListener(captor.capture());
        return captor.getValue();
    }

    private List<SseEvent> events() {
        return SsePushConnectionTest.SseStream.parse(responseBody.toString());
    }

    private static JsonNode parse(String json) {
        return new ObjectMapper().readTree(json);
    }
}
