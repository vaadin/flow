/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.server.frontend.MockLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionBindingEvent;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.ComponentTest.TestComponent;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.communication.AtmospherePushConnection;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.testcategory.SlowTests;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import org.slf4j.Logger;

public class VaadinSessionTest {

    /**
     * Event fired before a connector is detached from the application.
     */
    public static class UIDetachEvent extends EventObject {
        public UIDetachEvent(UI source) {
            super(source);
        }
    }

    public static class VaadinSessionWithMockLogger extends VaadinSession {
        public final MockLogger mockLogger = new MockLogger();

        public VaadinSessionWithMockLogger(VaadinService service) {
            super(service);
            mockLogger.includeStackTrace = true;
        }

        @Override
        Logger getLogger() {
            return mockLogger;
        }

        @Override
        protected void checkSetConfiguration() {
        }
    }

    private VaadinSessionWithMockLogger session;
    private VaadinServlet mockServlet;
    private VaadinServletService mockService;
    private HttpSession mockHttpSession;
    private WrappedSession mockWrappedSession;
    private VaadinServletRequest vaadinRequest;
    private UI ui;
    private Lock httpSessionLock;

    @Before
    public void setup() throws Exception {
        httpSessionLock = new ReentrantLock();
        mockService = new MockVaadinServletService();
        mockServlet = mockService.getServlet();

        mockHttpSession = Mockito.mock(HttpSession.class);
        mockWrappedSession = new WrappedHttpSession(mockHttpSession) {
            final ReentrantLock lock = new ReentrantLock();

            {
                lock.lock();
            }

            @Override
            public Object getAttribute(String name) {
                Object res;
                try {
                    Thread.sleep(100); // for deadlock testing
                    org.junit.Assert.assertTrue("Deadlock detected",
                            httpSessionLock.tryLock(5, TimeUnit.SECONDS)); // simulates
                                                                           // servlet
                                                                           // container's
                                                                           // session
                                                                           // locking
                    String lockAttribute = mockService.getServiceName()
                            + ".lock";
                    if (lockAttribute.equals(name)) {
                        res = lock;
                    } else if ((VaadinSession.class.getName() + ".Mock Servlet")
                            .equals(name)) {
                        res = session;
                    } else {
                        res = super.getAttribute(name);
                    }
                    httpSessionLock.unlock();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return res;
            }
        };

        session = new VaadinSessionWithMockLogger(mockService);
        mockService.storeSession(session, mockWrappedSession);

        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        session.lock();
        session.setConfiguration(configuration);
        session.unlock();

        ui = new UI();
        vaadinRequest = new VaadinServletRequest(
                Mockito.mock(HttpServletRequest.class), mockService) {
            @Override
            public String getParameter(String name) {
                if ("restartApplication".equals(name)
                        || "ignoreRestart".equals(name)
                        || "closeApplication".equals(name)) {
                    return null;
                }
                return "1";
            }

            @Override
            public String getPathInfo() {
                return "/APP/";
            }

            @Override
            public String getMethod() {
                return "POST";
            }

            @Override
            public WrappedSession getWrappedSession(
                    boolean allowSessionCreation) {
                return mockWrappedSession;
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return new HashMap<>();
            }

        };

        ui.getInternals().setSession(session);

        ui.doInit(vaadinRequest, session.getNextUIid());

        session.addUI(ui);

    }

    /**
     * Test for issue #6349
     */
    @Test
    public void testCurrentInstancePollution() throws InterruptedException {

        // Create a sync object
        final AtomicInteger state = new AtomicInteger();

        // For sleeping while we wait
        final Runnable napper = () -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                return;
            }
        };

