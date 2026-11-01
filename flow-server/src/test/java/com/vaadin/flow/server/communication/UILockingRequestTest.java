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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.SessionLockCheckStrategy;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ApplicationConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sends real heartbeat and UIDL requests through the request handlers while
 * another thread keeps the lock of a different UI of the same session.
 */
class UILockingRequestTest {

    private static final long TIMEOUT_SECONDS = 5;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private MockVaadinServletService service;
    private VaadinSession session;
    private UI ui1;
    private UI ui2;

    private final CountDownLatch ui1Locked = new CountDownLatch(1);
    private final CountDownLatch releaseUI1 = new CountDownLatch(1);
    private Future<?> ui1Holder;

    @BeforeEach
    void setUp() {
        service = new MockVaadinServletService();
        session = new MockVaadinSession(service) {
            {
                applyLockStrategy(SessionLockCheckStrategy.THROW);
                applyUILocking(true);
            }
        };
        ui1 = addUI(1);
        ui2 = addUI(2);
    }

    @AfterEach
    void tearDown() throws Exception {
        releaseUI1.countDown();
        if (ui1Holder != null) {
            ui1Holder.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        executor.shutdownNow();
        CurrentInstance.clearAll();
    }

    @Test
    void heartbeatForOtherUI_answeredWhileUIIsLocked() throws Exception {
        ui2.accessSynchronously(
                () -> ui2.getInternals().setLastHeartbeatTimestamp(0));
        holdUI1();
        VaadinResponse response = mock(VaadinResponse.class);

        boolean handled = handleInOtherThread(() -> new HeartbeatHandler()
                .handleRequest(session, request("heartbeat", ui2), response));

        assertTrue(handled);
        verify(response).setHeader("Cache-Control", "no-cache");
        assertTrue(ui2.getInternals().getLastHeartbeatTimestamp() > 0);
        assertUI1StillLocked();
    }

    @Test
    void uidlForOtherUI_handledWithOnlyItsOwnLockWhileUIIsLocked()
            throws Exception {
        holdUI1();
        AtomicReference<UI> rpcUI = new AtomicReference<>();
        AtomicBoolean ownLockHeld = new AtomicBoolean();
        AtomicBoolean sessionLockHeld = new AtomicBoolean(true);
        AtomicReference<Object> attribute = new AtomicReference<>();
        UidlRequestHandler handler = createUidlHandler((ui, body) -> {
            rpcUI.set(ui);
            ownLockHeld.set(ui.getInternals().hasUILock());
            sessionLockHeld.set(session.hasLock());
            // Session state stays usable while holding only the UI lock
            session.setAttribute("handled-by", ui.getUIId());
            attribute.set(session.getAttribute("handled-by"));
        });
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        boolean handled = handleInOtherThread(() -> handler.handleRequest(
                session, request("uidl", ui2), response(output)));

        assertTrue(handled);
        assertSame(ui2, rpcUI.get());
        assertTrue(ownLockHeld.get(), "UI2 must be locked during the RPC");
        assertFalse(sessionLockHeld.get(),
                "The session must not be locked during the RPC");
        assertEquals(2, attribute.get());
        assertEquals("{\"ui\":2}", output.toString(StandardCharsets.UTF_8));
        assertUI1StillLocked();
    }

    @Test
    void repeatedRequestsForOtherUI_allAnsweredWhileUIStaysLocked()
            throws Exception {
        holdUI1();
        UidlRequestHandler handler = createUidlHandler((ui, body) -> {
        });

        for (int i = 0; i < 10; i++) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            assertTrue(handleInOtherThread(() -> handler.handleRequest(session,
                    request("uidl", ui2), response(output))));
            assertEquals("{\"ui\":2}", output.toString(StandardCharsets.UTF_8));
            assertTrue(handleInOtherThread(
                    () -> new HeartbeatHandler().handleRequest(session,
                            request("heartbeat", ui2),
                            mock(VaadinResponse.class))));
        }
        assertUI1StillLocked();
    }

