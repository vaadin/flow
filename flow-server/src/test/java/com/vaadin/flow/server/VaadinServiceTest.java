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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpSessionBindingEvent;
import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.i18n.DefaultI18NProvider;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.internal.menu.MenuRegistry;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.communication.PwaHandler;
import com.vaadin.flow.server.communication.StreamRequestHandler;
import com.vaadin.flow.server.communication.WebComponentBootstrapHandler;
import com.vaadin.flow.server.communication.WebComponentProvider;
import com.vaadin.flow.server.menu.AvailableViewInfo;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

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

    @Route("test")
    @Tag("div")
    public static class AnnotatedTestView extends Component {

    }

    @Route(value = "flow", autoLayout = false)
    @Tag("div")
    public static class OptOutAutoLayoutTestView extends Component {

    }

    private class TestSessionDestroyListener implements SessionDestroyListener {

        int callCount = 0;

        @Override
        public void sessionDestroy(SessionDestroyEvent event) {
            callCount++;
        }
    }

    private class ThrowingSessionDestroyListener
            implements SessionDestroyListener {

        @Override
        public void sessionDestroy(SessionDestroyEvent event) {
            throw new RuntimeException();
        }
    }

    private class TestServiceDestroyListener implements ServiceDestroyListener {

        int callCount = 0;

        @Override
        public void serviceDestroy(ServiceDestroyEvent event) {
            callCount++;
        }
    }

    private class ThrowingServiceDestroyListener
            implements ServiceDestroyListener {

        @Override
        public void serviceDestroy(ServiceDestroyEvent event) {
            throw new RuntimeException();
        }
    }

    private String createCriticalNotification(String caption, String message,
            String details, String url) {
        return VaadinService.createCriticalNotificationJSON(caption, message,
                details, url);
    }

    private abstract static class TestVaadinService extends VaadinService {

        @Override
        protected List<RequestHandler> createRequestHandlers()
                throws ServiceException {
            return super.createRequestHandlers();
        }
    }

    @Before
    @After
    public void clearCurrentInstances() {
        CurrentInstance.clearAll();
    }

    @Test
    public void requestEnd_serviceFailure_threadLocalsCleared() {
        MockVaadinServletService service = new MockVaadinServletService() {
            @Override
            void cleanupSession(VaadinSession session) {
                throw new RuntimeException("BOOM");
            }
        };
        service.init();

        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        service.requestStart(request, response);

        Assert.assertSame(service, VaadinService.getCurrent());
        Assert.assertSame(request, VaadinRequest.getCurrent());
        Assert.assertSame(response, VaadinResponse.getCurrent());

        VaadinSession session = Mockito.mock(VaadinSession.class);
        VaadinSession.setCurrent(session);

        try {
            service.requestEnd(request, response, session);
            Assert.fail("Should have thrown an exception");
        } catch (Exception e) {
            Assert.assertNull("VaadinService.current",
                    VaadinService.getCurrent());
            Assert.assertNull("VaadinSession.current",
                    VaadinSession.getCurrent());
            Assert.assertNull("VaadinRequest.current",
                    VaadinRequest.getCurrent());
            Assert.assertNull("VaadinResponse.current",
                    VaadinResponse.getCurrent());
        } finally {
            CurrentInstance.clearAll();
        }
    }

    @Test
    public void requestEnd_interceptorFailure_allInterceptorsInvoked_doNotThrowAndThreadLocalsCleared() {
        VaadinRequestInterceptor interceptor1 = Mockito
                .mock(VaadinRequestInterceptor.class);
        VaadinRequestInterceptor interceptor2 = Mockito
                .mock(VaadinRequestInterceptor.class);
        Mockito.doThrow(new RuntimeException("BOOM")).when(interceptor2)
                .requestEnd(any(), any(), any());
        VaadinRequestInterceptor interceptor3 = Mockito
                .mock(VaadinRequestInterceptor.class);

        VaadinServiceInitListener initListener = event -> {
            event.addVaadinRequestInterceptor(interceptor1);
            event.addVaadinRequestInterceptor(interceptor2);
            event.addVaadinRequestInterceptor(interceptor3);
        };
        MockInstantiator instantiator = new MockInstantiator(initListener);
        MockVaadinServletService service = new MockVaadinServletService();
        service.init(instantiator);

        Map<String, Object> attributes = new HashMap<>();
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        Mockito.when(request.getAttribute(anyString()))
                .then(i -> attributes.get(i.getArgument(0, String.class)));
        Mockito.doAnswer(i -> attributes.put(i.getArgument(0, String.class),
                i.getArgument(1))).when(request)
                .setAttribute(anyString(), any());
        VaadinResponse response = Mockito.mock(VaadinResponse.class);
        service.requestStart(request, response);

        Assert.assertSame(service, VaadinService.getCurrent());
        Assert.assertSame(request, VaadinRequest.getCurrent());
        Assert.assertSame(response, VaadinResponse.getCurrent());

        VaadinSession session = Mockito.mock(VaadinSession.class);
        VaadinSession.setCurrent(session);

        try {
            service.requestEnd(request, response, session);

            Assert.assertNull("VaadinService.current",
                    VaadinService.getCurrent());
            Assert.assertNull("VaadinSession.current",
                    VaadinSession.getCurrent());
            Assert.assertNull("VaadinRequest.current",
                    VaadinRequest.getCurrent());
            Assert.assertNull("VaadinResponse.current",
                    VaadinResponse.getCurrent());
        } finally {
            CurrentInstance.clearAll();
        }

    }

    @Test
    public void should_reported_routing_server() {

        // this test needs a fresh empty statistics, so we need to clear
        // them for resusing forks for unit tests
        UsageStatistics.resetEntries();

        VaadinServiceInitListener initListener = event -> {
            RouteConfiguration.forApplicationScope().setRoute("test",
                    TestView.class);
        };
        MockInstantiator instantiator = new MockInstantiator(initListener);

        MockVaadinServletService service = new MockVaadinServletService();

        service.init(instantiator);

        Assert.assertTrue(UsageStatistics.getEntries().anyMatch(
                e -> Constants.STATISTIC_ROUTING_SERVER.equals(e.getName())));
        Assert.assertFalse(UsageStatistics.getEntries().anyMatch(
                e -> Constants.STATISTIC_HAS_AUTO_LAYOUT.equals(e.getName())));
    }

    @Test
    public void should_reported_routing_hybrid() {
        UsageStatistics.resetEntries();
        VaadinServiceInitListener initListener = event -> {
            RouteConfiguration.forApplicationScope().setRoute("test",
                    TestView.class);
        };
        UsageStatistics.markAsUsed(Constants.STATISTIC_ROUTING_CLIENT,
                Version.getFullVersion());
        MockInstantiator instantiator = new MockInstantiator(initListener);

        MockVaadinServletService service = new MockVaadinServletService();

        service.init(instantiator);

        Assert.assertTrue(UsageStatistics.getEntries().anyMatch(
                e -> Constants.STATISTIC_ROUTING_HYBRID.equals(e.getName())));
        Assert.assertFalse(UsageStatistics.getEntries().anyMatch(
                e -> Constants.STATISTIC_ROUTING_CLIENT.equals(e.getName())));
        Assert.assertFalse(UsageStatistics.getEntries().anyMatch(
                e -> Constants.STATISTIC_ROUTING_SERVER.equals(e.getName())));
        Assert.assertTrue(UsageStatistics.getEntries().anyMatch(
                e -> Constants.STATISTIC_HAS_FLOW_ROUTE.equals(e.getName())));
        Assert.assertFalse(UsageStatistics.getEntries().anyMatch(
                e -> Constants.STATISTIC_HAS_AUTO_LAYOUT.equals(e.getName())));
    }

    @Test
    public void should_reported_auto_layout_server() {
        UsageStatistics.resetEntries();
        @Layout
        class AutoLayout extends Component implements RouterLayout {
        }
        VaadinServiceInitListener initListener = event -> {
            ApplicationRouteRegistry.getInstance(event.getSource().getContext())
                    .setLayout(AutoLayout.class);
            RouteConfiguration.forApplicationScope()
                    .setAnnotatedRoute(AnnotatedTestView.class);
        };
        MockVaadinServletService service = new MockVaadinServletService();
        runWithClientRoute("client-test", false, service, () -> {
            service.init(new MockInstantiator(initListener));

            Assert.assertTrue(UsageStatistics.getEntries()
                    .anyMatch(e -> Constants.STATISTIC_HAS_AUTO_LAYOUT
                            .equals(e.getName())));
            Assert.assertTrue(UsageStatistics.getEntries().anyMatch(
                    e -> Constants.STATISTIC_HAS_SERVER_ROUTE_WITH_AUTO_LAYOUT
                            .equals(e.getName())));
            Assert.assertFalse(UsageStatistics.getEntries().anyMatch(
                    e -> Constants.STATISTIC_HAS_CLIENT_ROUTE_WITH_AUTO_LAYOUT
                            .equals(e.getName())));
        });
    }

    @Test
    public void should_reported_auto_layout_client() {
        UsageStatistics.resetEntries();
        @Layout
        class AutoLayout extends Component implements RouterLayout {
        }
        VaadinServiceInitListener initListener = event -> {
            ApplicationRouteRegistry.getInstance(event.getSource().getContext())
                    .setLayout(AutoLayout.class);
        };
        MockVaadinServletService service = new MockVaadinServletService();
        runWithClientRoute("test", true, service, () -> {
            service.init(new MockInstantiator(initListener));

            Assert.assertTrue(UsageStatistics.getEntries()
                    .anyMatch(e -> Constants.STATISTIC_HAS_AUTO_LAYOUT
                            .equals(e.getName())));
            Assert.assertFalse(UsageStatistics.getEntries().anyMatch(
                    e -> Constants.STATISTIC_HAS_SERVER_ROUTE_WITH_AUTO_LAYOUT
                            .equals(e.getName())));
            Assert.assertTrue(UsageStatistics.getEntries().anyMatch(
                    e -> Constants.STATISTIC_HAS_CLIENT_ROUTE_WITH_AUTO_LAYOUT
                            .equals(e.getName())));
        });
    }

    @Test
    public void should_reported_auto_layout_routes_not_used() {
        UsageStatistics.resetEntries();
        @Route(value = "not-in-auto-layout")
        @Tag("div")
        class LayoutTestView extends Component {
        }
        @Layout("layout")
        class AutoLayout extends Component implements RouterLayout {
        }
        VaadinServiceInitListener initListener = event -> {
            ApplicationRouteRegistry.getInstance(event.getSource().getContext())
                    .setLayout(AutoLayout.class);
            RouteConfiguration.forApplicationScope()
                    .setAnnotatedRoute(OptOutAutoLayoutTestView.class);
            RouteConfiguration.forApplicationScope()
                    .setAnnotatedRoute(LayoutTestView.class);
        };
        MockVaadinServletService service = new MockVaadinServletService();
        runWithClientRoute("test", false, service, () -> {
            service.init(new MockInstantiator(initListener));

            Assert.assertTrue(UsageStatistics.getEntries()
                    .anyMatch(e -> Constants.STATISTIC_HAS_AUTO_LAYOUT
                            .equals(e.getName())));
            Assert.assertFalse(UsageStatistics.getEntries().anyMatch(
                    e -> Constants.STATISTIC_HAS_SERVER_ROUTE_WITH_AUTO_LAYOUT
                            .equals(e.getName())));
            Assert.assertFalse(UsageStatistics.getEntries().anyMatch(
                    e -> Constants.STATISTIC_HAS_CLIENT_ROUTE_WITH_AUTO_LAYOUT
                            .equals(e.getName())));
        });
    }

    private void runWithClientRoute(String route, boolean flowLayout,
            MockVaadinServletService service, Runnable runnable) {
        try (MockedStatic<MenuRegistry> menuRegistry = Mockito
                .mockStatic(MenuRegistry.class)) {
            menuRegistry
                    .when(() -> MenuRegistry.collectClientMenuItems(false,
                            service.getDeploymentConfiguration()))
                    .thenReturn(createClientRoutesMap(route, flowLayout));
            runnable.run();
        }
    }

    private static Map<String, AvailableViewInfo> createClientRoutesMap(
            String route, boolean flowLayout) {
        Map<String, AvailableViewInfo> clientViews = new HashMap<>();
        clientViews.put(route, new AvailableViewInfo(route, null, false, route,
                false, false, null, null, null, flowLayout, null));
        return clientViews;
    }

    @Test
    public void testFireSessionDestroy() {
        VaadinService service = createService();

        TestSessionDestroyListener listener = new TestSessionDestroyListener();

        service.addSessionDestroyListener(listener);

        MockVaadinSession vaadinSession = new MockVaadinSession(service);
        service.fireSessionDestroy(vaadinSession);
        Assert.assertEquals(
                "'fireSessionDestroy' method doesn't call 'close' for the session",
                1, vaadinSession.getCloseCount());

        vaadinSession.valueUnbound(Mockito.mock(HttpSessionBindingEvent.class));

        Assert.assertEquals(
                "'fireSessionDestroy' method may not call 'close' "
                        + "method for closing session",
                1, vaadinSession.getCloseCount());

        Assert.assertEquals("SessionDestroyListeners not called exactly once",
                1, listener.callCount);
    }

    @Test
    public void testSessionDestroyListenerCalled_whenAnotherListenerThrows() {
        VaadinService service = createService();

        ThrowingSessionDestroyListener throwingListener = new ThrowingSessionDestroyListener();
        TestSessionDestroyListener listener = new TestSessionDestroyListener();

        service.addSessionDestroyListener(throwingListener);
        service.addSessionDestroyListener(listener);

        final AtomicInteger errorCount = new AtomicInteger();
        MockVaadinSession vaadinSession = new MockVaadinSession(service);
        vaadinSession.lock();
        vaadinSession.setErrorHandler(e -> errorCount.getAndIncrement());
        vaadinSession.unlock();
        service.fireSessionDestroy(vaadinSession);

        Assert.assertEquals("ErrorHandler not called exactly once", 1,
                errorCount.get());

        Assert.assertEquals("SessionDestroyListener not called exactly once", 1,
                listener.callCount);
    }

    @Test
    public void testSessionDestroyListenerCalled_andOtherUiDetachCalled_whenUiClosingThrows() {
        VaadinService service = createService();

        TestSessionDestroyListener listener = new TestSessionDestroyListener();

        service.addSessionDestroyListener(listener);

        final AtomicBoolean secondUiDetached = new AtomicBoolean();
        List<UI> uis = new ArrayList<>();
        MockVaadinSession vaadinSession = new MockVaadinSession(service) {
            @Override
            public Collection<UI> getUIs() {
                return uis;
            }
        };
        vaadinSession.lock();
        UI throwingUI = new UI() {
            @Override
            protected void onDetach(DetachEvent detachEvent) {
                throw new RuntimeException();
            }

            @Override
            public VaadinSession getSession() {
                return vaadinSession;
            }
        };

        throwingUI.getInternals().setSession(vaadinSession);
        uis.add(throwingUI);

        UI detachingUI = new UI() {
            @Override
            protected void onDetach(DetachEvent detachEvent) {
                secondUiDetached.set(true);
            }

            @Override
            public VaadinSession getSession() {
                return vaadinSession;
            }
        };
        detachingUI.getInternals().setSession(vaadinSession);
        uis.add(detachingUI);

        vaadinSession.unlock();

        service.fireSessionDestroy(vaadinSession);

        Assert.assertTrue("Second UI detach not called properly",
                secondUiDetached.get());

        Assert.assertEquals("SessionDestroyListener not called exactly once", 1,
                listener.callCount);
    }

    @Test
    public void testServiceDestroyListenerCalled_whenAnotherListenerThrows() {
        VaadinService service = createService();

        ThrowingServiceDestroyListener throwingListener = new ThrowingServiceDestroyListener();
        TestServiceDestroyListener listener = new TestServiceDestroyListener();

        service.addServiceDestroyListener(throwingListener);
        service.addServiceDestroyListener(listener);

        assertThrows(RuntimeException.class, service::destroy);

        Assert.assertEquals("ServiceDestroyListener not called exactly once", 1,
                listener.callCount);
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
        Lookup lookup = Mockito.mock(Lookup.class);
        servletConfig.getServletContext().setAttribute(Lookup.class.getName(),
                lookup);
        StaticFileHandlerFactory factory = Mockito
                .mock(StaticFileHandlerFactory.class);
        Mockito.when(lookup.lookup(StaticFileHandlerFactory.class))
                .thenReturn(factory);
        VaadinServlet servlet = new VaadinServlet() {
            @Override
            protected DeploymentConfiguration createDeploymentConfiguration() {
                return new MockDeploymentConfiguration();
            }
        };
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
    public void testServiceInitListener_accessApplicationRouteRegistry_registryAvailable() {

        VaadinServiceInitListener initListener = event -> {
            Assert.assertNotNull("service init should have set thread local",
                    VaadinService.getCurrent());

            Router router = event.getSource().getRouter();
            Assert.assertNotNull("Router should be initialized", router);

            Assert.assertNotNull("registry should be initialized",
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
        Assert.assertEquals("test", availableRoutes.get(0).getTemplate());
    }

    @Test
    public void dependencyFilterOrder_bundeFiltersAfterApplicationFilters() {
        DependencyFilter applicationFilter = (dependencies,
                service) -> dependencies;

        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();

        // Service that pretends to have a proper bundle
        MockVaadinServletService service = new MockVaadinServletService(
                configuration);

        service.init(new MockInstantiator(evt -> {
            evt.addDependencyFilter(applicationFilter);
        }));

        List<DependencyFilter> filters = new ArrayList<>();
        service.getDependencyFilters().forEach(filters::add);

        Assert.assertEquals(1, filters.size());

        Assert.assertSame(applicationFilter, filters.get(0));
    }

    @Test
    public void loadInstantiators_instantiatorIsLoadedUsingFactoryFromLookup()
            throws ServiceException {
        VaadinService service = createService();

        Lookup lookup = Mockito.mock(Lookup.class);

        service.getContext().setAttribute(Lookup.class, lookup);

        InstantiatorFactory factory = createInstantiatorFactory();

        Mockito.when(lookup.lookupAll(InstantiatorFactory.class))
                .thenReturn(Collections.singletonList(factory));

        Optional<Instantiator> loadedInstantiator = service.loadInstantiators();

        Instantiator instantiator = factory.createInstantitor(null);

        Assert.assertSame(instantiator, loadedInstantiator.get());
    }

    @Test(expected = ServiceException.class)
    public void loadInstantiators_twoFactoriesInLookup_throws()
            throws ServiceException {
        VaadinService service = createService();

        Lookup lookup = Mockito.mock(Lookup.class);

        service.getContext().setAttribute(Lookup.class, lookup);

        InstantiatorFactory factory1 = createInstantiatorFactory();
        InstantiatorFactory factory2 = createInstantiatorFactory();

        Mockito.when(lookup.lookupAll(InstantiatorFactory.class))
                .thenReturn(Arrays.asList(factory1, factory2));

        service.loadInstantiators();
    }

    @Test
    public void createRequestHandlers_pwaHandlerIsInList_webComponentHandlersAreInList()
            throws ServiceException {
        TestVaadinService service = Mockito.mock(TestVaadinService.class);
        I18NProvider i18NProvider = Mockito.mock(DefaultI18NProvider.class);
        Instantiator instantiator = Mockito.mock(Instantiator.class);
        Mockito.when(instantiator.getI18NProvider()).thenReturn(i18NProvider);
        Mockito.when(service.getInstantiator()).thenReturn(instantiator);
        Mockito.doCallRealMethod().when(service).createRequestHandlers();

        List<RequestHandler> handlers = service.createRequestHandlers();
        Set<?> set = handlers.stream().map(Object::getClass)
                .collect(Collectors.toSet());
        Assert.assertTrue(set.contains(PwaHandler.class));
        Assert.assertTrue(set.contains(WebComponentProvider.class));
        Assert.assertTrue(set.contains(WebComponentBootstrapHandler.class));
    }

    @Test
    public void fireSessionDestroy_sessionStateIsSetToClosed() {
        VaadinService service = createService();

        AtomicReference<VaadinSessionState> stateRef = new AtomicReference<>();
        MockVaadinSession vaadinSession = new MockVaadinSession(service) {
            @Override
            protected void setState(VaadinSessionState state) {
                stateRef.set(state);
            }
        };

        service.fireSessionDestroy(vaadinSession);

        Assert.assertEquals(VaadinSessionState.CLOSED, stateRef.get());
    }

    @Test
    public void removeFromHttpSession_setExplicitSessionCloseAttribute()
            throws ServiceException {
        WrappedSession httpSession = Mockito.mock(WrappedSession.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);
        VaadinService service = new MockVaadinServletService() {

            @Override
            protected VaadinSession readFromHttpSession(
                    WrappedSession wrappedSession) {
                return session;
            }
        };

        service.init();

        service.removeFromHttpSession(httpSession);

        Assert.assertTrue(session.sessionClosedExplicitly);
    }

    @Test
    public void reinitializeSession_setVaadinSessionAttriuteWithLock() {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);

        VaadinSession vaadinSession = Mockito.mock(VaadinSession.class);
        VaadinSession newVaadinSession = Mockito.mock(VaadinSession.class);

        WrappedSession session = mockSession(request, vaadinSession, "foo");

        Mockito.doAnswer(invocation -> {
            mockSession(request, newVaadinSession, "bar");
            return null;
        }).when(session).invalidate();

        VaadinService.reinitializeSession(request);

        Mockito.verify(vaadinSession, Mockito.times(2)).lock();
        Mockito.verify(vaadinSession).setAttribute(
                VaadinService.PRESERVE_UNBOUND_SESSION_ATTRIBUTE, Boolean.TRUE);
        Mockito.verify(vaadinSession).setAttribute(
                VaadinService.PRESERVE_UNBOUND_SESSION_ATTRIBUTE, null);
        Mockito.verify(vaadinSession, Mockito.times(2)).unlock();
    }

    @Test
    public void getExecutor_getsDefaultVaadinExecutor()
            throws InterruptedException {
        VaadinService service = createService();
        Executor executor = service.getExecutor();
        AtomicReference<String> threadName = new AtomicReference<>();
        Assert.assertNotNull(executor);
        CountDownLatch latch = new CountDownLatch(1);
        executor.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });
        latch.await();
        Assert.assertNotNull("Task has not been not executed",
                threadName.get());
        Assert.assertTrue("Task was not executed by Vaadin default executor",
                threadName.get().startsWith("VaadinTaskExecutor-"));
    }

    @Test
    public void serviceDestroy_defaultExecutor_executorStopped() {
        VaadinService service = createService();
        Executor executor = service.getExecutor();
        Assert.assertTrue(
                "Expected the default executor to be an ExecutorService instance",
                executor instanceof ExecutorService);
        Assert.assertFalse("Expected executor service to be started",
                ((ExecutorService) executor).isShutdown());
        service.destroy();
        Assert.assertTrue("Expected executor service to be stopped",
                ((ExecutorService) executor).isShutdown());
    }

    @Test
    public void getExecutor_customExecutorProvided_getsCustomExecutor()
            throws InterruptedException {
        AtomicBoolean taskSubmitted = new AtomicBoolean(false);
        Executor executor = command -> {
            taskSubmitted.set(true);
            command.run();
        };
        VaadinServiceInitListener initListener = event -> {
            event.setExecutor(executor);
        };
        CountDownLatch latch = new CountDownLatch(1);
        MockInstantiator instantiator = new MockInstantiator(initListener);
        MockVaadinServletService service = new MockVaadinServletService(false);
        service.init(instantiator);
        Assert.assertSame(
                "Expected VaadinService to return the custom executor",
                executor, service.getExecutor());
        service.getExecutor().execute(latch::countDown);
        latch.await();
        Assert.assertTrue(
                "Task should have been submitted to the custom executor",
                taskSubmitted.get());
    }

    @Test
    public void serviceDestroy_customExecutorProvided_executorNotStopped() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        VaadinServiceInitListener initListener = event -> {
            event.setExecutor(executor);
        };
        MockInstantiator instantiator = new MockInstantiator(initListener);
        MockVaadinServletService service = new MockVaadinServletService(false);
        service.init(instantiator);

        Assert.assertSame(
                "Expected VaadinService to return the custom executor",
                executor, service.getExecutor());

        service.destroy();
        Assert.assertFalse("Expected custom executor not to be stopped",
                executor.isShutdown());

    }

    @Test
    public void getExecutor_nullExecutorProvided_resetsToDefaultVaadinExecutor() {
        Executor executor = command -> {
        };
        VaadinServiceInitListener setExecutorInitListener = event -> {
            event.setExecutor(executor);
        };
        VaadinServiceInitListener resetExecutorInitListener = event -> {
            event.setExecutor(null);
        };
        MockInstantiator instantiator = new MockInstantiator(
                setExecutorInitListener, resetExecutorInitListener);
        MockVaadinServletService service = new MockVaadinServletService(false);
        service.init(instantiator);
        Assert.assertNotSame("Custom executor should not be used", executor,
                service.getExecutor());
    }

    @Test
    public void init_nullExecutor_throws() {
        RuntimeException error = assertThrows(RuntimeException.class, () -> {
            // init method is called by the mock service constructor
            new MockVaadinServletService() {
                @Override
                protected Executor createDefaultExecutor() {
                    return null;
                }
            };
        });
        if (error.getCause() instanceof ServiceException serviceException) {
            Assert.assertTrue(
                    "Expected VaadinService initialization to fail with null executor",
                    serviceException.getMessage()
                            .contains("Unable to create the default Executor"));
        } else {
            Assert.fail("Expected ServiceException to be thrown");
        }
    }

    private WrappedSession mockSession(VaadinRequest request,
            VaadinSession vaadinSession, String attributeName) {
        WrappedSession session = Mockito.mock(WrappedSession.class);
        Mockito.when(request.getWrappedSession()).thenReturn(session);

        Mockito.when(session.getAttributeNames())
                .thenReturn(Collections.singleton(attributeName));

        Mockito.when(session.getAttribute(attributeName))
                .thenReturn(vaadinSession);

        VaadinService service = Mockito.mock(VaadinService.class);

        Mockito.when(vaadinSession.getService()).thenReturn(service);
        return session;
    }

    private InstantiatorFactory createInstantiatorFactory() {
        InstantiatorFactory factory = Mockito.mock(InstantiatorFactory.class);

        Instantiator instantiator = Mockito.mock(Instantiator.class);
        Mockito.when((factory.createInstantitor(any())))
                .thenReturn(instantiator);

        return factory;
    }

    private static VaadinService createService() {
        return new MockVaadinServletService();
    }
}
