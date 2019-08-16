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
package com.vaadin.flow.server;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSessionBindingEvent;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.communication.StreamRequestHandler;
import com.vaadin.flow.server.startup.BundleDependencyFilter;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import net.jcip.annotations.NotThreadSafe;

/**
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@NotThreadSafe
public class VaadinServiceTest {

    @Tag("div")
    public static class TestView extends Component {

    }

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
    public void serviceContainsStreamRequestHandler()
            throws ServiceException, ServletException {
        ServletConfig servletConfig = new MockServletConfig();
        VaadinServlet servlet = new VaadinServlet();
        servlet.init(servletConfig);
        VaadinService service = servlet.getService();
        Assert.assertTrue(service.createRequestHandlers().stream()
                .filter(StreamRequestHandler.class::isInstance).findAny()
                .isPresent());
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
        AtomicBoolean listener1Run = new AtomicBoolean(false);
        AtomicBoolean listener2Run = new AtomicBoolean(false);

        BootstrapListener listener1 = evt -> listener1Run.set(true);
        BootstrapListener listener2 = evt -> listener2Run.set(true);

        VaadinServiceInitListener initListener = evt -> evt
                .addBootstrapListener(listener1);
        MockInstantiator instantiator = new MockInstantiator(initListener) {
            @Override
            public Stream<BootstrapListener> getBootstrapListeners(
                    Stream<BootstrapListener> serviceInitListeners) {
                List<BootstrapListener> defaultListeners = serviceInitListeners
                        .collect(Collectors.toList());

                assertEquals(Collections.singletonList(listener1),
                        defaultListeners);

                return Stream.of(listener2);
            }
        };

        MockVaadinServletService service = new MockVaadinServletService();

        service.init(instantiator);

        Assert.assertFalse(listener1Run.get());
        Assert.assertFalse(listener2Run.get());

        service.modifyBootstrapPage(null);

        Assert.assertFalse(listener1Run.get());
        Assert.assertTrue(listener2Run.get());
    }

    @Test
    public void testServiceInitListener_accessApplicationRouteRegistry_registryAvailable() {

        VaadinServiceInitListener initListener = event -> {
            Assert.assertNotNull("service init should have set thread local",
                    VaadinService.getCurrent());

            Router router = event.getSource().getRouter();
            Assert.assertNotEquals("Router should be initialized", router);

            Assert.assertNotEquals("registry should be initialized",
                    router.getRegistry());

            RouteConfiguration.forApplicationScope().setRoute("test",
                    TestView.class);
        };
        MockInstantiator instantiator = new MockInstantiator(initListener);

        MockVaadinServletService service = new MockVaadinServletService();

        service.init(instantiator);

        // the following will allow the route configuration call to work
        VaadinService.setCurrent(service);
        List<RouteData> availableRoutes = RouteConfiguration
                .forApplicationScope().getAvailableRoutes();
        VaadinService.setCurrent(null);

        Assert.assertEquals(1, availableRoutes.size());
        Assert.assertEquals(availableRoutes.get(0).getUrl(), "test");
    }

    @Test
    public void dependencyFilterOrder_bundeFiltersAfterApplicationFilters() {
        DependencyFilter applicationFilter = (dependencies,
                filterContext) -> dependencies;

        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration() {
            @Override
            public boolean useCompiledFrontendResources() {
                return true;
            }
            @Override
            public boolean isCompatibilityMode() {
                return true;
            };
        };

        // Service that pretends to have a proper bundle
        MockVaadinServletService service = new MockVaadinServletService(
                configuration) {
            @Override
            public boolean isResourceAvailable(String url, WebBrowser browser,
                    AbstractTheme theme) {
                if (url.equals("frontend://vaadin-flow-bundle-1.html")) {
                    return true;
                } else {
                    return super.isResourceAvailable(url, browser, theme);
                }
            }

            @Override
            public InputStream getResourceAsStream(String path,
                    WebBrowser browser, AbstractTheme theme) {
                if (path.equals(ApplicationConstants.FRONTEND_PROTOCOL_PREFIX
                        + "vaadin-flow-bundle-manifest.json")) {
                    String data = "{\"vaadin-flow-bundle-1.html\": [\"file.html\"]}";
                    return new ByteArrayInputStream(
                            data.getBytes(StandardCharsets.UTF_8));
                } else {
                    return super.getResourceAsStream(path, browser, theme);
                }
            }
        };

        service.init(new MockInstantiator(evt -> {
            evt.addDependencyFilter(applicationFilter);
        }));

        List<DependencyFilter> filters = new ArrayList<>();
        service.getDependencyFilters().forEach(filters::add);

        Assert.assertEquals(3, filters.size());

        Assert.assertSame(applicationFilter, filters.get(0));
        Assert.assertSame(BundleDependencyFilter.class,
                filters.get(1).getClass());
        Assert.assertSame(BundleDependencyFilter.class,
                filters.get(2).getClass());
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
