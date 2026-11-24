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
package com.vaadin.flow.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SynchronizedRequestHandlerTest {

    @AfterEach
    void clearCurrentInstances() {
        CurrentInstance.clearAll();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void handleRequest_handlerSetsCurrentUI_previousInstancesRestoredAfterUnlock(
            boolean readAndWriteOutsideSessionLock) throws Exception {
        MockVaadinSession session = new MockVaadinSession();
        UI ui = session.runWithLock(() -> new MockUI(session));
        UI previousUI = session.runWithLock(() -> new MockUI(session));
        CurrentInstance.clearAll();
        VaadinSession.setCurrent(session);
        UI.setCurrent(previousUI);
        VaadinRequest request = mock(VaadinRequest.class);
        when(request.getService()).thenReturn(session.getService());
        when(request.getReader())
                .thenReturn(new BufferedReader(new StringReader("")));
        AtomicBoolean written = new AtomicBoolean();

        SynchronizedRequestHandler handler = new SynchronizedRequestHandler() {
            @Override
            public boolean synchronizedHandleRequest(VaadinSession session,
                    VaadinRequest request, VaadinResponse response) {
                UI.setCurrent(ui);
                return true;
            }

            @Override
            public Optional<ResponseWriter> synchronizedHandleRequest(
                    VaadinSession session, VaadinRequest request,
                    VaadinResponse response, String requestBody) {
                UI.setCurrent(ui);
                return Optional.of(() -> {
                    assertFalse(session.hasLock());
                    assertSame(previousUI, UI.getCurrent());
                    written.set(true);
                });
            }

            @Override
            public boolean isReadAndWriteOutsideSessionLock() {
                return readAndWriteOutsideSessionLock;
            }
        };

        assertTrue(handler.handleRequest(session, request,
                mock(VaadinResponse.class)));

        assertEquals(readAndWriteOutsideSessionLock, written.get());
        assertFalse(session.hasLock());
        assertSame(previousUI, UI.getCurrent());
        assertSame(session, VaadinSession.getCurrent());
    }

    @Test
    void handleRequest_handlerSetsCurrentUIAndThrows_failureWrappedWithUIAndPreviousInstancesRestored()
            throws Exception {
        MockVaadinSession session = new MockVaadinSession();
        UI ui = session.runWithLock(() -> new MockUI(session));
        CurrentInstance.clearAll();
        VaadinSession.setCurrent(session);
        IllegalStateException failure = new IllegalStateException("BOOM");

        SynchronizedRequestHandler handler = new SynchronizedRequestHandler() {
            @Override
            public boolean synchronizedHandleRequest(VaadinSession session,
                    VaadinRequest request, VaadinResponse response) {
                UI.setCurrent(ui);
                throw failure;
            }
        };

        FailedUIRequestException thrown = assertThrows(
                FailedUIRequestException.class,
                () -> handler.handleRequest(session, mock(VaadinRequest.class),
                        mock(VaadinResponse.class)));
        assertSame(failure, thrown.getCause());
        assertSame(ui, thrown.getUI());
        assertFalse(session.hasLock());
        assertNull(UI.getCurrent());
        assertSame(session, VaadinSession.getCurrent());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void handleRequest_handlerThrowsWithoutCurrentUI_originalExceptionThrownUnwrapped(
            boolean ioException) {
        MockVaadinSession session = new MockVaadinSession();
        CurrentInstance.clearAll();
        Exception failure = ioException ? new IOException("BOOM")
                : new IllegalStateException("BOOM");
        SynchronizedRequestHandler handler = new SynchronizedRequestHandler() {
            @Override
            public boolean synchronizedHandleRequest(VaadinSession session,
                    VaadinRequest request, VaadinResponse response)
                    throws IOException {
                if (failure instanceof IOException io) {
                    throw io;
                }
                throw (RuntimeException) failure;
            }
        };

        assertSame(failure,
                assertThrows(Exception.class,
                        () -> handler.handleRequest(session,
                                mock(VaadinRequest.class),
                                mock(VaadinResponse.class))));
    }

    @Test
    void handleRequest_nestedHandlerSetsCurrentUIAndThrows_failureWrappedOnce()
            throws Exception {
        MockVaadinSession session = new MockVaadinSession();
        UI ui = session.runWithLock(() -> new MockUI(session));
        CurrentInstance.clearAll();
        IllegalStateException failure = new IllegalStateException("BOOM");
        SynchronizedRequestHandler inner = new SynchronizedRequestHandler() {
            @Override
            public boolean synchronizedHandleRequest(VaadinSession session,
                    VaadinRequest request, VaadinResponse response) {
                UI.setCurrent(ui);
                throw failure;
            }
        };
        SynchronizedRequestHandler outer = new SynchronizedRequestHandler() {
            @Override
            public boolean synchronizedHandleRequest(VaadinSession session,
                    VaadinRequest request, VaadinResponse response)
                    throws IOException {
                UI.setCurrent(ui);
                return inner.handleRequest(session, request, response);
            }
        };

        FailedUIRequestException thrown = assertThrows(
                FailedUIRequestException.class,
                () -> outer.handleRequest(session, mock(VaadinRequest.class),
                        mock(VaadinResponse.class)));
        assertSame(failure, thrown.getCause());
        assertSame(ui, thrown.getUI());
    }

    @Test
    void getRequestBody_underLimit_readsBody() throws Exception {
        String body = "a".repeat(1000);
        assertEquals(body, SynchronizedRequestHandler
                .getRequestBody(new StringReader(body), 1000));
    }

    @Test
    void getRequestBody_overLimit_throws() {
        String body = "a".repeat(1001);
        assertThrows(RequestBodyTooLargeException.class,
                () -> SynchronizedRequestHandler
                        .getRequestBody(new StringReader(body), 1000));
    }

    @Test
    void getRequestBody_negativeLimit_readsWithoutLimit() throws Exception {
        String body = "a".repeat(200_000);
        assertEquals(body, SynchronizedRequestHandler
                .getRequestBody(new StringReader(body), -1));
    }
}
