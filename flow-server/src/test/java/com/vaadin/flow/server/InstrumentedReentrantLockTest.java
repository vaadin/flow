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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.tests.util.MockUI;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InstrumentedReentrantLockTest {

    @AfterEach
    void clearCurrentInstances() {
        CurrentInstance.clearAll();
    }

    @Test
    void lockAcquired_javaScriptInvocationsLeftFromEarlierHold_warningLogged() {
        MockVaadinServletService service = new MockVaadinServletService();
        InstrumentedReentrantLock lock = new InstrumentedReentrantLock(service);
        VaadinSession session = bindNewSession(service, lock);
        Logger logger = mock(Logger.class);
        when(logger.isWarnEnabled()).thenReturn(true);
        try (MockedStatic<LoggerFactory> loggerFactory = mockStatic(
                LoggerFactory.class, CALLS_REAL_METHODS)) {
            loggerFactory.when(() -> LoggerFactory.getLogger(
                    "com.vaadin.flow.component.internal.PendingJavaScriptInvocationUtil"))
                    .thenReturn(logger);

            lock.lock();
            try {
                MockUI ui = new MockUI(session);
                ui.doInit(null, 1, "foo");
                session.addUI(ui);
                for (int i = 0; i < 1001; i++) {
                    ui.getPage().executeJs("window.foo = $0", i);
                }
            } finally {
                lock.unlock();
            }
            verify(logger, never()).warn(anyString());

            // Nothing is sent without a request or push, so the invocations
            // are still waiting the next time the lock is acquired
            lock.lock();
            lock.unlock();

            verify(logger).warn(contains(
                    "1001 JavaScript invocations scheduled for this UI have not been sent to the browser yet. "
                            + "The newest one still waiting was scheduled for node id=1"));
        }
    }

    private static VaadinSession bindNewSession(VaadinService service,
            InstrumentedReentrantLock lock) {
        WrappedSession wrappedSession = mock(WrappedSession.class);
        when(wrappedSession.getAttribute(service.getServiceName() + ".lock"))
                .thenReturn(lock);
        VaadinSession session = new VaadinSession(service);
        lock.lock();
        try {
            session.refreshTransients(wrappedSession, service);
        } finally {
            lock.unlock();
        }
        return session;
    }
}
