/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.router;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.History.HistoryStateChangeEvent;

public class RouterTest {

    private final class TestResolver implements Resolver {
        private final AtomicReference<Location> resolvedLocation = new AtomicReference<>();
        private final AtomicReference<NavigationEvent> handledEvent = new AtomicReference<>();

        @Override
        public NavigationHandler resolve(NavigationEvent eventToResolve) {
            Assert.assertNull(resolvedLocation.get());
            resolvedLocation.set(eventToResolve.getLocation());
            return new NavigationHandler() {
                @Override
                public void handle(NavigationEvent eventToHandle) {
                    Assert.assertNull(handledEvent.get());
                    handledEvent.set(eventToHandle);
                }
            };
        }
    }

    @Test
    public void testResolve() {
        RouterUI ui = new RouterUI();

        Router router = new Router();
        TestResolver resolver = new TestResolver();
        router.reconfigure(c -> c.setResolver(resolver));

        Assert.assertNull(resolver.resolvedLocation.get());
        Assert.assertNull(resolver.handledEvent.get());

        Location testLocation = new Location("");

        router.navigate(ui, testLocation);

        Assert.assertSame(testLocation, resolver.resolvedLocation.get());
        Assert.assertSame(testLocation,
                resolver.handledEvent.get().getLocation());
        Assert.assertSame(ui, resolver.handledEvent.get().getUI());
    }

    @Test
    public void testChangeLocation() {
        RouterUI ui = new RouterUI();

        Router router = new Router();
        TestResolver resolver = new TestResolver();
        router.reconfigure(c -> c.setResolver(resolver));

        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn(null);

        router.initializeUI(ui, request);

        Assert.assertEquals(Arrays.asList(""),
                resolver.resolvedLocation.get().getSegments());

        resolver.resolvedLocation.set(null);
        resolver.handledEvent.set(null);

        ui.getPage().getHistory().getHistoryStateChangeHandler()
                .onHistoryStateChange(new HistoryStateChangeEvent(
                        ui.getPage().getHistory(), null, "foo"));

        Assert.assertEquals(Arrays.asList("foo"),
                resolver.resolvedLocation.get().getSegments());
    }

    @Test
    public void testResolveError() {
        RouterUI ui = new RouterUI();

        Router router = new Router();
        router.reconfigure(c -> c.setResolver(event -> null));

        router.navigate(ui, new Location(""));

        Assert.assertTrue(ui.getElement().getTextContent().contains("404"));
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

        RouterConfiguration configuration = router.getConfiguration();

        ((ModifiableRouterConfiguration) configuration).setResolver(e -> null);
    }

    @Test
    public void testLeakedConfigurationImmutable() {
        Router router = new Router();

        AtomicReference<ModifiableRouterConfiguration> configurationLeak = new AtomicReference<>();

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
            configuration.setResolver(resolveEvent -> handlerEvent -> {
                usedHandler.set("resolver");
            });

            configuration.setRoute("*", e -> {
                usedHandler.set("route");
            });
        });

        router.navigate(new RouterUI(), new Location(""));

        Assert.assertEquals("resolver", usedHandler.get());
    }

    @Test
    public void testSetRouteIfNoResolverHandler() {
        Router router = new Router();

        AtomicReference<String> usedHandler = new AtomicReference<>();

        router.reconfigure(configuration -> {
            configuration.setResolver(resolveEvent -> null);

            configuration.setRoute("*", e -> {
                usedHandler.set("route");
            });
        });

        router.navigate(new RouterUI(), new Location(""));

        Assert.assertEquals("route", usedHandler.get());
    }
}
