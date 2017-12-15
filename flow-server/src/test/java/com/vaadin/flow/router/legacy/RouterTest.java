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
package com.vaadin.flow.router.legacy;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationHandler;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.RouterInterface;
import com.vaadin.flow.router.legacy.ViewRendererTest.ErrorView;
import com.vaadin.flow.router.legacy.ViewRendererTest.TestView;
import com.vaadin.flow.server.MockServletConfig;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.util.CurrentInstance;
import com.vaadin.ui.History.HistoryStateChangeEvent;
import com.vaadin.ui.UI;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class RouterTest {

    public static class RouterTestUI extends UI {
        final Router router;

        public RouterTestUI() {
            this(new Router());
        }

        public RouterTestUI(Router router) {
            this.router = router;
        }

        @Override
        public Optional<RouterInterface> getRouterInterface() {
            return Optional.of(router);
        }
    }

    private final class TestResolver implements Resolver {
        private final AtomicReference<Location> resolvedLocation = new AtomicReference<>();
        private final AtomicReference<NavigationEvent> handledEvent = new AtomicReference<>();

        @Override
        public Optional<NavigationHandler> resolve(
                NavigationEvent eventToResolve) {
            Assert.assertNull(resolvedLocation.get());
            resolvedLocation.set(eventToResolve.getLocation());
            return Optional.of(event -> {
                Assert.assertNull(handledEvent.get());
                handledEvent.set(event);
                return HttpServletResponse.SC_OK;
            });
        }
    }

    @After
    public void tearDown() {
        CurrentInstance.clearAll();
    }

    @Test
    public void testResolve() {
        UI ui = new RouterTestUI();

        Router router = new Router();
        TestResolver resolver = new TestResolver();
        router.reconfigure(c -> c.setResolver(resolver));

        Assert.assertNull(resolver.resolvedLocation.get());
        Assert.assertNull(resolver.handledEvent.get());

        Location testLocation = new Location("");

        router.navigate(ui, testLocation, NavigationTrigger.PROGRAMMATIC);

        Assert.assertSame(testLocation, resolver.resolvedLocation.get());
        Assert.assertSame(testLocation,
                resolver.handledEvent.get().getLocation());
        Assert.assertSame(ui, resolver.handledEvent.get().getUI());
    }

    @Test
    public void testChangeLocation() {
        UI ui = new RouterTestUI();

        Router router = new Router();
        TestResolver resolver = new TestResolver();
        router.reconfigure(c -> c.setResolver(resolver));

        VaadinRequest request = requestWithPathInfo(null);

        router.initializeUI(ui, request);

        Assert.assertEquals(Arrays.asList(""),
                resolver.resolvedLocation.get().getSegments());

        resolver.resolvedLocation.set(null);
        resolver.handledEvent.set(null);

        ui.getPage().getHistory().getHistoryStateChangeHandler()
                .onHistoryStateChange(new HistoryStateChangeEvent(
                        ui.getPage().getHistory(), null, new Location("foo"),
                        NavigationTrigger.HISTORY));

        Assert.assertEquals(Arrays.asList("foo"),
                resolver.resolvedLocation.get().getSegments());
    }

    private static VaadinRequest requestWithPathInfo(String pathInfo) {
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn(pathInfo);
        return request;
    }

    @Test
    public void testResolveError() throws ServletException {
        UI ui = new RouterTestUI();
        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        VaadinResponse response = Mockito.mock(VaadinResponse.class);

        ServletConfig servletConfig = new MockServletConfig();
        VaadinServlet servlet = new VaadinServlet();
        servlet.init(servletConfig);
        VaadinService service = servlet.getService();
        service.setCurrentInstances(request, response);

        Router router = new Router();
        router.reconfigure(c -> c.setResolver(event -> Optional.empty()));

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assert.assertTrue(ui.getElement().getTextRecursively().contains("404"));
        // 404 code should be sent ONLY on initial request
        Mockito.verifyZeroInteractions(response);

        // to verify that the setup has been correct and the mocks work,
        // test the case where 404 should be sent
        router.initializeUI(ui, request);

        ArgumentCaptor<Integer> statusCodeCaptor = ArgumentCaptor
                .forClass(Integer.class);
        Mockito.verify(response).setStatus(statusCodeCaptor.capture());
        Assert.assertEquals(Integer.valueOf(HttpServletResponse.SC_NOT_FOUND),
                statusCodeCaptor.getValue());
    }

    @Test
    public void testResolverError_noCurrentResponse() {
        UI ui = new RouterTestUI();

        Router router = new Router();
        router.reconfigure(c -> c.setResolver(event -> Optional.empty()));

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assert.assertTrue(ui.getElement().getTextRecursively().contains("404"));
    }

    @Test
    public void testReconfigureThreadSafety() throws InterruptedException {
        Router router = new Router();
        Resolver newResolver = e -> null;

        CountDownLatch configUpdated = new CountDownLatch(1);
        CountDownLatch configVerified = new CountDownLatch(1);

        Thread updaterThread = new Thread() {
            @Override
            public void run() {
                router.reconfigure(config -> {
                    config.setResolver(newResolver);

                    // Signal that config has been updated
                    configUpdated.countDown();

                    // Wait until main thread has verified that the
                    // configuration is not yet in effect
                    try {
                        configVerified.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        };
        updaterThread.start();

        // Wait until updater thread has updated the config
        configUpdated.await();

        Assert.assertNotSame("Update should not yet be visible", newResolver,
                router.getConfiguration().getResolver());

        // Allow the update thread to exit the configure method
        configVerified.countDown();

        // Wait for updater thread to finish
        updaterThread.join();

        Assert.assertSame("Update should now be visible", newResolver,
                router.getConfiguration().getResolver());
    }

    @Test(expected = IllegalStateException.class)
    public void testConfigurationImmutable() {
        Router router = new Router();

        ImmutableRouterConfiguration configuration = router.getConfiguration();

        ((RouterConfiguration) configuration).setResolver(e -> null);
    }

    @Test
    public void testLeakedConfigurationImmutable() {
        Router router = new Router();

        AtomicReference<RouterConfiguration> configurationLeak = new AtomicReference<>();

        router.reconfigure(configurationLeak::set);

        Resolver newResolver = e -> null;
        configurationLeak.get().setResolver(newResolver);

        Assert.assertNotSame(newResolver,
                router.getConfiguration().getResolver());
    }

    @Test
    public void testResolverBeforeSetRoute() {
        Router router = new Router();

        AtomicReference<String> usedHandler = new AtomicReference<>();

        router.reconfigure(configuration -> {
            configuration
                    .setResolver(resolveEvent -> Optional.of(handlerEvent -> {
                        usedHandler.set("resolver");
                        return HttpServletResponse.SC_OK;
                    }));

            configuration.setRoute("*", e -> {
                usedHandler.set("route");
                return HttpServletResponse.SC_OK;
            });
        });

        router.navigate(new RouterTestUI(), new Location(""),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("resolver", usedHandler.get());
    }

    @Test
    public void testSetRouteIfNoResolverHandler() {
        Router router = new Router();

        AtomicReference<String> usedHandler = new AtomicReference<>();

        router.reconfigure(configuration -> {
            configuration.setResolver(resolveEvent -> Optional.empty());

            configuration.setRoute("*", e -> {
                usedHandler.set("route");
                return HttpServletResponse.SC_OK;
            });
        });

        router.navigate(new RouterTestUI(), new Location(""),
                NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals("route", usedHandler.get());
    }

    @Test
    public void testNavigateToEmptyLocation_triggersDefaultErrorView() {
        UI ui = new RouterTestUI();

        Router router = new Router();
        router.reconfigure(c -> {
        });

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(new DefaultErrorView().getText(),
                ui.getElement().getTextRecursively());
    }

    @Test
    public void testNavigateToEmptyLocation_triggersErrorView() {
        UI ui = new RouterTestUI();

        Router router = new Router();
        router.reconfigure(c -> {
            c.setErrorView(ErrorView.class);
        });

        router.navigate(ui, new Location(""), NavigationTrigger.PROGRAMMATIC);

        Assert.assertEquals(new ErrorView().getText(),
                ui.getElement().getTextRecursively());
    }

    @Test
    public void testNavigateWithToggledSlash() {
        UI ui = new RouterTestUI();

        Router router = new Router();
        router.reconfigure(c -> {
            c.setRoute("foo/{name}", TestView.class);
            c.setRoute("bar/*", TestView.class);
        });

        router.navigate(ui, new Location("foo/bar/"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("foo/bar",
                ui.getInternals().getActiveViewLocation().getPath());

        router.navigate(ui, new Location("bar"),
                NavigationTrigger.PROGRAMMATIC);
        Assert.assertEquals("bar/",
                ui.getInternals().getActiveViewLocation().getPath());
    }

    @Test
    public void testStatusCodeUpdates() {
        RouterTestUI ui = new RouterTestUI();

        Router router = (Router) ui.getRouterInterface().get();

        router.reconfigure(c -> {
            c.setRoute("*", e -> 123);
        });

        VaadinResponse response = Mockito.mock(VaadinResponse.class);

        try {
            CurrentInstance.set(VaadinResponse.class, response);

            VaadinRequest request = requestWithPathInfo(null);

            router.initializeUI(ui, request);

            // Response status should be set when initializing
            Mockito.verify(response).setStatus(123);

            router.navigate(ui, new Location("foo"),
                    NavigationTrigger.PROGRAMMATIC);

            // Non-init navigation shouldn't set any status code
            Mockito.verifyNoMoreInteractions(response);

        } finally {
            CurrentInstance.clearAll();
        }
    }
}
