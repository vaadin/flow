/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSessionBindingEvent;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.easymock.EasyMock;
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
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.communication.StreamRequestHandler;
import com.vaadin.flow.server.startup.BundleDependencyFilter;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
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

    @Test
    public void loadInstantiators_instantiatorIsLoadedUsingFactoryFromLookup()
            throws ServiceException {
        VaadinService service = createService();
        Lookup lookup = service.getContext().getAttribute(Lookup.class);

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
        Lookup lookup = service.getContext().getAttribute(Lookup.class);

        InstantiatorFactory factory1 = createInstantiatorFactory(lookup);
        InstantiatorFactory factory2 = createInstantiatorFactory(lookup);

        Mockito.when(lookup.lookupAll(InstantiatorFactory.class))
                .thenReturn(Arrays.asList(factory1, factory2));

        service.loadInstantiators();
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

        Mockito.when(instantiator.init(Mockito.any())).thenReturn(true);
        return factory;
    }

    private static VaadinService createService() throws ServiceException {
        VaadinService service = new MockVaadinServletService();
        service.init();
        return service;
    }
}