    @Test
    void uidlForLockedUI_waitsUntilUIIsReleased() throws Exception {
        holdUI1();
        AtomicReference<UI> rpcUI = new AtomicReference<>();
        UidlRequestHandler handler = createUidlHandler(
                (ui, body) -> rpcUI.set(ui));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Future<Boolean> ui1Request = executor
                .submit(() -> handler.handleRequest(session,
                        request("uidl", ui1), response(output)));

        // The request waits for the UI lock, not for the session lock
        waitUntil(() -> ui1.getInternals().getUILock().hasQueuedThreads());
        assertFalse(ui1Request.isDone());
        assertFalse(((ReentrantLock) session.getLockInstance()).isLocked());

        // Meanwhile the other UI keeps answering
        assertTrue(handleInOtherThread(
                () -> new HeartbeatHandler().handleRequest(session,
                        request("heartbeat", ui2),
                        mock(VaadinResponse.class))));

        releaseUI1.countDown();
        assertTrue(ui1Request.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertSame(ui1, rpcUI.get());
        assertEquals("{\"ui\":1}", output.toString(StandardCharsets.UTF_8));
    }

    /**
     * Starts a thread that holds the lock of UI1 until the test ends.
     */
    private void holdUI1() throws InterruptedException {
        ui1Holder = executor.submit(() -> ui1.accessSynchronously(() -> {
            ui1Locked.countDown();
            try {
                releaseUI1.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        assertTrue(ui1Locked.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    private void assertUI1StillLocked() {
        assertTrue(ui1.getInternals().getUILock().isLocked());
        assertFalse(ui1Holder.isDone());
    }

    /**
     * Runs the request in another thread, failing instead of hanging if it is
     * blocked.
     */
    private boolean handleInOtherThread(Callable<Boolean> request)
            throws Exception {
        Future<Boolean> result = executor.submit(request);
        try {
            return result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            result.cancel(true);
            fail("The request was blocked although its UI is not locked");
            return false;
        }
    }

    private interface RpcCallback {
        void handleRpc(UI ui, String body);
    }

    private static UidlRequestHandler createUidlHandler(RpcCallback rpc) {
        ServerRpcHandler rpcHandler = new ServerRpcHandler() {
            @Override
            public void handleRpc(UI ui, String requestBody,
                    VaadinRequest request) {
                rpc.handleRpc(ui, requestBody);
            }
        };
        return new UidlRequestHandler() {
            @Override
            protected ServerRpcHandler createRpcHandler() {
                return rpcHandler;
            }

            @Override
            void writeUidl(UI ui, Writer writer, boolean resync)
                    throws IOException {
                writer.write("{\"ui\":" + ui.getUIId() + "}");
            }
        };
    }

    private VaadinRequest request(String type, UI ui) throws IOException {
        VaadinRequest request = mock(VaadinRequest.class);
        when(request.getService()).thenReturn(service);
        when(request.getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER))
                .thenReturn(type);
        when(request.getParameter(ApplicationConstants.UI_ID_PARAMETER))
                .thenReturn(String.valueOf(ui.getUIId()));
        when(request.getReader())
                .thenReturn(new BufferedReader(new StringReader("{}")));
        return request;
    }

    private static VaadinResponse response(ByteArrayOutputStream output)
            throws IOException {
        VaadinResponse response = mock(VaadinResponse.class);
        when(response.getOutputStream()).thenReturn(output);
        return response;
    }

    /**
     * Adds a UI the same way bootstrap does: with the session lock and the lock
     * of the new UI held.
     */
    private UI addUI(int id) {
        UI ui = new TestUI(id);
        session.lock();
        try {
            assertTrue(ui.getInternals().tryLockUI());
            try {
                ui.getInternals().setSession(session);
                session.addUI(ui);
            } finally {
                ui.getInternals().unlockUI();
            }
        } finally {
            session.unlock();
        }
        return ui;
    }

    private static void waitUntil(BooleanSupplier condition)
            throws InterruptedException {
        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("Condition not met within " + TIMEOUT_SECONDS + "s");
            }
            Thread.sleep(10);
        }
    }

    private static class TestUI extends UI {
        private final int id;

        private TestUI(int id) {
            this.id = id;
        }

        @Override
        public int getUIId() {
            return id;
        }
    }
}
