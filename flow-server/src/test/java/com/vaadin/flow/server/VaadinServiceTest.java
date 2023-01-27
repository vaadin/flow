/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import net.jcip.annotations.NotThreadSafe;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpSessionBindingEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.communication.PwaHandler;
import com.vaadin.flow.server.communication.StreamRequestHandler;
import com.vaadin.flow.server.communication.WebComponentBootstrapHandler;
import com.vaadin.flow.server.communication.WebComponentProvider;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

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

    private static abstract class TestVaadinService extends VaadinService {

        @Override
        protected List<RequestHandler> createRequestHandlers()
                throws ServiceException {
            return super.createRequestHandlers();
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
    }

    @Test
    public void should_reported_routing_hybrid() {
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
    }

    @Test
    public void testFireSessionDestroy()
            throws ServletException, ServiceException {
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
            protected DeploymentConfiguration createDeploymentConfiguration()
                    throws ServletException {
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
    public void currentInstancesAfterPendingAccessTasks()
            throws ServiceException {
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
        Assert.assertEquals(availableRoutes.get(0).getTemplate(), "test");
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

        InstantiatorFactory factory = createInstantiatorFactory(lookup);

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

        InstantiatorFactory factory1 = createInstantiatorFactory(lookup);
        InstantiatorFactory factory2 = createInstantiatorFactory(lookup);

        Mockito.when(lookup.lookupAll(InstantiatorFactory.class))
                .thenReturn(Arrays.asList(factory1, factory2));

        service.loadInstantiators();
    }

    @Test
    public void createRequestHandlers_pwaHandlerIsInList_webComponentHandlersAreInList()
            throws ServiceException {
        TestVaadinService service = Mockito.mock(TestVaadinService.class);
        Mockito.doCallRealMethod().when(service).createRequestHandlers();

        List<RequestHandler> handlers = service.createRequestHandlers();
        Set<?> set = handlers.stream().map(Object::getClass)
                .collect(Collectors.toSet());
        Assert.assertTrue(set.contains(PwaHandler.class));
        Assert.assertTrue(set.contains(WebComponentProvider.class));
        Assert.assertTrue(set.contains(WebComponentBootstrapHandler.class));
    }

    @Test
    public void fireSessionDestroy_sessionStateIsSetToClosed()
            throws ServletException, ServiceException {
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

    private InstantiatorFactory createInstantiatorFactory(Lookup lookup) {
        InstantiatorFactory factory = Mockito.mock(InstantiatorFactory.class);

        Instantiator instantiator = Mockito.mock(Instantiator.class);
        Mockito.when((factory.createInstantitor(Mockito.any())))
                .thenReturn(instantiator);

        return factory;
    }

    private static VaadinService createService() throws ServiceException {
        VaadinService service = new MockVaadinServletService();
        return service;
    }
}
