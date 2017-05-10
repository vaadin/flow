/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.server;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSessionBindingEvent;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.server.communication.StreamResourceRequestHandler;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.util.CurrentInstance;

/**
 *
 * @author Vaadin Ltd
 */
public class VaadinServiceTest {

    private class TestSessionDestroyListener implements SessionDestroyListener {

        int callCount = 0;

        @Override
        public void sessionDestroy(SessionDestroyEvent event) {
            callCount++;
        }
    }

    private String createCriticalNotification(String caption, String message,
            String details, String url) {
        return VaadinService.createCriticalNotificationJSON(caption, message,
                details, url);
    }

    @Test
    public void testFireSessionDestroy() throws ServletException {
        VaadinService service = createService();

        TestSessionDestroyListener listener = new TestSessionDestroyListener();

        service.addSessionDestroyListener(listener);

        MockVaadinSession vaadinSession = new MockVaadinSession(service);
        service.fireSessionDestroy(vaadinSession);
        Assert.assertEquals(
                "'fireSessionDestroy' method doesn't call 'close' for the session",
                1, vaadinSession.getCloseCount());

        vaadinSession.valueUnbound(
                EasyMock.createMock(HttpSessionBindingEvent.class));

        Assert.assertEquals(
                "'fireSessionDestroy' method may not call 'close' "
                        + "method for closing session",
                1, vaadinSession.getCloseCount());

        Assert.assertEquals("SessionDestroyListeners not called exactly once",
                1, listener.callCount);
    }

    @Test
    public void captionIsSetToACriticalNotification() {
        String notification = createCriticalNotification("foobar", "message",
                "details", "url");

        assertThat(notification, containsString("\"caption\":\"foobar\""));
    }

    @Test
    public void nullCaptionIsSetToACriticalNotification() {
        String notification = createCriticalNotification(null, "message",
                "details", "url");

        assertThat(notification, containsString("\"caption\":null"));
    }

    @Test
    public void messageWithDetailsIsSetToACriticalNotification() {
        String notification = createCriticalNotification("caption", "foo",
                "bar", "url");

        assertThat(notification, containsString("\"details\":\"bar\""));
    }

    @Test
    public void nullMessageSentAsNullInACriticalNotification() {
        String notification = createCriticalNotification("caption", null,
                "foobar", "url");

        assertThat(notification, containsString("\"message\":null"));
    }

    @Test
    public void nullMessageIsSetToACriticalNotification() {
        String notification = createCriticalNotification("caption", null, null,
                "url");

        assertThat(notification, containsString("\"message\":null"));
    }

    @Test
    public void messageSetToACriticalNotification() {
        String notification = createCriticalNotification("caption", "foobar",
                null, "url");

        assertThat(notification, containsString("\"message\":\"foobar\""));
    }

    @Test
    public void urlIsSetToACriticalNotification() {
        String notification = createCriticalNotification("caption", "message",
                "details", "foobar");

        assertThat(notification, containsString("\"url\":\"foobar\""));
    }

    @Test
    public void nullUrlIsSetToACriticalNotification() {
        String notification = createCriticalNotification("caption", "message",
                "details", null);

        assertThat(notification, containsString("\"url\":null"));
    }

    @Test
    public void serviceContainsStreamResourceHandler()
            throws ServiceException, ServletException {
        ServletConfig servletConfig = new MockServletConfig();
        VaadinServlet servlet = new VaadinServlet();
        servlet.init(servletConfig);
        VaadinService service = servlet.getService();
        Assert.assertTrue(service.createRequestHandlers().stream()
                .filter(StreamResourceRequestHandler.class::isInstance)
                .findAny().isPresent());
    }

    @Test
    public void currentInstancesAfterPendingAccessTasks() {
        VaadinService service = createService();

        MockVaadinSession session = new MockVaadinSession(service);
        session.lock();
        service.accessSession(session, () -> {
            CurrentInstance.set(String.class, "Set in task");
        });

        CurrentInstance.set(String.class, "Original value");
        service.runPendingAccessTasks(session);
        Assert.assertEquals(
                "Original CurrentInstance should be set after the task has been run",
                "Original value", CurrentInstance.get(String.class));
    }

    @Test
    public void testBootstrapListenersCreation() throws ServiceException {
        // in this test the actual behavior of the listeners is not evaluated.
        // That test can be found at the BootstrapHandlerTest.
        BootstrapListener listener1 = evt -> {
        };
        BootstrapListener listener2 = evt -> {
        };

        MockVaadinServletService mock = new MockVaadinServletService(
                new VaadinServlet(), new MockDeploymentConfiguration());

        MockVaadinServletService service = Mockito.spy(mock);

        List<VaadinServiceInitListener> initListeners = new ArrayList<>();
        initListeners.add(evt -> {
            evt.addBootstrapListener(listener1);
            evt.addBootstrapListener(listener2);
        });

        Mockito.when(service.getServiceInitListeners())
                .thenReturn(initListeners.iterator());

        Mockito.when(service.processBootstrapListeners(Mockito.anyList()))
                .thenAnswer(invocation -> {
                    List<BootstrapListener> defaultListeners = (List<BootstrapListener>) invocation
                            .getArguments()[0];

                    assertEquals(2, defaultListeners.size());
                    assertTrue(defaultListeners.contains(listener1));
                    assertTrue(defaultListeners.contains(listener2));

                    // verify whether the list is modifiable
                    assertTrue(defaultListeners.remove(listener1));
                    assertEquals(1, defaultListeners.size());
                    assertTrue(defaultListeners.add(listener1));
                    assertEquals(2, defaultListeners.size());

                    return defaultListeners;
                });

        service.init();

        Mockito.verify(service, Mockito.times(2)).getServiceInitListeners();
        Mockito.verify(service).processBootstrapListeners(Mockito.anyList());
    }

    private static VaadinService createService() {
        ServletConfig servletConfig = new MockServletConfig();
        VaadinServlet servlet = new VaadinServlet();
        try {
            servlet.init(servletConfig);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        VaadinService service = servlet.getService();
        return service;
    }
}