        // We need to unlock session before running this test
        session.unlock();
        try {

            // Create a thread that holds the session lock until we tell it to
            // unlock; this will cause VaadinSession.access() to enqueue tasks
            // instead of running them immediately.
            Thread thread = new Thread(() -> {
                session.accessSynchronously(() -> {
                    state.incrementAndGet();
                    while (state.get() == 1)
                        napper.run();
                });
            });

            // Start thread and wait for it to grab the lock
            thread.start();
            while (state.get() == 0)
                napper.run();

            // Enqueue two session commands
            session.access(() -> {
                UI.setCurrent(ui); // command #1 sets current UI
                state.incrementAndGet();
            });
            final AtomicReference<UI> uiRef = new AtomicReference<>();
            session.access(() -> {
                uiRef.set(UI.getCurrent()); // command #2 reads current UI
                state.incrementAndGet();
            });

            // Release the session lock, which will run the queue
            state.incrementAndGet();

            // Wait for enqueued tasks to complete
            while (state.get() < 4)
                napper.run();

            // Command #2 should not have seen command #1's UI
            Assert.assertNull(uiRef.get());
        } finally {
            session.lock();
        }
    }

    /**
     * This reproduces #14452 situation with deadlock - see diagram
     */
    @Test
    @Category(SlowTests.class)
    public void testInvalidationDeadlock() {

        // this simulates servlet container's session invalidation from another
        // thread
        new Thread(() -> {
            try {
                Thread.sleep(150); // delay selected so that VaadinSession
                // will be already locked by the main
                // thread
                // when we get here
                httpSessionLock.lock();// simulating servlet container's
                // session lock
                mockService.fireSessionDestroy(session);
                httpSessionLock.unlock();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        try {
            mockService.findVaadinSession(vaadinRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void threadLocalsAfterUnderlyingSessionTimeout()
            throws InterruptedException {

        final AtomicBoolean detachCalled = new AtomicBoolean(false);
        ui.addDetachListener(e -> {
            detachCalled.set(true);
            Assert.assertEquals(ui, UI.getCurrent());
            Assert.assertEquals(session, VaadinSession.getCurrent());
            Assert.assertEquals(mockService, VaadinService.getCurrent());
            Assert.assertEquals(mockServlet, VaadinServlet.getCurrent());
        });

        session.valueUnbound(Mockito.mock(HttpSessionBindingEvent.class));
        mockService.runPendingAccessTasks(session); // as soon as we changed
                                                    // session.accessSynchronously
                                                    // to session.access in
                                                    // VaadinService.fireSessionDestroy,
                                                    // we need to run the
                                                    // pending task ourselves
        Assert.assertTrue(detachCalled.get());
    }

    @Test
    @Category(SlowTests.class)
    public void threadLocalsAfterSessionDestroy() throws InterruptedException {
        final AtomicBoolean detachCalled = new AtomicBoolean(false);
        ui.addDetachListener(e -> {
            detachCalled.set(true);
            Assert.assertEquals(ui, UI.getCurrent());
            Assert.assertEquals(session, VaadinSession.getCurrent());
            Assert.assertEquals(mockService, VaadinService.getCurrent());
            Assert.assertEquals(mockServlet, VaadinServlet.getCurrent());
        });
        CurrentInstance.clearAll();
        session.close();
        mockService.cleanupSession(session);
        mockService.runPendingAccessTasks(session); // as soon as we changed
                                                    // session.accessSynchronously
                                                    // to session.access in
                                                    // VaadinService.fireSessionDestroy,
                                                    // we need to run the
                                                    // pending task ourselves
        Assert.assertTrue(detachCalled.get());
    }

    @Test
    public void testValueUnbound() {
        MockVaadinSession vaadinSession = new MockVaadinSession(mockService);

        vaadinSession.valueUnbound(Mockito.mock(HttpSessionBindingEvent.class));
        org.junit.Assert.assertEquals(
                "'valueUnbound' method doesn't call 'close' for the session", 1,
                vaadinSession.getCloseCount());

        vaadinSession.valueUnbound(Mockito.mock(HttpSessionBindingEvent.class));

        org.junit.Assert.assertEquals(
                "'valueUnbound' method may not call 'close' "
                        + "method for closing session",
                1, vaadinSession.getCloseCount());
    }

    private static class SerializationPushConnection
            extends AtmospherePushConnection {

        private transient VaadinSession session = VaadinSession.getCurrent();

        public SerializationPushConnection(UI ui) {
            super(ui);
        }

        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            session = VaadinSession.getCurrent();
        }
    }

    @Test
    @Category(SlowTests.class)
    public void threadLocalsWhenDeserializing() throws Exception {
        ApplicationConfiguration configuration = Mockito
                .mock(ApplicationConfiguration.class);

        Mockito.when(configuration.isDevModeSessionSerializationEnabled())
                .thenReturn(true);

        mockServlet.getServletContext().setAttribute(
                ApplicationConfiguration.class.getName(), configuration);

        VaadinSession.setCurrent(session);
        session.lock();
        SerializationPushConnection pc = new SerializationPushConnection(ui);
        Assert.assertEquals("Session should be set when instance is created",
                session, pc.session);
        ui.getPushConfiguration().setPushMode(PushMode.MANUAL);
        ui.getInternals().setPushConnection(pc);
        int uiId = ui.getUIId();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(session);
        out.close();

        session.unlock();

        CurrentInstance.clearAll();

        ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bos.toByteArray()));

        VaadinSession deserializedSession = (VaadinSession) in.readObject();

        Assert.assertNull("Current session shouldn't leak from deserialisation",
                VaadinSession.getCurrent());

        Assert.assertNotSame("Should get a new session", session,
                deserializedSession);

        // Restore http session and service instance so the session can be
        // locked
        deserializedSession.refreshTransients(mockWrappedSession, mockService);
        deserializedSession.lock();

        UI deserializedUi = deserializedSession.getUIById(uiId);
        SerializationPushConnection deserializedPc = (SerializationPushConnection) deserializedUi
                .getInternals().getPushConnection();

        Assert.assertEquals(
                "Current session should be available in SerializationTestLabel.readObject",
                deserializedSession, deserializedPc.session);
        deserializedSession.unlock();
    }

    @Test
    public void setLocale_setLocaleForAllUIs() {
        UI anotherUI = new UI();

        anotherUI.getInternals().setSession(session);

        anotherUI.doInit(vaadinRequest, session.getNextUIid());

        session.addUI(anotherUI);

        Assert.assertEquals(2, session.getUIs().size());

        Set<Locale> locales = session.getUIs().stream().map(UI::getLocale)
                .collect(Collectors.toSet());

        Optional<Locale> newLocale = Stream.of(Locale.getAvailableLocales())
                .filter(locale -> !locales.contains(locale)
                        && !locale.toString().trim().isEmpty())
                .findFirst();

        session.setLocale(newLocale.get());

        Locale expectedlocale = newLocale.get();

        Iterator<UI> uis = session.getUIs().iterator();
        Assert.assertEquals(expectedlocale, uis.next().getLocale());
        Assert.assertEquals(expectedlocale, uis.next().getLocale());
    }

    @Test
    public void valueUnbound_explicitVaadinSessionClose_wrappedSessionIsNotCleanedUp() {
        ReentrantLock lock = Mockito.mock(ReentrantLock.class);
        Mockito.when(lock.isHeldByCurrentThread()).thenReturn(true);
        mockService = new MockVaadinServletService() {
            @Override
            protected Lock getSessionLock(WrappedSession wrappedSession) {
                return lock;
            }

        };

        VaadinSession vaadinSession = new VaadinSession(mockService) {
            @Override
            public boolean hasLock() {
                return true;
            }
        };

        vaadinSession.sessionClosedExplicitly = true;

        WrappedSession httpSession = Mockito.mock(WrappedSession.class);
        vaadinSession.refreshTransients(httpSession, mockService);

        VaadinSession.setCurrent(vaadinSession);
        mockService.setCurrentInstances(Mockito.mock(VaadinRequest.class),
                Mockito.mock(VaadinResponse.class));

        try {
            vaadinSession
                    .valueUnbound(Mockito.mock(HttpSessionBindingEvent.class));

            Assert.assertNotNull(vaadinSession.getSession());
        } finally {
            CurrentInstance.clearAll();
        }
    }

    @Test
    public void valueUnbound_implicitVaadinSessionClose_wrappedSessionIsCleanedUp() {
        ReentrantLock lock = Mockito.mock(ReentrantLock.class);
        Mockito.when(lock.isHeldByCurrentThread()).thenReturn(true);
        mockService = new MockVaadinServletService() {
            @Override
            protected Lock getSessionLock(WrappedSession wrappedSession) {
                return lock;
            }
        };

        VaadinSession vaadinSession = new VaadinSession(mockService) {
            @Override
            public boolean hasLock() {
                return true;
            }
        };

        WrappedSession httpSession = Mockito.mock(WrappedSession.class);
        vaadinSession.refreshTransients(httpSession, mockService);

        VaadinSession.setCurrent(vaadinSession);
        mockService.setCurrentInstances(Mockito.mock(VaadinRequest.class),
                Mockito.mock(VaadinResponse.class));

        try {
            vaadinSession
                    .valueUnbound(Mockito.mock(HttpSessionBindingEvent.class));

            Assert.assertNull(vaadinSession.getSession());
        } finally {
            CurrentInstance.clearAll();
        }
    }

    @Test
    public void setState_closedState_sessionFieldIsCleanedUp() {
        ReentrantLock lock = Mockito.mock(ReentrantLock.class);
        Mockito.when(lock.isHeldByCurrentThread()).thenReturn(true);
        mockService = new MockVaadinServletService() {
            @Override
            protected Lock getSessionLock(WrappedSession wrappedSession) {
                return lock;
            }
        };

        VaadinSession vaadinSession = new VaadinSession(mockService);
        WrappedSession httpSession = Mockito.mock(WrappedSession.class);
        vaadinSession.refreshTransients(httpSession, mockService);

        vaadinSession.setState(VaadinSessionState.CLOSING);
        vaadinSession.setState(VaadinSessionState.CLOSED);

        Assert.assertNull(vaadinSession.getSession());
    }

    @Test
    public void valueUnbound_sessionIsNotInitialized_noAnyInteractions() {
        VaadinSession session = Mockito.spy(TestVaadinSession.class);

        try {
            Field serviceField = VaadinSession.class
                    .getDeclaredField("service");
            serviceField.setAccessible(true);
            serviceField.set(session, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        HttpSessionBindingEvent event = Mockito
                .mock(HttpSessionBindingEvent.class);
        session.valueUnbound(null);

        Mockito.verify(session).valueUnbound(null);
        Mockito.verify(session).getLogger();
        Mockito.verifyNoInteractions(event);
        Mockito.verifyNoMoreInteractions(session);
    }

    public static class TestVaadinSession extends VaadinSession {

        public TestVaadinSession() {
            super(new MockVaadinServletService());
        }
    }

    @Test
    public void findComponent_existingComponentFound() {
        TestComponent testComponent = createTestComponentInSession();
        int nodeId = testComponent.getElement().getNode().getId();
        int uiId = testComponent.getUI().get().getUIId();
        VaadinSession session = testComponent.getUI().get().getSession();
        Assert.assertSame(testComponent,
                session.findElement(uiId, nodeId).getComponent().get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void findComponent_nonExistingNodeIdThrows() {
        TestComponent testComponent = createTestComponentInSession();
        int nodeId = testComponent.getElement().getNode().getId();
        int uiId = testComponent.getUI().get().getUIId();
        VaadinSession session = testComponent.getUI().get().getSession();
        session.findElement(uiId, nodeId * 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void findComponent_nonExistingAppIdThrows() {
        TestComponent testComponent = createTestComponentInSession();
        int nodeId = testComponent.getElement().getNode().getId();
        VaadinSession session = testComponent.getUI().get().getSession();
        session.findElement(123, nodeId);
    }

    private TestComponent createTestComponentInSession() {
        TestComponent testComponent = new TestComponent();
        ui.add(testComponent);
        return testComponent;
    }

    @Test
    public void checkHasLock_noCheckInDevMode() {
        Assume.assumeTrue(session.hasLock());
        Assume.assumeFalse(session.getConfiguration().isProductionMode());
        Assert.assertEquals(SessionLockCheckStrategy.ASSERT,
                session.getConfiguration().getSessionLockCheckStrategy());
        final MockDeploymentConfiguration configuration = (MockDeploymentConfiguration) session
                .getConfiguration();

        session.checkHasLock();

        configuration.setLockCheckStrategy(SessionLockCheckStrategy.LOG);
        session.setConfiguration(configuration);
        session.mockLogger.clearLogs();
        session.checkHasLock();
        Assert.assertEquals("", session.mockLogger.getLogs());

        configuration.setLockCheckStrategy(SessionLockCheckStrategy.ASSERT);
        session.setConfiguration(configuration);
        session.checkHasLock();
    }

    @Test
    public void checkHasLock_assert() {
        final MockDeploymentConfiguration configuration = (MockDeploymentConfiguration) session
                .getConfiguration();
        configuration.setProductionMode(true);
        session.setConfiguration(configuration);
        session.unlock();
        Assume.assumeFalse(session.hasLock());

        try {
            // this should throw AssertionError since assertions are enabled
            // during tests
            session.checkHasLock();
            // don't use fail() since it will throw AssertionError
            throw new RuntimeException("lock check passed");
        } catch (AssertionError ex) {
            // okay
        }
    }

    @Test
    public void checkHasLock_throw() {
        final MockDeploymentConfiguration configuration = (MockDeploymentConfiguration) session
                .getConfiguration();
        configuration.setProductionMode(true);
        configuration.setLockCheckStrategy(SessionLockCheckStrategy.THROW);
        session.setConfiguration(configuration);
        session.unlock();
        Assume.assumeFalse(session.hasLock());

        try {
            // this should throw IllegalStateException
            session.checkHasLock();
            Assert.fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException ex) {
            // okay
        }
    }

    @Test
    public void checkHasLock_log() {
        final MockDeploymentConfiguration configuration = (MockDeploymentConfiguration) session
                .getConfiguration();
        configuration.setProductionMode(true);
        configuration.setLockCheckStrategy(SessionLockCheckStrategy.LOG);
        session.setConfiguration(configuration);
        session.unlock();
        Assume.assumeFalse(session.hasLock());

        // this should log a warning message into the logger, including the
        // stacktrace.
        session.checkHasLock();
        Assert.assertTrue(session.mockLogger.getLogs().trim(),
                session.mockLogger.getLogs().contains(
                        "[Warning] Cannot access state in VaadinSession or UI without locking the session.\n"
                                + "java.lang.IllegalStateException: Cannot access state in VaadinSession or UI without locking the session.\n"
                                + "\tat com.vaadin.flow.server.SessionLockCheckStrategy$2.checkHasLock(SessionLockCheckStrategy.java:"));
    }
}
