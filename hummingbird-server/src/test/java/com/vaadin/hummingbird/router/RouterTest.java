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
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.History.LocationChangeEvent;

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
        router.setResolver(resolver);

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
        router.setResolver(resolver);

        VaadinRequest request = Mockito.mock(VaadinRequest.class);
        Mockito.when(request.getPathInfo()).thenReturn(null);

        router.initializeUI(ui, request);

        Assert.assertEquals(Arrays.asList(""),
                resolver.resolvedLocation.get().getSegments());

        resolver.resolvedLocation.set(null);
        resolver.handledEvent.set(null);

        ui.getPage().getHistory().getLocationChangeHandler().onLocationChange(
                new LocationChangeEvent(ui.getPage().getHistory(), null,
                        "foo"));

        Assert.assertEquals(Arrays.asList("foo"),
                resolver.resolvedLocation.get().getSegments());
    }

    @Test
    public void testResolveError() {
        RouterUI ui = new RouterUI();

        Router router = new Router();
        router.setResolver(event -> null);

        router.navigate(ui, new Location(""));

        Assert.assertTrue(ui.getElement().getTextContent().contains("404"));
    }
}
